(ns konfo-backend.index.toteutussuunnitelma
  (:refer-clojure :exclude [get])
  (:require [konfo-backend.elastic-tools :refer [get-source get-sources]]))

(defonce index "toteutussuunnitelma")

(defn get
  [id]
  (get-source index id))

(defn get-many
  ([oids excludes]
   (get-sources index oids excludes))
  ([oids]
   (get-many oids [])))
