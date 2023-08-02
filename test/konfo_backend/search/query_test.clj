(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [post-filter-query
                                                hakutulos-aggregations
                                                jarjestajat-aggregations
                                                tarjoajat-aggregations]]
            [konfo-backend.search.rajain-tools :refer [koulutustyypit]]
            [konfo-backend.test-tools :refer [set-fixed-time]]
            [konfo-backend.tools]
            [clojure.string :refer [replace-first split join]]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.test]))

(deftest oppilaitos-query-test
  (testing
   "Query with filters"
    (is (= (post-filter-query {:sijainti ["kunta_091"] :koulutustyyppi ["amm" "KK"]})
           {:nested {:path "search_terms"
                     :query {:bool {:filter
                                    [{:terms {:search_terms.koulutustyypit.keyword ["amm" "kk"]}}
                                     {:term {:search_terms.sijainti.keyword "kunta_091"}}]}}}})))
  (testing
   "Query with hakukaynnissa filters"
    (set-fixed-time "2020-01-01T01:01:00")
      (is
       (= (post-filter-query {:hakukaynnissa true})
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
                            {:gt "2020-01-01T01:01"}}}]}}]}}}}]}}]}}}}))))

(defn- make-terms-agg [field-name term-props reverse-nested-path]
  {:terms (merge {:field field-name
                  :min_doc_count 0
                  :size 1000} term-props)
   :aggs {:real_hits {:reverse_nested (if reverse-nested-path {:path reverse-nested-path} {})}}})

(defn- default-agg
  ([field-name constrained-filter term-props reverse-nested-path]
   (let [terms-agg (make-terms-agg field-name term-props reverse-nested-path)]
     (if constrained-filter
       {:filter constrained-filter
        :aggs {:rajain terms-agg}}
       terms-agg)))
  ([field] (default-agg field nil nil nil))
  ([field constrained] (default-agg field constrained nil nil)))

(defn- default-nested-agg ([rajain-key field-name constrained-filter term-props reverse-nested-path]
                           (let [terms-agg (make-terms-agg field-name term-props reverse-nested-path)
                                 nested-agg {:nested  {:path (-> field-name
                                                                 (replace-first ".keyword" "")
                                                                 (split #"\.")
                                                                 (drop-last) (#(join "." %)))}
                                             :aggs {:rajain terms-agg}}]
                             (if constrained-filter
                               {:filter constrained-filter
                                :aggs {(keyword rajain-key) nested-agg}}
                               nested-agg)))
  ([rajain-key field-name] (default-nested-agg rajain-key field-name nil nil nil)))

(defn- default-bool-term-agg
  ([field-name field-val constrained-filter]
   (let [own-filter {:term {(keyword field-name) field-val}}]
    {:filter {:bool {:filter (if constrained-filter [constrained-filter own-filter] [own-filter])}}
    :aggs {:real_hits {:reverse_nested {}}}}))
  ([field-name field-val]
   (default-bool-term-agg field-name field-val nil)))

(defn- max-agg
  [field-name constrained-filter]
  (let [agg {:max {:field field-name}}]
    (if constrained-filter
      (let [filter-vec (if (vector? constrained-filter) constrained-filter [constrained-filter])]
        {:filter {:bool {:filter filter-vec}}
         :aggs {:max-val agg}})
      agg)))

(def jotpa-term {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}})
(def jotpa-bool-filter {:bool {:filter [jotpa-term]}})
(def maksullinen-term {:term {:search_terms.metadata.maksullisuustyyppi.keyword "maksullinen"}})
(def lukuvuosimaksu-term {:term {:search_terms.metadata.maksullisuustyyppi.keyword "lukuvuosimaksu"}})

(def alkamiskaudet-include ["henkilokohtainen" "2020-kevat" "2020-syksy" "2021-kevat" "2021-syksy" "2022-kevat"])

(deftest hakutulos-aggregations-test
  (testing
   "Aggregations"
    (set-fixed-time "2020-01-01T01:01:00")
      (is
       (match? (m/match-with [map? m/equals]
                             {:hits_aggregation
                              {:nested {:path "search_terms"}
                               :aggs
                               {:koulutuksenkestokuukausina {:filter
                                                             {:bool
                                                              {:filter
                                                               [{:range
                                                                 {:search_terms.metadata.suunniteltuKestoKuukausina
                                                                  {:gte 0}}}]}},
                                                             :aggs {:real_hits {:reverse_nested {}}}}
                                :koulutuksenkestokuukausina-max (max-agg "search_terms.metadata.suunniteltuKestoKuukausina" nil)
                                :maksuton (default-bool-term-agg "search_terms.metadata.maksullisuustyyppi.keyword" "maksuton")
                                :maksullinen (default-bool-term-agg "search_terms.metadata.maksullisuustyyppi.keyword" "maksullinen")
                                :maksullinen-max (max-agg "search_terms.metadata.maksunMaara" maksullinen-term)
                                :lukuvuosimaksu (default-bool-term-agg "search_terms.metadata.maksullisuustyyppi.keyword" "lukuvuosimaksu")
                                :lukuvuosimaksu-max (max-agg "search_terms.metadata.maksunMaara" lukuvuosimaksu-term)
                                :koulutusala (default-agg "search_terms.koulutusalat.keyword")
                                :yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid")
                                :kunta (default-agg "search_terms.sijainti.keyword" nil {:include "kunta.*"} nil)
                                :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset")
                                :maakunta (default-agg "search_terms.sijainti.keyword" nil {:include "maakunta.*"} nil)
                                :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa")
                                :opetustapa (default-agg "search_terms.opetustavat.keyword")
                                :opetusaika (default-agg "search_terms.metadata.opetusajat.koodiUri.keyword")
                                :opetuskieli (default-agg "search_terms.opetuskielet.keyword")
                                :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat")
                                :alkamiskausi (default-agg "search_terms.paatellytAlkamiskaudet.keyword" nil {:include alkamiskaudet-include} nil)
                                :koulutustyyppi (default-agg "search_terms.koulutustyypit.keyword" nil {:size (count koulutustyypit) :include koulutustyypit} nil)
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
                                :jotpa (default-bool-term-agg "search_terms.hasJotpaRahoitus" true)
                                :tyovoimakoulutus (default-bool-term-agg "search_terms.isTyovoimakoulutus" true)
                                :taydennyskoulutus (default-bool-term-agg "search_terms.isTaydennyskoulutus" true)}}})
               (hakutulos-aggregations {}))))

  (testing
   "aggregations with selected filters"
    (set-fixed-time "2020-01-01T01:01:00")
      (is
       (match? (m/match-with [map? m/equals]
        {:hits_aggregation
         {:nested {:path "search_terms"}
          :aggs
          {:koulutusala (default-agg "search_terms.koulutusalat.keyword" jotpa-bool-filter)
           :yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid" jotpa-bool-filter nil nil)
           :kunta (default-agg "search_terms.sijainti.keyword" jotpa-bool-filter {:include "kunta.*"} nil)
           :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset" jotpa-bool-filter nil nil)
           :maakunta (default-agg "search_terms.sijainti.keyword" jotpa-bool-filter {:include "maakunta.*"} nil)
           :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa" jotpa-bool-filter nil nil)
           :opetustapa (default-agg "search_terms.opetustavat.keyword" jotpa-bool-filter)
           :opetusaika (default-agg "search_terms.metadata.opetusajat.koodiUri.keyword" jotpa-bool-filter)
           :opetuskieli (default-agg "search_terms.opetuskielet.keyword" jotpa-bool-filter)
           :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat" jotpa-bool-filter nil nil)
           :koulutustyyppi (default-agg "search_terms.koulutustyypit.keyword" jotpa-bool-filter {:size (count koulutustyypit) :include koulutustyypit} nil)
           :alkamiskausi (default-agg "search_terms.paatellytAlkamiskaudet.keyword" jotpa-bool-filter {:include alkamiskaudet-include} nil)
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
           :koulutuksenkestokuukausina {:filter
                                         {:bool
                                          {:filter
                                           [jotpa-term
                                            {:range
                                             {:search_terms.metadata.suunniteltuKestoKuukausina
                                              {:gte 0}}}]}},
                                         :aggs {:real_hits {:reverse_nested {}}}}
           :koulutuksenkestokuukausina-max (max-agg "search_terms.metadata.suunniteltuKestoKuukausina" nil)
           :maksuton (default-bool-term-agg "search_terms.metadata.maksullisuustyyppi.keyword" "maksuton" jotpa-term)
           :maksullinen (default-bool-term-agg "search_terms.metadata.maksullisuustyyppi.keyword" "maksullinen" jotpa-term)
           :maksullinen-max (max-agg "search_terms.metadata.maksunMaara" [maksullinen-term])
           :lukuvuosimaksu (default-bool-term-agg "search_terms.metadata.maksullisuustyyppi.keyword" "lukuvuosimaksu" jotpa-term)
           :lukuvuosimaksu-max (max-agg "search_terms.metadata.maksunMaara" [lukuvuosimaksu-term])
           :jotpa (default-bool-term-agg "search_terms.hasJotpaRahoitus" true)
           :tyovoimakoulutus (default-bool-term-agg "search_terms.isTyovoimakoulutus" true)
           :taydennyskoulutus (default-bool-term-agg "search_terms.isTaydennyskoulutus" true)}}})
        (hakutulos-aggregations {:jotpa true})))))

(def sijainti-term {:term {:search_terms.sijainti.keyword "kunta_564"}})
(def onkotuleva-term {:term {:search_terms.onkoTuleva false}})
(def onkotuleva-sijainti-bool-filter {:bool {:filter [sijainti-term onkotuleva-term]}})

(deftest jarjestajat-aggregations-test
  (testing "should form aggregations for jarjestajat query with selected filters"
    (set-fixed-time "2020-01-01T01:01:00")
      (is
       (match? (m/match-with [map? m/equals]
        {:hits_aggregation
         {:nested {:path "search_terms"}
          :aggs
          {:yhteishaku                          (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid" onkotuleva-sijainti-bool-filter nil "search_terms")
           :kunta                               (default-agg "search_terms.sijainti.keyword" {:bool {:filter [onkotuleva-term]}} {:include "kunta.*"} "search_terms")
           :maakunta                            (default-agg "search_terms.sijainti.keyword" {:bool {:filter [onkotuleva-term]}} {:include "maakunta.*"} "search_terms")
           :pohjakoulutusvaatimus               (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset" onkotuleva-sijainti-bool-filter nil "search_terms")
           :oppilaitos                          (default-agg "search_terms.oppilaitosOid.keyword" onkotuleva-sijainti-bool-filter {:min_doc_count 1
                                                                                                                                   :size          10000} "search_terms")
           :hakutapa                            (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa" onkotuleva-sijainti-bool-filter nil "search_terms")
           :opetustapa                          (default-agg "search_terms.opetustavat.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
           :opetuskieli                         (default-agg "search_terms.opetuskielet.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
           :opetusaika                          (default-agg "search_terms.metadata.opetusajat.koodiUri.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
           :valintatapa                         (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat" onkotuleva-sijainti-bool-filter nil "search_terms")
           :lukiopainotukset                    (default-agg "search_terms.lukiopainotukset.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
           :lukiolinjaterityinenkoulutustehtava (default-agg "search_terms.lukiolinjaterityinenkoulutustehtava.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
           :alkamiskausi                        (default-agg "search_terms.paatellytAlkamiskaudet.keyword" onkotuleva-sijainti-bool-filter {:include alkamiskaudet-include} "search_terms")
           :osaamisala                          (default-agg "search_terms.osaamisalat.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
           :koulutuksenkestokuukausina {:filter
                                        {:bool
                                         {:filter
                                          [sijainti-term
                                           onkotuleva-term
                                           {:range
                                            {:search_terms.metadata.suunniteltuKestoKuukausina
                                             {:gte 0}}}]}},
                                        :aggs {:real_hits {:reverse_nested {:path "search_terms"}}}}
           :koulutuksenkestokuukausina-max (max-agg "search_terms.metadata.suunniteltuKestoKuukausina" nil)
           :hakukaynnissa                       {:filter
                                                 {:bool
                                                  {:filter
                                                   [sijainti-term
                                                    onkotuleva-term
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
                                                                 {:gt
                                                                  "2020-01-01T01:01"}}}]}}]}}}}]}}]}}
                                                 :aggs {:real_hits {:reverse_nested {:path "search_terms"}}}}}}})
        (jarjestajat-aggregations {:sijainti ["kunta_564"]} false)))))

(deftest tarjoajat-aggregations-test
  (testing "should form aggregations for tarjoajat query with selected filters"
    (set-fixed-time "2020-01-01T01:01:00")
      (is
       (match? (m/match-with [map? m/equals]
              {:hits_aggregation
                {:nested {:path "search_terms"}
                 :aggs
                 {:yhteishaku (default-nested-agg :yhteishaku "search_terms.hakutiedot.yhteishakuOid" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :kunta (default-agg "search_terms.sijainti.keyword" {:bool {:filter [onkotuleva-term]}} {:include "kunta.*"} "search_terms")
                  :maakunta (default-agg "search_terms.sijainti.keyword" {:bool {:filter [onkotuleva-term]}} {:include "maakunta.*"} "search_terms")
                  :pohjakoulutusvaatimus (default-nested-agg :pohjakoulutusvaatimus "search_terms.hakutiedot.pohjakoulutusvaatimukset" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :hakutapa (default-nested-agg :hakutapa "search_terms.hakutiedot.hakutapa" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :opetustapa (default-agg "search_terms.opetustavat.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :opetusaika (default-agg "search_terms.metadata.opetusajat.koodiUri.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :opetuskieli (default-agg "search_terms.opetuskielet.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :valintatapa (default-nested-agg :valintatapa "search_terms.hakutiedot.valintatavat" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :koulutusala (default-agg "search_terms.koulutusalat.keyword" onkotuleva-sijainti-bool-filter nil "search_terms")
                  :koulutustyyppi (default-agg "search_terms.koulutustyypit.keyword" onkotuleva-sijainti-bool-filter {:size (count koulutustyypit) :include koulutustyypit} "search_terms")
                  :koulutuksenkestokuukausina {:filter
                                               {:bool
                                                {:filter
                                                 [sijainti-term
                                                  onkotuleva-term
                                                  {:range
                                                   {:search_terms.metadata.suunniteltuKestoKuukausina
                                                    {:gte 0}}}]}},
                                               :aggs {:real_hits {:reverse_nested {:path "search_terms"}}}}
                  :koulutuksenkestokuukausina-max (max-agg "search_terms.metadata.suunniteltuKestoKuukausina" nil)
                  :alkamiskausi (default-agg "search_terms.paatellytAlkamiskaudet.keyword" onkotuleva-sijainti-bool-filter {:include alkamiskaudet-include} "search_terms")
                  :hakukaynnissa {:filter
                                  {:bool
                                   {:filter
                                    [sijainti-term
                                     onkotuleva-term
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
                                  :aggs {:real_hits {:reverse_nested {:path "search_terms"}}}}}}})
               (tarjoajat-aggregations {:sijainti ["kunta_564"]} false)))))
