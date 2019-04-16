(ns konfo-backend.index.eperuste
  (:require
    [konfo-backend.tools :refer [julkaistut julkaistu?]]
    [konfo-backend.elastic-tools :refer [get-source search]]))

(defonce index "eperuste")

(def eperuste-search (partial search index))

(defn get
  [id]
  (let [eperuste (get-source index id)]
    (when (some-> eperuste :tila (= "valmis"))
      eperuste)))

(defn- kuvaus-result-mapper
  [koulutuskoodi result]
  (when-let [source (some-> result :hits first :_source)]
    (let [koulutus (first (filter #(= (:koulutuskoodiUri %) koulutuskoodi) (:koulutukset source)))]
      {:nimi             (:nimi koulutus)
       :koulutuskoodiUri (:koulutuskoodiUri koulutus)
       :kuvaus           (:kuvaus source)
       :id               (:id source)})))

(defn get-by-koulutuskoodi
  [koulutuskoodi-uri]
  (let [koulutuskoodi (first (clojure.string/split koulutuskoodi-uri #"#"))]
    (eperuste-search (partial kuvaus-result-mapper koulutuskoodi)
                     :_source [:koulutukset.nimi, :koulutukset.koulutuskoodiUri :id, :kuvaus.fi :kuvaus.en :kuvaus.sv]
                     :query {:bool {:must {:term {:koulutukset.koulutuskoodiUri koulutuskoodi}},
                                    :filter {:term {:tila "valmis"}}}})))