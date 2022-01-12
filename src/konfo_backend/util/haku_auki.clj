(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]])
  (:import [org.joda.time.format DateTimeFormat]
           (java.time LocalDate)))

(def DATE_FORMAT (DateTimeFormat/forPattern "yyyy-MM-dd'T'hh:mm"))

(defn with-is-haku-auki [data]
  (let [now (LocalDate/now)]
    (prewalk (fn [x]
               (if-let [alkaa (:alkaa x)]
                 (let [before-alkaa (.isBefore (.parseLocalTime DATE_FORMAT alkaa) now)]
                   (-> x
                       (assoc :haku-auki (boolean (if-let [loppuu (:loppuu x)]
                                                    (and before-alkaa (.isAfter
                                                                          (.parseLocalTime DATE_FORMAT loppuu) now))
                                                    before-alkaa)))))
                 x)) data)))