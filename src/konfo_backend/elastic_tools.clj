(ns konfo-backend.elastic-tools
  (:require
   [clj-elasticsearch.elastic-connect :as e]
   [clj-http.client :as http]
   [clj-elasticsearch.elastic-utils :refer [elastic-url]]
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]))

(def limit-to-use-search-after 10000)

(def max-size 200)

(def minimum-size-for-search-after 5)

(defn- clean-unwanted-fields
  [map]
  (postwalk
   #(if (map? %)
      (dissoc % :muokkaaja :_id :_tunniste :_enrichedData)
      %)
   map))

(defn get-source
  ([index id excludes]
   (when id
     (let [result (e/get-document index id :_source_excludes (str/join "," excludes))]
       (when (:found result)
         (clean-unwanted-fields (:_source result))))))
  ([index id]
   (get-source index id [])))

(defn get-sources
  [index ids excludes]
  (mapv #(clean-unwanted-fields %)
        (if (seq excludes)
          (e/multi-get index ids :_source_excludes (str/join "," excludes))
          (e/multi-get index ids))))

(defn get-sources-with-selected-fields
  [index ids includes]
  (mapv #(clean-unwanted-fields %) (e/multi-get index ids :_source_includes (str/join "," includes))))

(defn search-without-mapper
  [index & query-parts]
  (let [query-parts-without-nils (apply concat (remove (fn [[_ v]] (nil? v)) (partition 2 query-parts)))]
    (apply e/search index query-parts-without-nils)))

(defn search
  [index mapper & query-parts]
  (->> (apply search-without-mapper
              index
              query-parts)
       mapper))

(defn count
  [index & query-parts]
  (apply e/count
         index
         query-parts))

(defn ->size
  [size]
  (if (pos? size) (min size max-size) 0))

(defn ->from
  [page size]
  (if (pos? page) (* (- page 1) size) 0))


; pit is automatically deleted after keep_alive expires by elastic_search, so we skip deleting it through REST
; since library does not support using DELETE with body
(defn- get-pit
  [index]
  (let [json-request {:as :json
                      :content-type :json
                      :socket-timeout 120000}
        pit (:body (http/post (elastic-url index "_pit?keep_alive=1m") json-request))]
    {:id (:id pit)
     :keep_alive "1m"}))

; https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after
(defn- do-search-after
  [search-fn from size query-parts initial-search result-gatherer-fn]
  (let [get-pit-from-result (fn [result]
                              {:id (:pit_id result)
                               :keep_alive "1m"})
        search-with-do-after (fn [sort pit-val]
                               (apply search-fn
                                      nil
                                      :pit pit-val
                                      :from 0
                                      :size size
                                      :search_after sort
                                      :track_total_hits false
                                      query-parts))
        take-last-sort (fn [results] (->> (:hits results)
                                          :hits
                                          last
                                          :sort))]
    (loop [n (clojure.core/count (range (/ (- from limit-to-use-search-after) size)))
           pit (get-pit-from-result initial-search)
           last-sort (take-last-sort initial-search)
           final-result []]
      (if-let [result (when (and (not (neg? n))
                                 (some? last-sort))
                        (search-with-do-after last-sort pit))]
        (recur (dec n)
               (get-pit-from-result result)
               (take-last-sort result)
               (result-gatherer-fn final-result result))
        final-result))))

(defn do-search-after-concanate-results [search-fn index mapper to size query-parts]
  (let [pit (get-pit index)
        initial-from   (- limit-to-use-search-after size)
        initial-search (apply search-fn nil :pit pit :from initial-from :size size query-parts)]
    (do-search-after search-fn to size query-parts initial-search
                     (fn [final-result result] (concat final-result (mapper result))))))

(defn search-all-with-do-after [index mapper to & query-parts]
  (let [initial-results (apply search index mapper :size limit-to-use-search-after query-parts)
        rest-of-results (do-search-after-concanate-results search-without-mapper index mapper to max-size query-parts)]
    (concat initial-results rest-of-results)))

(defn do-search-after-paged [search-fn index mapper from size & query-parts]
  (let [size (if (< size minimum-size-for-search-after) minimum-size-for-search-after size)
        initial-from (- limit-to-use-search-after size)
        pit (get-pit index)
        initial-search (apply search-fn nil :pit pit :from initial-from :size size :sort (:sort query-parts) query-parts)]
    (if (< (get-in initial-search [:hits :total :value] 0) from)
      (-> initial-search
          (assoc :hits [])
          mapper)
      (mapper
       (do-search-after search-fn from size query-parts initial-search (fn [_ result] result))))))

(defn search-with-pagination
  [index page size mapper & query-parts]
  (let [size (->size size)
        from (->from page size)]
    (if (< limit-to-use-search-after (+ from size))
      (apply do-search-after-paged search-without-mapper index mapper from size query-parts)
      (apply search index mapper :from from :size size query-parts))))
