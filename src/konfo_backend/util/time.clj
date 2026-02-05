(ns konfo-backend.util.time
  (:require [clj-time.core :as time]
            [clj-time.format :as format]
            [clojure.tools.logging :as log])
  (:import (java.util Locale)
           (org.joda.time DateTime LocalDate)))

(defn current-date-time ^DateTime [] (time/now))
(defn current-local-date ^LocalDate [] (time/today))

(defonce timezone-fi (time/time-zone-for-id "Europe/Helsinki"))

(defn formatter-for-helsinki [fmt-str] (format/formatter fmt-str timezone-fi))

(defonce kouta-date-time-formatter (formatter-for-helsinki "yyyy-MM-dd'T'HH:mm"))

(def finnish-format (formatter-for-helsinki "d.M.yyyy 'klo' HH:mm"))
(def swedish-format (formatter-for-helsinki "d.M.yyyy 'kl.' HH:mm"))
; Asetetaan US-locale, jotta AM/PM olisi järjestelmällisesti kirjoitettu isoilla kirjaimilla.
(def english-format (.withLocale (formatter-for-helsinki "MMM. d, yyyy 'at' hh:mm a z") Locale/US))

(defn- parse-date-time
  [s]
  (when s
    (let [fmt (format/formatter "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
      (try
        (time/to-time-zone (format/parse fmt s) timezone-fi)
        (catch Exception e
          (log/error (str "Unable to parse" s) e))))))

(defn format-localized-dates [date]
  (if-let [parsed (parse-date-time date)]
    {:fi (format/unparse finnish-format parsed)
     :sv (format/unparse swedish-format parsed)
     :en (format/unparse english-format parsed)}
    {}))

(defn current-date-formatted
  []
  (format/unparse-local-date (format/formatter "yyyy-MM-dd") (current-local-date)))

(defn ->kouta-date-time-string [date-time] (format/unparse kouta-date-time-formatter date-time))

(defn kouta-date-time-string->date-time [string] (format/parse kouta-date-time-formatter string))

(defn current-time-as-kouta-format
  []
  (->kouta-date-time-string (current-date-time)))

(defn add-days-to-kouta-date-time-string [string days]
  (-> (kouta-date-time-string->date-time string) (time/plus (time/days days)) (->kouta-date-time-string)))

(defn within? [gte time lt]
  (if (nil? lt)
    (time/after? time gte)
    (time/within? (time/interval gte lt) time)))

(defn currently-in-between? [gte lt]
  (if (nil? gte)
    false
    (within? gte (current-date-time) lt)))

(defn currently-after? [lt]
  (if (nil? lt)
    false
    (time/after? (current-date-time) lt)))

(defn kevat-date? [date] (< (time/month date) 8))
(defn is-kevat? [] (kevat-date? (current-local-date)))

(defn current-year [] (time/year (current-local-date)))
