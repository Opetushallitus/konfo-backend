(ns konfo-backend.search.koulutus.query
  (:require
    [konfo-backend.tools :refer [not-blank?]]
    [konfo-backend.search.tools :refer :all]
    [clojure.string :refer [lower-case]]
    [konfo-backend.tools :refer [current-time-as-kouta-format hakuaika-kaynnissa? ->koodi-with-version-wildcard ->lower-case-vec]]))

(defonce source-fields
  ["oid",
   "nimi",
   "koulutustyyppi",
   "johtaaTutkintoon",
   "koulutus"])

(defn sort
  [lng]
  [{:johtaaTutkintoon {:order "desc"}},
   {:_score {:order "desc"}},
   {(->lng-keyword "nimi.%s.keyword" lng) {:order "asc" :unmapped_type "string"}}])

(defonce toteutus-source-fields
  ["toteutukset.metadata.ammattinimikkeet",
   "toteutukset.metadata.asiasanat",
   "toteutukset.hakuOnKaynnissa",
   "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi",
   "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"])

(defn- toteutus-keyword-match-fields
  [lng]
  [(str "toteutukset.koulutus.nimi." lng),
   (str "toteutukset.nimi." lng),
   (str "toteutukset.metadata.ammattinimikkeet." lng),
   (str "toteutukset.metadata.asiasanat." lng)])

(defn- create-toteutus-query
  [keyword filters lng]
  {:nested {:path "toteutukset"
            :inner_hits {:_source toteutus-source-fields}
            :score_mode "max"
            :query {:function_score {:query { :bool (cond-> {:filter (vec (concat [{:term {:toteutukset.kielivalinta (lower-case lng)}}] filters))}
                                                            (not-blank? keyword) (assoc :must {:multi_match {:query keyword
                                                                                                             :fields (toteutus-keyword-match-fields lng)}}) )}
                                     :boost_mode "replace"
                                     :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value (current-time-as-kouta-format)}}}, :weight 100}] }} }})

(defn- create-toteutus-filters
  [constraints lng]
  (vec (remove nil? (conj []
                          (when (paikkakunta? constraints)         {:term {(->lng-keyword "toteutukset.tarjoajat.paikkakunta.nimi.%s.keyword" lng) (lower-case (:paikkakunta constraints))}})
                          (when (koulutustyyppi? constraints)      (if (= 1 (count (:koulutustyyppi constraints)))
                                                                     {:term {:toteutukset.koulutus.koulutustyyppi.keyword (lower-case (first (:koulutustyyppi constraints)))}}
                                                                     {:terms {:toteutukset.koulutus.koulutustyyppi.keyword (->lower-case-vec (:koulutustyyppi constraints))}}))
                          (when (opetuskieli? constraints)         (let [wildcards (map (fn [x] {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword (->koodi-with-version-wildcard x)}}) (:opetuskieli constraints))]
                                                                     (if (= 1 (count wildcards))
                                                                       (first wildcards)
                                                                       {:bool {:should (vec wildcards) :minimum_should_match 1}})))
                          (when (vain-haku-kaynnissa? constraints) {:term {:toteutukset.hakuOnKaynnissa {:value (current-time-as-kouta-format)}}})))))

(defn- create-koulutus-query
  [keyword filters lng]
  {:constant_score {:boost 1
                    :filter { :bool (cond-> {:must_not {:exists {:field "toteutukset"}}
                                             :filter (vec (concat [{:term {:kielivalinta (lower-case lng)}}] filters))}
                                            (not-blank? keyword) (assoc :must {:match {(->lng-keyword "nimi.%s" lng) keyword}}))}}})

(defn- create-koulutus-filters
  [constraints lng]
  (vec (remove nil? (conj []
                          (when (paikkakunta? constraints)    {:term {(->lng-keyword "tarjoajat.paikkakunta.nimi.%s.keyword" lng) (lower-case (:paikkakunta constraints))}})
                          (when (koulutustyyppi? constraints) (if (= 1 (count (:koulutustyyppi constraints)))
                                                                {:term {:koulutustyyppi.keyword (lower-case (first (:koulutustyyppi constraints)))}}
                                                                {:terms {:koulutustyyppi.keyword (->lower-case-vec (:koulutustyyppi constraints))}}))))))

(defn- query-koulutukset?
  [keyword constraints]
  (and (not (vain-haku-kaynnissa? constraints))
       (not (opetuskieli? constraints))
       (or (not-blank? keyword) (paikkakunta? constraints) (koulutustyyppi? constraints))))

(defn create-query
  [keyword lng constraints]
  (let [toteutus-query (create-toteutus-query keyword (create-toteutus-filters constraints lng) lng)]
    (if (query-koulutukset? keyword constraints)
      {:bool { :should [toteutus-query, (create-koulutus-query keyword (create-koulutus-filters constraints lng) lng)]}}
      toteutus-query)))