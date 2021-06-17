(ns konfo-backend.search.query
  (:require
    [konfo-backend.koodisto.koodisto :refer [list-koodi-urit]]
    [konfo-backend.index.haku :refer [list-yhteishaut]]
    [konfo-backend.tools :refer [not-blank?]]
    [konfo-backend.search.tools :refer :all]
    [clojure.string :refer [lower-case]]
    [konfo-backend.elastic-tools :refer [->size ->from]]
    [konfo-backend.tools :refer [current-time-as-kouta-format half-year-past-as-kouta-format ->koodi-with-version-wildcard ->lower-case-vec]]))

(defn- ->terms-query
  [key coll]
  (if (= 1 (count coll))
    {:term  {(keyword key) (lower-case (first coll))}}
    {:terms {(keyword key) (->lower-case-vec coll)}}))

(defn- some-hakuaika-kaynnissa
  []
  { :nested {:path "hits.hakutiedot.hakuajat"
             :query {:bool {:filter [{:range {:hits.hakutiedot.hakuajat.alkaa {:lte (current-time-as-kouta-format)}}}
                                     {:bool  {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}},
                                                       {:range {:hits.hakutiedot.hakuajat.paattyy {:gt (current-time-as-kouta-format)}}}]}}]}}}})

(defn- some-past-hakuaika-still-viable
  []
  { :nested {:path "hits.hakutiedot.hakuajat"
             :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}},
                                     {:range {:hits.hakutiedot.hakuajat.paattyy {:gt (half-year-past-as-kouta-format)}}}]}}}})

; NOTE Rajattavat hakutiedot täytyy yhdistää hakuaika-käynnissä rajaukseen poissulkevasti ( AND ) aikarajan perusteella (käynnissä vs 6kk vanhat)
(defn- hakutieto-query
  ([inner-query]
   {:nested {:path "hits.hakutiedot"
             :query { :bool { :filter inner-query }}}})
  ([haku-kaynnissa inner-query]
   {:nested {:path "hits.hakutiedot"
             :query { :bool { :filter [(if haku-kaynnissa
                                         (some-hakuaika-kaynnissa)
                                         (some-past-hakuaika-still-viable))
                                       inner-query] }}}}))

(defn- haku-kaynnissa-not-already-included?
  [constraints]
  (every? false? [(hakutapa? constraints)
                  (pohjakoulutusvaatimus? constraints)
                  (valintatapa? constraints)
                  (yhteishaku? constraints)]))

(defn- filters
  [constraints]
  (let [haku-kaynnissa (haku-kaynnissa? constraints)
        ; NOTE haku-käynnissä rajainta halutaan käyttää vain jos jotain muuta hakutietorajainta ei ole käytössä (koska se sisältyy niihin jos ne on käytössä)
        use-haku-kaynnissa (and haku-kaynnissa
                                (haku-kaynnissa-not-already-included? constraints))]

    (cond-> []
            (koulutustyyppi? constraints)        (conj (->terms-query :hits.koulutustyypit.keyword           (:koulutustyyppi constraints)))
            (opetuskieli? constraints)           (conj (->terms-query :hits.opetuskielet.keyword             (:opetuskieli constraints)))
            (sijainti? constraints)              (conj (->terms-query :hits.sijainti.keyword                 (:sijainti constraints)))
            (koulutusala? constraints)           (conj (->terms-query :hits.koulutusalat.keyword             (:koulutusala constraints)))
            (opetustapa? constraints)            (conj (->terms-query :hits.opetustavat.keyword              (:opetustapa constraints)))

            ; NOTE hakukäynnissä rajainta EI haluta käyttää jos se sisältyy muihin rajaimiin (koska ao. rivit käyttäytyvät OR ehtoina)
            use-haku-kaynnissa                   (conj (hakutieto-query (some-hakuaika-kaynnissa)))
            (hakutapa? constraints)              (conj (hakutieto-query haku-kaynnissa (->terms-query :hits.hakutiedot.hakutapa                 (:hakutapa constraints))))
            (pohjakoulutusvaatimus? constraints) (conj (hakutieto-query haku-kaynnissa (->terms-query :hits.hakutiedot.pohjakoulutusvaatimukset (:pohjakoulutusvaatimus constraints))))
            (valintatapa? constraints)           (conj (hakutieto-query haku-kaynnissa (->terms-query :hits.hakutiedot.valintatavat             (:valintatapa constraints))))
            (yhteishaku? constraints)            (conj (hakutieto-query haku-kaynnissa (->terms-query :hits.hakutiedot.yhteishakuOid            (:yhteishaku constraints)))))))

(defn- generate-keyword-query
  [keyword]
  (->> ["fi" "sv" "en"]
       (map (fn [language] {:match {(->lng-keyword "hits.terms.%s" language) {:query (lower-case keyword) :operator "and" :fuzziness "AUTO:8,12"}}}))))

(defn- bool
  [keyword constraints]
  (cond-> {}
          (not-blank? keyword)       (assoc :should (generate-keyword-query keyword))
          (constraints? constraints) (assoc :filter (filters constraints))))

(defn query
  [keyword constraints]
  {:nested {:path "hits", :query {:bool (bool keyword constraints)}}})

(defn match-all-query
  []
  {:match_all {}})

(defn- ->name-sort
  [order lng]
  {(->lng-keyword "nimi.%s.keyword" lng) {:order order :unmapped_type "string"}})

(defn sorts
  [sort order lng]
  (case sort
    "score" [{:_score {:order order}} (->name-sort "asc" lng)]
    "name" [(->name-sort order lng)]
    [{:_score {:order order}} (->name-sort "asc" lng)]))

(defn- inner-hits-filters
  [tuleva? constraints]
  {:bool {:must {:term {"hits.onkoTuleva" tuleva?}}
          :filter (filters constraints)}})

(defn inner-hits-query
  [oid lng page size order tuleva? constraints]
  (let [size (->size size)
        from (->from page size)]
    {:bool {:must [{:term {:oid oid}}
                   {:nested {:inner_hits {:_source ["hits.koulutusOid", "hits.toteutusOid", "hits.toteutusNimi", "hits.opetuskielet", "hits.oppilaitosOid", "hits.kuva", "hits.nimi", "hits.metadata" "hits.hakutiedot"]
                                          :from from
                                          :size size
                                          :sort {(str "hits.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}
                             :path "hits"
                             :query (inner-hits-filters tuleva? constraints)}}]}}))

(defn inner-hits-query-osat
  [oid lng page size order tuleva?]
  (let [size (->size size)
        from (->from page size)]
    {:nested {:inner_hits {:_source ["hits.koulutusOid", "hits.toteutusOid", "hits.oppilaitosOid", "hits.kuva", "hits.nimi", "hits.metadata"]
                           :from    from
                           :size    size
                           :sort    {(str "hits.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}
              :path       "hits"
              :query      {:bool {:must [{:term {"hits.onkoTuleva" tuleva?}}
                                         {:term {"hits.tarjoajat" oid}}]}}}}))

(defn external-query
  [keyword lng constraints]
  {:nested {:path "hits",
            :inner_hits {},
            :query {:bool {:must   {:match {(->lng-keyword "hits.terms.%s" lng) {:query (lower-case keyword) :operator "and" :fuzziness "AUTO:8,12"}}}
                           :filter (cond-> [{:term {"hits.onkoTuleva" false}}]
                                           (koulutustyyppi? constraints)  (conj (->terms-query :hits.koulutustyypit.keyword (:koulutustyyppi constraints))))}}}})

(defn- ->term-filter
  [field term]
  {(keyword term) {:term {field term}}})

(defn- ->term-filters
  [field terms]
  (reduce merge {} (map #(->term-filter field %) terms)))

(defn- ->filters-aggregation
  [field terms]
  {:filters {:filters (->term-filters field terms)} :aggs {:real_hits {:reverse_nested {}}}})

(defn- ->filters-aggregation-for-subentity
  [field terms]
  {:filters {:filters (->term-filters field terms)}})

(defn- koodisto-filters
  [field koodisto]
  (->filters-aggregation field (list-koodi-urit koodisto)))

(defn- koodisto-filters-for-subentity
  [field koodisto]
  (->filters-aggregation-for-subentity field (list-koodi-urit koodisto)))

(defn- koulutustyyppi-filters
  [field]
  (->filters-aggregation field '["amm" "amm-tutkinnon-osa" "amm-osaamisala" "lk" "amk" "yo" "amk-alempi" "amk-ylempi" "kandi" "kandi-ja-maisteri" "maisteri" "tohtori"]))

(defn- koulutustyyppi-filters-for-subentity
  [field]
  (->filters-aggregation-for-subentity field '["amm" "amm-tutkinnon-osa" "amm-osaamisala"]))

; NOTE Hakutietosuodattimien sisältö riippuu haku-käynnissä valinnasta
(defn- ->hakutieto-term-filter
  [haku-kaynnissa field term]
  {(keyword term) (hakutieto-query haku-kaynnissa {:term {field term}})})

(defn- ->hakutieto-term-filters
  [haku-kaynnissa field terms]
  (reduce merge {} (map #(->hakutieto-term-filter haku-kaynnissa field %) terms)))

(defn- ->hakutieto-filters-aggregation
  [haku-kaynnissa field terms]
  {:filters {:filters (->hakutieto-term-filters haku-kaynnissa field terms) } :aggs {:real_hits {:reverse_nested {}}}})

(defn- hakutieto-koodisto-filters
  [haku-kaynnissa field koodisto]
  (->hakutieto-filters-aggregation haku-kaynnissa field (list-koodi-urit koodisto)))

(defn- hakukaynnissa-filter
  []
  {:filters {:filters {:hakukaynnissa (hakutieto-query (some-hakuaika-kaynnissa))}} :aggs {:real_hits {:reverse_nested {}}}})

(defn- deduct-hakukaynnissa-aggs-from-other-filters
  [constraints]
  {:filters {:filters {:hakukaynnissa { :bool { :filter (cond-> []
                                                                (hakutapa? constraints)              (conj (hakutieto-query true (->terms-query :hits.hakutiedot.hakutapa                 (:hakutapa constraints))))
                                                                (pohjakoulutusvaatimus? constraints) (conj (hakutieto-query true (->terms-query :hits.hakutiedot.pohjakoulutusvaatimukset (:pohjakoulutusvaatimus constraints))))
                                                                (valintatapa? constraints)           (conj (hakutieto-query true (->terms-query :hits.hakutiedot.valintatavat             (:valintatapa constraints))))
                                                                (yhteishaku? constraints)            (conj (hakutieto-query true (->terms-query :hits.hakutiedot.yhteishakuOid            (:yhteishaku constraints)))))}}}}
   :aggs {:real_hits {:reverse_nested {}}}})

(defn- yhteishaku-filter
  [haku-kaynnissa]
  (->hakutieto-filters-aggregation haku-kaynnissa :hits.hakutiedot.yhteishakuOid (list-yhteishaut)))

(defn- generate-default-aggs
  [constraints]
  (let [no-other-hakutieto-filters-used (haku-kaynnissa-not-already-included? constraints)
        haku-kaynnissa (haku-kaynnissa? constraints)]
      {:maakunta              (koodisto-filters :hits.sijainti.keyword                 "maakunta")
       :kunta                 (koodisto-filters :hits.sijainti.keyword                 "kunta")
       :opetuskieli           (koodisto-filters :hits.opetuskielet.keyword             "oppilaitoksenopetuskieli")
       :koulutusala           (koodisto-filters :hits.koulutusalat.keyword             "kansallinenkoulutusluokitus2016koulutusalataso1")
       :koulutusalataso2      (koodisto-filters :hits.koulutusalat.keyword             "kansallinenkoulutusluokitus2016koulutusalataso2")
       :koulutustyyppi        (koulutustyyppi-filters :hits.koulutustyypit.keyword)
       :koulutustyyppitaso2   (koodisto-filters :hits.koulutustyypit.keyword           "koulutustyyppi")
       :opetustapa            (koodisto-filters :hits.opetustavat.keyword              "opetuspaikkakk")

       :hakukaynnissa         (if no-other-hakutieto-filters-used (hakukaynnissa-filter) (deduct-hakukaynnissa-aggs-from-other-filters constraints))
       :hakutapa              (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.hakutapa     "hakutapa")
       :pohjakoulutusvaatimus (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo")
       :valintatapa           (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.valintatavat "valintatapajono")
       :yhteishaku            (yhteishaku-filter haku-kaynnissa)}))

(defn- jarjestajat-aggs
  [tuleva? constraints]
  (let [no-other-hakutieto-filters-used (haku-kaynnissa-not-already-included? constraints)
        haku-kaynnissa (haku-kaynnissa? constraints)]
    {:inner_hits_agg {:filter (inner-hits-filters tuleva? constraints)
                       :aggs {:maakunta (koodisto-filters-for-subentity :hits.sijainti.keyword "maakunta")
                              :kunta (koodisto-filters-for-subentity :hits.sijainti.keyword "kunta")
                              :opetuskieli (koodisto-filters-for-subentity :hits.opetuskielet.keyword "oppilaitoksenopetuskieli")
                              :opetustapa (koodisto-filters-for-subentity :hits.opetustavat.keyword "opetuspaikkakk")

                              :hakukaynnissa         (if no-other-hakutieto-filters-used (hakukaynnissa-filter) (deduct-hakukaynnissa-aggs-from-other-filters constraints))
                              :hakutapa              (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.hakutapa     "hakutapa")
                              :pohjakoulutusvaatimus (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo")
                              :valintatapa           (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.valintatavat "valintatapajono")
                              :yhteishaku            (yhteishaku-filter haku-kaynnissa)}}}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "hits"}, :aggs (aggs-generator)}})

(defn- tarjoajat-aggs
  [tuleva? constraints]
  (let [no-other-hakutieto-filters-used (haku-kaynnissa-not-already-included? constraints)
        haku-kaynnissa (haku-kaynnissa? constraints)]
    {:inner_hits_agg {:filter (inner-hits-filters tuleva? constraints)
                      :aggs {:maakunta            (koodisto-filters-for-subentity :hits.sijainti.keyword "maakunta")
                             :kunta               (koodisto-filters-for-subentity :hits.sijainti.keyword "kunta")
                             :opetuskieli         (koodisto-filters-for-subentity :hits.opetuskielet.keyword "oppilaitoksenopetuskieli")
                             :koulutusala         (koodisto-filters-for-subentity :hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1")
                             :koulutusalataso2    (koodisto-filters-for-subentity :hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2")
                             :koulutustyyppi      (koulutustyyppi-filters-for-subentity :hits.koulutustyypit.keyword)
                             :koulutustyyppitaso2 (koodisto-filters-for-subentity :hits.koulutustyypit.keyword "koulutustyyppi")
                             :opetustapa          (koodisto-filters-for-subentity :hits.opetustavat.keyword "opetuspaikkakk")

                             :hakukaynnissa         (if no-other-hakutieto-filters-used (hakukaynnissa-filter) (deduct-hakukaynnissa-aggs-from-other-filters constraints))
                             :hakutapa              (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.hakutapa     "hakutapa")
                             :pohjakoulutusvaatimus (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo")
                             :valintatapa           (hakutieto-koodisto-filters haku-kaynnissa :hits.hakutiedot.valintatavat "valintatapajono")
                             :yhteishaku            (yhteishaku-filter haku-kaynnissa)}}}))

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(generate-default-aggs constraints)))

(defn jarjestajat-aggregations
  [tuleva? constraints]
  (aggregations #(jarjestajat-aggs tuleva? constraints)))

(defn tarjoajat-aggregations
  [tuleva? constraints]
  (aggregations #(tarjoajat-aggs tuleva? constraints)))
