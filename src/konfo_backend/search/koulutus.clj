(ns konfo-backend.search.koulutus
  (:require
    [konfo-backend.elastic-tools :refer :all]))

(def koulutukset (partial search "koulutus"))

(defn- hakuaika-nyt [hakuaika]
  (let [alkuPvm (:alkuPvm hakuaika)
        loppuPvm (if (:loppuPvm hakuaika) (:loppuPvm hakuaika) (+ (System/currentTimeMillis) 100000))]
    (<= alkuPvm (System/currentTimeMillis) loppuPvm)))

(defn haettavissa [searchData]
  (if (some true? (map #(contains? % :hakuaika) (:hakukohteet searchData)))
    (some true? (map #(hakuaika-nyt (:hakuaika %)) (:hakukohteet searchData)))
    (some true? (map #(hakuaika-nyt %) (flatten (map #(:hakuaikas %) (:haut searchData)))))))

(defn- create-hakutulos [koulutushakutulos]
  (let [koulutus (:_source koulutushakutulos)
        score (:_score koulutushakutulos)]
    {:score score
     :oid (:oid koulutus)
     :nimi (get-in koulutus [:searchData :nimi])
     :tarjoaja (get-in koulutus [:organisaatio :nimi])
     :avoin (:isAvoimenYliopistonKoulutus koulutus)
     :koulutustyyppi (:uri (:koulutustyyppi koulutus))
     :tyyppi (get-in koulutus [:searchData :tyyppi])
     :johtaaTutkintoon (:johtaaTutkintoon koulutus)
     :aiheet (:aihees koulutus)
     :haettavissa (haettavissa (:searchData koulutus))}))

(defn- create-hakutulokset [hakutulos]
  (let [result (:hits hakutulos)
        count (:total hakutulos)]
    {:count count
     :result (map create-hakutulos result)}))

(defn- match-keyword [keyword]
  { :dis_max { :queries [(constant_score_query_multi_match keyword ["searchData.nimi.kieli_fi"] 10)
                         (constant_score_query_multi_match keyword ["tutkintonimikes.nimi.kieli_fi" "koulutusala.nimi.kieli_fi" "tutkinto.nimi.kieli_fi"] 5)
                         (constant_score_query_multi_match keyword ["aihees.nimi.kieli_fi" "searchData.oppiaineet.kieli_fi" "ammattinimikkeet.nimi.kieli_fi"] 4)
                         (constant_score_query_multi_match keyword ["searchData.organisaatio.nimi.kieli_fi"] 2)]}})

(defn- match-koulutustyyppi [koulutustyyppi]
  (constant_score_query_terms :searchData.tyyppi (clojure.string/split koulutustyyppi #",") 10))

(defn- match-oids [oids]
  (constant_score_query_terms :searchData.organisaatio.oid (vec oids) 10))

(defn koulutus-query
  [keyword oids constraints]
  (let [koulutustyyppi (:koulutustyyppi constraints)
        keyword-search? (not (clojure.string/blank? keyword))
        koulutustyyppi-search? (and koulutustyyppi (not keyword-search?))
        oids? (not-empty oids)
        oids-search? (and oids? (not keyword-search?) (not koulutustyyppi-search?))]

        { :bool (merge (if keyword-search? {:must (match-keyword keyword)} {})
                       (if koulutustyyppi-search? {:must (match-koulutustyyppi koulutustyyppi)} {})
                       (if oids-search? {:must (match-oids oids)} {})
                       { :must_not { :range { :searchData.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}} }
                       { :filter (remove nil?
                                         [(if (and oids? (not oids-search?)) { :terms { :searchData.organisaatio.oid (vec oids) }})
                                          (if (and koulutustyyppi (not koulutustyyppi-search?)) { :terms { :searchData.tyyppi (clojure.string/split koulutustyyppi #",") }})
                                          {:match { :tila "JULKAISTU" }}
                                          {:match { :searchData.haut.tila "JULKAISTU"}}])})}))

(defn oid-search
  [keyword constraints]
  (if (and (not keyword) (not (:koulutustyyppi constraints)))
    []
    (koulutukset (query-perf-string "koulutus" keyword constraints)
                 0
                 10000
                 (fn [x] (map #(get-in % [:_source :organisaatio :oid]) (:hits x)))
                 :query (koulutus-query keyword [] constraints)
                 :_source ["organisaatio.oid"])))

(defn text-search
  [keyword page size oids constraints]
  (if (and (:paikkakunta constraints) (empty? oids))
    {:count 0 :result []}
    (koulutukset (query-perf-string "koulutus" keyword constraints)
                 page
                 size
                 create-hakutulokset
                 :query (koulutus-query keyword oids constraints)
                 :_source ["oid", "koulutustyyppi", "organisaatio", "isAvoimenYliopistonKoulutus", "searchData.tyyppi",
                           "johtaaTutkintoon", "aihees.nimi", "searchData.nimi", "searchData.haut.hakuaikas"]
                 :sort [{ :johtaaTutkintoon :asc },
                        :_score,
                        { :searchData.nimi.kieli_fi.keyword :asc},
                        { :searchData.organisaatio.nimi.kieli_fi.keyword :asc}])))