(ns konfo-backend.tools
  (:require
    [clj-time.format :as format]
    [clj-time.coerce :as coerce]
    [clj-time.core :as core]))

(defn julkaistu?
  [e]
  (and (not (nil? e)) (= "julkaistu" (:tila e))))

(defn julkaistut
  [coll]
  (filter julkaistu? coll))

(def iso-local-date-time-formatter (format/formatter "yyyy-MM-dd'T'HH:mm"))

(defn ->iso-local-date-time-string
  [date-time]
  (format/unparse iso-local-date-time-formatter date-time))

(defn iso-local-date-time-string->date-time
  [string]
  (format/parse iso-local-date-time-formatter string))

(defn long->date-time
  [long]
  (coerce/from-long long))

(defn current-time-as-iso-local-date-time-string
  []
  (->iso-local-date-time-string (long->date-time (System/currentTimeMillis))))

(defn within?
  [gte time lt]
  (core/within? (core/interval gte lt) time))

(defn hakuaika-kaynnissa?
  [hakuaika]
  (let [gte (iso-local-date-time-string->date-time (:gte hakuaika))
        lt  (iso-local-date-time-string->date-time (:lt hakuaika))]
    (within? gte (long->date-time (System/currentTimeMillis)) lt)))

