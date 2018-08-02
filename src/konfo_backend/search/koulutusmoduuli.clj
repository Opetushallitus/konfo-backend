(ns konfo-backend.search.koulutusmoduuli
  (:require
    [konfo-backend.elastic-tools :refer :all]
    [clojure.tools.logging :as log]))

(def koulutusmoduulit (partial search "koulutusmoduuli"))

(defn- match-keyword [keyword lng]
  { :dis_max { :queries [(constant_score_query_multi_match keyword [(str "searchData.nimi.kieli_" lng) ] 10)
                         (constant_score_query_multi_match keyword [(str "tutkintonimikes.nimi.kieli_" lng) (str "koulutusala.nimi.kieli_" lng) (str "tutkinto.nimi.kieli_" lng)] 5)
                         ; TODO: ei löydy koulutusmoduulilta
                         ; (constant_score_query_multi_match keyword [(str "aihees.nimi.kieli_" lng) (str "searchData.oppiaineet.kieli_" lng) (str "ammattinimikkeet.nimi.kieli_" lng)] 4)
                         (constant_score_query_multi_match keyword [(str "searchData.organisaatio.nimi.kieli_" lng)] 2)]}})

(defn- match-koulutustyyppi [koulutustyyppi]
  (constant_score_query_terms :searchData.tyyppi (clojure.string/split koulutustyyppi #",") 10))

(defn- match-opetuskieli [kieli]
  (constant_score_query_terms :opetuskielis.uri (clojure.string/split kieli #",") 10))

(defn- match-oids [oids]
  (constant_score_query_terms :oid (vec oids) 10))

(defn- create-hakutulos [komohakutulos]
  (let [komo (:_source komohakutulos)
        score (:_score komohakutulos)]
    {:score      score
     :oid        (:oid komo)
     :nimi       (get-in komo [:searchData :nimi])
     :tila        (:tila komo)
     :komotyyppi (:koulutusmoduuliTyyppi komo)
     :tyyppi     (get-in komo [:searchData :tyyppi])}))

(defn- create-hakutulokset [hakutulos]
  (let [result (:hits hakutulos)
        count (:total hakutulos)]
    {:count count
     :result (map create-hakutulos result)}))

(defn koulutusmoduuli-query
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
                   {:filter {:match { :tila "JULKAISTU" }}}
                   ; TODO: opintopolunNayttaminenLoppuu
                   ; { :must_not { :range { :searchData.opintopolunNayttaminenLoppuu { :format "yyyy-MM-dd" :lt "now"}}} }
                   )}))

(comment defn text-search
  [keyword lng page size oids constraints]
  (if (empty? oids)
    {:count 0 :result []}
    (koulutusmoduulit (query-perf-string "koulutusmoduuli" keyword constraints)
                 page
                 size
                 create-hakutulokset
                 :query (koulutusmoduuli-query keyword lng oids constraints)
                 :_source ["searchData.nimi", "oid", "koulutusmoduuliTyyppi", "searchData.tyyppi", "tila"]
                 :sort [:_score,
                        { (clojure.core/keyword (str "searchData.nimi.kieli_" lng ".keyword")) :asc}])))

(defn oid-search
  [keyword lng page size oids constraints]
  (if (empty? oids)
    {:count 0 :result []}
    (koulutusmoduulit (query-perf-string "koulutusmoduuli" keyword constraints)
                      page
                      size
                      create-hakutulokset
                      :query { :bool { :must (match-oids oids) }}
                      :_source ["searchData.nimi", "oid", "koulutusmoduuliTyyppi", "searchData.tyyppi", "tila"]
                      :sort [:_score,
                             { (clojure.core/keyword (str "searchData.nimi.kieli_" lng ".keyword")) :asc}])))
