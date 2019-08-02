(ns konfo-backend.old-search.toteutus
  (:require
    [konfo-backend.elastic-tools :refer :all]))

(def koulutukset (partial old-search "koulutus"))

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
     :koulutusOid (:komoOid koulutus)
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

(defn- match-keyword [keyword lng]
  { :dis_max { :queries [(constant_score_query_multi_match keyword [(str "searchData.nimi.kieli_" lng) ] 10)
                         (constant_score_query_multi_match keyword [(str "tutkintonimikes.nimi.kieli_" lng) (str "koulutusala.nimi.kieli_" lng) (str "tutkinto.nimi.kieli_" lng)] 5)
                         (constant_score_query_multi_match keyword [(str "aihees.nimi.kieli_" lng) (str "searchData.oppiaineet.kieli_" lng) (str "ammattinimikkeet.nimi.kieli_" lng)] 4)
                         (constant_score_query_multi_match keyword [(str "searchData.organisaatio.nimi.kieli_" lng)] 2)]}})

(defn- match-koulutustyyppi [koulutustyyppi]
  (constant_score_query_terms :searchData.tyyppi (clojure.string/split koulutustyyppi #",") 10))

(defn- match-opetuskieli [kieli]
  (constant_score_query_terms :opetuskielis.uri (clojure.string/split kieli #",") 10))

(defn- match-oids [oids]
  (constant_score_query_terms :searchData.organisaatio.oid (vec oids) 10))

(defn koulutus-query
  [keyword lng oids constraints]
  (let [koulutustyyppi (:koulutustyyppi constraints)
        kieli (:kieli constraints)
        keyword-search? (not (clojure.string/blank? keyword))
        koulutustyyppi-search? (and koulutustyyppi (not keyword-search?))
        kieli-search? (and kieli (not keyword-search?) (not koulutustyyppi-search?))
        oids? (not-empty oids)
        oids-search? (and oids? (not keyword-search?) (not koulutustyyppi-search?) (not kieli-search?))]

        { :bool (merge (if keyword-search? {:must (match-keyword keyword lng)} {})
                       (if koulutustyyppi-search? {:must (match-koulutustyyppi koulutustyyppi)} {})
                       (if kieli-search? {:must (match-opetuskieli kieli)} {})
                       (if oids-search? {:must (match-oids oids)} {})
                       { :must_not { :range { :searchData.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}} }
                       { :filter (remove nil?
                                         [(if (and oids? (not oids-search?)) { :terms { :searchData.organisaatio.oid (vec oids) }})
                                          (if (and kieli (not kieli-search?)) { :terms { :opetuskielis.uri (clojure.string/split kieli #",") }})
                                          (if (and koulutustyyppi (not koulutustyyppi-search?)) { :terms { :searchData.tyyppi (clojure.string/split koulutustyyppi #",") }})
                                          {:match { :tila "JULKAISTU" }}
                                          {:match { :searchData.haut.tila "JULKAISTU"}}])})}))

(defn filter-organisaatio-oids
  [keyword lng constraints]
  (if (and (not keyword) (not (:koulutustyyppi constraints)) (not (:kieli constraints)))
    []
    (koulutukset (query-perf-string "koulutus" keyword constraints)
                 0
                 10000
                 (fn [x] (map #(get-in % [:_source :organisaatio :oid]) (:hits x)))
                 :query (koulutus-query keyword lng [] constraints)
                 :_source ["organisaatio.oid"])))

(defn filter-komo-oids
  [keyword lng oids constraints]
  (if (and (not keyword) (not (:koulutustyyppi constraints)) (not (:kieli constraints)))
    []
    (koulutukset (query-perf-string "koulutus" keyword constraints)
                 0
                 10000
                 (fn [x] (map :_source (:hits x)))
                 :query (koulutus-query keyword lng oids constraints)
                 :_source ["searchData.haut.hakuaikas", "searchData.hakukohteet.hakuaika", "searchData.tyyppi", "komoOid", "searchData.nimi", "organisaatio", "aihees", "oid"])))

(defn text-search
  [keyword lng page size oids constraints]
  (if (and (:paikkakunta constraints) (empty? oids))
    {:count 0 :result []}
    (koulutukset (query-perf-string "koulutus" keyword constraints)
                 page
                 size
                 create-hakutulokset
                 :query (koulutus-query keyword lng oids constraints)
                 :_source ["oid", "koulutustyyppi", "organisaatio", "isAvoimenYliopistonKoulutus", "searchData.tyyppi", "komoOid",
                           "johtaaTutkintoon", "aihees.nimi", "searchData.nimi", "searchData.haut.hakuaikas", "searchData.hakukohteet.hakuaika"]
                 :sort [{ :johtaaTutkintoon :asc },
                        :_score,
                        { (clojure.core/keyword (str "searchData.nimi.kieli_" lng ".keyword")) :asc},
                        { (clojure.core/keyword (str "searchData.organisaatio.nimi.kieli_" lng ".keyword")) :asc}])))