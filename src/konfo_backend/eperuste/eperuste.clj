(ns konfo-backend.eperuste.eperuste
  (:require [konfo-backend.index.eperuste :as eperuste-index]
            [konfo-backend.index.tutkinnonosa :as tutkinnonosa-index]
            [konfo-backend.index.osaamisalakuvaus :as osaamisalakuvaus-index]
            [konfo-backend.index.toteutussuunnitelma :as toteutussuunnitelma-index]
            [clojure.string :as string]))

(defn get-eperuste-by-id
  [id]
  (eperuste-index/get id))

(defn get-kuvaus-by-eperuste-id
  [id with-osaamisalakuvaukset?]
  (when-let [eperuste (some-> id eperuste-index/get)]
    (cond-> (select-keys eperuste [:id :kuvaus :tyotehtavatJoissaVoiToimia :suorittaneenOsaaminen])
      with-osaamisalakuvaukset? (assoc :osaamisalat (osaamisalakuvaus-index/get-kuvaukset-by-eperuste-id id)))))

(defn get-tutkinnonosa-by-id
  [id]
  (tutkinnonosa-index/get id))

(defn in?
  [value coll]
  (some #(= value %) coll))

(defn get-tutkinnonosa-kuvaukset
  [eperuste-id koodi-urit]
  (cond->> (some->> (eperuste-index/get-tutkinnon-osa-kuvaukset-by-eperuste-ids [eperuste-id])
                    (first)
                    :tutkinnonOsat
                    (filter #(= (:tila %) "valmis")))
    (seq koodi-urit) (filter #(in? (:koodiUri %) koodi-urit))))

(defn get-osaamisala-kuvaukset
  [eperuste-id koodi-urit]
  (cond->> (osaamisalakuvaus-index/get-kuvaukset-by-eperuste-id eperuste-id)
    (seq koodi-urit) (filter #(in? (:osaamisalakoodiUri %) koodi-urit))))

(defn- vaatimukset->html [vaatimukset lang]
  (let [items (keep #(get-in % [:vaatimus lang]) vaatimukset)]
    (when (seq items)
      (str "<ul>" (apply str (map #(str "<li>" % "</li>") items)) "</ul>"))))

(defn- structured->html [lang {:keys [kohde vaatimukset kohdealueet]}]
  (let [kohde-text       (get kohde lang)
        top-html         (vaatimukset->html vaatimukset lang)
        kohdealueet-html (some->> (seq kohdealueet)
                                  (keep (fn [{:keys [kuvaus vaatimukset]}]
                                          (let [kuvaus-text (get kuvaus lang)
                                                items-html  (vaatimukset->html vaatimukset lang)]
                                            (when (or kuvaus-text kohde-text items-html)
                                              (str (when kuvaus-text (str "<p>" kuvaus-text "</p>"))
                                                   (when kohde-text (str "<p>" kohde-text "</p>"))
                                                   items-html)))))
                                  (apply str))
        parts            (keep identity
                               [(when (and kohde-text (empty? kohdealueet))
                                  (str "<p>" kohde-text "</p>"))
                                top-html
                                (when-not (string/blank? kohdealueet-html) kohdealueet-html)])]
    (when (seq parts)
      (apply str parts))))

(defn- ->kielistetty-html [structured]
  (when structured
    (->> [:fi :sv :en]
         (keep (fn [lang]
                 (when-let [html (structured->html lang structured)]
                   [lang html])))
         (into {}))))

(defn- extract-ammattitaito-fields [osa-data]
  (let [omat (get-in osa-data [:tosa :omatutkinnonosa])]
    {:ammattitaidonosoittamistavat (:ammattitaidonosoittamistavat omat)
     :ammattitaitovaatimukset      (or (->kielistetty-html (:ammattitaitovaatimukset omat))
                                       (->kielistetty-html (:ammattitaitovaatimuksetlista omat)))}))

(defn enrich-paikalliset-tutkinnon-osat
  [paikalliset]
  (when (seq paikalliset)
    (let [ids              (distinct (map :opetussuunnitelmaId paikalliset))
          suunnitelmat-map (->> (toteutussuunnitelma-index/get-many ids)
                                (into {} (map (juxt :oid identity))))]
      (mapv (fn [osa]
              (let [suunnitelma (suunnitelmat-map (:opetussuunnitelmaId osa))
                    osa-data    (first (filter #(= (str (:id %)) (str (:tutkinnonosaId osa)))
                                               (:paikallisetTutkinnonOsat suunnitelma)))]
                (merge osa (extract-ammattitaito-fields osa-data))))
            paikalliset))))
