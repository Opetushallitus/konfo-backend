(ns konfo-backend.search.response
  (:require [konfo-backend.index.toteutus :refer [get-kuvaukset]]
            [konfo-backend.search.rajain-counts :refer [generate-default-rajain-counts
                                                        generate-rajain-counts-for-jarjestajat]]
            [konfo-backend.search.rajain-definitions :refer [all-aggregation-defs]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.tools :refer [hit-haku-kaynnissa? log-pretty
                                         reduce-merge-map rename-key]]))

(defn- buckets-to-map
  [buckets]
  (into {} (map (fn [x] [(keyword (:key x)) x]) buckets)))

(defn- hits
  [response]
  (map (fn [x] (-> (:_source x) (assoc :_score (:_score x)))) (get-in response [:hits :hits])))

(defn- autocomplete-hits
  [response]
  (map (fn [x] (let [res (:_source x)]
                 (select-keys res [:oid :nimi :toteutustenTarjoajat])))
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
        mapper (fn [key] {key (get-in hits-buckets (concat [(keyword key)] [:real_hits :doc_count]))})]
    (reduce-merge-map mapper (keys hits-buckets))))

(defn doc_count-by-filter
  [response]
  (reduce-merge-map #(->doc_count response %) (map :id all-aggregation-defs)))

(defn- rajain-counts
  [response]
  (generate-default-rajain-counts (doc_count-by-filter response)))

(defn- filters-for-jarjestajat
  [response]
  (generate-rajain-counts-for-jarjestajat (doc_count-by-filter response)
                                          (get-uniform-buckets (get-in response [:aggregations :hits_aggregation :oppilaitos]) :oppilaitos)))

(defn parse
  [response]
  (log-pretty response)
  {:total   (get-in response [:hits :total :value])
   :hits    (hits response)
   :filters (rajain-counts response)})

(defn parse-for-autocomplete
  [lng response]
  {:total   (get-in response [:hits :total :value])
   :hits    (autocomplete-hits response)})

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
   (parse-inner-hits response rajain-counts))
  ([response filter-generator]
   (let [inner-hits (some-> response :hits :hits (first) :inner_hits :search_terms :hits)
         total-inner-hits (:value (:total inner-hits))]
     {:total   (or total-inner-hits 0)
      :hits    (inner-hits-with-kuvaukset inner-hits)
      :filters (filter-generator response)})))

(defn parse-inner-hits-for-jarjestajat
  [response]
  (parse-inner-hits response filters-for-jarjestajat))
