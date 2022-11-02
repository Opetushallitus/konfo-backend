(ns konfo-backend.search.query
  (:require
    [konfo-backend.elastic-tools :refer [->from ->size]]
    [konfo-backend.index.haku :refer [list-yhteishaut]]
    [konfo-backend.koodisto.koodisto :refer [list-koodi-urit]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(defn query
  [keyword constraints user-lng suffixes]
  {:nested {:path "search_terms", :query {:bool (fields keyword constraints user-lng suffixes)}}})

(defn match-all-query
  []
  {:match_all {}})

;OY-3870 Kenttä nimi_sort lisätty indekseihin oppilaitos-kouta-search ja koulutus-kouta-search.
(defn- ->name-sort
  [order lng]
  [{(->lng-keyword "nimi.%s.keyword" lng) {:order order :unmapped_type "string"}}
   {(->lng-keyword "nimi_sort.%s.keyword" lng) {:order order :unmapped_type "string"}}])

(defn sorts
  [sort order lng]
  (if (= "name" sort)
    (->name-sort order lng)
    (vec (concat [{:_score {:order order}}] (->name-sort "asc" lng)))))

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
    :filter (filters constraints (current-time-as-kouta-format))}})

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
  [field term current-time constraints]
  {(keyword term) {:bool {:filter (distinct (terms-filters {:term {field term}} current-time constraints))}}})

(defn- ->term-filters
  [field terms current-time constraints]
  (reduce merge {} (map #(->term-filter field % current-time constraints) terms)))

(defn ->filters-aggregation
  [field terms current-time constraints]
  {:filters {:filters
             (->term-filters field terms current-time constraints)} :aggs {:real_hits {:reverse_nested {}}}})

(defn- ->filters-aggregation-for-subentity
  [field terms current-time constraints]
  {:filters {:filters (->term-filters field terms current-time constraints)}})

(defn- koodisto-filters
  [field koodisto current-time constraints]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->filters-aggregation field list current-time constraints)))

(defn- koodisto-filters-for-subentity
  [field koodisto current-time constraints]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->filters-aggregation-for-subentity field list current-time constraints)))

(defn- koulutustyyppi-filters
  [field current-time constraints]
  (->filters-aggregation field '["amm", "amm-muu", "amm-tutkinnon-osa" "amm-osaamisala" "lk" "amk" "amk-muu" "amm-ope-erityisope-ja-opo" "ope-pedag-opinnot" "yo" "kk-opintojakso" "kk-opintokokonaisuus" "erikoislaakari" "amk-alempi" "amk-ylempi" "kandi" "kandi-ja-maisteri" "maisteri" "tohtori" "tuva" "tuva-normal" "tuva-erityisopetus" "telma", "vapaa-sivistystyo", "vapaa-sivistystyo-opistovuosi", "vapaa-sivistystyo-muu" "aikuisten-perusopetus"] current-time constraints))

(defn- koulutustyyppi-filters-for-subentity
  [field current-time constraints]
  (->filters-aggregation-for-subentity field '["amm" "amm-tutkinnon-osa" "amm-osaamisala" "amm-muu"] current-time constraints))

; NOTE Hakutietosuodattimien sisältö riippuu haku-käynnissä valinnasta
(defn- ->hakutieto-term-filter
  [field term current-time constraints]
  {(keyword term) {:bool {:filter (hakutieto-filters {:term {field term}} current-time constraints)}}})

(defn- ->hakutieto-term-filters
  [field terms current-time constraints]
  (reduce merge {} (map #(->hakutieto-term-filter field % current-time constraints) terms)))

(defn ->hakutieto-filters-aggregation
  [field terms current-time constraints]
  {:filters
   {:filters (->hakutieto-term-filters field terms current-time constraints)}
   :aggs {:real_hits {:reverse_nested {}}}})

(defn- hakutieto-koodisto-filters
  [field koodisto current-time constraints]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->hakutieto-filters-aggregation field list current-time constraints)))

(defn hakukaynnissa-filter
  [current-time constraints]
  {:filters
   {:filters
    {:hakukaynnissa
     {:bool
      {:filter (distinct
                 (conj
                   (filters constraints current-time)
                   (hakuaika-filter-query current-time)))}}}}
   :aggs {:real_hits {:reverse_nested {}}}})

(defn jotpa-filter
  [current-time constraints]
  {:filters
   {:filters
    {:jotpa
     {:bool
      {:filter (distinct
                 (conj
                   (filters constraints current-time)
                     {:term {:search_terms.hasJotpaRahoitus true}}))}}}}
   :aggs {:real_hits {:reverse_nested {}}}})

(defn- remove-nils [record]
  (apply merge (for [[k v] record :when (not (nil? v))] {k v})))

(defn- yhteishaku-filter
  [current-time constraints]
  (if-let [list (seq (list-yhteishaut))]
    (->hakutieto-filters-aggregation :search_terms.hakutiedot.yhteishakuOid list current-time constraints)))

(defn- generate-default-aggs
  [constraints]
  (let [current-time (current-time-as-kouta-format)]
    (remove-nils {:maakunta (koodisto-filters :search_terms.sijainti.keyword "maakunta" current-time constraints)
                  :kunta (koodisto-filters :search_terms.sijainti.keyword "kunta" current-time constraints)
                  :opetuskieli (koodisto-filters :search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli" current-time constraints)
                  :koulutusala (koodisto-filters :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1" current-time constraints)
                  :koulutusalataso2 (koodisto-filters :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2" current-time constraints)
                  :koulutustyyppi (koulutustyyppi-filters :search_terms.koulutustyypit.keyword current-time constraints)
                  :koulutustyyppitaso2 (koodisto-filters :search_terms.koulutustyypit.keyword "koulutustyyppi" current-time constraints)
                  :opetustapa (koodisto-filters :search_terms.opetustavat.keyword "opetuspaikkakk" current-time constraints)

                  :hakukaynnissa (hakukaynnissa-filter current-time constraints)
                  :jotpa (jotpa-filter current-time constraints)
                  :hakutapa (hakutieto-koodisto-filters :search_terms.hakutiedot.hakutapa "hakutapa" current-time constraints)
                  :pohjakoulutusvaatimus (hakutieto-koodisto-filters :search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo" current-time constraints)
                  :valintatapa (hakutieto-koodisto-filters :search_terms.hakutiedot.valintatavat "valintatapajono" current-time constraints)
                  :yhteishaku (yhteishaku-filter current-time constraints)})))

(defn- generate-aggs-for
  [filter-name filter-aggs tuleva? constraints current-time]
  {:filter {:bool
            {:must   {:term {"search_terms.onkoTuleva" tuleva?}}
             :filter (filters constraints current-time)}}
   :aggs   (if (nil? filter-aggs)
             {}
             {filter-name filter-aggs})})

(defn- jarjestajat-aggs
  [tuleva? constraints oppilaitos-oids]
  (let [current-time (current-time-as-kouta-format)
        lukiopainotukset-aggs (koodisto-filters-for-subentity :search_terms.lukiopainotukset.keyword "lukiopainotukset" current-time constraints)
        lukiolinjat-er-aggs (koodisto-filters-for-subentity :search_terms.lukiolinjaterityinenkoulutustehtava.keyword "lukiolinjaterityinenkoulutustehtava" current-time constraints)
        osaamisala-aggs (koodisto-filters-for-subentity :search_terms.osaamisalat.keyword "osaamisala" current-time constraints)]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs   (remove-nils {:maakunta              (koodisto-filters-for-subentity :search_terms.sijainti.keyword "maakunta" current-time constraints)
                            :kunta                 (koodisto-filters-for-subentity :search_terms.sijainti.keyword "kunta" current-time constraints)
                            :opetuskieli           (koodisto-filters-for-subentity :search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli" current-time constraints)
                            :opetustapa            (koodisto-filters-for-subentity :search_terms.opetustavat.keyword "opetuspaikkakk" current-time constraints)

                            :hakukaynnissa         (hakukaynnissa-filter current-time constraints)
                            :hakutapa              (hakutieto-koodisto-filters :search_terms.hakutiedot.hakutapa "hakutapa" current-time constraints)
                            :pohjakoulutusvaatimus (hakutieto-koodisto-filters :search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo" current-time constraints)
                            :valintatapa           (hakutieto-koodisto-filters :search_terms.hakutiedot.valintatavat "valintatapajono" current-time constraints)
                            :oppilaitos            (->filters-aggregation-for-subentity "search_terms.oppilaitosOid.keyword" oppilaitos-oids current-time constraints)
                            :yhteishaku            (yhteishaku-filter current-time constraints)})}
     :lukiopainotukset_aggs                    (generate-aggs-for "lukiopainotukset" lukiopainotukset-aggs tuleva? constraints current-time)
     :lukiolinjaterityinenkoulutustehtava_aggs (generate-aggs-for "lukiolinjaterityinenkoulutustehtava" lukiolinjat-er-aggs tuleva? constraints current-time)
     :osaamisala_aggs                          (generate-aggs-for :osaamisala osaamisala-aggs tuleva? constraints current-time)}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "search_terms"}, :aggs (aggs-generator)}})

(defn- tarjoajat-aggs
  [tuleva? constraints]
  (let [current-time (current-time-as-kouta-format)]
   {:inner_hits_agg {:filter (inner-hits-filters tuleva? constraints)
                    :aggs   (remove-nils {:maakunta              (koodisto-filters-for-subentity :search_terms.sijainti.keyword "maakunta" current-time constraints)
                                          :kunta                 (koodisto-filters-for-subentity :search_terms.sijainti.keyword "kunta" current-time constraints)
                                          :opetuskieli           (koodisto-filters-for-subentity :search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli" current-time constraints)
                                          :koulutusala           (koodisto-filters-for-subentity :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1" current-time constraints)
                                          :koulutusalataso2      (koodisto-filters-for-subentity :search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2" current-time constraints)
                                          :koulutustyyppi        (koulutustyyppi-filters-for-subentity :search_terms.koulutustyypit.keyword current-time constraints)
                                          :koulutustyyppitaso2   (koodisto-filters-for-subentity :search_terms.koulutustyypit.keyword "koulutustyyppi" current-time constraints)
                                          :opetustapa            (koodisto-filters-for-subentity :search_terms.opetustavat.keyword "opetuspaikkakk" current-time constraints)

                                          :hakukaynnissa         (hakukaynnissa-filter current-time constraints)
                                          :hakutapa              (hakutieto-koodisto-filters :search_terms.hakutiedot.hakutapa "hakutapa" current-time constraints)
                                          :pohjakoulutusvaatimus (hakutieto-koodisto-filters :search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo" current-time constraints)
                                          :valintatapa           (hakutieto-koodisto-filters :search_terms.hakutiedot.valintatavat "valintatapajono" current-time constraints)
                                          :yhteishaku            (yhteishaku-filter current-time {})})}}))

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(generate-default-aggs constraints)))

(defn jarjestajat-aggregations
  [tuleva? constraints oppilaitos-oids]
  (aggregations #(jarjestajat-aggs tuleva? constraints oppilaitos-oids)))

(defn tarjoajat-aggregations
  [tuleva? constraints]
  (aggregations #(tarjoajat-aggs tuleva? constraints)))
