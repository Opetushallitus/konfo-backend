(ns konfo-backend.index.osaamismerkki
  (:refer-clojure :exclude [get])
  (:require
   [konfo-backend.elastic-tools :refer [get-source search]]
   [konfo-backend.tools :refer [koodi-uri-no-version]]
   [konfo-backend.constants :refer [language-keys]]
   [clojure.string :as s]))

(defonce index "osaamismerkki")

(def osaamismerkki-search (partial search index))

(defn get
  [koodiuri]
  (get-source index (koodi-uri-no-version koodiuri)))

(defn- ->koodiuri-query
  [koodiuris]
  (let [terms (if (= 1 (count koodiuris))
                {:term {:koodiUri (first koodiuris)}}
                {:terms {:koodiUri (vec koodiuris)}})]
    {:bool {:must terms}}))

(defn parse-osaamismerkki-kuvaus-items
  [kuvaus-item-list entity-key]
  (let [translations (map #(-> %
                               (entity-key)
                               (select-keys language-keys))
                          kuvaus-item-list)]
    (into {} (for [language language-keys
                   :let [x (clojure.string/join ", " (remove nil? (map
                                                                    #(language %)
                                                                    translations)))]
                   :when (seq x)]
               {language (str x ".")}))))

(defn parse-osaamismerkki-kuvaus
  [osaamismerkki-data]
  {:osaamistavoitteet (parse-osaamismerkki-kuvaus-items
                        (:osaamistavoitteet osaamismerkki-data) :osaamistavoite)
   :arviointikriteerit (parse-osaamismerkki-kuvaus-items
                         (:arviointikriteerit osaamismerkki-data) :arviointikriteeri)})

(defn parse-kuvaukset
  [result]
  (into {} (->> (get-in result [:hits :hits])
                (map :_source)
                (map parse-osaamismerkki-kuvaus))))


(defn get-kuvaukset-by-osaamismerkki-koodiuris
  [koodiuris]
  (when (not-empty koodiuris)
    (let [koodiuris-without-version (map #(koodi-uri-no-version %) koodiuris)]
      (osaamismerkki-search parse-kuvaukset
                            :_source [:osaamistavoitteet
                                      :arviointikriteerit]
                            :size (count koodiuris-without-version)
                            :query (->koodiuri-query koodiuris-without-version)))))
