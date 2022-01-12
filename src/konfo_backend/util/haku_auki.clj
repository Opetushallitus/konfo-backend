(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]])
  (:import [org.joda.time.format DateTimeFormat]
           [org.joda.time DateTime DateTimeZone]))

(def DATE_FORMAT (DateTimeFormat/forPattern "yyyy-MM-dd'T'hh:mm"))
(def ZONE (DateTimeZone/forID "Europe/Helsinki"))

(defn with-is-haku-auki [data]
  (let [now (DateTime/now)]
    (prewalk (fn [x]
               (if-let [alkaa (:alkaa x)]
                 (let [before-alkaa (.isBefore (.withZone (.parseDateTime DATE_FORMAT alkaa) ZONE) now)]
                   (-> x
                       (assoc :haku-auki (boolean (if-let [loppuu (:loppuu x)]
                                                    (and before-alkaa (.isAfter
                                                                        (.withZone
                                                                          (.parseDateTime DATE_FORMAT loppuu)
                                                                          ZONE) now))
                                                    before-alkaa)))))
                 x)) data)))