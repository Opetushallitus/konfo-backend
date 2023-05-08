(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [constraints-post-filter-query
                                                hakutulos-aggregations
                                                jarjestajat-aggregations
                                                tarjoajat-aggregations]]
            [konfo-backend.search.rajain.query-tools :refer [koulutustyypit]]
            [konfo-backend.tools]
            [clojure.string :refer [replace-first split join]]
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

(defn- make-terms-agg [field-name term-props]
  {:terms (merge {:field field-name
                  :min_doc_count 0
                  :size 1000} term-props)
   :aggs {:real_hits {:reverse_nested {}}}})

(defn- default-agg
  ([field-name constrained-filter term-props]
   (let [terms-agg (make-terms-agg field-name term-props)]
     (if constrained-filter
       {:filter constrained-filter
        :aggs {:rajain terms-agg}}
       terms-agg)))
  ([field] (default-agg field nil nil))
  ([field constrained] (default-agg field constrained nil)))

(defn- default-nested-agg ([rajain-key field-name constrained-filter term-props]
  (let [terms-agg (make-terms-agg field-name term-props)
        nested-agg {:nested  {:path (-> field-name
                                        (replace-first ".keyword" "")
                                        (split #"\.")
                                        (drop-last) (#(join "." %)))}
                    :aggs {:rajain terms-agg}}]
    (if constrained-filter
      {:filter constrained-filter
       :aggs {(keyword rajain-key) nested-agg}}
      nested-agg)))
  ([rajain-key field-name] (default-nested-agg rajain-key field-name nil nil)))

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
                                :yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid")
                                :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"})
                                :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset")
                                :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"})
                                :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa")
                                :opetustapa (default-agg "search_terms.opetustavat.keyword")
                                :opetuskieli (default-agg "search_terms.opetuskielet.keyword")
                                :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat")
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
           :yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid" jotpa-bool-filter nil)
           :kunta (default-agg "search_terms.sijainti.keyword" jotpa-bool-filter)
           :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset" jotpa-bool-filter nil)
           :maakunta (default-agg "search_terms.sijainti.keyword" jotpa-bool-filter)
           :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa" jotpa-bool-filter nil)
           :opetustapa (default-agg "search_terms.opetustavat.keyword" jotpa-bool-filter)
           :opetuskieli (default-agg "search_terms.opetuskielet.keyword" jotpa-bool-filter)
           :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat" jotpa-bool-filter nil)
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
            {:yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid")
             :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"})
             :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset")
             :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"})
             :oppilaitos (default-agg "search_terms.oppilaitosOid.keyword" nil {:min_doc_count 1
                                                                                :size 10000})
             :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa")
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
             :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat")}}
           :lukiopainotukset-aggs (default-agg "search_terms.lukiopainotukset.keyword")
           :lukiolinjaterityinenkoulutustehtava-aggs (default-agg "search_terms.lukiolinjaterityinenkoulutustehtava.keyword")
           :osaamisala-aggs (default-agg "search_terms.osaamisalat.keyword")}}}
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
                    :yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid")
                    :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"})
                    :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset")
                    :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"})
                    :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa")
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
                    :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat")}}}}}
               (tarjoajat-aggregations false {:sijainti ["kunta_564"]}))))))
