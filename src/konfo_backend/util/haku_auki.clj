(ns konfo-backend.util.haku-auki
  (:require
   [clojure.walk :refer [prewalk]]
   [konfo-backend.tools :refer :all]))

(defn with-is-haku-auki [data]
  (prewalk (fn [x]
             (if (not (nil? (:alkaa x)))
               (-> x
                 (assoc :hakuAuki (hakuaika-kaynnissa? x))
                 (assoc :hakuMennyt (hakuaika-menneisyydessa? x)))
               x))
           data))
