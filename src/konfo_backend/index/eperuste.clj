(ns konfo-backend.index.eperuste
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]
    [konfo-backend.tools :refer [now-in-millis]]))

;TODO tilan pitäisi olla "julkaistu" eikä "valmis"

(defonce index "eperuste")

(def eperuste-search (partial search index))

(defn get
  [id]
  (get-source index id))

(defonce source [:koulutukset.nimi,
                 :koulutukset.koulutuskoodiUri
                 :id,
                 :kuvaus.fi
                 :kuvaus.en
                 :kuvaus.sv
                 :tyotehtavatJoissaVoiToimia.fi
                 :tyotehtavatJoissaVoiToimia.en
                 :tyotehtavatJoissaVoiToimia.sv
                 :suorittaneenOsaaminen.fi
                 :suorittaneenOsaaminen.en
                 :suorittaneenOsaaminen.sv
                 :voimassaoloAlkaa
                 :voimassaoloLoppuu
                 :siirtymaPaattyy])

(defn- ->id-query
  [eperuste-ids]
  (let [terms (if (= 1 (count eperuste-ids))
                {:term  {:id (first eperuste-ids)}}
                {:terms {:id (vec eperuste-ids)}})]
    {:bool {:must terms, :filter {:term {:tila "valmis"}}}}))

(defn- parse-kuvaukset
  [result]
  (->> (get-in result [:hits :hits])
       (map :_source)
       (map #(select-keys % [:id :kuvaus :tyotehtavatJoissaVoiToimia :suorittaneenOsaaminen]))))

(defn get-kuvaukset-by-eperuste-ids
  [eperuste-ids]
  (eperuste-search parse-kuvaukset
                   :_source source
                   :size (count eperuste-ids)
                   :query (->id-query eperuste-ids)))
