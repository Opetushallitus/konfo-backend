(ns konfo-backend.search.koulutus.eperustedata
  (:require [konfo-backend.tools :refer [ammatillinen? amm-osaamisala? amm-tutkinnon-osa? osaamismerkki? koodi-uri-no-version]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.index.eperuste :as eperuste]
            [konfo-backend.index.osaamismerkki :as osaamismerkki]
            [clojure.string :as string]
            [konfo-backend.index.osaamisalakuvaus :as osaamisala]
            [konfo-backend.constants :refer [language-keys]]))

(defn- select-amm-kuvaus
  [eperuste]
  (or (:suorittaneenOsaaminen eperuste) (:tyotehtavatJoissaVoiToimia eperuste) (:kuvaus eperuste)))

(defn- trans [obj lang] (get-in obj [lang] ""))

(defn- translate-vaatimukset
  [kohde lang vaatimukset]
  (let [vaatimus-items (remove string/blank? (map (fn [v] (trans (:vaatimus v) lang)) vaatimukset))]
    (if (empty? vaatimus-items)
      ""
      (str (trans kohde lang) " " (string/join ", " vaatimus-items) "."))))

(defn- create-ammattitaitovaatimukset-translation
  [kohde vaatimukset kohdealueet lang]
  (let [result
        (string/trim
         (str (translate-vaatimukset kohde lang vaatimukset)
              " "
              (->> kohdealueet
                   (map (fn [ka]
                          (let [vaatimukset-translation
                                (translate-vaatimukset kohde lang (:vaatimukset ka))]
                            (if (string/blank? vaatimukset-translation)
                              ""
                              vaatimukset-translation))))
                   (string/join " "))))]
    (if (string/blank? result) nil result)))

(defn- strip-ammattitaitovaatimukset-html [html-str]
  (when (string? html-str)
    (-> html-str
        (string/replace #"<[^>]*>" #(case %1
                                      "</li>" ", "
                                      "</ul>" ". "
                                      " "))
        (string/replace #"(\s|\u008a)+" " ")
        (string/trim)
        (string/replace #",\.|(., \.)" ".")
        (string/replace #"\s," ","))))

(defn- generate-ammattitaitovaatimukset
  [tutkinnon-osa]
  (cond (:ammattitaitovaatimukset2019 tutkinnon-osa)
        (let [kohde (get-in tutkinnon-osa [:ammattitaitovaatimukset2019 :kohde])
              vaatimukset (get-in tutkinnon-osa [:ammattitaitovaatimukset2019 :vaatimukset] [])
              kohdealueet (get-in tutkinnon-osa [:ammattitaitovaatimukset2019 :kohdealueet] [])]
          {:fi (create-ammattitaitovaatimukset-translation kohde vaatimukset kohdealueet :fi)
           :sv (create-ammattitaitovaatimukset-translation kohde vaatimukset kohdealueet :sv)
           :en (create-ammattitaitovaatimukset-translation kohde vaatimukset kohdealueet :en)})
        ;Eperusteissa kaikilla tutkinnon osilla ei ole rakenteista ammattitaitovaatimukset2019-kenttää, 
        ;joten yritetään siivota html:ää sisältävä ammattitaitovaatimukset-kenttä tekstiksi.
        (:ammattitaitovaatimukset tutkinnon-osa)
        (->> (select-keys (:ammattitaitovaatimukset tutkinnon-osa) language-keys)
             (map (fn [[lng html]] [lng (strip-ammattitaitovaatimukset-html html)]))
             (into {}))))

(defn select-amm-tutkinnon-osa-kuvaus
  [tutkinnon-osa]
  (or (generate-ammattitaitovaatimukset tutkinnon-osa)
      (:ammattitaidonOsoittamistavat tutkinnon-osa)))

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

(defn- get-osaamismerkit-data
  [hits]
  (->> hits
       :hits
       (filter osaamismerkki?)
       (filter #(some? (:osaamismerkki %)))
       (map #(get % :osaamismerkki))
       (set)
       (osaamismerkki/get-kuvaukset-by-osaamismerkki-koodiuris)))

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
  (some (fn [tutkinnon-osa]
          (some->> kuvaukset
                   (filter #(= (:id %) (:eperuste tutkinnon-osa)))
                   (first)
                   :tutkinnonOsat
                   (filter #(= (:koodiUri %) (get-in tutkinnon-osa [:tutkinnonOsat :koodiUri])))
                   (first)
                   (select-amm-tutkinnon-osa-kuvaus)))
        tutkinnon-osat))


;Toimii koulutukselle, jolla on rikastettu tai rikastamaton koulutus-koodiurit
(defn get-first-koodi-uri
  [koulutus]
  (let [koulutukset (map :koodiUri (:koulutukset koulutus))
        koulutus-koodi-urit (if (seq koulutukset) koulutukset (:koulutuksetKoodiUri koulutus))]
      (first koulutus-koodi-urit))) ;Ainoastaan korkeakoulutuksilla voi olla useampi kuin yksi koulutusKoodi

;esim. pelastusalan koulutuksille syötetään tiedot käsin, koska ei ole ePerustetta
(defonce koulutus-koodit-without-eperuste ["koulutus_381501" "koulutus_381502" "koulutus_381503" "koulutus_381521"])

(defn amm-koulutus-with-eperuste? [koulutus]
  (if (and koulutus (ammatillinen? koulutus))
    (when-let [koulutus-koodiuri (get-first-koodi-uri koulutus)]
      (boolean (not-any? #(string/starts-with? koulutus-koodiuri %) koulutus-koodit-without-eperuste)))
    false))

(defn with-eperustedata
  [result]
  (let [amm-kuvaukset (get-amm-kuvaukset result)
        amm-osaamisala-kuvaukset (get-amm-osaamisala-kuvaukset result)
        amm-tutkinnon-osa-kuvaukset (get-amm-tutkinnon-osa-kuvaukset result)
        osaamismerkit (get-osaamismerkit-data result)]
    (->> (for [hit (:hits result)]
           (cond (amm-koulutus-with-eperuste? hit) (assoc hit :kuvaus (find-amm-kuvaus amm-kuvaukset hit))
                 (amm-osaamisala? hit) (assoc hit
                                              :kuvaus (find-amm-osaamisala-kuvaus amm-osaamisala-kuvaukset hit))
                 (amm-tutkinnon-osa? hit) (assoc hit
                                                 :kuvaus
                                                 (find-amm-tutkinnon-osa-kuvaus
                                                   amm-tutkinnon-osa-kuvaukset
                                                   (:tutkinnonOsat hit)))
                 (osaamismerkki? hit) (let [osaamismerkki-id (:osaamismerkki hit)
                                            osaamismerkki (if (nil? osaamismerkki-id)
                                                            {}
                                                            ((keyword (koodi-uri-no-version osaamismerkki-id)) osaamismerkit))]
                                        (-> hit
                                            (assoc :kuvaus (:kuvaus osaamismerkki))
                                            (assoc :kuvake (:kuvake osaamismerkki))))
                 :else hit))
         (vec)
         (assoc result :hits))))
