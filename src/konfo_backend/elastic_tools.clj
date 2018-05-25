(ns konfo-backend.elastic-tools
  (:require
    [clj-elasticsearch.elastic-connect :refer :all]
    [clojure.tools.logging :as log]))

(defn index-name [name] name)

(defn insert-query-perf [query duration started res-size]
  (create
    (index-name "query_perf")
    (index-name "query_perf")
    {:created        (System/currentTimeMillis)
     :started        started
     :duration_mills duration
     :query          query
     :response_size  res-size}))