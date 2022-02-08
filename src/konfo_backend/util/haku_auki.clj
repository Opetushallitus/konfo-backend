(ns konfo-backend.util.haku-auki
  (:require
   [clojure.walk :refer [prewalk]]
   [konfo-backend.tools :refer :all]))

(defn with-is-haku-auki [data]
  (prewalk (fn [x]
             (if (not (nil? (:alkaa x)))
               (assoc x :hakuAuki (hakuaika-kaynnissa? x))
               x))
           data))
