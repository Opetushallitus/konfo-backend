(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]]
    [clj-time.format :as f])
  (:import (org.joda.time DateTime)))

(def FORMATTER (f/formatter "yyyy-MM-dd'T'HH:mm"))

(defn with-is-haku-auki [data]
  (let [now (DateTime/now)]
    (prewalk (fn [x]
               (if-let [alkaa (some->> (:alkaa x)
                                       (f/parse FORMATTER))]
                 (let [before-alkaa (.isAfter alkaa now)]
                   (-> x
                       (assoc :haku-auki (boolean (if-let [loppuu (some->> (:loppuu x)
                                                                           (f/parse FORMATTER))]
                                                    (and before-alkaa (.isBefore loppuu now))
                                                    before-alkaa)))))
                 x)) data)))
