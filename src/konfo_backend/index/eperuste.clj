(ns konfo-backend.index.eperuste
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]))

;TODO tilan pitäisi olla "julkaistu" eikä "valmis"

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

(defn- kuvaukset-result-mapper
  [result]

  (defn- koulutus-result-mapper
    [source koulutus]
    {:nimi             (:nimi koulutus)
     :koulutuskoodiUri (:koulutuskoodiUri koulutus)
     :kuvaus           (:kuvaus source)
     :id               (:id source)})

  (defn- source-result-mapper
    [source]
    (vec (map #(koulutus-result-mapper source %) (:koulutukset source))))

  (vec (apply concat (map #(source-result-mapper (:_source %)) (:hits result)))))

(defn get-kuvaus-by-koulutuskoodi
  [koulutuskoodi-uri]
  (let [koulutuskoodi (koodi-uri-no-version koulutuskoodi-uri)]
    (eperuste-search (partial kuvaus-result-mapper koulutuskoodi)
                     :_source [:koulutukset.nimi, :koulutukset.koulutuskoodiUri :id, :kuvaus.fi :kuvaus.en :kuvaus.sv]
                     :query {:bool {:must {:term {:koulutukset.koulutuskoodiUri koulutuskoodi}},
                                    :filter {:term {:tila "valmis"}}}})))

(defn get-kuvaukset-by-koulutuskoodit
  [koulutuskoodi-uris]
  (let [koulutuskoodit (vec (distinct (map koodi-uri-no-version koulutuskoodi-uris)))]
    (eperuste-search kuvaukset-result-mapper
                     :_source [:koulutukset.nimi, :koulutukset.koulutuskoodiUri :id, :kuvaus.fi :kuvaus.en :kuvaus.sv]
                     :query {:bool {:must {:terms {:koulutukset.koulutuskoodiUri koulutuskoodit}},
                                    :filter {:term {:tila "valmis"}}}})))