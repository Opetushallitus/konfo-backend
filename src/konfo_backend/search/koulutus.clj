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

(defn koulutus-query
  ([keyword oids constraints]
    { :bool {
           :must { :dis_max { :queries [(constant_score_query_multi_match keyword ["searchData.nimi.kieli_fi"] 10)
                                        (constant_score_query_multi_match keyword ["tutkintonimikes.nimi.kieli_fi" "koulutusala.nimi.kieli_fi" "tutkinto.nimi.kieli_fi"] 5)
                                        (constant_score_query_multi_match keyword ["aihees.nimi.kieli_fi" "searchData.oppiaineet.kieli_fi" "ammattinimikkeet.nimi.kieli_fi"] 4)
                                        (constant_score_query_multi_match keyword ["searchData.organisaatio.nimi.kieli_fi"] 2)]}},
           :filter (remove nil?
                           [(if (not-empty oids) { :terms { :searchData.organisaatio.oid (vec oids) }})
                            (if-let [koulutustyyppi (:koulutustyyppi constraints)] { :terms { :searchData.tyyppi (clojure.string/split koulutustyyppi #",") }})
                            {:match { :tila "JULKAISTU" }}
                            {:match { :searchData.haut.tila "JULKAISTU"}}]),
           :must_not { :range { :searchData.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}}
           }})
  ([keyword]
    (koulutus-query keyword nil nil)))

(defn oid-search
  [keyword constraints]
  (koulutukset keyword
               0
               10000
               (fn [x] (map #(get-in % [:_source :organisaatio :oid]) (:hits x)))
               :query (koulutus-query keyword [] constraints)
               :_source ["organisaatio.oid"]))

(defn text-search
  [keyword page size oids constraints]
  (koulutukset keyword
               page
               size
               create-hakutulokset
               :query (koulutus-query keyword oids constraints)
               :_source ["oid", "koulutustyyppi", "organisaatio", "isAvoimenYliopistonKoulutus", "searchData.tyyppi",
                         "johtaaTutkintoon", "aihees.nimi", "searchData.nimi", "searchData.haut.hakuaikas"]
               :sort [{ :johtaaTutkintoon :asc },
                      :_score,
                      { :searchData.nimi.kieli_fi.keyword :asc},
                      { :searchData.organisaatio.nimi.kieli_fi.keyword :asc}]))