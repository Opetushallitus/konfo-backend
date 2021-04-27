(ns konfo-backend.index.lokalisointi
  (:refer-clojure :exclude [get])
  (:require
    [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "lokalisointi")

(defn get
  [lng]
  (when-let [lokalisointi (get-source index lng)]
    (:translation lokalisointi)))