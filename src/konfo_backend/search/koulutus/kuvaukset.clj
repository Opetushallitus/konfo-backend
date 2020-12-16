(ns konfo-backend.search.koulutus.kuvaukset
  (:require
    [konfo-backend.tools :refer [not-blank? ammatillinen? koodi-uri-no-version amm-osaamisala? amm-tutkinnon-osa?]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.search.query :refer [query match-all-query aggregations inner-hits-query sorts external-query]]
    [konfo-backend.search.response :refer [parse parse-inner-hits parse-external]]
    [konfo-backend.elastic-tools :as e]
    [konfo-backend.index.eperuste :as eperuste]
    [konfo-backend.index.osaamisalakuvaus :as osaamisala]
    [konfo-backend.tools :refer [log-pretty]]))

(defn- select-amm-kuvaus
  [eperuste]
  (or (:suorittaneenOsaaminen eperuste) (:tyotehtavatJoissaVoiToimia eperuste) (:kuvaus eperuste)))

(defn- select-amm-tutkinnon-osa-kuvaus
  [tutkinnon-osa]
  (or (:ammattitaitovaatimukset tutkinnon-osa) (:ammattitaidonOsoittamistavat tutkinnon-osa)))

(defn- get-amm-kuvaukset
  [hits]
  (->> hits
       :hits
       (filter ammatillinen?)
       (filter #(some? (:eperuste %)))
       (map :eperuste)
       (set)
       (eperuste/get-kuvaukset-by-eperuste-ids)))

(defn- get-amm-osaamisala-kuvaukset
  [hits]
  (->> hits
       :hits
       (filter amm-osaamisala?)
       (filter #(some? (:eperuste %)))
       (map :eperuste)
       (set)
       (osaamisala/get-kuvaukset-by-eperuste-ids)))

(defn- get-amm-tutkinnon-osa-kuvaukset
  [hits]
  (->> hits
       :hits
       (filter amm-tutkinnon-osa?)
       (mapcat :tutkinnonOsat)
       (map :eperuste)
       (set)
       (eperuste/get-tutkinnon-osa-kuvaukset-by-eperuste-ids)))

(defn- find-amm-kuvaus
  [kuvaukset hit]
  (when-let [kuvaus (first (filter #(= (:id %) (:eperuste hit)) kuvaukset))]
    (select-amm-kuvaus kuvaus)))

(defn- find-amm-osaamisala-kuvaus
  [kuvaukset hit]
  (some->> kuvaukset
           (filter #(= (str (:eperuste-id %)) (str (:eperuste hit))))
           (filter #(= (:osaamisalakoodiUri %) (get-in hit [:osaamisala :koodiUri])))
           (first)
           :kuvaus))

(defn- find-amm-tutkinnon-osa-kuvaus
  [kuvaukset tutkinnon-osat]
  (some (fn [tutkinnon-osa] (some->> kuvaukset
                                    (filter #(= (:id %) (:eperuste tutkinnon-osa)))
                                    (first)
                                    :tutkinnonOsat
                                    (filter #(= (:koodiUri %) (get-in tutkinnon-osa [:tutkinnonOsat :koodiUri])))
                                    (first)
                                    (select-amm-tutkinnon-osa-kuvaus ))) tutkinnon-osat))

(defn with-kuvaukset
  [result]
  (let [amm-kuvaukset (get-amm-kuvaukset result)
        amm-osaamisala-kuvaukset (get-amm-osaamisala-kuvaukset result)
        amm-tutkinnon-osa-kuvaukset (get-amm-tutkinnon-osa-kuvaukset result)]
    (->> (for [hit (:hits result)]
           (cond
             (ammatillinen? hit)      (assoc hit :kuvaus (find-amm-kuvaus amm-kuvaukset hit))
             (amm-osaamisala? hit)    (assoc hit :kuvaus (find-amm-osaamisala-kuvaus amm-osaamisala-kuvaukset hit))
             (amm-tutkinnon-osa? hit) (assoc hit :kuvaus (find-amm-tutkinnon-osa-kuvaus amm-tutkinnon-osa-kuvaukset (:tutkinnonOsat hit)))
             :else                    hit))
         (vec)
         (assoc result :hits))))