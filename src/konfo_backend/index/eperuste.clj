(ns konfo-backend.index.eperuste
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]
    [konfo-backend.tools :refer [now-in-millis]]))

;TODO tilan pitäisi olla "julkaistu" eikä "valmis"

(defonce index "eperuste")

(def eperuste-search (partial search index))

(defn- voimassa?
  [eperuste]
  (when-let [alkaa (:voimassaoloAlkaa eperuste)]
    (if-let [lopppuu (:voimassaoloLoppuu eperuste)]
      (<= alkaa (now-in-millis) lopppuu)
      (<= alkaa (now-in-millis)))))

(defn- siirtyma?
  [eperuste]
  (when-let [siirtyma (:siirtymaPaattyy eperuste)]
    (<= (now-in-millis) siirtyma)))

(defn- valmis?
  [eperuste]
  (some-> eperuste :tila (= "valmis")))

(defn- get-koulutus
  [eperuste koulutus-koodi-uri]
  (->> eperuste
       :koulutukset
       (filter #(= (:koulutuskoodiUri %) koulutus-koodi-uri))
       (first)))

(defn- koulutus?
  [eperuste koulutus-koodi-uri]
  (not (nil? (get-koulutus eperuste koulutus-koodi-uri))))

(defn get
  [id]
  (when-let [eperuste (get-source index id)]
    (when (and (valmis? eperuste) (or (voimassa? eperuste) (siirtyma? eperuste)))
      eperuste)))

(defn- find-eperuste-for-koulutuskoodi
  [sources koulutus-koodi-uri]
  (when-let [eperusteet (filter #(koulutus? % koulutus-koodi-uri) sources)]
    (or (first (filter voimassa? eperusteet))
        (first (filter siirtyma? eperusteet)))))

(defn- get-kuvaus
  [koulutus-koodi-uri sources]
  (when-let [eperuste (find-eperuste-for-koulutuskoodi sources koulutus-koodi-uri)]
    (let [koulutus (get-koulutus eperuste koulutus-koodi-uri)]
      (merge (select-keys eperuste [:id :kuvaus :tyotehtavatJoissaVoiToimia :suorittaneenOsaaminen])
             (select-keys koulutus [:nimi :koulutuskoodiUri])))))

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

(defn- ->query
  [koulutuskoodit]
  (let [terms (if (= 1 (count koulutuskoodit))
                {:term  {:koulutukset.koulutuskoodiUri (first koulutuskoodit)}}
                {:terms {:koulutukset.koulutuskoodiUri (vec koulutuskoodit)}})]
    {:bool {:must terms, :filter {:term {:tila "valmis"}}}}))

(defn- ->id-query
  [eperuste-ids]
  (let [terms (if (= 1 (count eperuste-ids))
                {:term  {:id (first eperuste-ids)}}
                {:terms {:id (vec eperuste-ids)}})]
    {:bool {:must terms, :filter {:term {:tila "valmis"}}}}))



(defn- kuvaukset-result-mapper
  [koulutus-koodi-urit result]
  (when-let [sources (seq (map :_source (get-in result [:hits :hits])))]
    (->> (for [koulutus-koodi-uri koulutus-koodi-urit]
           (get-kuvaus koulutus-koodi-uri sources))
         (remove nil?)
         (vec))))

(defn get-kuvaus-by-koulutuskoodi
  [koulutuskoodi-uri]
  (let [koulutuskoodi (koodi-uri-no-version koulutuskoodi-uri)]
    (eperuste-search #(first (kuvaukset-result-mapper [koulutuskoodi] %))
                     :_source source
                     :query (->query [koulutuskoodi]))))

(defn get-kuvaukset-by-koulutuskoodit
  [koulutuskoodi-uris]
  (when-let [koulutuskoodit (seq (distinct (map koodi-uri-no-version koulutuskoodi-uris)))]
    (eperuste-search (partial kuvaukset-result-mapper koulutuskoodit)
                     :_source source
                     :size (* 10 (count koulutuskoodit))
                     :query (->query koulutuskoodit))))

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
