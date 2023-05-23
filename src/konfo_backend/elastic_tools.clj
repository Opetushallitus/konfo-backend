(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]
    [clj-elasticsearch.elastic-utils :as u]
    [clj-log.error-log :refer [with-error-logging]]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(def limit-to-use-search-after 10000)

(def max-size 200)

(defn get-source
  ([index id excludes]
   (when id
     (let [result (e/get-document index id :_source_excludes (clojure.string/join "," excludes))]
       (when (:found result)
         (:_source result)))))
  ([index id]
   (get-source index id [])))

(defn get-sources
  [index ids excludes]
  (if (seq excludes)
    (e/multi-get index ids :_source_excludes (clojure.string/join "," excludes))
    (e/multi-get index ids)))

(defn search
  [index mapper & query-parts]
  (let [query-parts-without-nils (apply concat (remove (fn [[_ v]] (nil? v)) (partition 2 query-parts)))]
    (->> (apply e/search
                index
                query-parts-without-nils)
         mapper)))

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
        initial-search (apply search-fn index mapper :from initial-from :size size query-parts)
        search-with-do-after (fn [sort from-index]
                               (apply search-fn index mapper :from from-index :size size :search_after sort query-parts))
        take-last-sort (fn [results] (->> (:hits results)
                                          :hits
                                          last
                                          :sort))
        next-result    (fn [last-sort from-index] (search-with-do-after last-sort from-index))
        last-sort      (atom (take-last-sort initial-search))
        final-result (atom initial-search)]
    (doseq [multiplier (range (/ (- from limit-to-use-search-after) size))]
      (let [result (next-result @last-sort (+ initial-from (* (+ multiplier 1) size)))]
        (reset! last-sort (take-last-sort result))
        (reset! final-result result)))
    @final-result))

(defn search-with-pagination
  [index page size mapper & query-parts]
  (let [size (->size size)
        from (->from page size)]
    (if (< limit-to-use-search-after from)
      (do-search-after search index mapper from size query-parts)
      (apply search index mapper :from from :size size query-parts))))
