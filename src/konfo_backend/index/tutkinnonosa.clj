(ns konfo-backend.index.tutkinnonosa
  (:refer-clojure :exclude [get])
  (:require
   [konfo-backend.elastic-tools :refer [get-source]]))

(defonce index "tutkinnonosa")

(defn get
  [id]
  (get-source index id))
