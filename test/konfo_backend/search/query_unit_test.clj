(ns konfo-backend.search.query-unit-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [hakukaynnissa-filter jotpa-filter ->nested-filters-aggregation ->filters-aggregation]]
            [konfo-backend.search.tools :refer [filters hakuaika-filter-query]]))

(deftest filters-test
  (testing "Should form filter for the query with jotpa as the only constraint"
    (is (= [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]
           (filters {:jotpa true} "2022-08-26T07:21"))))

  (testing "Should form filters for työelämä constraints"
    (is (= [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}
                             {:term {:search_terms.isTyovoimakoulutus true}}
                             {:term {:search_terms.isTaydennyskoulutus true}}]}}]
           (filters {:jotpa true :tyovoimakoulutus true :taydennyskoulutus true} "2022-08-26T07:21"))))

  (testing "Should form filter for the query with two terms queries"
    (is (= [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}]
           (filters {:koulutustyyppi ["koulutustyyppi_26"] :opetuskieli ["oppilaitoksenopetuskieli_2"]}
                    "2022-08-26T07:21"))))

  (testing "Should form filter for the query with a hakutieto query"
    (is (= [{:nested {:path "search_terms.hakutiedot"
                      :query {:bool
                              {:filter
                               {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}}}}}]
           (filters {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]}
                    "2022-08-26T07:21"))))

  (testing "Should form filter for the query with a hakutieto query with several selected constraints"
    (is (= [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_26"}}
            {:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_2"}}
            {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}
            {:nested {:path "search_terms.hakutiedot"
                      :query
                      {:bool
                       {:filter
                        {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_am"}}}}}}
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
           (filters {:sijainti []
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
                    "2022-08-26T07:21")))))

;; (deftest hakuaika-filter-query-test
;;   (testing "Should form hakukaynnissa filter query with current time"
;;     (is (= {:bool
;;              {:should
;;               [{:bool
;;                 {:filter
;;                  [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                   {:bool
;;                    {:should
;;                     [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                      {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                {:nested
;;                 {:path "search_terms.hakutiedot.hakuajat"
;;                  :query
;;                  {:bool
;;                   {:filter
;;                    [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                     {:bool
;;                      {:should
;;                       [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                        {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
;;            (hakuaika-filter-query "2022-08-26T07:21")))))


;; (deftest hakukaynnissa-filter-test
;;   (testing "Should form aggs filter for hakukaynnissa without selected filters"
;;     (is (= {:filters
;;             {:filters
;;              {:hakukaynnissa
;;               {:bool
;;                {:filter
;;                 [{:bool
;;                   {:should
;;                    [{:bool
;;                      {:filter
;;                       [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                        {:bool
;;                         {:should
;;                          [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                     {:nested
;;                      {:path "search_terms.hakutiedot.hakuajat"
;;                       :query
;;                       {:bool
;;                        {:filter
;;                         [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                          {:bool
;;                           {:should
;;                            [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                             {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (hakukaynnissa-filter "2022-08-26T07:21" {:jotpa false}))))

;;   (testing "Should form aggs filter for hakukaynnissa with jotpa as the selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:hakukaynnissa
;;               {:bool
;;                {:filter
;;                 [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}
;;                  {:bool
;;                   {:should
;;                    [{:bool
;;                      {:filter
;;                       [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                        {:bool
;;                         {:should
;;                          [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                     {:nested
;;                      {:path "search_terms.hakutiedot.hakuajat"
;;                       :query
;;                       {:bool
;;                        {:filter
;;                         [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                          {:bool
;;                           {:should
;;                            [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                             {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (hakukaynnissa-filter "2022-08-26T07:21" {:jotpa true}))))

;;   (testing "Should form aggs filter for hakukaynnissa with hakukaynnissa as the selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:hakukaynnissa
;;               {:bool
;;                {:filter
;;                 [{:bool
;;                   {:should
;;                    [{:bool
;;                      {:filter
;;                       [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                        {:bool
;;                         {:should
;;                          [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                     {:nested
;;                      {:path "search_terms.hakutiedot.hakuajat"
;;                       :query
;;                       {:bool
;;                        {:filter
;;                         [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                          {:bool
;;                           {:should
;;                            [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                             {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (hakukaynnissa-filter "2022-08-26T07:21" {:hakukaynnissa true})))))

;; (deftest jotpa-filter-test
;;   (testing "Should form aggs filter for jotpa without selected filters"
;;     (is (= {:filters
;;              {:filters
;;               {:jotpa
;;                {:bool
;;                 {:filter
;;                  [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (jotpa-filter "2022-08-26T07:21" {}))))

;;   (testing "Should form aggs filter for jotpa with hakukaynnissa as selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:jotpa
;;               {:bool
;;                {:filter
;;                 [{:bool
;;                   {:should
;;                    [{:bool
;;                      {:filter
;;                       [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                        {:bool
;;                         {:should
;;                          [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                     {:nested
;;                      {:path "search_terms.hakutiedot.hakuajat"
;;                       :query
;;                       {:bool
;;                        {:filter
;;                         [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                          {:bool
;;                           {:should
;;                            [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                             {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
;;                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}
;;                }}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (jotpa-filter "2022-08-26T07:21" {:hakukaynnissa true}))))

;;   (testing "Should form aggs filter for jotpa with pohjakoulutusvaatimus as selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:jotpa
;;               {:bool
;;                {:filter
;;                 [{:nested {:path "search_terms.hakutiedot"
;;                            :query {:bool {:filter
;;                                           {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                   "pohjakoulutusvaatimuskonfo_am"}}}}}}
;;                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (jotpa-filter "2022-08-26T07:21" {:pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am"]}))))

;;   (testing "Should form aggs filter for jotpa with jotpa as the selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:jotpa
;;               {:bool
;;                {:filter
;;                 [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (jotpa-filter "2022-08-26T07:21" {:jotpa true})))))

;; (deftest hakutieto-filters-aggregation-test
;;   (testing "Should form aggs filters for pohjakoulutusvaatimus without selected filters"
;;     (is (= {:filters
;;              {:filters
;;               {:pohjakoulutusvaatimuskonfo_am
;;                {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
;;                          :query {:bool {:filter
;;                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                  "pohjakoulutusvaatimuskonfo_am"}}}}}}]}}
;;                :pohjakoulutusvaatimuskonfo_003
;;                {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
;;                          :query {:bool {:filter
;;                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                  "pohjakoulutusvaatimuskonfo_003"}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->nested-filters-aggregation
;;              "hakutiedot"
;;              :search_terms.hakutiedot.pohjakoulutusvaatimukset
;;              ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
;;              "2022-08-26T07:21"
;;              {}))))

;;   (testing "Should form aggs filters for pohjakoulutusvaatimus with jotpa as the selected filter"
;;     (is (= {:filters
;;              {:filters
;;               {:pohjakoulutusvaatimuskonfo_am
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                          :query {:bool {:filter
;;                                         {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                  "pohjakoulutusvaatimuskonfo_am"}}}}}}
;;                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
;;                :pohjakoulutusvaatimuskonfo_003
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                     "pohjakoulutusvaatimuskonfo_003"}}}}}}
;;                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->nested-filters-aggregation
;;              "hakutiedot"
;;              :search_terms.hakutiedot.pohjakoulutusvaatimukset
;;              ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
;;              "2022-08-26T07:21"
;;              {:jotpa true}))))

;;   (testing "Should form aggs filters for hakutapa with hakukaynnissa and pohjakoulutusvaatimus as selected filters"
;;     (is (= {:filters
;;              {:filters
;;               {:hakutapa_01
;;                {:bool
;;                 {:filter
;;                  [{:nested
;;                    {:path "search_terms.hakutiedot"
;;                     :query {:bool {:filter {:term {:search_terms.hakutiedot.hakutapa "hakutapa_01"}}}}}}
;;                   {:bool
;;                    {:should
;;                     [{:bool
;;                       {:filter
;;                        [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                         {:bool
;;                          {:should
;;                           [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                            {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                      {:nested
;;                       {:path "search_terms.hakutiedot.hakuajat"
;;                        :query
;;                        {:bool
;;                         {:filter
;;                          [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                           {:bool
;;                            {:should
;;                             [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                              {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
;;                   {:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:terms {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                     ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]}}}}}}]}}

;;                :hakutapa_02
;;                {:bool
;;                 {:filter
;;                  [{:nested
;;                    {:path "search_terms.hakutiedot"
;;                     :query {:bool {:filter {:term {:search_terms.hakutiedot.hakutapa "hakutapa_02"}}}}}}
;;                   {:bool
;;                    {:should
;;                     [{:bool
;;                       {:filter
;;                        [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                         {:bool
;;                          {:should
;;                           [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                            {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                      {:nested
;;                       {:path "search_terms.hakutiedot.hakuajat"
;;                        :query
;;                        {:bool
;;                         {:filter
;;                          [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                           {:bool
;;                            {:should
;;                             [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                              {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
;;                   {:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:terms {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                                     ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->nested-filters-aggregation
;;              "hakutiedot"
;;              :search_terms.hakutiedot.hakutapa
;;              ["hakutapa_01" "hakutapa_02"]
;;              "2022-08-26T07:21"
;;              {:jotpa false
;;               :pohjakoulutusvaatimus ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
;;               :hakukaynnissa true}))))

;;   (testing "Should form aggs filters for pohjakoulutusvaatimus with hakutapa as a selected filter"
;;     (is (= {:filters
;;              {:filters
;;               {:pohjakoulutusvaatimuskonfo_am
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                             :query
;;                             {:bool
;;                              {:filter
;;                               {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                       "pohjakoulutusvaatimuskonfo_am"}}}}}}
;;                   {:nested {:path "search_terms.hakutiedot"
;;                             :query
;;                             {:bool
;;                              {:filter
;;                               {:terms {:search_terms.hakutiedot.hakutapa ["hakutapa_01" "hakutapa_02"]}}}}}}]}}
;;                :pohjakoulutusvaatimuskonfo_003
;;                {:bool {:filter
;;                        [{:nested {:path "search_terms.hakutiedot"
;;                                   :query
;;                                   {:bool
;;                                    {:filter
;;                                     {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
;;                                             "pohjakoulutusvaatimuskonfo_003"}}}}}}
;;                         {:nested {:path "search_terms.hakutiedot"
;;                                   :query
;;                                   {:bool
;;                                    {:filter
;;                                     {:terms {:search_terms.hakutiedot.hakutapa ["hakutapa_01" "hakutapa_02"]}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->nested-filters-aggregation
;;              "hakutiedot"
;;              :search_terms.hakutiedot.pohjakoulutusvaatimukset
;;              ["pohjakoulutusvaatimuskonfo_am" "pohjakoulutusvaatimuskonfo_003"]
;;              "2022-08-26T07:21"
;;              {:hakutapa ["hakutapa_01" "hakutapa_02"]}))))

;;   (testing "Should form aggs filters for valintatapa with hakutapa as a selected filter"
;;     (is (= {:filters
;;              {:filters
;;               {:valintatapajono_av
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:term {:search_terms.hakutiedot.valintatavat
;;                                                     "valintatapajono_av"}}}}}}
;;                   {:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:terms {:search_terms.hakutiedot.hakutapa
;;                                                     ["hakutapa_01" "hakutapa_02"]}}}}}}]}}
;;                :valintatapajono_yp
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:term {:search_terms.hakutiedot.valintatavat
;;                                                    "valintatapajono_yp"}}}}}}
;;                   {:nested {:path "search_terms.hakutiedot"
;;                                    :query {:bool {:filter
;;                                                   {:terms {:search_terms.hakutiedot.hakutapa
;;                                                            ["hakutapa_01" "hakutapa_02"]}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->nested-filters-aggregation
;;              "hakutiedot"
;;              :search_terms.hakutiedot.valintatavat
;;              ["valintatapajono_av" "valintatapajono_yp"]
;;              "2022-08-26T07:21"
;;              {:hakutapa ["hakutapa_01" "hakutapa_02"]}))))

;;   (testing "Should form aggs filters for valintatapa with one valintatapa as a selected filter"
;;     (is (= {:filters
;;              {:filters
;;               {:valintatapajono_av
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:term {:search_terms.hakutiedot.valintatavat
;;                                                     "valintatapajono_av"}}}}}}]}}
;;                :valintatapajono_yp
;;                {:bool
;;                 {:filter
;;                  [{:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_yp"}}}}}}
;;                   {:nested {:path "search_terms.hakutiedot"
;;                             :query {:bool {:filter
;;                                            {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_av"}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->nested-filters-aggregation
;;              "hakutiedot"
;;              :search_terms.hakutiedot.valintatavat
;;              ["valintatapajono_av" "valintatapajono_yp"]
;;              "2022-08-26T07:21"
;;              {:valintatapa ["valintatapajono_av"]})))))

;; (deftest filters-aggregation-test
;;   (testing "Should form aggs filters for opetuskieli without any selected filters"
;;     (is (= {:filters
;;             {:filters
;;              {:oppilaitoksenopetuskieli_4 {:bool {:filter [{:term {:search_terms.opetuskielet.keyword
;;                                                                   "oppilaitoksenopetuskieli_4"}}]}}
;;               :oppilaitoksenopetuskieli_5 {:bool {:filter [{:term {:search_terms.opetuskielet.keyword
;;                                                                   "oppilaitoksenopetuskieli_5"}}]}}
;;               }}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.opetuskielet.keyword ["oppilaitoksenopetuskieli_4" "oppilaitoksenopetuskieli_5"]
;;              "2022-08-26T07:21"
;;              {}))))

;;   (testing "Should form aggs filters for opetuskieli with jotpa and hakukaynnissa as selected filters"
;;     (is (= {:filters
;;             {:filters
;;              {:oppilaitoksenopetuskieli_4
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.opetuskielet.keyword
;;                          "oppilaitoksenopetuskieli_4"}}
;;                  {:bool
;;                   {:should
;;                    [{:bool
;;                      {:filter
;;                       [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                        {:bool
;;                         {:should
;;                          [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                     {:nested
;;                      {:path "search_terms.hakutiedot.hakuajat"
;;                       :query
;;                       {:bool
;;                        {:filter
;;                         [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                          {:bool
;;                           {:should
;;                            [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                             {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
;;                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
;;               :oppilaitoksenopetuskieli_5
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.opetuskielet.keyword
;;                          "oppilaitoksenopetuskieli_5"}}
;;                  {:bool
;;                   {:should
;;                    [{:bool
;;                      {:filter
;;                       [{:range {:search_terms.toteutusHakuaika.alkaa {:lte "2022-08-26T07:21"}}}
;;                        {:bool
;;                         {:should
;;                          [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}}
;;                           {:range {:search_terms.toteutusHakuaika.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}
;;                     {:nested
;;                      {:path "search_terms.hakutiedot.hakuajat"
;;                       :query
;;                       {:bool
;;                        {:filter
;;                         [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte "2022-08-26T07:21"}}}
;;                          {:bool
;;                           {:should
;;                            [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
;;                             {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt "2022-08-26T07:21"}}}]}}]}}}}]}}
;;                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}
;;                  ]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.opetuskielet.keyword ["oppilaitoksenopetuskieli_4" "oppilaitoksenopetuskieli_5"]
;;              "2022-08-26T07:21"
;;              {:jotpa true :hakukaynnissa true}
;;              ))))

;;   (testing "Should form aggs filters for koulutustyyppi without any selected filters"
;;     (is (= {:filters
;;             {:filters
;;              {:amm {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amm"}}]}}
;;               :amk {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amk"}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {}))))

;;   (testing "Should form aggs filters for koulutustyyppi with one opetustapa as a selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:amm {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amm"}}
;;                             {:term {:search_terms.opetustavat.keyword "opetuspaikkakk_4"}}]}}
;;               :amk {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                             {:term {:search_terms.opetustavat.keyword "opetuspaikkakk_4"}}
;;                             ]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:opetustapa ["opetuspaikkakk_4"]}))))

;;   (testing "Should form aggs filters for koulutustyyppi with two opetustapa filters"
;;     (is (= (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:opetustapa ["opetuspaikkakk_3" "opetuspaikkakk_4"]})
;;            {:filters
;;             {:filters
;;              {:amm {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amm"}}
;;                             {:terms {:search_terms.opetustavat.keyword ["opetuspaikkakk_3" "opetuspaikkakk_4"]}}]}}
;;               :amk {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                             {:terms {:search_terms.opetustavat.keyword ["opetuspaikkakk_3" "opetuspaikkakk_4"]}}
;;                             ]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            )))

;;   (testing "Should form aggs filters for koulutustyyppi with yhteishaku filter"
;;     (is (= {:filters
;;             {:filters
;;              {:amm {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amm"}}
;;                             {:nested
;;                              {:path "search_terms.hakutiedot"
;;                               :query
;;                               {:bool {:filter
;;                                       {:terms {:search_terms.hakutiedot.yhteishakuOid
;;                                               ["1.2.246.562.29.00000000000000000001"
;;                                                "1.2.246.562.29.00000000000000000002"]}}}}}}]}}
;;               :amk {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                             {:nested
;;                              {:path "search_terms.hakutiedot"
;;                               :query
;;                               {:bool {:filter
;;                                       {:terms {:search_terms.hakutiedot.yhteishakuOid
;;                                               ["1.2.246.562.29.00000000000000000001"
;;                                                "1.2.246.562.29.00000000000000000002" ]}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:yhteishaku ["1.2.246.562.29.00000000000000000001" "1.2.246.562.29.00000000000000000002"]}))))

;;   (testing "Should form aggs filters for koulutustyyppi with yhteishaku filter"
;;     (is (= {:filters
;;             {:filters
;;              {:amm {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amm"}}
;;                             {:nested
;;                              {:path "search_terms.hakutiedot"
;;                               :query
;;                               {:bool {:filter
;;                                       {:terms {:search_terms.hakutiedot.yhteishakuOid
;;                                               ["1.2.246.562.29.00000000000000000001"
;;                                                "1.2.246.562.29.00000000000000000002"]}}}}}}]}}
;;               :amk {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                             {:nested
;;                              {:path "search_terms.hakutiedot"
;;                               :query
;;                               {:bool {:filter
;;                                       {:terms {:search_terms.hakutiedot.yhteishakuOid
;;                                               ["1.2.246.562.29.00000000000000000001"
;;                                                "1.2.246.562.29.00000000000000000002" ]}}}}}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:yhteishaku ["1.2.246.562.29.00000000000000000001" "1.2.246.562.29.00000000000000000002"]}))))

;;   (testing "Should form aggs filters for sijainti with koulutustyyppi filter"
;;     (is (= {:filters
;;             {:filters
;;              {:kunta_01
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.sijainti.keyword "kunta_01"}}
;;                  {:terms {:search_terms.koulutustyypit.keyword ["amm" "amk"]}}]}}
;;               :maakunta_02
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.sijainti.keyword "maakunta_02"}}
;;                  {:terms {:search_terms.koulutustyypit.keyword ["amm" "amk"]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.sijainti.keyword ["kunta_01" "maakunta_02"]
;;              "2022-08-26T07:21"
;;              {:koulutustyyppi ["amm" "amk"]}))))

;;   (testing "Should form aggs filters for koulutustyyppi with sijainti filter"
;;     (is (= {:filters
;;             {:filters
;;              {:amm
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.koulutustyypit.keyword "amm"}}
;;                  {:terms {:search_terms.sijainti.keyword ["kunta_01" "maakunta_02"]}}]}}
;;               :amk
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                  {:terms {:search_terms.sijainti.keyword ["kunta_01" "maakunta_02"]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:sijainti ["kunta_01" "maakunta_02"]}))))

;;   (testing "Should form aggs filters for koulutustyyppi with koulutusala filter"
;;     (is (= {:filters
;;             {:filters
;;              {:amm
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.koulutustyypit.keyword "amm"}}
;;                  {:terms {:search_terms.koulutusalat.keyword ["kansallinenkoulutusluokitus2016koulutusalataso1_01"
;;                                                               "kansallinenkoulutusluokitus2016koulutusalataso1_02"]}}]}}
;;               :amk
;;               {:bool
;;                {:filter
;;                 [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                  {:terms {:search_terms.koulutusalat.keyword ["kansallinenkoulutusluokitus2016koulutusalataso1_01"
;;                                                               "kansallinenkoulutusluokitus2016koulutusalataso1_02"]}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:koulutusala ["kansallinenkoulutusluokitus2016koulutusalataso1_01"
;;                             "kansallinenkoulutusluokitus2016koulutusalataso1_02"]}))))

;;   (testing "Should not have duplicates for amm filter agg when it is also the selected filter"
;;     (is (= {:filters
;;             {:filters
;;              {:amm {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amm"}}]}}
;;               :amk {:bool {:filter
;;                            [{:term {:search_terms.koulutustyypit.keyword "amk"}}
;;                             {:term {:search_terms.koulutustyypit.keyword "amm"}}]}}}}
;;             :aggs {:real_hits {:reverse_nested {}}}}
;;            (->filters-aggregation
;;              :search_terms.koulutustyypit.keyword ["amm" "amk"]
;;              "2022-08-26T07:21"
;;              {:koulutustyyppi ["amm"]})))))

(use 'clojure.test)
(run-tests)
