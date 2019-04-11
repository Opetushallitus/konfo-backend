(ns konfo-backend.search.koulutus-search
  (:require
    [cheshire.core :as cheshire]
    [clojure.tools.logging :as log]
    [konfo-backend.tools :refer [current-time-as-iso-local-date-time-string hakuaika-kaynnissa?]]
    [konfo-backend.elastic-tools :refer [kouta-search]]))

(defonce index "koulutus-kouta-search")

(def koulutus-kouta-search (partial kouta-search index))

(defn not-blank?
  [str]
  (not (clojure.string/blank? str)))

(def debug-pretty true)

(defn log-pretty
  [json]
  (when debug-pretty
    (log/debug (cheshire/generate-string json {:pretty true}))))

(defn- constant-score-query
  [query]
  {:constant_score {
     :filter query,
     :boost 1 }})

(defn- multi-match-keyword
  [keyword lng]
  (when (not-blank? keyword)
    {:multi_match {
       :query keyword
       :fields [(str "toteutukset.koulutus.nimi." lng),
                (str "toteutukset.nimi." lng),
                (str "toteutukset.metadata.ammattinimikkeet." lng),
                (str "toteutukset.metadata.asiasanat." lng)]}}))

(defn- match-keyword
  [keyword lng]
  (when (not-blank? keyword)
    {:match { (clojure.core/keyword (str "nimi." lng)) keyword}}))

(defn- match-paikkakunta
  [paikkakunta lng]
  (when paikkakunta
    {:term {(keyword (str "toteutukset.tarjoajat.paikkakunta.nimi." lng ".keyword")) (clojure.string/lower-case paikkakunta)}}))

(defn- match-vain-haku-kaynnissa
  [vainHakuKaynnissa lng]
  (when (true? vainHakuKaynnissa)
    {:term {:toteutukset.hakuOnKaynnissa {:value (current-time-as-iso-local-date-time-string)}}}))

(defn- match-koulutustyyppi
  [koulutustyyppi lng]
  (when koulutustyyppi
    {:term {:toteutukset.koulutus.koulutustyyppi.keyword (clojure.string/lower-case koulutustyyppi)}}))

(defn- match-opetuskieli
  [opetuskieli lng]
  (when opetuskieli
    {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword (str "kieli_" (clojure.string/lower-case opetuskieli) "#*")}}))

(defn- constraint-filters
  [constraints lng]
  (let [paikkakunta    (match-paikkakunta (:paikkakunta constraints) lng)
        haku-kaynnissa (match-vain-haku-kaynnissa (:vainHakuKaynnissa constraints) lng)
        koulutustyyppi (match-koulutustyyppi (:koulutustyyppi constraints) lng)
        opetuskieli    (match-opetuskieli (:opetuskieli constraints) lng)]
    (vec (remove nil? (conj [{:term {:toteutukset.kielivalinta (clojure.string/lower-case lng)}}] paikkakunta haku-kaynnissa koulutustyyppi opetuskieli)))))

(defn- ->keyword-and-constraints-query
  [keyword constraints lng]
  { :bool (cond-> {}
                  (not-blank? keyword) (assoc :must (multi-match-keyword keyword lng))
                  true (assoc :filter (constraint-filters constraints lng)))})

(defn- wrap-with-nested-toteutus-query
  [query lng]
  {:nested {
    :path "toteutukset"
    :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet", "toteutukset.metadata.asiasanat", "toteutukset.hakuOnKaynnissa", "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi", "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]}
    :score_mode "max"
    :query query }})

(defn- wrap-with-function-score-query-weight-haku-kaynnissa
  [query lng]
  {:function_score {
    :query query
    :boost_mode "replace"
    :functions [{:filter (match-vain-haku-kaynnissa true lng), :weight 100}] }})

(defn- match-paikkakunta-koulutus
  [paikkakunta lng]
  (when paikkakunta
    {:term {(keyword (str "tarjoajat.paikkakunta.nimi." lng ".keyword")) (clojure.string/lower-case paikkakunta)}}))

(defn- match-koulutustyyppi-koulutus
  [koulutustyyppi lng]
  (when koulutustyyppi
    {:term {:koulutustyyppi.keyword (clojure.string/lower-case koulutustyyppi)}}))

(defn- koulutus-constraint-filters
  [constraints lng]
  (let [paikkakunta    (match-paikkakunta-koulutus (:paikkakunta constraints) lng)
        koulutustyyppi (match-koulutustyyppi-koulutus (:koulutustyyppi constraints) lng)]
    (vec (remove nil? (conj [{:term {:kielivalinta (clojure.string/lower-case lng)}}] paikkakunta koulutustyyppi)))))

(defn- ->koulutus-bool-query
  [keyword constraints lng]
  { :bool (cond-> {:must_not {:exists {:field "toteutukset"}}}
                  (not-blank? keyword) (assoc :must (match-keyword keyword lng))
                  true (assoc :filter (koulutus-constraint-filters constraints lng)))})

(defn- wrap-with-koulutus-bool-query
  [query keyword constraints lng]
  (if (and (or (not-blank? keyword) (not-blank? (:paikkakunta constraints)) (not-blank? (:koulutustyyppi constraints))) (not (true? (:vainHakuKaynnissa constraints))))
    {:bool { :should [query, (constant-score-query (->koulutus-bool-query keyword constraints lng))]}}
    query))

(defn koulutus-query
  [keyword lng constraints]
  (-> (->keyword-and-constraints-query keyword constraints lng)
      (wrap-with-function-score-query-weight-haku-kaynnissa lng)
      (wrap-with-nested-toteutus-query lng)
      (wrap-with-koulutus-bool-query keyword constraints lng)))

(defn do-query?
  [keyword constraints]
  (or (not-blank? keyword) (not (empty? constraints))))

(defn- parse-koulutus
  [koulutus]
  (-> (:_source koulutus)
      (assoc :asiasanat (distinct (apply concat (map #(get-in % [:_source :metadata :asiasanat]) (get-in koulutus [:inner_hits :toteutukset :hits :hits])))))
      (assoc :ammattinimikkeet (distinct (apply concat (map #(get-in % [:_source :metadata :ammattinimikkeet]) (get-in koulutus [:inner_hits :toteutukset :hits :hits])))))
      (assoc :alemmanKorkeakoulututkinnonOsaamisalat (distinct (map :nimi (apply concat (map #(get-in % [:_source :metadata :alemmanKorkeakoulututkinnonOsaamisalat]) (get-in koulutus [:inner_hits :toteutukset :hits :hits]))))))
      (assoc :ylemmanKorkeakoulututkinnonOsaamisalat (distinct (map :nimi (apply concat (map #(get-in % [:_source :metadata :ylemmanKorkeakoulututkinnonOsaamisalat]) (get-in koulutus [:inner_hits :toteutukset :hits :hits]))))))
      (assoc :hakuOnKaynnissa (not (empty? (filter true? (map hakuaika-kaynnissa? (distinct (apply concat (map #(get-in % [:_source :hakuOnKaynnissa]) (get-in koulutus [:inner_hits :toteutukset :hits :hits ])))))))))))

(defn- parse-result
  [result]
  (log-pretty result)
  (let [res (-> {}
               (assoc :total_count (:total result))
               (assoc :koulutukset (map parse-koulutus (:hits result))))]
  res))

(defn search
  [keyword lng page size & {:as constraints}]
    (when (do-query? keyword constraints)
      (let [query (koulutus-query keyword lng constraints)]
        (log-pretty query)
        (koulutus-kouta-search
          page
          size
          parse-result
          :_source ["oid", "nimi", "koulutustyyppi", "johtaaTutkintoon", "koulutus"],
          :sort [{:johtaaTutkintoon {:order "desc"}}, {:_score {:order "desc"}}, {(clojure.core/keyword (str "nimi." lng ".keyword")) {:order "asc" :unmapped_type "string"}}],
          :query query))))