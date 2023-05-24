(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]
    [clojure.string :as str]))

(def limit-to-use-search-after 10000)

(def max-size 200)

(defn get-source
  ([index id excludes]
   (when id
     (let [result (e/get-document index id :_source_excludes (str/join "," excludes))]
       (when (:found result)
         (:_source result)))))
  ([index id]
   (get-source index id [])))

(defn get-sources
  [index ids excludes]
  (if (seq excludes)
    (e/multi-get index ids :_source_excludes (str/join "," excludes))
    (e/multi-get index ids)))

(defn search-without-mapper
  [index & query-parts]
  (let [query-parts-without-nils (apply concat (remove (fn [[_ v]] (nil? v)) (partition 2 query-parts)))]
    (apply e/search index query-parts-without-nils)))

(defn search
  [index mapper & query-parts]
  (mapper (search-without-mapper index query-parts)))

(defn count
  [index & query-parts]
  (apply e/count
         index
         query-parts))

(defn ->size
  [size]
  (if (pos? size) (if (< size max-size) size max-size) 0))

(defn ->from
  [page size]
  (if (pos? page) (* (- page 1) size) 0))

; https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after
(defn do-search-after [search-fn index mapper from size query-parts]
  (let [initial-from   (- limit-to-use-search-after size)
        initial-search (apply search-fn index :from initial-from :size size query-parts)
        search-with-do-after (fn [sort]
                               (apply search-fn index :from 0 :size size :search_after sort query-parts))
        take-last-sort (fn [results] (->> (:hits results)
                                          :hits
                                          last
                                          :sort))
        last-sort      (atom (take-last-sort initial-search))
        final-result (atom initial-search)]
    (if (< (get-in initial-search [:hits :total :value] 0) from)
      (-> initial-search
          (assoc :hits [])
          mapper)
      (do
        (doseq [_ (range (/ (- from limit-to-use-search-after) size))]
          (when (not (nil? @last-sort))
            (let [result (search-with-do-after @last-sort)]
              (reset! last-sort (take-last-sort result))
              (reset! final-result result))))
        (mapper @final-result)))))

(defn search-with-pagination
  [index page size mapper & query-parts]
  (let [size (->size size)
        from (->from page size)]
    (if (< limit-to-use-search-after (+ from size))
      (do-search-after search-without-mapper index mapper from size query-parts)
      (apply search index mapper :from from :size size query-parts))))
