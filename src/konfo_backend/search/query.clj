(ns konfo-backend.search.query
  (:require
    [konfo-backend.koodisto.koodisto :refer [list-koodi-urit]]
    [konfo-backend.index.haku :refer [list-yhteishaut]]
    [konfo-backend.search.tools :refer :all]
    [clojure.string :refer [lower-case]]
    [konfo-backend.elastic-tools :refer [->size ->from]]
    [konfo-backend.tools :refer [not-blank? current-time-as-kouta-format ten-months-past-as-kouta-format ->lower-case-vec]]
    [konfo-backend.config :refer [config]]))

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
  []
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
  []
  (aggregations #(generate-default-aggs)))

(defn jarjestajat-aggregations
  [tuleva? constraints]
  (aggregations #(jarjestajat-aggs tuleva? constraints)))

(defn tarjoajat-aggregations
  [tuleva? constraints]
  (aggregations #(tarjoajat-aggs tuleva? constraints)))
