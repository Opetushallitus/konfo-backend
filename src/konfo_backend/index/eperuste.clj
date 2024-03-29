(ns konfo-backend.index.eperuste
  (:refer-clojure :exclude [get])
  (:require
   [konfo-backend.elastic-tools :refer [get-source search]]))

;TODO tilan pitäisi olla "julkaistu" eikä "valmis"

(defonce index "eperuste")

(def eperuste-search (partial search index))

(defn get
  [id]
  (get-source index id))

(defn- ->id-query
  [eperuste-ids]
  (let [terms (if (= 1 (count eperuste-ids))
                {:term  {:id (first eperuste-ids)}}
                {:terms {:id (vec eperuste-ids)}})]
    {:bool {:must terms}}))

(defn- parse-kuvaukset
  [result]
  (->> (get-in result [:hits :hits])
       (map :_source)
       (map #(select-keys % [:id :kuvaus :tyotehtavatJoissaVoiToimia :suorittaneenOsaaminen]))))

(defn get-kuvaukset-by-eperuste-ids
  [eperuste-ids]
  (when (not-empty eperuste-ids)
    (eperuste-search parse-kuvaukset
                     :_source [:koulutukset.nimi,
                               :koulutukset.koulutuskoodiUri
                               :id,
                               :kuvaus.fi
                               :kuvaus.en ;TODO Tarvitseeko näitä kieliä oikeasti eritellä? kts. nimi
                               :kuvaus.sv
                               :tyotehtavatJoissaVoiToimia.fi
                               :tyotehtavatJoissaVoiToimia.en
                               :tyotehtavatJoissaVoiToimia.sv
                               :suorittaneenOsaaminen.fi
                               :suorittaneenOsaaminen.en
                               :suorittaneenOsaaminen.sv
                               :voimassaoloAlkaa
                               :voimassaoloLoppuu
                               :siirtymaPaattyy]
                     :size (count eperuste-ids)
                     :query (->id-query eperuste-ids))))

(defn- parse-tutkinnon-osa-kuvaukset
  [result]
  (let [hits (->> (get-in result [:hits :hits]) (map :_source) (vec))]
    (if (seq hits)
      (vec (for [hit hits]
             (assoc hit :tutkinnonOsat (vec (remove #(= (:tila %) "poistettu") (:tutkinnonOsat hit))))))
      hits)))

(defn get-tutkinnon-osa-kuvaukset-by-eperuste-ids
  [eperuste-ids]
  (when (not-empty eperuste-ids)
    (eperuste-search parse-tutkinnon-osa-kuvaukset
                     :_source [:id
                               :tutkinnonOsat.id
                               :tutkinnonOsat.koodiUri
                               :tutkinnonOsat.tila
                               :tutkinnonOsat.ammattitaitovaatimukset2019
                               :tutkinnonOsat.ammattitaitovaatimukset.fi
                               :tutkinnonOsat.ammattitaitovaatimukset.sv
                               :tutkinnonOsat.ammattitaitovaatimukset.en
                               :tutkinnonOsat.ammattitaidonOsoittamistavat.fi
                               :tutkinnonOsat.ammattitaidonOsoittamistavat.sv
                               :tutkinnonOsat.ammattitaidonOsoittamistavat.en]
                     :size (count eperuste-ids)
                     :query (->id-query eperuste-ids))))
