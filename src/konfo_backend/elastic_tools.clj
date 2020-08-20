(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]
    [clj-elasticsearch.elastic-utils :as u]
    [clj-log.error-log :refer [with-error-logging]]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(defn get-source
  [index id]
  (let [result (e/get-document index id)]
    (when (:found result)
      (:_source result))))

(defn get-sources
  [index ids excludes]
  (if (seq excludes)
    (e/multi-get index ids :_source_excludes (clojure.string/join "," excludes))
    (e/multi-get index ids)))

(defn search
  [index mapper & query-parts]
  (->> (apply e/search
              index
              query-parts)
       mapper))

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

(defn- virkailija-alias?
  [alias]
  (str/ends-with? alias "-virkailija"))

(defn- virkailija-alias->oppija-alias
  [virkailija-alias]
  (str/replace virkailija-alias "-virkailija" ""))

(defn- find-virkailija-aliases
  []
  (->> (e/list-aliases)
       (mapcat #(->> (val %) :aliases (map key)))
       (map name)
       (filter virkailija-alias?)))

(defn update-aliases-on-startup
  []
  (doseq [virkailija-alias (find-virkailija-aliases)
          :let [oppija-alias (virkailija-alias->oppija-alias virkailija-alias)]]
    (if-let [new-index (e/move-read-alias-to-write-index virkailija-alias oppija-alias)]
      (log/info oppija-alias " alias points to index" new-index "!!")
      (log/warn "Cannot find write index for" virkailija-alias "alias!!"))))