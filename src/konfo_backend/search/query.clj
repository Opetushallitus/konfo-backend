(ns konfo-backend.search.query
  (:require
    [konfo-backend.koodisto.koodisto :refer [list-koodi-urit]]
    [konfo-backend.index.haku :refer [list-yhteishaut]]
    [konfo-backend.search.tools :refer :all]
    [clojure.string :refer [lower-case]]
    [konfo-backend.elastic-tools :refer [->size ->from]]
    [konfo-backend.tools :refer [not-blank? current-time-as-kouta-format ten-months-past-as-kouta-format ->lower-case-vec]]
    [konfo-backend.config :refer [config]]))

(defn- ->terms-query
  [key coll]
  (if (= 1 (count coll))
    {:term {(keyword key) (lower-case (first coll))}}
    {:terms {(keyword key) (->lower-case-vec coll)}}))

(defn- some-hakuaika-kaynnissa
  []
  {:should [{:bool {:filter [{:range {:search_terms.toteutusHakuaika.alkaa {:lte (current-time-as-kouta-format)}}}
                             {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}},
                                              {:range {:search_terms.toteutusHakuaika.paattyy {:gt (current-time-as-kouta-format)}}}]}}]}}
            {:nested {:path  "search_terms.hakutiedot.hakuajat"
                      :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte (current-time-as-kouta-format)}}}
                                              {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                                               {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt (current-time-as-kouta-format)}}}]}}]}}}}]})

(defn- some-past-hakuaika-still-viable
  []
  {:nested {:path  "search_terms.hakutiedot.hakuajat"
            :query {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                    {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt (ten-months-past-as-kouta-format)}}}]}}}})

(defn- hakuaika-filter-query
  []
  {:bool (some-hakuaika-kaynnissa)})

(defn- hakutieto-query
  [inner-query]
  {:nested {:path  "search_terms.hakutiedot"
            :query {:bool {:filter (vec (remove nil? [(some-past-hakuaika-still-viable) inner-query]))}}}})

(defn- filters
  [constraints]
  (cond-> []
          (koulutustyyppi? constraints) (conj (->terms-query :search_terms.koulutustyypit.keyword (:koulutustyyppi constraints)))
          (opetuskieli? constraints) (conj (->terms-query :search_terms.opetuskielet.keyword (:opetuskieli constraints)))
          (sijainti? constraints) (conj (->terms-query :search_terms.sijainti.keyword (:sijainti constraints)))
          (koulutusala? constraints) (conj (->terms-query :search_terms.koulutusalat.keyword (:koulutusala constraints)))
          (opetustapa? constraints) (conj (->terms-query :search_terms.opetustavat.keyword (:opetustapa constraints)))

          ; NOTE hakukäynnissä rajainta EI haluta käyttää jos se sisältyy muihin rajaimiin (koska ao. rivit käyttäytyvät OR ehtoina)
          (haku-kaynnissa? constraints) (conj (hakuaika-filter-query))
          (has-jotpa-rahoitus? constraints) (conj {:bool {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}})
          (hakutapa? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.hakutapa (:hakutapa constraints))))
          (pohjakoulutusvaatimus? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.pohjakoulutusvaatimukset (:pohjakoulutusvaatimus constraints))))
          (valintatapa? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.valintatavat (:valintatapa constraints))))
          (yhteishaku? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.yhteishakuOid (:yhteishaku constraints))))))

(defn generate-search-params
  [suffixes search-params usr-lng]
  (for [language ["fi" "sv" "en"]
        suffix (conj suffixes nil)]
    (if (= language usr-lng)
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :language-default]))
                                                                                (str "." suffix "^" (:boost search-params))))
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :default]))
                                                                                (str "." suffix "^" (get-in config [:search-terms-boost :default])))))))

(defn- generate-keyword-query
  [usr-lng suffixes]
  (for [search-params [{:term "koulutusnimi" :boost (get-in config [:search-terms-boost :koulutusnimi])}
                       {:term "toteutusNimi" :boost (get-in config [:search-terms-boost :toteutusNimi])}
                       {:term "asiasanat" :boost (get-in config [:search-terms-boost :asiasanat])}
                       {:term "tutkintonimikkeet" :boost (get-in config [:search-terms-boost :tutkintonimikkeet])}
                       {:term "ammattinimikkeet" :boost (get-in config [:search-terms-boost :ammattinimikkeet])}
                       {:term "koulutus_organisaationimi" :boost (get-in config [:search-terms-boost :koulutus_organisaationimi])}
                       {:term "toteutus_organisaationimi" :boost (get-in config [:search-terms-boost :toteutus_organisaationimi])}]]
    (generate-search-params suffixes search-params usr-lng)))

(defn- fields
  [keyword constraints user-lng suffixes]
  (let [fields? (not-blank? keyword)
        filter? (constraints? constraints)]
    (cond-> {}
            fields? (-> (assoc :must {:multi_match {:query       keyword,
                                                    :fields      (flatten (generate-keyword-query user-lng suffixes))
                                                    :tie_breaker 0.9
                                                    :operator    "and"
                                                    :type        "cross_fields"}}))
            filter? (assoc :filter (filters constraints)))))

(defn query
  [keyword constraints user-lng suffixes]
  {:nested {:path "search_terms", :query {:bool (fields keyword constraints user-lng suffixes)}}})

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

(defn- lukio-filters [constraints]
  (cond-> []
          (lukiopainotukset? constraints) (conj (->terms-query :search_terms.lukiopainotukset.keyword (:lukiopainotukset constraints)))
          (lukiolinjaterityinenkoulutustehtava? constraints) (conj (->terms-query :search_terms.lukiolinjaterityinenkoulutustehtava.keyword (:lukiolinjaterityinenkoulutustehtava constraints)))))

(defn- osaamisala-filters [constraints]
  [(->terms-query :search_terms.osaamisalat.keyword (:osaamisala constraints))])

(defn- inner-hits-filters
  [tuleva? constraints]
  {:bool
   {:must
    [{:term {"search_terms.onkoTuleva" tuleva?}}
     {:bool
      (let [lukiolinjat-and-osaamisala-filters
            (concat []
                    (when (or (lukiolinjaterityinenkoulutustehtava? constraints) (lukiopainotukset? constraints))
                      (lukio-filters constraints))
                    (when (osaamisala? constraints)
                      (osaamisala-filters constraints)))]
        (if (empty? lukiolinjat-and-osaamisala-filters)
          {}
          {:should lukiolinjat-and-osaamisala-filters}))}]
    :filter (filters constraints)}})

(defn inner-hits-query
  [oid lng page size order tuleva? constraints]
  (let [size (->size size)
        from (->from page size)]
    {:bool {:must [{:term {:oid oid}}
                   {:nested {:inner_hits {:_source ["search_terms.koulutusOid", "search_terms.toteutusOid", "search_terms.toteutusNimi", "search_terms.opetuskielet", "search_terms.oppilaitosOid", "search_terms.kuva", "search_terms.nimi", "search_terms.metadata" "search_terms.hakutiedot" "search_terms.toteutusHakuaika" "search_terms.jarjestaaUrheilijanAmmKoulutusta"]
                                          :from    from
                                          :size    size
                                          :sort    {(str "search_terms.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}
                             :path       "search_terms"
                             :query      (inner-hits-filters tuleva? constraints)}}]}}))

(defn inner-hits-query-osat
  [oid lng page size order tuleva?]
  (let [size (->size size)
        from (->from page size)]
    {:nested {:inner_hits {:_source ["search_terms.koulutusOid", "search_terms.toteutusOid", "search_terms.oppilaitosOid", "search_terms.kuva", "search_terms.nimi", "search_terms.metadata"]
                           :from    from
                           :size    size
                           :sort    {(str "search_terms.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}
              :path       "search_terms"
              :query      {:bool {:must [{:term {"search_terms.onkoTuleva" tuleva?}}
                                         {:term {"search_terms.tarjoajat" oid}}]}}}}))

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
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->filters-aggregation field list)))

(defn- koodisto-filters-for-subentity
  [field koodisto]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->filters-aggregation-for-subentity field list)))

(defn- koulutustyyppi-filters
  [field]
  (->filters-aggregation field '["amm", "amm-muu", "amm-tutkinnon-osa" "amm-osaamisala" "lk" "amk" "amk-muu" "amm-ope-erityisope-ja-opo" "yo" "kk-opintojakso" "erikoislaakari" "amk-alempi" "amk-ylempi" "kandi" "kandi-ja-maisteri" "maisteri" "tohtori" "tuva" "tuva-normal" "tuva-erityisopetus" "telma", "vapaa-sivistystyo", "vapaa-sivistystyo-opistovuosi", "vapaa-sivistystyo-muu" "aikuisten-perusopetus"]))

(defn- koulutustyyppi-filters-for-subentity
  [field]
  (->filters-aggregation-for-subentity field '["amm" "amm-tutkinnon-osa" "amm-osaamisala" "amm-muu"]))

; NOTE Hakutietosuodattimien sisältö riippuu haku-käynnissä valinnasta
(defn- ->hakutieto-term-filter
  [field term]
  {(keyword term) (hakutieto-query {:term {field term}})})

(defn- ->hakutieto-term-filters
  [field terms]
  (reduce merge {} (map #(->hakutieto-term-filter field %) terms)))

(defn- ->hakutieto-filters-aggregation
  [field terms]
  {:filters {:filters (->hakutieto-term-filters field terms)} :aggs {:real_hits {:reverse_nested {}}}})

(defn- hakutieto-koodisto-filters
  [field koodisto]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->hakutieto-filters-aggregation field list)))

(defn- hakukaynnissa-filter
  []
  {:filters {:filters {:hakukaynnissa (hakuaika-filter-query)}} :aggs {:real_hits {:reverse_nested {}}}})

(defn- jotpa-filter
  []
  {:filters {:filters {:jotpa {:term {:search_terms.hasJotpaRahoitus true}}}} :aggs {:real_hits {:reverse_nested {}}}})

(defn- remove-nils [record]
  (apply merge (for [[k v] record :when (not (nil? v))] {k v})))

(defn- yhteishaku-filter
  []
  (if-let [list (seq (list-yhteishaut))]
    (->hakutieto-filters-aggregation :search_terms.hakutiedot.yhteishakuOid list)))

(defn- generate-default-aggs
  [constraints]
  (remove-nils {:maakunta              (koodisto-filters :search_terms.sijainti.keyword "maakunta")
                :kunta                 (koodisto-filters :search_terms.sijainti.keyword "kunta")
                :opetuskieli           (koodisto-filters :search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli")
                :koulutusala           (koodisto-filters :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1")
                :koulutusalataso2      (koodisto-filters :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2")
                :koulutustyyppi        (koulutustyyppi-filters :search_terms.koulutustyypit.keyword)
                :koulutustyyppitaso2   (koodisto-filters :search_terms.koulutustyypit.keyword "koulutustyyppi")
                :opetustapa            (koodisto-filters :search_terms.opetustavat.keyword "opetuspaikkakk")

                :hakukaynnissa         (hakukaynnissa-filter)
                :jotpa                 (jotpa-filter)
                :hakutapa              (hakutieto-koodisto-filters :search_terms.hakutiedot.hakutapa "hakutapa")
                :pohjakoulutusvaatimus (hakutieto-koodisto-filters :search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo")
                :valintatapa           (hakutieto-koodisto-filters :search_terms.hakutiedot.valintatavat "valintatapajono")
                :yhteishaku            (yhteishaku-filter)}))

(defn- generate-aggs-for
  [filter-name filter-aggs tuleva? constraints]
  {:filter {:bool
            {:must   {:term {"search_terms.onkoTuleva" tuleva?}}
             :filter (filters constraints)}}
   :aggs   (if (nil? filter-aggs)
             {}
             {filter-name filter-aggs})})

(defn- jarjestajat-aggs
  [tuleva? constraints]
  (let [lukiopainotukset-aggs (koodisto-filters-for-subentity :search_terms.lukiopainotukset.keyword "lukiopainotukset")
        lukiolinjat-er-aggs (koodisto-filters-for-subentity :search_terms.lukiolinjaterityinenkoulutustehtava.keyword "lukiolinjaterityinenkoulutustehtava")
        osaamisala-aggs (koodisto-filters-for-subentity :search_terms.osaamisalat.keyword "osaamisala")]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs   (remove-nils {:maakunta              (koodisto-filters-for-subentity :search_terms.sijainti.keyword "maakunta")
                            :kunta                 (koodisto-filters-for-subentity :search_terms.sijainti.keyword "kunta")
                            :opetuskieli           (koodisto-filters-for-subentity :search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli")
                            :opetustapa            (koodisto-filters-for-subentity :search_terms.opetustavat.keyword "opetuspaikkakk")

                            :hakukaynnissa         (hakukaynnissa-filter)
                            :hakutapa              (hakutieto-koodisto-filters :search_terms.hakutiedot.hakutapa "hakutapa")
                            :pohjakoulutusvaatimus (hakutieto-koodisto-filters :search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo")
                            :valintatapa           (hakutieto-koodisto-filters :search_terms.hakutiedot.valintatavat "valintatapajono")
                            :yhteishaku            (yhteishaku-filter)})}
     :lukiopainotukset_aggs                    (generate-aggs-for "lukiopainotukset" lukiopainotukset-aggs tuleva? constraints)
     :lukiolinjaterityinenkoulutustehtava_aggs (generate-aggs-for "lukiolinjaterityinenkoulutustehtava" lukiolinjat-er-aggs tuleva? constraints)
     :osaamisala_aggs                          (generate-aggs-for :osaamisala osaamisala-aggs tuleva? constraints)}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "search_terms"}, :aggs (aggs-generator)}})

(defn- tarjoajat-aggs
  [tuleva? constraints]
  {:inner_hits_agg {:filter (inner-hits-filters tuleva? constraints)
                    :aggs   (remove-nils {:maakunta              (koodisto-filters-for-subentity :search_terms.sijainti.keyword "maakunta")
                                          :kunta                 (koodisto-filters-for-subentity :search_terms.sijainti.keyword "kunta")
                                          :opetuskieli           (koodisto-filters-for-subentity :search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli")
                                          :koulutusala           (koodisto-filters-for-subentity :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1")
                                          :koulutusalataso2      (koodisto-filters-for-subentity :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2")
                                          :koulutustyyppi        (koulutustyyppi-filters-for-subentity :search_terms.koulutustyypit.keyword)
                                          :koulutustyyppitaso2   (koodisto-filters-for-subentity :search_terms.koulutustyypit.keyword "koulutustyyppi")
                                          :opetustapa            (koodisto-filters-for-subentity :search_terms.opetustavat.keyword "opetuspaikkakk")

                                          :hakukaynnissa         (hakukaynnissa-filter)
                                          :hakutapa              (hakutieto-koodisto-filters :search_terms.hakutiedot.hakutapa "hakutapa")
                                          :pohjakoulutusvaatimus (hakutieto-koodisto-filters :search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo")
                                          :valintatapa           (hakutieto-koodisto-filters :search_terms.hakutiedot.valintatavat "valintatapajono")
                                          :yhteishaku            (yhteishaku-filter)})}})

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(generate-default-aggs constraints)))

(defn jarjestajat-aggregations
  [tuleva? constraints]
  (aggregations #(jarjestajat-aggs tuleva? constraints)))

(defn tarjoajat-aggregations
  [tuleva? constraints]
  (aggregations #(tarjoajat-aggs tuleva? constraints)))
