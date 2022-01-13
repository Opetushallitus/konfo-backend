(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]])
  (:import [org.joda.time.format DateTimeFormat]
           (java.time LocalDateTime)))

(def DATE_FORMAT (DateTimeFormat/forPattern "yyyy-MM-dd'T'hh:mm"))

(defn with-is-haku-auki [data]
  (let [now (LocalDateTime/now)]
    (prewalk (fn [x]
               (if-let [alkaa (:alkaa x)]
                 (let [before-alkaa (.isBefore (.parseLocalDateTime DATE_FORMAT alkaa) now)]
                   (-> x
                       (assoc :haku-auki (boolean (if-let [loppuu (:loppuu x)]
                                                    (and before-alkaa (.isAfter
                                                                          (.parseLocalDateTime DATE_FORMAT loppuu) now))
                                                    before-alkaa)))))
                 x)) data)))