(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]
    [clj-log.error-log :refer [with-error-logging]]
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

(defn get-source
  [index id]
  (let [result (e/get-document index id)]
    (when (:found result)
      (:_source result))))

(defn insert-query-perf [query duration started res-size]
  (e/create
    "query_perf"
    {:created        (System/currentTimeMillis)
     :started        started
     :duration_mills duration
     :query          query
     :response_size  res-size}))

(defn query-perf-string [type keyword constraints]
  (println [type keyword (:koulutustyyppi constraints) (:paikkakunta constraints)])
  (clojure.string/join "/" (remove clojure.string/blank? [type keyword (:koulutustyyppi constraints) (:paikkakunta constraints)])))

(defn search
  [index mapper & query-parts]
  (try
    (->> (apply e/search
                index
                query-parts)
         mapper)
    (catch Exception e
      (log/error "Got exception when searching" e)
      (throw e))))

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

(defn old-search
  [index perf-log-msg page size mapper & query-parts]
  (log/debug query-parts)
  (with-error-logging
    (let [start (System/currentTimeMillis)
          size  (->size size)
          from  (->from page size)
            res (->> (apply e/search
                       index
                       :from from
                       :size size
                       query-parts)
                       :hits
                       (mapper))]
      (insert-query-perf perf-log-msg (- (System/currentTimeMillis) start) start (count res))
      res)))

(defn multi_match [keyword fields]
  {:multi_match { :query keyword,
                 :fields fields,
                 :operator "and" }})

(defn constant_score_query_multi_match [keyword fields boost]
  { :constant_score {
                     :filter { :multi_match { :query keyword
                                             :fields fields
                                             :operator "and" }},
                     :boost boost }})

(defn constant_score_query_match_tyypit [keyword boost]
  { :constant_score {
                     :filter { :match { :tyypit { :query keyword }}},
                     :boost boost }})

(defn constant_score_query_terms [key values boost]         ;:oid (vec oids)
  { :constant_score {
                     :filter { :terms { key values }},
                     :boost boost }})

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