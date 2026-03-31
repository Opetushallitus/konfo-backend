(ns konfo-backend.util.time
  (:require [clojure.tools.logging :as log])
  (:import (java.time LocalDate ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Locale)))

(defn current-date-time
  "Funktio nykyisen päivän hakemiseen, jotta tämän voi ylikirjoittaa testeissä."
  ^ZonedDateTime [] (ZonedDateTime/now (ZoneId/of "Europe/Helsinki")))

(defn current-local-date
  "Funktio nykyisen päivän hakemiseen, jotta tämän voi ylikirjoittaa testeissä."
  ^LocalDate [] (LocalDate/now (ZoneId/of "Europe/Helsinki")))

(defonce timezone-fi (ZoneId/of "Europe/Helsinki"))
(defonce timezone-utc (ZoneId/of "UTC"))

(defn formatter-for-helsinki [fmt-str] (-> (DateTimeFormatter/ofPattern fmt-str) (.withZone timezone-fi)))
(defn formatter-for-utc [fmt-str] (-> (DateTimeFormatter/ofPattern fmt-str) (.withZone timezone-utc)))

(defonce kouta-date-time-formatter (formatter-for-helsinki "yyyy-MM-dd'T'HH:mm"))

(def finnish-format (formatter-for-helsinki "d.M.yyyy 'klo' HH:mm"))
(def swedish-format (formatter-for-helsinki "d.M.yyyy 'kl.' HH:mm"))
; Asetetaan US-locale, jotta AM/PM olisi järjestelmällisesti kirjoitettu isoilla kirjaimilla.
(def english-format (.withLocale (formatter-for-helsinki "MMM. d, yyyy 'at' hh:mm a z") Locale/US))

(defn- parse-date-time
  ^ZonedDateTime [s]
  (when s
    (let [fmt (formatter-for-utc "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
      (try
        (-> (ZonedDateTime/parse s fmt) (.withZoneSameInstant timezone-fi))
        (catch Exception e
          (log/error (str "Unable to parse" s) e))))))

(defn format-localized-dates [date]
  (if-let [parsed (parse-date-time date)]
    {:fi (.format finnish-format parsed)
     :sv (.format swedish-format parsed)
     :en (.format english-format parsed)}
    {}))

(defn current-date-formatted
  []
  (.format (current-local-date) (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn ->kouta-date-time-string [date-time] (.format kouta-date-time-formatter date-time))

(defn kouta-date-time-string->date-time ^ZonedDateTime [string] (ZonedDateTime/parse string kouta-date-time-formatter))

(defn current-time-as-kouta-format
  []
  (->kouta-date-time-string (current-date-time)))

(defn add-days-to-kouta-date-time-string [string days]
  (-> (kouta-date-time-string->date-time string) (.plusDays days) (->kouta-date-time-string)))

(defn within? [^ZonedDateTime gte ^ZonedDateTime time ^ZonedDateTime lt]
  (let [after-or-equal-to-start (or (.isAfter time gte) (.isEqual time gte))
        before-end (if (nil? lt) true (.isBefore time lt))]
    (and after-or-equal-to-start before-end)))

(defn currently-in-between? [gte lt]
  (if (nil? gte)
    false
    (within? gte (current-date-time) lt)))

(defn currently-after? [lt]
  (if (nil? lt)
    false
    (-> (current-date-time) (.isAfter lt))))

(defn kevat-date? [date] (< (.getMonthValue date) 8))
(defn is-kevat? [] (kevat-date? (current-local-date)))

(defn current-year [] (.getYear (current-local-date)))
