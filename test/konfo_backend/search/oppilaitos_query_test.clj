(ns konfo-backend.search.oppilaitos-query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.oppilaitos.query :refer [query aggregations]]))

(deftest oppilaitos-query-test
  (testing "Query with keyword"
    (is (= (query "Hauska" "fi" {})
           {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi "hauska"}}}}}})))

  (testing "Query with filters"
    (is (= (query nil "fi" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:filter [{:terms {:hits.koulutustyyppi.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}})))

  (testing "Query with keyword and filters"
    (is (= (query "Hauska" "fi" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi "hauska"}}
                                                  :filter [{:terms {:hits.koulutustyyppi.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}}))))

(deftest oppilaitos-aggregations-test
  (testing "Aggregations"
    (with-redefs [konfo-backend.koodisto.koodisto/list (fn [x] [(str x "_01") (str x "_02")])]
      (is (= (aggregations)
             {:hits_aggregation {:nested {:path "hits"}
                                 :aggs {:sijainti {:filters {:filters {:maakunta_01 {:term {:hits.sijainti.keyword "maakunta_01"}}
                                                                       :maakunta_02 {:term {:hits.sijainti.keyword "maakunta_02"}}}}
                                                   :aggs {:oppilaitokset {:reverse_nested {}}}}}}})))))