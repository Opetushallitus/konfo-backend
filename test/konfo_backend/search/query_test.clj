(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer [debug-pretty]]
            [konfo-backend.search.query :refer [query hakutulos-aggregations]]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(deftest oppilaitos-query-test
  (testing "Query with keyword"
    (is (= (query "Hauska" {})
           {:nested {:path "hits", :query {:bool {:should [{:match {:hits.terms.fi {:query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}
                                                           {:match {:hits.terms.sv {:query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}
                                                           {:match {:hits.terms.en {:query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}]}}}})))

  (testing "Query with filters"
    (is (= (query nil {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:filter [{:terms {:hits.koulutustyypit.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}})))

  (testing "Query with hakukaynnissa filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")
                  konfo-backend.tools/half-year-past-as-kouta-format (fn [] "2019-06-01T01:01")]
      (is (= (query nil {:hakukaynnissa true})
             {:nested {:path "hits", :query {:bool {:filter [{:nested {:path "hits.hakutiedot", :query {:bool {:filter {:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:filter [{:range {:hits.hakutiedot.hakuajat.alkaa {:lte "2020-01-01T01:01"}}}
                                                                                                                                                                                           {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}}
                                                                                                                                                                                                            {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2020-01-01T01:01"}}}]}}]}}}}}}}}]}}}}))))

  (testing "Query with keyword and filters"
    (is (= (query "Hauska" {:sijainti ["kunta_091"] :koulutustyyppi ["amm", "KK"]})
           {:nested {:path "hits", :query {:bool {:should [{:match {:hits.terms.fi { :query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}
                                                           {:match {:hits.terms.sv { :query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}
                                                           {:match {:hits.terms.en { :query "hauska" :operator "and" :fuzziness "AUTO:8,12"}}}]
                                                  :filter [{:terms {:hits.koulutustyypit.keyword ["amm", "kk"]}}
                                                           {:term {:hits.sijainti.keyword "kunta_091"}}]}}}}))))

(deftest oppilaitos-aggregations-test
  (testing "Aggregations"
    (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit (fn [x] [(str x "_01") (str x "_02")])
                  konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")
                  konfo-backend.tools/half-year-past-as-kouta-format (fn [] "2019-06-01T01:01")
                  konfo-backend.index.haku/list-yhteishaut (fn [] ["1.2.246.562.29.00000000000000000001"])]
      (is (= (hakutulos-aggregations false)
             {:hits_aggregation {:nested {:path "hits"}, :aggs {:koulutusala {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso1_01 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_01"}},
                                                                                                  :kansallinenkoulutusluokitus2016koulutusalataso1_02 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :yhteishaku {:filters {:filters {:1.2.246.562.29.00000000000000000001 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.yhteishakuOid "1.2.246.562.29.00000000000000000001"}}]}}}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :koulutusalataso2 {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso2_01 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2_01"}},
                                                                                                       :kansallinenkoulutusluokitus2016koulutusalataso2_02 {:term {:hits.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :kunta {:filters {:filters {:kunta_01 {:term {:hits.sijainti.keyword "kunta_01"}},
                                                                                            :kunta_02 {:term {:hits.sijainti.keyword "kunta_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :pohjakoulutusvaatimus {:filters {:filters {:pohjakoulutusvaatimuskonfo_01 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_01"}}]}}}},
                                                                                                            :pohjakoulutusvaatimuskonfo_02 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_02"}}]}}}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :maakunta {:filters {:filters {:maakunta_01 {:term {:hits.sijainti.keyword "maakunta_01"}},
                                                                                               :maakunta_02 {:term {:hits.sijainti.keyword "maakunta_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :koulutustyyppitaso2 {:filters {:filters {:koulutustyyppi_01 {:term {:hits.koulutustyypit.keyword "koulutustyyppi_01"}},
                                                                                                          :koulutustyyppi_02 {:term {:hits.koulutustyypit.keyword "koulutustyyppi_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :hakutapa {:filters {:filters {:hakutapa_01 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.hakutapa "hakutapa_01"}}]}}}},
                                                                                               :hakutapa_02 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.hakutapa "hakutapa_02"}}]}}}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :opetustapa {:filters {:filters {:opetuspaikkakk_01 {:term {:hits.opetustavat.keyword "opetuspaikkakk_01"}},
                                                                                                 :opetuspaikkakk_02 {:term {:hits.opetustavat.keyword "opetuspaikkakk_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :opetuskieli {:filters {:filters {:oppilaitoksenopetuskieli_01 {:term {:hits.opetuskielet.keyword "oppilaitoksenopetuskieli_01"}},
                                                                                                  :oppilaitoksenopetuskieli_02 {:term {:hits.opetuskielet.keyword "oppilaitoksenopetuskieli_02"}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :hakukaynnissa {:filters {:filters {:hakukaynnissa {:nested {:path "hits.hakutiedot", :query {:bool {:filter {:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:filter [{:range {:hits.hakutiedot.hakuajat.alkaa {:lte "2020-01-01T01:01"}}} {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2020-01-01T01:01"}}}]}}]}}}}}}}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :valintatapa {:filters {:filters {:valintatapajono_01 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.valintatavat "valintatapajono_01"}}]}}}},
                                                                                                  :valintatapajono_02 {:nested {:path "hits.hakutiedot", :query {:bool {:filter [{:nested {:path "hits.hakutiedot.hakuajat", :query {:bool {:should [{:bool {:must_not {:exists {:field "hits.hakutiedot.hakuajat.paattyy"}}}} {:range {:hits.hakutiedot.hakuajat.paattyy {:gt "2019-06-01T01:01"}}}]}}}} {:term {:hits.hakutiedot.valintatavat "valintatapajono_02"}}]}}}}}}, :aggs {:real_hits {:reverse_nested {}}}},
                                                                :koulutustyyppi {:filters {:filters {:amm-osaamisala {:term {:hits.koulutustyypit.keyword "amm-osaamisala"}},
                                                                                                     :kandi {:term {:hits.koulutustyypit.keyword "kandi"}},
                                                                                                     :amk-ylempi {:term {:hits.koulutustyypit.keyword "amk-ylempi"}},
                                                                                                     :tohtori {:term {:hits.koulutustyypit.keyword "tohtori"}},
                                                                                                     :amm-tutkinnon-osa {:term {:hits.koulutustyypit.keyword "amm-tutkinnon-osa"}},
                                                                                                     :lk {:term {:hits.koulutustyypit.keyword "lk"}},
                                                                                                     :maisteri {:term {:hits.koulutustyypit.keyword "maisteri"}},
                                                                                                     :amk {:term {:hits.koulutustyypit.keyword "amk"}},
                                                                                                     :amk-alempi {:term {:hits.koulutustyypit.keyword "amk-alempi"}},
                                                                                                     :kandi-ja-maisteri {:term {:hits.koulutustyypit.keyword "kandi-ja-maisteri"}},
                                                                                                     :yo {:term {:hits.koulutustyypit.keyword "yo"}},
                                                                                                     :amm {:term {:hits.koulutustyypit.keyword "amm"}}
                                                                                                     :tuva {:term {:hits.koulutustyypit.keyword "tuva"}},
                                                                                                     :telma {:term {:hits.koulutustyypit.keyword "telma"}}}}, :aggs {:real_hits {:reverse_nested {}}}}}}})))))
