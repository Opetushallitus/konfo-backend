(ns konfo-backend.search.query
  (:require [clojure.string :as str]
            [konfo-backend.elastic-tools :refer [->from ->size]]
            [konfo-backend.search.filter.query-tools :refer [hakuaika-filter-query]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [assoc-if current-time-as-kouta-format
                                         remove-nils]]))

(defonce koulutustyypit ["amm"
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
                         "taiteen-perusopetus"
                         "muu"])


(defn match-all-query
  []
  {:match_all {}})

(defn koulutus-wildcard-query
  [search-phrase user-lng constraints]
  {:nested {:path "search_terms", :query {:bool (wildcard-query-fields search-phrase constraints user-lng) }}})

(defn query
  [keyword constraints user-lng suffixes]
  {:nested {:path "search_terms", :query {:bool (fields keyword constraints user-lng suffixes)}}})

(defn search-term-query [search-term user-lng suffixes]
  (if (not (str/blank? search-term))
    {:nested {:path "search_terms", :query {:bool {:must (make-search-term-query search-term user-lng suffixes)}}}}
    (match-all-query)))

(defn constraints-post-filter-query [constraints]
  (when (constraints? constraints)
    {:nested {:path "search_terms", :query {:bool {:filter (filters constraints (current-time-as-kouta-format))}}}}))

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

(defn- with-real-hits [agg]
  (assoc agg :aggs {:real_hits {:reverse_nested {}}}))

(defn- ->field-key [field-name]
  (str "search_terms." (name field-name)))

(defn- generate-rajain-agg [rajain-key constraints current-time]
  (let [rajain-agg-constraints (dissoc constraints (keyword rajain-key))]
    {:constrained (with-real-hits (if (constraints? rajain-agg-constraints)
                                    {:filter {:bool {:filter (filters rajain-agg-constraints current-time)}}}
                                    ; Käytetään "reverse_nested":iä dummy-aggregaationa kun ei ole rajaimia, jotta aggregaation tulosten rakenne pysyy samanlaisena
                                    {:reverse_nested {}}))}))

(defn rajain-aggregation ([rajain-key field-name current-time constraints include]
                          {:terms (assoc-if {:field field-name
                                             :min_doc_count 0
                                             :size (if (seq? include) (count include) 1000)}
                                            :include include (not-empty include))
                           :aggs (generate-rajain-agg rajain-key constraints current-time)})
  ([rajain-key field-name current-time constraints] (rajain-aggregation rajain-key field-name current-time constraints nil)))

; NOTE Hakutietosuodattimien sisältö riippuu haku-käynnissä valinnasta
(defn nested-rajain-aggregation
  ([rajain-key field-name current-time constraints include]
   {:nested {:path (-> field-name (str/replace-first ".keyword" "") (str/split #"\.") (drop-last) (#(str/join "." %)))}
    :aggs {(keyword rajain-key) (rajain-aggregation rajain-key field-name current-time constraints include)}})
  ([rajain-key field-name current-time constraints] (nested-rajain-aggregation rajain-key field-name current-time constraints nil)))

(defn bool-agg-filter [own-filter constraints current-time]
  (with-real-hits {:filter {:bool
                            {:filter (distinct (conj (filters constraints current-time) own-filter))}}}))

(defn- without-tyoelama-constraints [constraints] (dissoc constraints :jotpa :tyovoimakoulutus :taydennyskoulutus))

(defn hakukaynnissa-filter
  [current-time constraints]
  (bool-agg-filter (hakuaika-filter-query current-time) constraints current-time))

(defn jotpa-filter
  [current-time constraints]
  (bool-agg-filter (tyoelama-filters-query {:jotpa true}) (without-tyoelama-constraints constraints) current-time))

(defn tyovoimakoulutus-filter
  [current-time constraints]
  (bool-agg-filter (tyoelama-filters-query {:tyovoimakoulutus true}) (without-tyoelama-constraints constraints) current-time))

(defn taydennyskoulutus-filter
  [current-time constraints]
  (bool-agg-filter (tyoelama-filters-query {:taydennyskoulutus true}) (without-tyoelama-constraints constraints) current-time))

(defn- generate-default-aggs
  [constraints current-time]
  (remove-nils
   {:maakunta (rajain-aggregation "maakunta" (->field-key "sijainti.keyword") current-time constraints "maakunta.*")
    :kunta (rajain-aggregation "kunta" (->field-key "sijainti.keyword") current-time constraints "kunta.*")
    :opetuskieli (rajain-aggregation "opetuskieli" (->field-key "opetuskielet.keyword") current-time constraints)
    :opetustapa (rajain-aggregation "opetustapa" (->field-key "opetustavat.keyword") current-time constraints)
    :hakukaynnissa (hakukaynnissa-filter current-time constraints)
    :hakutapa (nested-rajain-aggregation "hakutapa" "search_terms.hakutiedot.hakutapa" current-time constraints)
    :pohjakoulutusvaatimus (nested-rajain-aggregation "pohjakoulutusvaatimus" "search_terms.hakutiedot.pohjakoulutusvaatimukset" current-time constraints)
    :valintatapa (nested-rajain-aggregation "valintatapa" "search_terms.hakutiedot.valintatavat" current-time constraints)
    :yhteishaku (nested-rajain-aggregation "yhteishaku" "search_terms.hakutiedot.yhteishakuOid" current-time constraints)
    :koulutusala (rajain-aggregation "koulutusala" (->field-key "koulutusalat.keyword") current-time constraints)
    :koulutustyyppi (rajain-aggregation "koulutustyyppi" (->field-key "koulutustyypit.keyword") current-time constraints koulutustyypit)}))

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
                         (rajain-aggregation "oppilaitosOid" "search_terms.oppilaitosOid.keyword" current-time {} oppilaitos-oids))})))

(defn- jarjestajat-aggs
  [tuleva? constraints oppilaitos-oids]
  (let [current-time (current-time-as-kouta-format)
        default-aggs (generate-default-aggs {} current-time)
        lukiopainotukset-aggs (rajain-aggregation "lukiopainotukset" (->field-key "koulutustyypit") current-time {})
        lukiolinjat-er-aggs (rajain-aggregation "lukiolinjaterityinenkoulutustehtava" (->field-key "lukiolinjaterityinenkoulutustehtava") current-time {})
        osaamisala-aggs (rajain-aggregation "osaamisala" (->field-key "osaamisala") current-time {})]
    {:inner_hits_agg
     {:filter (inner-hits-filters tuleva? constraints)
      :aggs
      (-> default-aggs
          (add-oppilaitos-aggs oppilaitos-oids current-time)
          (dissoc :koulutusala
                  :koulutustyyppi))}
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
