(ns konfo-backend.koodisto.koodisto
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]))

(defonce index-name "koodisto")

(defn get-koodisto
  [koodisto]
  (get-source index-name koodisto))

(defn list-koodit
  [koodisto]
  (vec (:koodit (get-koodisto koodisto))))

(defn list-koodi-urit
  [koodisto]
  (if (= "koulutustyyppi" koodisto)
    (vector "koulutustyyppi_1", "koulutustyyppi_4", "koulutustyyppi_11", "koulutustyyppi_12") ;TODO: fixme
    (vec (map :koodiUri (list-koodit koodisto)))))