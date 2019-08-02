(ns konfo-backend.search.koulutus.response
  (:require
    [konfo-backend.tools :refer [log-pretty]]
    [konfo-backend.search.tools :refer :all]
    [konfo-backend.tools :refer [hakuaika-kaynnissa?]]))

(defn- get-all
  [x keys]
  (apply concat (map #(get-in % keys) x)))

(defn- combine-all
  ([x keys]
   (distinct (get-all x keys)))
  ([x key keys]
   (distinct (map key (get-all x keys)))))

(defn- contains-true?
  [coll]
  (not (empty? (filter true? coll))))

(defn- parse-koulutus
  [koulutus]
  (let [toteutukset (map :_source (get-in koulutus [:inner_hits :toteutukset :hits :hits]))]
    (-> (:_source koulutus)
        (assoc :asiasanat (combine-all toteutukset [:metadata :asiasanat]))
        (assoc :ammattinimikkeet (combine-all toteutukset [:metadata :ammattinimikkeet]))
        (assoc :alemmanKorkeakoulututkinnonOsaamisalat (combine-all toteutukset :nimi [:metadata :alemmanKorkeakoulututkinnonOsaamisalat]))
        (assoc :ylemmanKorkeakoulututkinnonOsaamisalat (combine-all toteutukset :nimi [:metadata :ylemmanKorkeakoulututkinnonOsaamisalat]))
        (assoc :hakuOnKaynnissa (contains-true? (map hakuaika-kaynnissa? (get-all toteutukset [:hakuOnKaynnissa])))))))

(defn parse-response
  [response]
  (log-pretty response)
  (-> {}
      (assoc :total_count (:total response))
      (assoc :koulutukset (map parse-koulutus (:hits response)))))