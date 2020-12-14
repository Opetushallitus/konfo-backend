(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer [debug-pretty]]
            [konfo-backend.search.query :refer [query aggregations]]))

(deftest oppilaitos-query-test
  (testing "Query with keyword"
    (is (= (query "Hauska" "fi" {})
           {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi {:query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}}}}})))

  (testing "Query with filters"
    (is (= (query nil "fi" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:filter [{:terms {:hits.koulutustyypit.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}})))

  (testing "Query with keyword and filters"
    (is (= (query "Hauska" "fi" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:must {:match {:hits.terms.fi { :query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}
                                                  :filter [{:terms {:hits.koulutustyypit.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}}))))

(deftest oppilaitos-aggregations-test
  (testing "Aggregations"
    (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit (fn [x] [(str x "_01") (str x "_02")])]
      ;(debug-pretty (aggregations))
      (is (= (aggregations)
             {:hits_aggregation {:nested {:path "hits"}
                                 :aggs {:maakunta              {:filters {:filters {:maakunta_01 {:term {:hits.sijainti.keyword "maakunta_01"}}
                                                                                    :maakunta_02 {:term {:hits.sijainti.keyword "maakunta_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :kunta                 {:filters {:filters {:kunta_01 {:term {:hits.sijainti.keyword "kunta_01"}}
                                                                                    :kunta_02 {:term {:hits.sijainti.keyword "kunta_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :opetuskieli           {:filters {:filters {:oppilaitoksenopetuskieli_01 {:term {:hits.opetuskielet.keyword "oppilaitoksenopetuskieli_01"}}
                                                                                    :oppilaitoksenopetuskieli_02 {:term {:hits.opetuskielet.keyword "oppilaitoksenopetuskieli_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :opetustapa            {:filters {:filters {:opetuspaikkakk_01 {:term {:hits.opetustavat.keyword "opetuspaikkakk_01"}}
                                                                                    :opetuspaikkakk_02 {:term {:hits.opetustavat.keyword "opetuspaikkakk_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :koulutusala           {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso1_01 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_01"}}
                                                                                    :kansallinenkoulutusluokitus2016koulutusalataso1_02 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :koulutusalataso2      {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso2_01 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2_01"}}
                                                                                    :kansallinenkoulutusluokitus2016koulutusalataso2_02 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :koulutustyyppi        {:filters {:filters {:amm {:term {:hits.koulutustyypit.keyword "amm"}},
                                                                                    :amm-tutkinnon-osa {:term {:hits.koulutustyypit.keyword "amm-tutkinnon-osa"}},
                                                                                    :amm-osaamisala {:term {:hits.koulutustyypit.keyword "amm-osaamisala"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}
                                        :koulutustyyppitaso2   {:filters {:filters {:koulutustyyppi_01 {:term {:hits.koulutustyypit.keyword "koulutustyyppi_01"}}
                                                                                    :koulutustyyppi_02 {:term {:hits.koulutustyypit.keyword "koulutustyyppi_02"}}}}
                                                                :aggs {:real_hits {:reverse_nested {}}}}}}})))))