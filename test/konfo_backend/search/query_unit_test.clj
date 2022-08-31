(ns konfo-backend.search.query-unit-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [hakukaynnissa-filter jotpa-filter ->hakutieto-filters-aggregation]]
            [konfo-backend.search.tools :refer [filters hakuaika-filter-query hakutieto-aggs-filters]]))

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

  (testing "Should form filter for the query with a hakutieto query with several selected constraints"
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
                     :hakukaynnissa true
                     :valintatapa []
                     :koulutustyyppi ["koulutustyyppi_26"]}
                    "2022-08-26T07:21")
           [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}
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
    (is (= (hakukaynnissa-filter "2022-08-26T07:21" {:jotpa false})
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
    (is (= (hakukaynnissa-filter "2022-08-26T07:21" {:jotpa true})
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
                 {:term {:search_terms.hasJotpaRahoitus true}}]}}}}
            :aggs {:real_hits {:reverse_nested {}}}}))))

(deftest jotpa-filter-test
  (testing "Should form aggs filter for jotpa with hakukaynnissa as the selected filter"
    (is (= (jotpa-filter  "2022-08-26T07:21" {:jotpa true})
            {:filters
             {:filters
              {:jotpa
               {:bool
                {:filter
                 [{:term {:search_terms.hasJotpaRahoitus true}}]}}}}
             :aggs {:real_hits {:reverse_nested {}}}})))

  (testing "Should form aggs filter for jotpa with hakukaynnissa as selected filter"
    (is (= (jotpa-filter "2022-08-26T07:21" {:jotpa true :hakukaynnissa true})
           {:filters
            {:filters
             {:jotpa
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
                 {:term {:search_terms.hasJotpaRahoitus true}}]}
               }}}
            :aggs {:real_hits {:reverse_nested {}}}}))
    )

  (testing "Should form aggs filter for jotpa with pohjakoulutusvaatimus as selected filter"
    (is (= (jotpa-filter "2022-08-26T07:21" {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]})
           {:filters
            {:filters
             {:jotpa
              {:bool
               {:filter
                [{:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                         "pohjakoulutusvaatimuskonfo_am"}}
                 {:term {:search_terms.hasJotpaRahoitus true}}]}
               }}}
            :aggs {:real_hits {:reverse_nested {}}}}))
    )
  )

(deftest ->hakutieto-filters-aggregation-test
  (testing "Should form aggs filters for pohjakoulutusvaatimus without selected filters"
    (is (= (->hakutieto-filters-aggregation
             :search_terms.hakutiedot.pohjakoulutusvaatimukset
             ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
             "2022-08-26T07:21"
             {})
           {:filters
             {:filters
              {:pohjakoulutusvaatimuskonfo_am
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [{:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                 "pohjakoulutusvaatimuskonfo_am"}}]}}}}
               :pohjakoulutusvaatimuskonfo_003
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [{:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                 "pohjakoulutusvaatimuskonfo_003"}}]}}}}}}
            :aggs {:real_hits {:reverse_nested {}}}})))

  (testing "Should form aggs filters for pohjakoulutusvaatimus with jotpa as a selected filter"
    (is (= (->hakutieto-filters-aggregation
             :search_terms.hakutiedot.pohjakoulutusvaatimukset
             ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
             "2022-08-26T07:21"
             {:jotpa true})
           {:filters
             {:filters
              {:pohjakoulutusvaatimuskonfo_am
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [{:term {:search_terms.hasJotpaRahoitus true}}
                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                 "pohjakoulutusvaatimuskonfo_am"}}]}}}}
               :pohjakoulutusvaatimuskonfo_003
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [{:term {:search_terms.hasJotpaRahoitus true}}
                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                 "pohjakoulutusvaatimuskonfo_003"}}]}}}}}}
            :aggs {:real_hits {:reverse_nested {}}}})))

  (testing "Should form aggs filters for hakutapa with jotpa, hakukaynnissa and pohjakoulutusvaatimus as selected filters"
    (is (= (->hakutieto-filters-aggregation
             :search_terms.hakutiedot.hakutapa
             ["hakutapa_01" "hakutapa_02"]
             "2022-08-26T07:21"
             {:jotpa true
              :pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_001"]
              :hakukaynnissa true})
           {:filters
             {:filters
              {:hakutapa_01
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [
                                         {:terms {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                  ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_001"]}}
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
                                         {:term {:search_terms.hasJotpaRahoitus true}}
                                         {:term {:search_terms.hakutiedot.hakutapa
                                                 "hakutapa_01"}}]}}}}
               :hakutapa_02
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [{:terms {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                  ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_001"]}}
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
                                         {:term {:search_terms.hasJotpaRahoitus true}}
                                         {:term {:search_terms.hakutiedot.hakutapa
                                                 "hakutapa_02"}}]}}}}}}
            :aggs {:real_hits {:reverse_nested {}}}})))

  (testing "Should form aggs filters for pohjakoulutusvaatimus with hakutapa as a selected filter"
    (is (= (->hakutieto-filters-aggregation
             :search_terms.hakutiedot.pohjakoulutusvaatimukset
             ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
             "2022-08-26T07:21"
             {:hakutapa ["hakutapa_01" "hakutapa_02"]})
           {:filters
             {:filters
              {:pohjakoulutusvaatimuskonfo_am
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [
                                         {:terms {:search_terms.hakutiedot.hakutapa ["hakutapa_01" "hakutapa_02"]}}
                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                 "pohjakoulutusvaatimuskonfo_am"}}]}}}}
               :pohjakoulutusvaatimuskonfo_003
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [
                                         {:terms {:search_terms.hakutiedot.hakutapa ["hakutapa_01" "hakutapa_02"]}}
                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                 "pohjakoulutusvaatimuskonfo_003"}}]}}}}}}
            :aggs {:real_hits {:reverse_nested {}}}}
           )))

  (testing "Should form aggs filters for valintatapa with hakutapa as a selected filter"
    (is (= (->hakutieto-filters-aggregation
             :search_terms.hakutiedot.valintatapa
             ["valintatapajono_av" "valintatapajono_yp"]
             "2022-08-26T07:21"
             {:hakutapa ["hakutapa_01" "hakutapa_02"]})
           {:filters
             {:filters
              {:valintatapajono_av
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [
                                         {:terms {:search_terms.hakutiedot.hakutapa ["hakutapa_01" "hakutapa_02"]}}
                                         {:term {:search_terms.hakutiedot.valintatapa
                                                 "valintatapajono_av"}}]}}}}
               :valintatapajono_yp
               {:nested {:path "search_terms.hakutiedot"
                         :query {:bool {:filter
                                        [
                                         {:terms {:search_terms.hakutiedot.hakutapa ["hakutapa_01" "hakutapa_02"]}}
                                         {:term {:search_terms.hakutiedot.valintatapa
                                                 "valintatapajono_yp"}}]}}}}}}
            :aggs {:real_hits {:reverse_nested {}}}}
           )))
  )

(deftest hakutieto-aggs-filters-test
  (testing "Should create filter array with for pohjakoulutusvaatimus without any selected filters"
    (is (= (hakutieto-aggs-filters
             {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}
             "2022-08-26T07:21"
             {:hakukaynnissa false})
           [{:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}])))

  (testing "Should create filter array for single pohjakoulutusvaatimus with jotpa and hakukaynnissa as selected filters"
    (is (= (hakutieto-aggs-filters
             {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}
             "2022-08-26T07:21"
             {:jotpa true :hakukaynnissa true})
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
            {:term {:search_terms.hasJotpaRahoitus true}}
            {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}])))
  )

