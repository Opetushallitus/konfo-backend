(ns konfo-backend.search.oppilaitos
  (:require
    [clj-log.error-log :refer [with-error-logging]]
    [konfo-backend.elastic-tools :refer :all]))

(def oppilaitokset (partial search "organisaatio"))

(defn- create-hakutulos [hakutulos]
  (let [organisaatio (:_source hakutulos)]
    {:score (:_score hakutulos)
     :oid (:oid organisaatio)
     :nimi (get-in organisaatio [:nimi])
     :oppilaitostyyppi (get-in organisaatio [:searchData :oppilaitostyyppi])
     :tyyppi (get-in organisaatio [:searchData :tyyppi])
     :kayntiosoite (get-in organisaatio [:kayntiosoite :osoite])
     :postitoimipaikka (get-in organisaatio [:kayntiosoite :postitoimipaikka])}))

(defn- create-hakutulokset [hakutulokset]
  { :count (:total hakutulokset)
   :result (map create-hakutulos (:hits hakutulokset))})

(defn- oppilaitos-query [keyword oids constraints]
  (let [paikkakunta (:paikkakunta constraints)]
    {:bool (merge {
              :must [
                   {:dis_max {:queries [(constant_score_query_multi_match keyword ["searchData.oppilaitostyyppi.nimi.fi"] 1000)
                                        (constant_score_query_multi_match keyword ["nimi.fi"] 300)
                                        (if (nil? paikkakunta) (constant_score_query_multi_match keyword ["postiosoite.postitoimipaikka" "kayntiosoite.postitoimipaikka" "yhteystiedot.postitoimipaikka"] 5))
                                        (constant_score_query_multi_match keyword ["postiosoite.osoite" "kayntiosoite.osoite" "yhteystiedot.osoite"] 4)
                                        (constant_score_query_oids oids 2)]}},
                   {:dis_max {:queries [(constant_score_query_match_tyypit "Koulutustoimija" 200)
                                        (constant_score_query_match_tyypit "Oppilaitos" 200)
                                        (constant_score_query_match_tyypit "Toimipiste" 100)]}}]
              :must_not {:range {:lakkautusPvm {:format "yyyy-MM-dd" :lt "now"}}}}
              (when-let [p paikkakunta] {:filter (multi_match p ["postiosoite.postitoimipaikka" "kayntiosoite.postitoimipaikka" "yhteystiedot.postitoimipaikka"]) }))}))

(defn text-search
  [keyword page size oids constraints]
  (oppilaitokset keyword
                 page
                 size
                 create-hakutulokset
                 :query (oppilaitos-query keyword oids constraints)
                 :_source ["oid", "nimi", "kayntiosoite.osoite", "kayntiosoite.postitoimipaikka", "searchData.oppilaitostyyppi", "searchData.tyyppi"]
                 :sort [:_score, { :nimi.fi.keyword :asc} ]))

(defn- oids-by-paikkakunta-query [paikkakunta]
  {:bool { :must (multi_match paikkakunta ["postiosoite.postitoimipaikka" "kayntiosoite.postitoimipaikka" "yhteystiedot.postitoimipaikka"])
           :must_not {:range {:lakkautusPvm {:format "yyyy-MM-dd" :lt "now"}}}}})

(defn filter-organisaatio-oids [constraints]
  (if-let [paikkakunta (:paikkakunta constraints)]
      (oppilaitokset paikkakunta
                     0
                     10000
                     (fn [x] (map #(get-in % [:_source :oid]) (:hits x)))
                     :query (oids-by-paikkakunta-query paikkakunta)
                     :_source ["oid"]
                     :sort [:_score, { :nimi.fi.keyword :asc} ])))