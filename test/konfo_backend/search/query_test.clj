(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [hakutulos-aggregations
                                                jarjestajat-aggregations query
                                                tarjoajat-aggregations]]
            [konfo-backend.search.rajain.query-tools :refer [koulutustyypit]]
            [konfo-backend.tools]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.test]))

(deftest oppilaitos-query-test
  (testing
   "Query with filters"
    (is (= (query nil {:sijainti ["kunta_091"] :koulutustyyppi ["amm" "KK"]} "fi" ["words"])
           {:nested {:path "search_terms"
                     :query {:bool {:filter
                                    [{:terms {:search_terms.koulutustyypit.keyword ["amm" "kk"]}}
                                     {:term {:search_terms.sijainti.keyword "kunta_091"}}]}}}})))
  (testing
   "Query with hakukaynnissa filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
       (= (query nil {:hakukaynnissa true} "fi" ["words"])
          {:nested
           {:path "search_terms"
            :query
            {:bool
             {:filter
              [{:bool
                {:should
                 [{:bool
                   {:filter
                    [{:range
                      {:search_terms.toteutusHakuaika.alkaa
                       {:lte "2020-01-01T01:01"}}}
                     {:bool
                      {:should
                       [{:bool
                         {:must_not
                          {:exists
                           {:field
                            "search_terms.toteutusHakuaika.paattyy"}}}}
                        {:range
                         {:search_terms.toteutusHakuaika.paattyy
                          {:gt "2020-01-01T01:01"}}}]}}]}}
                  {:nested
                   {:path "search_terms.hakutiedot.hakuajat",
                    :query
                    {:bool
                     {:filter
                      [{:range
                        {:search_terms.hakutiedot.hakuajat.alkaa
                         {:lte "2020-01-01T01:01"}}}
                       {:bool
                        {:should
                         [{:bool
                           {:must_not
                            {:exists
                             {:field
                              "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                          {:range
                           {:search_terms.hakutiedot.hakuajat.paattyy
                            {:gt "2020-01-01T01:01"}}}]}}]}}}}]}}]}}}})))))

(defn- default-agg
  ([field constrained-filter term-props]
   {:terms (merge {:field field
                   :min_doc_count 0
                   :size 1000} term-props)
    :aggs {:constrained (merge (if constrained-filter {:filter constrained-filter} {:reverse_nested {}})
                               {:aggs {:real_hits {:reverse_nested {}}}})}})
  ([field] (default-agg field nil nil))
  ([field constrained] (default-agg field constrained nil)))

(def jotpa-term {:term {:search_terms.hasJotpaRahoitus true}})

(def jotpa-bool-filter {:bool {:filter [jotpa-term]}})

(deftest hakutulos-aggregations-test
  (testing
   "Aggregations"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
       (match? (m/match-with [map? m/equals]
        {:hits_aggregation
         {:nested {:path "search_terms"}
          :aggs
          {:koulutusala (default-agg "search_terms.koulutusalat.keyword")
           :yhteishaku {:nested {:path "search_terms.hakutiedot"}
                        :aggs {:yhteishaku (default-agg "search_terms.hakutiedot.yhteishakuOid")}}
           :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"})
           :pohjakoulutusvaatimus {:nested {:path "search_terms.hakutiedot"}
                                   :aggs {:pohjakoulutusvaatimus (default-agg "search_terms.hakutiedot.pohjakoulutusvaatimukset")}}
           :maakunta (default-agg "search_terms.sijainti.keyword"nil {:include "maakunta.*"})
           :hakutapa {:nested {:path "search_terms.hakutiedot"}
                      :aggs {:hakutapa (default-agg "search_terms.hakutiedot.hakutapa")}}
           :opetustapa (default-agg "search_terms.opetustavat.keyword")
           :opetuskieli (default-agg "search_terms.opetuskielet.keyword")
           :valintatapa {:nested {:path "search_terms.hakutiedot"}
                         :aggs {:valintatapa (default-agg "search_terms.hakutiedot.valintatavat")}}
           :koulutustyyppi (default-agg "search_terms.koulutustyypit.keyword" nil {:size (count koulutustyypit) :include koulutustyypit})
           :hakukaynnissa {:filter
                           {:bool
                            {:filter
                             [{:bool
                               {:should
                                [{:bool
                                  {:filter
                                   [{:range
                                     {:search_terms.toteutusHakuaika.alkaa
                                      {:lte "2020-01-01T01:01"}}}
                                    {:bool
                                     {:should
                                      [{:bool
                                        {:must_not
                                         {:exists
                                          {:field
                                           "search_terms.toteutusHakuaika.paattyy"}}}}
                                       {:range
                                        {:search_terms.toteutusHakuaika.paattyy
                                         {:gt "2020-01-01T01:01"}}}]}}]}}
                                 {:nested
                                  {:path "search_terms.hakutiedot.hakuajat" ,
                                   :query
                                   {:bool
                                    {:filter
                                     [{:range
                                       {:search_terms.hakutiedot.hakuajat.alkaa
                                        {:lte "2020-01-01T01:01"}}}
                                      {:bool
                                       {:should
                                        [{:bool
                                          {:must_not
                                           {:exists
                                            {:field
                                             "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                         {:range
                                          {:search_terms.hakutiedot.hakuajat.paattyy
                                           {:gt
                                            "2020-01-01T01:01"}}}]}}]}}}}]}}]}}
                           :aggs {:real_hits {:reverse_nested {}}}}
           :jotpa {:filter
                   {:bool
                    {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}}
                   :aggs {:real_hits {:reverse_nested {}}}}
           :tyovoimakoulutus {:filter
                              {:bool
                               {:filter [{:term {:search_terms.isTyovoimakoulutus true}}]}}
                              :aggs {:real_hits {:reverse_nested {}}}}
           :taydennyskoulutus {:filter
                               {:bool
                                {:filter [{:term {:search_terms.isTaydennyskoulutus true}}]}}
                               :aggs {:real_hits {:reverse_nested {}}}}}}})
        (hakutulos-aggregations {})))))

  (testing
   "aggregations with selected filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
       (match?
        {:hits_aggregation
         {:nested {:path "search_terms"}
          :aggs
          {:koulutusala (default-agg "search_terms.koulutusalat.keyword" jotpa-bool-filter)
           :yhteishaku {:nested {:path "search_terms.hakutiedot"}
                        :aggs {:yhteishaku (default-agg "search_terms.hakutiedot.yhteishakuOid" jotpa-bool-filter)}}
           :kunta (default-agg "search_terms.sijainti.keyword" jotpa-bool-filter)
           :pohjakoulutusvaatimus {:nested {:path "search_terms.hakutiedot"}
                                   :aggs {:pohjakoulutusvaatimus (default-agg "search_terms.hakutiedot.pohjakoulutusvaatimukset" jotpa-bool-filter)}}
           :maakunta (default-agg "search_terms.sijainti.keyword" jotpa-bool-filter)
           :hakutapa {:nested {:path "search_terms.hakutiedot"}
                      :aggs {:hakutapa (default-agg "search_terms.hakutiedot.hakutapa" jotpa-bool-filter)}}
           :opetustapa (default-agg "search_terms.opetustavat.keyword" jotpa-bool-filter)
           :opetuskieli (default-agg "search_terms.opetuskielet.keyword" jotpa-bool-filter)
           :valintatapa {:nested {:path "search_terms.hakutiedot"}
                         :aggs {:valintatapa (default-agg "search_terms.hakutiedot.valintatavat" jotpa-bool-filter)}}
           :koulutustyyppi (default-agg "search_terms.koulutustyypit.keyword" jotpa-bool-filter {:size (count koulutustyypit) :include koulutustyypit})
           :hakukaynnissa {:filter
                           {:bool
                            {:filter
                             [jotpa-term
                              {:bool
                               {:should
                                [{:bool
                                  {:filter
                                   [{:range
                                     {:search_terms.toteutusHakuaika.alkaa
                                      {:lte "2020-01-01T01:01"}}}
                                    {:bool
                                     {:should
                                      [{:bool
                                        {:must_not
                                         {:exists
                                          {:field
                                           "search_terms.toteutusHakuaika.paattyy"}}}}
                                       {:range
                                        {:search_terms.toteutusHakuaika.paattyy
                                         {:gt "2020-01-01T01:01"}}}]}}]}}
                                 {:nested
                                  {:path "search_terms.hakutiedot.hakuajat" ,
                                   :query
                                   {:bool
                                    {:filter
                                     [{:range
                                       {:search_terms.hakutiedot.hakuajat.alkaa
                                        {:lte "2020-01-01T01:01"}}}
                                      {:bool
                                       {:should
                                        [{:bool
                                          {:must_not
                                           {:exists
                                            {:field
                                             "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                         {:range
                                          {:search_terms.hakutiedot.hakuajat.paattyy
                                           {:gt
                                            "2020-01-01T01:01"}}}]}}]}}}}]}}]}}
                           :aggs {:real_hits {:reverse_nested {}}}}
           :jotpa {:filter
                   {:bool
                    {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}}
                   :aggs {:real_hits {:reverse_nested {}}}}
           :tyovoimakoulutus {:filter
                              {:bool
                               {:filter [{:term {:search_terms.isTyovoimakoulutus true}}]}}
                              :aggs {:real_hits {:reverse_nested {}}}}
           :taydennyskoulutus {:filter
                               {:bool
                                {:filter [{:term {:search_terms.isTaydennyskoulutus true}}]}}
                               :aggs {:real_hits {:reverse_nested {}}}}}}}
        (hakutulos-aggregations {:jotpa true}))))))

(deftest jarjestajat-aggregations-test
  (testing "should form aggregations for jarjestajat query with selected filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
       (= {:hits_aggregation
           {:nested {:path "search_terms"}
            :aggs
            {:inner_hits_agg
             {:filter {:bool
                       {:must [{:term {"search_terms.onkoTuleva" false}} {:bool {}}],
                        :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}},
              :aggs
              {:yhteishaku
               {:filters
                {:filters
                 {:1.2.246.562.29.00000000000000000001
                  {:bool
                   {:filter
                    [{:nested {:path "search_terms.hakutiedot"
                               :query
                               {:bool {:filter
                                       {:term {:search_terms.hakutiedot.yhteishakuOid "1.2.246.562.29.00000000000000000001"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :kunta
               {:filters
                {:filters {:kunta_01 {:bool {:filter [{:term {:search_terms.sijainti.keyword "kunta_01"}}]}}
                           :kunta_02 {:bool {:filter [{:term {:search_terms.sijainti.keyword "kunta_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :pohjakoulutusvaatimus
               {:filters
                {:filters
                 {:pohjakoulutusvaatimuskonfo_01
                  {:bool
                   {:filter
                    [{:nested {:path "search_terms.hakutiedot"
                               :query
                               {:bool {:filter
                                       {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_01"}}}}}}]}}
                  :pohjakoulutusvaatimuskonfo_02
                  {:bool
                   {:filter
                    [{:nested {:path "search_terms.hakutiedot"
                               :query {:bool {:filter
                                              {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset "pohjakoulutusvaatimuskonfo_02"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :maakunta
               {:filters
                {:filters
                 {:maakunta_01 {:bool {:filter [{:term {:search_terms.sijainti.keyword "maakunta_01"}}]}}
                  :maakunta_02 {:bool {:filter [{:term {:search_terms.sijainti.keyword "maakunta_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :hakutapa
               {:filters
                {:filters
                 {:hakutapa_01
                  {:bool {:filter
                          [{:nested {:path "search_terms.hakutiedot"
                                     :query {:bool
                                             {:filter
                                              {:term {:search_terms.hakutiedot.hakutapa "hakutapa_01"}}}}}}]}}
                  :hakutapa_02
                  {:bool {:filter
                          [{:nested {:path "search_terms.hakutiedot"
                                     :query {:bool
                                             {:filter
                                              {:term {:search_terms.hakutiedot.hakutapa "hakutapa_02"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :opetustapa
               {:filters
                {:filters
                 {:opetuspaikkakk_01
                  {:bool {:filter [{:term {:search_terms.opetustavat.keyword "opetuspaikkakk_01"}}]}}
                  :opetuspaikkakk_02
                  {:bool {:filter [{:term {:search_terms.opetustavat.keyword "opetuspaikkakk_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :opetuskieli
               {:filters
                {:filters
                 {:oppilaitoksenopetuskieli_01
                  {:bool {:filter [{:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_01"}}]}}
                  :oppilaitoksenopetuskieli_02
                  {:bool {:filter [{:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :hakukaynnissa
               {:filters
                {:filters
                 {:hakukaynnissa
                  {:bool
                   {:filter
                    [{:bool
                      {:should
                       [{:bool
                         {:filter
                          [{:range
                            {:search_terms.toteutusHakuaika.alkaa
                             {:lte "2020-01-01T01:01"}}}
                           {:bool
                            {:should
                             [{:bool
                               {:must_not
                                {:exists
                                 {:field
                                  "search_terms.toteutusHakuaika.paattyy"}}}}
                              {:range
                               {:search_terms.toteutusHakuaika.paattyy
                                {:gt "2020-01-01T01:01"}}}]}}]}}
                        {:nested
                         {:path "search_terms.hakutiedot.hakuajat"
                          :query
                          {:bool
                           {:filter
                            [{:range
                              {:search_terms.hakutiedot.hakuajat.alkaa
                               {:lte "2020-01-01T01:01"}}}
                             {:bool
                              {:should
                               [{:bool
                                 {:must_not
                                  {:exists
                                   {:field
                                    "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                {:range
                                 {:search_terms.hakutiedot.hakuajat.paattyy
                                  {:gt
                                   "2020-01-01T01:01"}}}]}}]}}}}]}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :valintatapa
               {:filters
                {:filters
                 {:valintatapajono_01
                  {:bool {:filter
                          [{:nested {:path "search_terms.hakutiedot"
                                     :query
                                     {:bool
                                      {:filter
                                       {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_01"}}}}}}]}}
                  :valintatapajono_02
                  {:bool {:filter
                          [{:nested
                            {:path "search_terms.hakutiedot"
                             :query {:bool
                                     {:filter
                                      {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_02"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :oppilaitos
               {:filters
                {:filters
                 {:1.2.246.562.10.98873174761
                  {:bool
                   {:filter
                    [{:term {"search_terms.oppilaitosOid.keyword" "1.2.246.562.10.98873174761"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}}}
             :lukiopainotukset_aggs
             {:filter
              {:bool
               {:must {:term {"search_terms.onkoTuleva" false}}
                :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}}
              :aggs
              {"lukiopainotukset"
               {:filters
                {:filters
                 {:lukiopainotukset_01
                  {:bool
                   {:filter
                    [{:term {:search_terms.lukiopainotukset.keyword "lukiopainotukset_01"}}]}}
                  :lukiopainotukset_02
                  {:bool
                   {:filter
                    [{:term {:search_terms.lukiopainotukset.keyword "lukiopainotukset_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}}}
             :lukiolinjaterityinenkoulutustehtava_aggs
             {:filter
              {:bool
               {:must {:term {"search_terms.onkoTuleva" false}}
                :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}}
              :aggs
              {"lukiolinjaterityinenkoulutustehtava"
               {:filters
                {:filters
                 {:lukiolinjaterityinenkoulutustehtava_01
                  {:bool
                   {:filter
                    [{:term {:search_terms.lukiolinjaterityinenkoulutustehtava.keyword "lukiolinjaterityinenkoulutustehtava_01"}}]}}
                  :lukiolinjaterityinenkoulutustehtava_02
                  {:bool
                   {:filter
                    [{:term {:search_terms.lukiolinjaterityinenkoulutustehtava.keyword "lukiolinjaterityinenkoulutustehtava_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}}}
             :osaamisala_aggs
             {:filter
              {:bool
               {:must {:term {"search_terms.onkoTuleva" false}}
                :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}}
              :aggs
              {:osaamisala
               {:filters
                {:filters
                 {:osaamisala_01
                  {:bool
                   {:filter
                    [{:term {:search_terms.osaamisalat.keyword "osaamisala_01"}}]}}
                  :osaamisala_02
                  {:bool
                   {:filter
                    [{:term {:search_terms.osaamisalat.keyword "osaamisala_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}}}}}}
          (jarjestajat-aggregations false {:sijainti ["kunta_564"]}))))))

(deftest tarjoajat-aggregations-test
  (testing "should form aggregations for tarjoajat query with selected filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
       (= {:hits_aggregation
           {:nested {:path "search_terms"}
            :aggs
            {:inner_hits_agg
             {:filter {:bool
                       {:must [{:term {"search_terms.onkoTuleva" false}} {:bool {}}]
                        :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}}
              :aggs
              {:yhteishaku
               {:filters {:filters
                          {:1.2.246.562.29.00000000000000000001
                           {:bool {:filter [{:nested
                                             {:path "search_terms.hakutiedot"
                                              :query
                                              {:bool {:filter
                                                      {:term {:search_terms.hakutiedot.yhteishakuOid
                                                              "1.2.246.562.29.00000000000000000001"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :kunta
               {:filters {:filters {:kunta_01 {:bool {:filter [{:term {:search_terms.sijainti.keyword "kunta_01"}}]}}
                                    :kunta_02 {:bool {:filter [{:term {:search_terms.sijainti.keyword "kunta_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :pohjakoulutusvaatimus
               {:filters
                {:filters
                 {:pohjakoulutusvaatimuskonfo_01
                  {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
                                             :query {:bool {:filter
                                                            {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                                    "pohjakoulutusvaatimuskonfo_01"}}}}}}]}}
                  :pohjakoulutusvaatimuskonfo_02
                  {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
                                             :query {:bool {:filter
                                                            {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                                    "pohjakoulutusvaatimuskonfo_02"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :maakunta
               {:filters {:filters
                          {:maakunta_01 {:bool {:filter [{:term {:search_terms.sijainti.keyword "maakunta_01"}}]}}
                           :maakunta_02 {:bool {:filter [{:term {:search_terms.sijainti.keyword "maakunta_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :hakutapa
               {:filters
                {:filters
                 {:hakutapa_01
                  {:bool {:filter
                          [{:nested {:path "search_terms.hakutiedot"
                                     :query {:bool
                                             {:filter
                                              {:term {:search_terms.hakutiedot.hakutapa "hakutapa_01"}}}}}}]}}
                  :hakutapa_02
                  {:bool {:filter
                          [{:nested {:path "search_terms.hakutiedot"
                                     :query {:bool
                                             {:filter
                                              {:term {:search_terms.hakutiedot.hakutapa "hakutapa_02"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :opetustapa
               {:filters
                {:filters
                 {:opetuspaikkakk_01
                  {:bool {:filter [{:term {:search_terms.opetustavat.keyword "opetuspaikkakk_01"}}]}}
                  :opetuspaikkakk_02
                  {:bool {:filter [{:term {:search_terms.opetustavat.keyword "opetuspaikkakk_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :opetuskieli
               {:filters
                {:filters
                 {:oppilaitoksenopetuskieli_01
                  {:bool {:filter [{:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_01"}}]}}
                  :oppilaitoksenopetuskieli_02
                  {:bool {:filter [{:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :hakukaynnissa
               {:filters
                {:filters
                 {:hakukaynnissa
                  {:bool
                   {:filter
                    [{:bool
                      {:should
                       [{:bool
                         {:filter
                          [{:range
                            {:search_terms.toteutusHakuaika.alkaa
                             {:lte "2020-01-01T01:01"}}}
                           {:bool
                            {:filter
                             [{:range
                               {:search_terms.hakutiedot.hakuajat.alkaa
                                {:lte "2020-01-01T01:01"}}}
                              {:bool
                               {:should
                                [{:bool
                                  {:must_not
                                   {:exists
                                    {:field
                                     "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                 {:range
                                  {:search_terms.hakutiedot.hakuajat.paattyy
                                   {:gt
                                    "2020-01-01T01:01"}}}]}}]}}]}}]}}]}}}}
                 :aggs {:real_hits {:reverse_nested {}}}}
                :valintatapa
                {:filters
                 {:filters
                  {:valintatapajono_01
                   {:bool {:filter
                           [{:nested {:path "search_terms.hakutiedot"
                                      :query
                                      {:bool
                                       {:filter
                                        {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_01"}}}}}}
                            {:term {:search_terms.sijainti.keyword "kunta_564"}}]}}
                  :valintatapajono_02
                  {:bool {:filter
                          [{:nested
                            {:path "search_terms.hakutiedot"
                             :query {:bool
                                     {:filter
                                      {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_02"}}}}}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :koulutusala
               {:filters
                {:filters
                 {:kansallinenkoulutusluokitus2016koulutusalataso1_01
                  {:bool {:filter [{:term {:search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_01"}}]}}
                  :kansallinenkoulutusluokitus2016koulutusalataso1_02
                  {:bool {:filter [{:term {:search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}
               :koulutustyyppi
               {:filters
                {:filters
                 {:amm-osaamisala {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amm-osaamisala"}}]}}
                  :kandi {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "kandi"}}]}}
                  :amk-ylempi {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amk-ylempi"}}]}}
                  :tohtori {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "tohtori"}}]}}
                  :amm-tutkinnon-osa {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amm-tutkinnon-osa"}}]}}
                  :lk {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "lk"}}]}}
                  :maisteri {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "maisteri"}}]}}
                  :amk {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amk"}}]}}
                  :amk-muu {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amk-muu"}}]}}
                  :amk-alempi {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amk-alempi"}}]}}
                  :kandi-ja-maisteri {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "kandi-ja-maisteri"}}]}}
                  :yo {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "yo"}}]}}
                  :amm {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amm"}}]}}
                  :amm-muu {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amm-muu"}}]}}
                  :tuva {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "tuva"}}]}}
                  :tuva-normal {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "tuva-normal"}}]}}
                  :tuva-erityisopetus {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "tuva-erityisopetus"}}]}}
                  :telma {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "telma"}}]}}
                  :amm-ope-erityisope-ja-opo {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "amm-ope-erityisope-ja-opo"}}]}}
                  :ope-pedag-opinnot {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "ope-pedag-opinnot"}}]}}
                  :vapaa-sivistystyo {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo"}}]}}
                  :vapaa-sivistystyo-opistovuosi {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo-opistovuosi"}}]}}
                  :vapaa-sivistystyo-muu {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo-muu"}}]}}
                  :aikuisten-perusopetus {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "aikuisten-perusopetus"}}]}}
                  :kk-opintojakso {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "kk-opintojakso"}}]}}
                  :kk-opintokokonaisuus {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "kk-opintokokonaisuus"}}]}}
                  :erikoislaakari {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "erikoislaakari"}}]}}
                  :erikoistumiskoulutus {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "erikoistumiskoulutus"}}]}}
                  :taiteen-perusopetus {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "taiteen-perusopetus"}}]}}
                  :muu {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "muu"}}]}}}}
                :aggs {:real_hits {:reverse_nested {}}}}}}}}}
          (tarjoajat-aggregations false {:sijainti ["kunta_564"]}))))))
