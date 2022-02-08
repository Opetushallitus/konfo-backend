(ns konfo-backend.util.haku-auki
  (:require
    [clojure.walk :refer [prewalk]]
    [clj-time.format :as f]
    [clj-time.core :as t]
    [konfo-backend.tools :refer :all]
    ))

(defn with-is-haku-auki [data]
  (let [now (t/now)
        on-going? (fn [d]
                    (.isBefore d now))]
    (prewalk (fn [x]
               (if-let [alkaa (some->> (:alkaa x)
                                       (f/parse kouta-date-time-formatter))]
                 (let [is-alkanut? (on-going? alkaa)]
                   (assoc x :hakuAuki (boolean (if-let [loppuu (some->> (:paattyy x)
                                                                         (f/parse kouta-date-time-formatter))]
                                                  (let [is-loppunut? (on-going? loppuu)]
                                                    (and is-alkanut? (not is-loppunut?)))
                                                  is-alkanut?))))
                 x)) data)))
