(ns konfo-backend.koodisto.koodisto
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]
    [clojure.core.memoize :as memo]))

(defonce index-name "koodisto")

(defn- get-koodisto
  [koodisto]
  (get-source index-name koodisto))

(def get-koodisto-with-cache
  (memo/ttl get-koodisto {} :ttl/threshold (* 1000 60 5))) ;5 minuutin cache

(defn- list-koodit
  [koodisto]
  (vec (:koodit (get-koodisto-with-cache koodisto))))

(defn list-koodi-urit
  [koodisto]
  (if (= "koulutustyyppi" koodisto)
    (vector "koulutustyyppi_1", "koulutustyyppi_4", "koulutustyyppi_11", "koulutustyyppi_12")
    (vec (map :koodiUri (list-koodit koodisto)))))