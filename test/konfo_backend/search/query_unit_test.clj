(ns konfo-backend.search.query-unit-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.test]
            [konfo-backend.search.rajain-definitions :refer [constraints? common-filters jotpa
                                                                    pohjakoulutusvaatimus hakutapa
                                                                    koulutustyyppi opetuskieli]]
            [konfo-backend.search.rajain-tools :refer [->terms-query koulutustyypit make-query-for-rajain]]))

(defonce default-ctx {:current-time "2022-08-26T07:21"})
(deftest filters-test
  (testing "Should detect accepted constraints"
    (is (nil? (constraints? {:non-existing "nothing"})))
    (is (not (nil? (constraints? {:sijainti ["jokupaikka"]}))))
    (is (not (nil? (constraints? {:sijainti ["jokupaikka"] :koulutustyyppi ["amm"]}))))
    (is (not (nil? (constraints? {:tyovoimakoulutus true}))))
    (is (not (nil? (constraints? {:maksullinen {:maksunmaara [0, 12]}}))))
    (is (nil? (constraints? {:sijainti []})))
    (is (nil? (constraints? {:maksullinen {}})))
    (is (nil? (constraints? {:tyovoimakoulutus false}))))

  (testing "Terms query should be constructed according to the given parameters"
    (is (= {:term {:search_terms.sijainti.keyword "kunta_01"}} (->terms-query "sijainti.keyword" ["kunta_01"])))
    (is (= {:term {:search_terms.sijainti.keyword "kunta_01"}} (->terms-query "sijainti.keyword" "KUNTA_01")))
    (is (= {:term {:search_terms.hasJotpaRahoitus true}} (->terms-query "hasJotpaRahoitus" true)))
    (is (= {:terms {:search_terms.sijainti.keyword ["kunta_01", "kunta_02"]}}
           (->terms-query "sijainti.keyword" ["kunta_01", "KUNTA_02"]))))
  (testing "Should form filter for the query with jotpa as the only constraint"
    (is (= [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]
           (common-filters {:jotpa true} "2022-08-26T07:21"))))

  (testing "Should form filters for työelämä constraints"
    (is (= [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}
                             {:term {:search_terms.isTyovoimakoulutus true}}
                             {:term {:search_terms.isTaydennyskoulutus true}}]}}]
           (common-filters {:jotpa true :tyovoimakoulutus true :taydennyskoulutus true} "2022-08-26T07:21"))))

  (testing "Should form filter for the query with two terms queries"
    (is (= [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}]
           (common-filters {:koulutustyyppi ["koulutustyyppi_26"] :opetuskieli ["oppilaitoksenopetuskieli_2"]}
                           "2022-08-26T07:21"))))

  (testing "Should form filter for the query with a hakutieto query"
    (is (= [{:nested {:path  "search_terms.hakutiedot"
                      :query {:bool
                              {:filter
                               {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}}}}}]
           (common-filters {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]}
                           "2022-08-26T07:21"))))

  (testing "Should form filter for maksullisuus -rajaingroup with single all-must -item"
    (is (= [{:bool {:should [{:term {:search_terms.metadata.maksullisuustyyppi.keyword "maksullinen"}}]}}]
           (common-filters {:maksullinen {:maksunmaara []}} "2023-06-08T07:21"))
    ))

  (testing "Should form filter for maksullisuus -rajaingroup with one all-must -rajain"
    (is (match? (m/match-with [map? m/equals]
                              [{:bool {:should
                                       [{:bool {:filter
                                                [{:term {:search_terms.metadata.maksullisuustyyppi.keyword "lukuvuosimaksu"}}
                                                 {:range {:search_terms.metadata.maksunMaara {:gte 0, :lte 10000}}}
                                                 {:term {:search_terms.onkoApuraha true}}]}}]}}])
           (common-filters {:lukuvuosimaksu {:maksunmaara [0,10000] :apuraha true}} "2023-06-08T07:21"))))

  (testing "Should form filter for maksullisuus -rajaingroup"
    (is (match? (m/match-with [map? m/equals]
                              [{:bool {:should [{:term {:search_terms.metadata.maksullisuustyyppi.keyword "maksuton"}}
                                                {:bool {:filter [{:term {:search_terms.metadata.maksullisuustyyppi.keyword "maksullinen"}}
                                                                 {:range {:search_terms.metadata.maksunMaara {:gte 100}}}]}}
                                                {:bool {:filter [{:term {:search_terms.metadata.maksullisuustyyppi.keyword "lukuvuosimaksu"}}
                                                                 {:range {:search_terms.metadata.maksunMaara {:gte 0, :lte 10000}}}]}}]}}])
                (common-filters {:maksuton ["maksuton"]
                                 :maksullinen {:maksunmaara [100]}
                                 :lukuvuosimaksu {:maksunmaara [0,10000] :apuraha nil}} "2023-06-08T07:21"))))

  (testing "Should form aggregation for jotpa with pohjakoulutusvaatimus as selected filter"
    (is (match? (m/match-with [map? m/equals]
                              {:filter
                               {:bool
                                {:filter
                                 [{:nested {:path  "search_terms.hakutiedot"
                                            :query {:bool {:filter
                                                           {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                                   "pohjakoulutusvaatimuskonfo_am"}}}}}}
                                  {:term {:search_terms.hasJotpaRahoitus true}}]}}
                               :aggs {:real_hits {:reverse_nested {}}}})
                ((:make-agg jotpa) {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]} default-ctx)
                ))))

  (deftest hakutieto-filters-aggregation-test
    (testing "Should form aggregation for pohjakoulutusvaatimus without selected filters"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs
                                 {:rajain
                                  {:aggs
                                   {:real_hits
                                    {:reverse_nested {}}}
                                   :terms
                                   {:field         "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                    :min_doc_count 0
                                    :size          1000}}}
                                 :nested {:path "search_terms.hakutiedot"}})
                  ((:make-agg pohjakoulutusvaatimus) {} default-ctx)))))

  (testing "Should form aggregation for pohjakoulutusvaatimus with jotpa as the selected filter"
    (is (match? (m/match-with [map? m/equals]
                              {:aggs
                               {:pohjakoulutusvaatimus
                                {:aggs
                                 {:rajain
                                  {:aggs
                                   {:real_hits
                                    {:reverse_nested {}}}
                                   :terms {:field         "search_terms.hakutiedot.pohjakoulutusvaatimukset"
                                           :min_doc_count 0
                                           :size          1000}}}
                                 :nested {:path "search_terms.hakutiedot"}}}
                               :filter {:bool {:filter [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}})
                ((:make-agg pohjakoulutusvaatimus) {:jotpa true} default-ctx))))

  (testing "Should form aggregation for hakutapa with hakukaynnissa and pohjakoulutusvaatimus as selected filters"
    (is (match? (m/match-with [map? m/equals]
                              {:aggs
                               {:hakutapa
                                {:aggs
                                 {:rajain
                                  {:aggs
                                   {:real_hits
                                    {:reverse_nested {}}}
                                   :terms {:field         "search_terms.hakutiedot.hakutapa"
                                           :min_doc_count 0
                                           :size          1000}}}
                                 :nested {:path "search_terms.hakutiedot"}}}
                               :filter
                               {:bool
                                {:filter
                                 [{:nested
                                   {:path "search_terms.hakutiedot"
                                    :query
                                    {:bool
                                     {:filter
                                      {:terms
                                       {:search_terms.hakutiedot.pohjakoulutusvaatimukset ["pohjakoulutusvaatimuskonfo_am"
                                                                                           "pohjakoulutusvaatimuskonfo_003"]}}}}}}
                                  {:bool
                                   {:should
                                    [{:bool
                                      {:filter
                                       [{:range
                                         {:search_terms.toteutusHakuaika.alkaa
                                          {:lte "2022-08-26T07:21"}}}
                                        {:bool
                                         {:should
                                          [{:bool
                                            {:must_not
                                             {:exists
                                              {:field "search_terms.toteutusHakuaika.paattyy"}}}}
                                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
                                     {:nested {:path "search_terms.hakutiedot.hakuajat"
                                               :query
                                               {:bool
                                                {:filter
                                                 [{:range
                                                   {:search_terms.hakutiedot.hakuajat.alkaa
                                                    {:lte "2022-08-26T07:21"}}}
                                                  {:bool
                                                   {:should
                                                    [{:bool
                                                      {:must_not
                                                       {:exists
                                                        {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                                     {:range
                                                      {:search_terms.hakutiedot.hakuajat.paattyy
                                                       {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}]}}})
                ((:make-agg hakutapa) {:jotpa                 false
                                       :pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
                                       :hakukaynnissa         true} default-ctx))))

  (deftest filters-aggregation-test
    (testing "Should form aggregation for opetuskieli without any selected filters"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs
                                 {:real_hits
                                  {:reverse_nested {}}}
                                 :terms {:field         "search_terms.opetuskielet.keyword"
                                         :min_doc_count 0
                                         :size          1000}})
                  ((:make-agg opetuskieli) {} default-ctx))))

    (testing "Should form aggregation for opetuskieli with jotpa and hakukaynnissa as selected filters"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs
                                 {:rajain
                                  {:aggs
                                   {:real_hits
                                    {:reverse_nested {}}}
                                   :terms
                                   {:field         "search_terms.opetuskielet.keyword"
                                    :min_doc_count 0
                                    :size          1000}}}
                                 :filter
                                 {:bool
                                  {:filter
                                   [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}
                                    {:bool
                                     {:should
                                      [{:bool
                                        {:filter
                                         [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
                                          {:bool
                                           {:should
                                            [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
                                             {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
                                       {:nested
                                        {:path "search_terms.hakutiedot.hakuajat"
                                         :query
                                         {:bool
                                          {:filter
                                           [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
                                            {:bool
                                             {:should
                                              [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                               {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
                                    ]}}})
                  ((:make-agg opetuskieli) {:jotpa true :hakukaynnissa true} default-ctx))))

    (testing "Should form aggregation for koulutustyyppi without any selected filters"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs  {:real_hits {:reverse_nested {}}}
                                 :terms {:field         "search_terms.koulutustyypit.keyword"
                                         :include       koulutustyypit
                                         :min_doc_count 0
                                         :size          (count koulutustyypit)}})
                  ((:make-agg koulutustyyppi) {} default-ctx))))

    (testing "Should form aggregation for koulutustyyppi with opetustapa filters"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs   {:rajain {:aggs  {:real_hits {:reverse_nested {}}}
                                                   :terms {:field         "search_terms.koulutustyypit.keyword"
                                                           :include       koulutustyypit
                                                           :min_doc_count 0
                                                           :size          (count koulutustyypit)}}}
                                 :filter {:bool {:filter [{:terms {:search_terms.opetustavat.keyword ["opetuspaikkakk_3"
                                                                                                      "opetuspaikkakk_4"]}}]}}})
                  ((:make-agg koulutustyyppi) {:opetustapa ["opetuspaikkakk_3" "opetuspaikkakk_4"]} default-ctx))))

    (testing "Should form aggregation for koulutustyyppi with yhteishaku filter"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs   {:rajain {:aggs  {:real_hits {:reverse_nested {}}}
                                                   :terms {:field         "search_terms.koulutustyypit.keyword"
                                                           :include       koulutustyypit
                                                           :min_doc_count 0
                                                           :size          (count koulutustyypit)}}}
                                 :filter {:bool {:filter [{:nested {:path  "search_terms.hakutiedot"
                                                                    :query {:bool
                                                                            {:filter
                                                                             {:terms
                                                                              {:search_terms.hakutiedot.yhteishakuOid
                                                                               ["1.2.246.562.29.00000000000000000001"
                                                                                "1.2.246.562.29.00000000000000000002"]}}}}}}]}}})
                  ((:make-agg koulutustyyppi) {:yhteishaku ["1.2.246.562.29.00000000000000000001"
                                                            "1.2.246.562.29.00000000000000000002"]} default-ctx))))


    (testing "Should not filter koulutustyypit inside aggregation when it is also the selected filter"
      (is (match? (m/match-with [map? m/equals]
                                {:aggs  {:real_hits {:reverse_nested {}}}
                                 :terms {:field         "search_terms.koulutustyypit.keyword"
                                         :include       koulutustyypit
                                         :min_doc_count 0
                                         :size          (count koulutustyypit)}})
                  ((:make-agg koulutustyyppi) {:koulutustyyppi ["amm"]} default-ctx))))
    )
  (testing "Should form filter for the query with a hakutieto query with several selected constraints"
    (is (match?
          [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
           {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}
           {:nested {:path "search_terms.hakutiedot"
                     :query
                     {:bool
                      {:filter
                       {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}}}}}
           {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}
           {:bool
            {:should
             [{:bool
               {:filter
                [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
                 {:bool
                  {:should
                   [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
                    {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
              {:nested
               {:path "search_terms.hakutiedot.hakuajat"
                :query
                {:bool
                 {:filter
                  [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
                   {:bool
                    {:should
                     [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                      {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}]
          (common-filters {:sijainti                            []
                           :lukiopainotukset                    []
                           :lukiolinjaterityinenkoulutustehtava []
                           :koulutusala                         []
                           :yhteishaku                          []
                           :pohjakoulutusvaatimus               ["pohjakoulutusvaatimuskonfo_am"]
                           :osaamisala                          []
                           :jotpa                               true
                           :hakutapa                            []
                           :opetustapa                          []
                           :opetuskieli                         ["oppilaitoksenopetuskieli_2"]
                           :hakukaynnissa                       true
                           :valintatapa                         []
                           :koulutustyyppi                      ["koulutustyyppi_26"]}
                          "2022-08-26T07:21"))))

(use 'clojure.test)
(run-tests)
