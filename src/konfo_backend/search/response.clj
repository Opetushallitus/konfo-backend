(ns konfo-backend.search.response
  (:require [konfo-backend.elastic-tools :as e]
            [konfo-backend.index.toteutus :refer [get-kuvaukset]]
            [konfo-backend.search.filters :refer [buckets-to-map generate-default-filter-counts
                                                  generate-filter-counts-for-jarjestajat]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [debug-pretty hit-haku-kaynnissa?
                                         log-pretty reduce-merge-map rename-key]]))

(defn- hits
  [response]
  (map (fn [x] (-> (:_source x) (assoc :_score (:_score x)))) (get-in response [:hits :hits])))

(defn- autocomplete-hits
  [response lng]
  (map (fn [x] (let [res (:_source x)]
                 {:label (get-in res [:nimi (keyword lng)])
                  :id (:oid res)}))
       (get-in response [:hits :hits])))

(defn get-uniform-buckets [agg agg-key]
  ;nested-aggregaatioilla (esim. search_terms.hakutiedot.yhteishakuOid) on yksi ylimääräinen aggregaatiokerros
  (let [rajain-agg (get-in agg [agg-key] agg)
        ;Jos aggregaatiolla on rajaimia, on lisäksi "rajain"-aggregaatiokerros
        agg-buckets (or (get-in rajain-agg [:buckets])
                        (get-in rajain-agg [:rajain :buckets])
                        [])]
    (if (and (map? agg) (empty? agg-buckets) (contains? agg :real_hits))
      {agg-key agg} ; single-bucket aggregaatio! esim. hakukaynnissa
      (buckets-to-map agg-buckets))))

(defn- ->doc_count
  [response agg-key]
  (let [hits-buckets (get-uniform-buckets (get-in response [:aggregations :hits_aggregation agg-key]) agg-key)
        inner-hits-buckets (get-uniform-buckets (get-in response [:aggregations :hits_aggregation :inner_hits_agg agg-key]) agg-key)
        [buckets get-count] (if (not-empty inner-hits-buckets)
                              [inner-hits-buckets (fn [key] (get-in inner-hits-buckets [(keyword key) :doc_count]))]
                              [hits-buckets (fn [key] (get-in hits-buckets [(keyword key) :real_hits :doc_count]))])
        mapper (fn [key] {key (get-count key)})]
    (reduce-merge-map mapper (keys buckets))))

(defn doc_count-by-filter
  [response]
  (let [agg-keys
        [:koulutustyyppi
         :opetuskieli
         :maakunta
         :kunta
         :koulutusala
         :opetustapa
         :valintatapa
         :hakukaynnissa
         :jotpa
         :tyovoimakoulutus
         :taydennyskoulutus
         :hakutapa
         :yhteishaku
         :pohjakoulutusvaatimus]]
    (reduce-merge-map #(->doc_count response %) agg-keys)))

(defn doc_count-by-koodi-uri-for-jarjestajat
  [response]
  (let [jarjestajat-agg-keys [:lukiopainotukset :lukiolinjaterityinenkoulutustehtava :osaamisala]]
    (merge
     (doc_count-by-filter response)
     (reduce-merge-map #(->doc_count response %) jarjestajat-agg-keys))))

(defn- filter-counts
  [response]
  (generate-default-filter-counts (doc_count-by-filter response)))

(defn- filters-for-jarjestajat
  [response]
  (generate-filter-counts-for-jarjestajat (doc_count-by-koodi-uri-for-jarjestajat response) (get-in response [:aggregations :hits_aggregation])))

(defn parse
  [response]
  (log-pretty response)
  {:total   (get-in response [:hits :total :value])
   :hits    (hits response)
   :filters (filter-counts response)})

(defn parse-for-autocomplete
  [lng response]
  {:hits    (autocomplete-hits response lng)})

(defn- inner-hit->toteutus-hit
  [inner-hit]
  (let [source (:_source inner-hit)]
    {:toteutusOid    (:toteutusOid source)
     :toteutusNimi   (:toteutusNimi source)
     :oppilaitosOid  (:oppilaitosOid source)
     :oppilaitosNimi (:nimi source)
     :kunnat         (get-in source [:metadata :kunnat])}))

(defn- valid-external-inner-hit [hit]
  (let [source (:_source hit)]
    (:toteutusOid source)))

(defn- external-hits
  [response]
  (map
   (fn [hit]
     (-> (:_source hit)
         (rename-key :eperuste :ePerusteId)
         (assoc :toteutukset (vec (map inner-hit->toteutus-hit (filter valid-external-inner-hit (get-in hit [:inner_hits :search_terms :hits :hits])))))))
   (get-in response [:hits :hits])))

(defn parse-external
  [response]
  {:total (get-in response [:hits :total :value])
   :hits  (filter (fn [hit] (not-empty (:toteutukset hit))) (external-hits response))})

; TODO: Välitetään draft tännekin ja käytetään sitä hakutietojen suodatukseen (kts. konfo-backend.index.toteutus)
(defn- inner-hits-with-kuvaukset
  [inner-hits]
  (let [hits (vec (map :_source (:hits inner-hits)))
        kuvaukset (get-kuvaukset (vec (distinct (remove nil? (map :toteutusOid hits)))))]
    (vec (for [hit hits
               :let [toteutusOid (:toteutusOid hit)]]
           (-> hit
               (select-keys [:koulutusOid
                             :oppilaitosOid
                             :toteutusNimi
                             :opetuskielet
                             :toteutusOid
                             :nimi
                             :koulutustyyppi
                             :kuva
                             :jarjestaaUrheilijanAmmKoulutusta
                             :opintojenLaajuusNumero
                             :opintojenLaajuusNumeroMin
                             :opintojenLaajuusNumeroMax
                             :opintojenLaajuusyksikko])
               (merge (:metadata hit))
               (assoc :hakuAuki (hit-haku-kaynnissa? hit))
               (assoc :kuvaus (if (not (nil? toteutusOid))
                                (or ((keyword toteutusOid) kuvaukset) {})
                                {})))))))

(defn parse-inner-hits
  ([response]
   (parse-inner-hits response filter-counts))
  ([response filter-generator]
   (let [inner-hits (some-> response :hits :hits (first) :inner_hits :search_terms :hits)
         total-inner-hits (:value (:total inner-hits))]
     {:total   (or total-inner-hits 0)
      :hits    (inner-hits-with-kuvaukset inner-hits)
      :filters (filter-generator response)})))

(defn parse-inner-hits-for-jarjestajat
  [response]
  (parse-inner-hits response filters-for-jarjestajat))
