(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer [debug-pretty]]
            [konfo-backend.search.query :refer [query aggregations]]))

(deftest oppilaitos-query-test
  (testing "Query with keyword"
    (is (= (query "Hauska" "fi" {})
           {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi "hauska"}}}}}})))

  (testing "Query with filters"
    (is (= (query nil "fi" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:filter [{:terms {:hits.koulutustyypit.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}})))

  (testing "Query with keyword and filters"
    (is (= (query "Hauska" "fi" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi "hauska"}}
                                                  :filter [{:terms {:hits.koulutustyypit.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}}))))

(deftest oppilaitos-aggregations-test
  (testing "Aggregations"
    (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit (fn [x] [(str x "_01") (str x "_02")])]
      ;(debug-pretty (aggregations))
      (is (= (aggregations)
             {:hits_aggregation {:nested {:path "hits"}
                                 :aggs {:sijainti         {:filters {:filters {:maakunta_01 {:term {:hits.sijainti.keyword "maakunta_01"}}
                                                                               :maakunta_02 {:term {:hits.sijainti.keyword "maakunta_02"}}}}
                                                           :aggs {:real_hits {:reverse_nested {}}}}
                                        :opetuskieli      {:filters {:filters {:oppilaitoksenopetuskieli_01 {:term {:hits.opetuskielet.keyword "oppilaitoksenopetuskieli_01"}}
                                                                               :oppilaitoksenopetuskieli_02 {:term {:hits.opetuskielet.keyword "oppilaitoksenopetuskieli_02"}}}}
                                                           :aggs {:real_hits {:reverse_nested {}}}}
                                        :koulutusalataso1 {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso1_01 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_01"}}
                                                                               :kansallinenkoulutusluokitus2016koulutusalataso1_02 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}}}
                                                           :aggs {:real_hits {:reverse_nested {}}}}
                                        :koulutustyyppi   {:filters {:filters {:amm {:term {:hits.koulutustyypit.keyword "amm"}}}}
                                                           :aggs {:real_hits {:reverse_nested {}}}}}}})))))