(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]]
    [clj-time.format :as f])
  (:import (org.joda.time DateTime)))

(def FORMATTER (f/formatter "yyyy-MM-dd'T'HH:mm"))

(defn with-is-haku-auki [data]
  (let [now (DateTime/now)
        on-going? (fn [d ^DateTime]
                    (.isBefore d now))]
    (prewalk (fn [x]
               (if-let [alkaa (some->> (:alkaa x)
                                       (f/parse FORMATTER))]
                 (let [is-alkanut? (on-going? alkaa)]
                   (assoc x :haku-auki (boolean (if-let [loppuu (some->> (:loppuu x)
                                                                         (f/parse FORMATTER))]
                                                  (let [is-loppunut? (on-going? loppuu)]
                                                    (and is-alkanut? (not is-loppunut?)))
                                                  is-alkanut?))))
                 x)) data)))
