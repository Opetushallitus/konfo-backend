(ns konfo-backend.test-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e]))

(defn refresh-and-wait
  [indexname timeout]
  (e/refresh-index indexname)
  (Thread/sleep timeout))