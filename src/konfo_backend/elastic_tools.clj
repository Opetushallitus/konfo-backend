(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]
    [clj-elasticsearch.elastic-utils :as u]
    [clj-log.error-log :refer [with-error-logging]]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(defn get-source
  ([index id excludes]
   (let [result (e/get-document index id :_source_excludes (clojure.string/join "," excludes))]
     (when (:found result)
       (:_source result))))
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
  (if (pos? size) (if (< size 200) size 200) 0))

(defn ->from
  [page size]
  (if (pos? page) (* (- page 1) size) 0))

(defn search-with-pagination
  [index page size mapper & query-parts]
  (let [size (->size size)
        from (->from page size)]
    (apply search index mapper :from from :size size query-parts)))
