(ns konfo-backend.search.query-unit-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [hakukaynnissa-filter]]
            [konfo-backend.search.tools :refer [filters hakuaika-filter-query]]))

(deftest filters-test
  (testing "Should form filter for the query with jotpa as the only constraint"
    (is (= (filters {:jotpa true} "2022-08-26T07:21")
           [{:bool {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}}])))

  (testing "Should form filter for the query with two terms queries"
    (is (= (filters {:koulutustyyppi ["koulutustyyppi_26"] :opetuskieli ["oppilaitoksenopetuskieli_2"]}
                    "2022-08-26T07:21")
           [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}])))

  (testing "Should form filter for the query with a hakutieto query"
    (is (= (filters {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]}
                    "2022-08-26T07:21")
           [{:nested {:path "search_terms.hakutiedot"
                      :query {:bool
                              {:filter
                               [{:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}]}}}}])))

  (testing "Should form filter for the query with a hakutieto query with all constraints"
    (is (= (filters {:sijainti []
                     :lukiopainotukset []
                     :lukiolinjaterityinenkoulutustehtava []
                     :koulutusala []
                     :yhteishaku []
                     :pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]
                     :osaamisala []
                     :jotpa true
                     :hakutapa []
                     :opetustapa []
                     :opetuskieli ["oppilaitoksenopetuskieli_2"]
                     :hakukaynnissa false
                     :valintatapa []
                     :koulutustyyppi ["koulutustyyppi_26"]}
                    "2022-08-26T07:21")
           [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}
            {:bool {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}}
            {:nested {:path "search_terms.hakutiedot"
                      :query
                      {:bool
                       {:filter
                        [{:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}]}}}}]))))

(deftest hakuaika-filter-query-test
  (testing "Should form hakukaynnissa filter query with current time"
    (is (= (hakuaika-filter-query "2022-08-26T07:21")
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
                       {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}})))
  )


(deftest hakukaynnissa-filter-test
  (testing "Should form aggs filter for hakukaynnissa without selected filters"
    (is (= (hakukaynnissa-filter "2022-08-26T07:21" {:hakukaynnissa true})
           {:filters
            {:filters
             {:hakukaynnissa
              {:bool
               {:filter
                [{:bool
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
                            {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}]}}}}
            :aggs {:real_hits {:reverse_nested {}}}})))

  (testing "Should form aggs filter for hakukaynnissa with jotpa as the selected filter"
    (is (= (hakukaynnissa-filter "2022-08-26T07:21" {:jotpa true :hakukaynnissa true})
           {:filters
            {:filters
             {:hakukaynnissa
              {:bool
               {:filter
                [{:bool
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
                 {:bool {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
            :aggs {:real_hits {:reverse_nested {}}}})))
  )

