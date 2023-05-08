(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [constraints-post-filter-query
                                                hakutulos-aggregations
                                                jarjestajat-aggregations
                                                tarjoajat-aggregations]]
            [konfo-backend.search.rajain.query-tools :refer [koulutustyypit]]
            [konfo-backend.tools]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.test]))

(deftest oppilaitos-query-test
  (testing
   "Query with filters"
    (is (= (constraints-post-filter-query {:sijainti ["kunta_091"] :koulutustyyppi ["amm" "KK"]})
           {:nested {:path "search_terms"
                     :query {:bool {:filter
                                    [{:terms {:search_terms.koulutustyypit.keyword ["amm" "kk"]}}
                                     {:term {:search_terms.sijainti.keyword "kunta_091"}}]}}}})))
  (testing
   "Query with hakukaynnissa filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
        (= (constraints-post-filter-query {:hakukaynnissa true})
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
           :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"})
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
       (match?
         {:hits_aggregation
           {:nested {:path "search_terms"}
            :aggs
            {:inner_hits_agg
             {:filter {:bool
                       {:must [{:term {"search_terms.onkoTuleva" false}} {:bool {}}],
                        :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}},
              :aggs
              {:yhteishaku {:nested {:path "search_terms.hakutiedot"}
                            :aggs {:yhteishaku (default-agg "search_terms.hakutiedot.yhteishakuOid")}}
               :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"})
               :pohjakoulutusvaatimus {:nested {:path "search_terms.hakutiedot"}
                                       :aggs {:pohjakoulutusvaatimus (default-agg "search_terms.hakutiedot.pohjakoulutusvaatimukset")}}
               :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"})
               :oppilaitos (default-agg "search_terms.oppilaitosOid.keyword" nil {:min_doc_count 1
                                                                                  :size 10000})
               :hakutapa {:nested {:path "search_terms.hakutiedot"}
                          :aggs {:hakutapa (default-agg "search_terms.hakutiedot.hakutapa")}}
               :opetustapa (default-agg "search_terms.opetustavat.keyword")
               :opetuskieli (default-agg "search_terms.opetuskielet.keyword")
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
               :valintatapa {:nested {:path "search_terms.hakutiedot"}
                             :aggs {:valintatapa (default-agg "search_terms.hakutiedot.valintatavat")}}}}
               :lukiopainotukset-aggs (default-agg "search_terms.lukiopainotukset.keyword")
               :lukiolinjaterityinenkoulutustehtava-aggs (default-agg "search_terms.lukiolinjaterityinenkoulutustehtava.keyword")
               :osaamisala-aggs (default-agg "search_terms.osaamisalat.keyword")
               }}}
          (jarjestajat-aggregations false {:sijainti ["kunta_564"]}))))))

(deftest tarjoajat-aggregations-test
  (testing "should form aggregations for tarjoajat query with selected filters"
    (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")]
      (is
       (match? {:hits_aggregation
           {:nested {:path "search_terms"}
            :aggs
            {:inner_hits_agg
             {:filter {:bool
                       {:must [{:term {"search_terms.onkoTuleva" false}} {:bool {}}]
                        :filter [{:term {:search_terms.sijainti.keyword "kunta_564"}}]}}
              :aggs
              {:koulutusala (default-agg "search_terms.koulutusalat.keyword")
               :yhteishaku {:nested {:path "search_terms.hakutiedot"}
                            :aggs {:yhteishaku (default-agg "search_terms.hakutiedot.yhteishakuOid")}}
               :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"})
               :pohjakoulutusvaatimus {:nested {:path "search_terms.hakutiedot"}
                                       :aggs {:pohjakoulutusvaatimus (default-agg "search_terms.hakutiedot.pohjakoulutusvaatimukset")}}
               :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"})
               :hakutapa {:nested {:path "search_terms.hakutiedot"}
                          :aggs {:hakutapa (default-agg "search_terms.hakutiedot.hakutapa")}}
               :opetustapa (default-agg "search_terms.opetustavat.keyword")
               :opetuskieli (default-agg "search_terms.opetuskielet.keyword")
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
               :valintatapa {:nested {:path "search_terms.hakutiedot"}
                             :aggs {:valintatapa (default-agg "search_terms.hakutiedot.valintatavat")}}}}}}}
          (tarjoajat-aggregations false {:sijainti ["kunta_564"]}))))))
