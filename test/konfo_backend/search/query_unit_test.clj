(ns konfo-backend.search.query-unit-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [blank?]]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]
            [konfo-backend.search.rajain.rajain-definitions :refer [constraints? common-filters]]))

(deftest filters-test
  (testing "Should detect accepted constraints"
    (is (nil? (constraints? {:non-existing "nothing"})))
    (is (not (nil? (constraints? {:sijainti "jokupaikka"}))))
    (is (not (nil? (constraints? {:sijainti "jokupaikka" :koulutustyyppi "amm"}))))
    (is (not (nil? (constraints? {:tyovoimakoulutus true}))))
    (is (nil? (constraints? {:tyovoimakoulutus false}))))

  (testing "Should form filter for the query with jotpa as the only constraint"
    (is (= [{:term {:search_terms.hasJotpaRahoitus true}}]
           (common-filters {:jotpa true} "2022-08-26T07:21"))))

  (testing "Should form filters for työelämä constraints"
    (is (= [{:term {:search_terms.hasJotpaRahoitus true}}
                             {:term {:search_terms.isTyovoimakoulutus true}}
                             {:term {:search_terms.isTaydennyskoulutus true}}]
           (common-filters {:jotpa true :tyovoimakoulutus true :taydennyskoulutus true} "2022-08-26T07:21"))))

  (testing "Should form filter for the query with two terms queries"
    (is (= [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}]
           (common-filters {:koulutustyyppi ["koulutustyyppi_26"] :opetuskieli ["oppilaitoksenopetuskieli_2"]}
                           "2022-08-26T07:21"))))

  (testing "Should form filter for the query with a hakutieto query"
    (is (= [{:nested {:path "search_terms.hakutiedot"
                      :query {:bool
                              {:filter
                               {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}}}}}]
           (common-filters {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]}
                           "2022-08-26T07:21"))))

  (testing "Should form filter for the query with a hakutieto query with several selected constraints"
    (is (= [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}
            {:nested {:path "search_terms.hakutiedot"
                      :query
                      {:bool
                       {:filter
                        {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}}}}}
            {:term {:search_terms.hasJotpaRahoitus true}}
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
           (common-filters {:sijainti                     []
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
                           "2022-08-26T07:21")))))

(use 'clojure.test)
(run-tests)
