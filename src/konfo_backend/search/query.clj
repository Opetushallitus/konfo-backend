(ns konfo-backend.search.query
  (:require [konfo-backend.elastic-tools :refer [->from ->size]]
            [konfo-backend.index.haku :refer [list-yhteishaut]]
            [konfo-backend.koodisto.koodisto :refer [list-koodi-urit]]
            [konfo-backend.search.filter.query-tools :refer [hakuaika-filter-query]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(def koulutustyypit ["amm"
                     "amm-muu"
                     "amm-tutkinnon-osa"
                     "amm-osaamisala"
                     "lk"
                     "amk"
                     "amk-muu"
                     "amm-ope-erityisope-ja-opo"
                     "ope-pedag-opinnot"
                     "yo"
                     "kk-opintojakso"
                     "kk-opintokokonaisuus"
                     "erikoislaakari"
                     "erikoistumiskoulutus"
                     "amk-alempi"
                     "amk-ylempi"
                     "kandi"
                     "kandi-ja-maisteri"
                     "maisteri"
                     "tohtori"
                     "tuva"
                     "tuva-normal"
                     "tuva-erityisopetus"
                     "telma"
                     "vapaa-sivistystyo"
                     "vapaa-sivistystyo-opistovuosi"
                     "vapaa-sivistystyo-muu"
                     "aikuisten-perusopetus"
                     "taiteen-perusopetus"])

(defn query
  [keyword constraints user-lng suffixes]
  {:nested {:path "search_terms", :query {:bool (fields keyword constraints user-lng suffixes)}}})

(defn match-all-query
  []
  {:match_all {}})

(defn koulutus-wildcard-query
  [search-phrase user-lng constraints]
  {:nested {:path "search_terms", :query {:bool (wildcard-query-fields search-phrase constraints user-lng) }}})

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

(defn- koodisto-filters
  [field-name koodisto current-time constraints]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->filters-aggregation
      (keyword (str "search_terms." field-name ".keyword")) list current-time constraints)))

(defn- koulutustyyppi-filters
  [current-time constraints]
  (->filters-aggregation
    :search_terms.koulutustyypit.keyword koulutustyypit current-time constraints))

; NOTE Hakutietosuodattimien sisältö riippuu haku-käynnissä valinnasta
(defn- ->nested-term-filter
  [nested-field-name field term current-time constraints]
  {(keyword term) {:bool {:filter (nested-filters {:term {field term}} nested-field-name current-time constraints)}}})

(defn- ->nested-term-filters
  [nested-field-name field terms current-time constraints]
  (reduce merge {} (map #(->nested-term-filter nested-field-name field % current-time constraints) terms)))

(defn ->nested-filters-aggregation
  [nested-field-name field terms current-time constraints]
  {:filters
   {:filters (->nested-term-filters nested-field-name field terms current-time constraints)}
   :aggs {:real_hits {:reverse_nested {}}}})

(defn- nested-koodisto-filters
  [nested-field-name field-name koodisto current-time constraints]
  (if-let [list (seq (list-koodi-urit koodisto))]
    (->nested-filters-aggregation
     nested-field-name (keyword (str "search_terms." nested-field-name "." field-name)) list current-time constraints)))

(defn- hakutieto-koodisto-filters
  [field-name koodisto current-time constraints]
  (nested-koodisto-filters "hakutiedot" field-name koodisto current-time constraints))

(defn bool-agg-filter [filter-name own-filter constraints current-time]
  {:filters
   {:filters
    {(keyword filter-name)
     {:bool
      {:filter (distinct (conj (filters constraints current-time) own-filter))}}}}
   :aggs {:real_hits {:reverse_nested {}}}})

(defn- without-tyoelama-constraints [constraints] (dissoc constraints :jotpa :tyovoimakoulutus :taydennyskoulutus))

(defn hakukaynnissa-filter
  [current-time constraints]
  (bool-agg-filter "hakukaynnissa" (hakuaika-filter-query current-time) constraints current-time))

(defn jotpa-filter
  [current-time constraints]
  (bool-agg-filter "jotpa" (tyoelama-filters-query {:jotpa true}) (without-tyoelama-constraints constraints) current-time))

(defn tyovoimakoulutus-filter
  [current-time constraints]
  (bool-agg-filter "tyovoimakoulutus" (tyoelama-filters-query {:tyovoimakoulutus true}) (without-tyoelama-constraints constraints) current-time))

(defn taydennyskoulutus-filter
  [current-time constraints]
  (bool-agg-filter "taydennyskoulutus" (tyoelama-filters-query {:taydennyskoulutus true}) (without-tyoelama-constraints constraints) current-time))

(defn- remove-nils [record]
  (apply merge (for [[k v] record :when (not (nil? v))] {k v})))

(defn- yhteishaku-filter
  [current-time constraints]
  (if-let [list (seq (list-yhteishaut))]
    (->nested-filters-aggregation "hakutiedot" :search_terms.hakutiedot.yhteishakuOid list current-time constraints)))

(defn- generate-default-aggs
  [constraints current-time]
  (remove-nils
   {:maakunta (koodisto-filters "sijainti" "maakunta" current-time constraints)
    :kunta (koodisto-filters "sijainti" "kunta" current-time constraints)
    :opetuskieli (koodisto-filters "opetuskielet" "oppilaitoksenopetuskieli" current-time constraints)
    :opetustapa (koodisto-filters "opetustavat" "opetuspaikkakk" current-time constraints)
    :hakukaynnissa (hakukaynnissa-filter current-time constraints)
    :hakutapa (hakutieto-koodisto-filters "hakutapa" "hakutapa" current-time constraints)
    :pohjakoulutusvaatimus (hakutieto-koodisto-filters "pohjakoulutusvaatimukset" "pohjakoulutusvaatimuskonfo" current-time constraints)
    :valintatapa (hakutieto-koodisto-filters "valintatavat" "valintatapajono" current-time constraints)
    :yhteishaku (yhteishaku-filter current-time constraints)
    :koulutusala (koodisto-filters "koulutusalat" "kansallinenkoulutusluokitus2016koulutusalataso1" current-time constraints)
    :koulutusalataso2 (koodisto-filters "koulutusalat" "kansallinenkoulutusluokitus2016koulutusalataso2" current-time constraints)
    :koulutustyyppi (koulutustyyppi-filters current-time constraints)
    :koulutustyyppitaso2 (koodisto-filters "koulutustyypit" "koulutustyyppi" current-time constraints)}))

(defn- generate-aggs-for
  [filter-name filter-aggs tuleva? constraints current-time]
  {:filter {:bool
            {:must   {:term {"search_terms.onkoTuleva" tuleva?}}
             :filter (filters constraints current-time)}}
   :aggs   (if (nil? filter-aggs)
             {}
             {filter-name filter-aggs})})

(defn- hakutulos-aggs
  [constraints]
  (let [current-time (current-time-as-kouta-format)]
    (remove-nils
     (merge (generate-default-aggs constraints current-time)
            {:jotpa (jotpa-filter current-time constraints)
             :tyovoimakoulutus (tyovoimakoulutus-filter current-time constraints)
             :taydennyskoulutus (taydennyskoulutus-filter current-time constraints)}))))

(defn- add-oppilaitos-aggs
  [default-aggs oppilaitos-oids current-time]
  (remove-nils
   (merge default-aggs
          {:oppilaitos (when-not
                        (empty? oppilaitos-oids)
                         (->filters-aggregation "search_terms.oppilaitosOid.keyword" oppilaitos-oids current-time {}))})))

(defn- jarjestajat-aggs
  [tuleva? constraints oppilaitos-oids]
  (let [current-time (current-time-as-kouta-format)
        default-aggs (generate-default-aggs {} current-time)
        lukiopainotukset-aggs (koodisto-filters "lukiopainotukset" "lukiopainotukset" current-time {})
        lukiolinjat-er-aggs (koodisto-filters "lukiolinjaterityinenkoulutustehtava" "lukiolinjaterityinenkoulutustehtava" current-time {})
        osaamisala-aggs (koodisto-filters "osaamisalat" "osaamisala" current-time {})]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs
      (-> default-aggs
          (add-oppilaitos-aggs oppilaitos-oids current-time)
          (dissoc :koulutusala
                  :koulutusalataso2
                  :koulutustyyppi
                  :koulutustyyppitaso2))}
     :lukiopainotukset_aggs (generate-aggs-for "lukiopainotukset" lukiopainotukset-aggs tuleva? constraints current-time)
     :lukiolinjaterityinenkoulutustehtava_aggs (generate-aggs-for "lukiolinjaterityinenkoulutustehtava" lukiolinjat-er-aggs tuleva? constraints current-time)
     :osaamisala_aggs (generate-aggs-for :osaamisala osaamisala-aggs tuleva? constraints current-time)}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "search_terms"}, :aggs (aggs-generator)}})

(defn- tarjoajat-aggs
  [tuleva? constraints]
  (let [current-time (current-time-as-kouta-format)]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs
      (generate-default-aggs {} current-time)}}))

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(hakutulos-aggs constraints)))

(defn jarjestajat-aggregations
  [tuleva? constraints oppilaitos-oids]
  (aggregations #(jarjestajat-aggs tuleva? constraints oppilaitos-oids)))

(defn tarjoajat-aggregations
  [tuleva? constraints]
  (aggregations #(tarjoajat-aggs tuleva? constraints)))
