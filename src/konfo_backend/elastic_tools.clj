(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]
    [clj-log.error-log :refer [with-error-logging]]
    [clojure.tools.logging :as log]))

(defn index-name [name] name)

(defn insert-query-perf [query duration started res-size]
  (e/create
    (index-name "query_perf")
    (index-name "query_perf")
    {:created        (System/currentTimeMillis)
     :started        started
     :duration_mills duration
     :query          query
     :response_size  res-size}))

(defn query-perf-string [type keyword constraints]
  (println [type keyword (:koulutustyyppi constraints) (:paikkakunta constraints)])
  (clojure.string/join "/" (remove clojure.string/blank? [type keyword (:koulutustyyppi constraints) (:paikkakunta constraints)])))

(defn search
  [index perf-log-msg page size mapper & query-parts]
  (log/debug query-parts)
  (with-error-logging
    (let [start (System/currentTimeMillis)
          size (if (pos? size) (if (< size 200) size 200) 0)
          from (if (pos? page) (* (- page 1) size) 0)
          res (->> (apply e/search
                     (index-name index)
                     (index-name index)
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