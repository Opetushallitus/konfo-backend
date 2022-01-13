(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]]
    [clj-time.format :as f])
  (:import (org.joda.time DateTime)))

(def FORMATTER (f/formatter "yyyy-MM-dd'T'hh:mm"))

(defn with-is-haku-auki [data]
  (let [now (DateTime/now)]
    (prewalk (fn [x]
               (if-let [alkaa (some->> (:alkaa x)
                                       (f/parse FORMATTER))]
                 (let [before-alkaa (.isBefore alkaa now)]
                   (-> x
                       (assoc :haku-auki (boolean (if-let [loppuu (some->> (:loppuu x)
                                                                           (f/parse FORMATTER))]
                                                    (and before-alkaa (.isAfter loppuu now))
                                                    before-alkaa)))))
                 x)) data)))
