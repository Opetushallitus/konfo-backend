(ns konfo-backend.search.query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.query :refer [query hakutulos-aggregations]]
            [konfo-backend.tools]))

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
   (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")
                 konfo-backend.tools/half-year-past-as-kouta-format (fn [] "2019-06-01T01:01")]
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

(deftest oppilaitos-aggregations-test
  (testing
   "Aggregations"
   (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit (fn [x] [(str x "_01")
                                                                          (str x "_02")])
                 konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")
                 konfo-backend.tools/half-year-past-as-kouta-format (fn [] "2019-06-01T01:01")
                 konfo-backend.index.haku/list-yhteishaut
                 (fn [] ["1.2.246.562.29.00000000000000000001"])]
     (is
      (=
       (hakutulos-aggregations false)
       {:hits_aggregation
        {:nested {:path "search_terms"}
         :aggs
         {:koulutusala {:filters {:filters
                                  {:kansallinenkoulutusluokitus2016koulutusalataso1_01
                                   {:term {:search_terms.koulutusalat.keyword
                                           "kansallinenkoulutusluokitus2016koulutusalataso1_01"}}
                                   :kansallinenkoulutusluokitus2016koulutusalataso1_02
                                   {:term {:search_terms.koulutusalat.keyword
                                           "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}}}
                        :aggs {:real_hits {:reverse_nested {}}}}
          :yhteishaku
          {:filters {:filters
                     {:1.2.246.562.29.00000000000000000001
                      {:nested
                       {:path "search_terms.hakutiedot"
                        :query
                        {:bool {:filter
                                [{:nested
                                  {:path "search_terms.hakutiedot.hakuajat"
                                   :query
                                   {:bool {:should
                                           [{:bool {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                            {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                     {:gt "2019-06-01T01:01"}}}]}}}}
                                 {:term {:search_terms.hakutiedot.yhteishakuOid
                                         "1.2.246.562.29.00000000000000000001"}}]}}}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :koulutusalataso2
          {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso2_01
                               {:term {:search_terms.koulutusalat.keyword
                                       "kansallinenkoulutusluokitus2016koulutusalataso2_01"}}
                               :kansallinenkoulutusluokitus2016koulutusalataso2_02
                               {:term {:search_terms.koulutusalat.keyword
                                       "kansallinenkoulutusluokitus2016koulutusalataso2_02"}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :kunta {:filters {:filters {:kunta_01 {:term {:search_terms.sijainti.keyword "kunta_01"}}
                                      :kunta_02 {:term {:search_terms.sijainti.keyword
                                                        "kunta_02"}}}}
                  :aggs {:real_hits {:reverse_nested {}}}}
          :pohjakoulutusvaatimus
          {:filters
           {:filters
            {:pohjakoulutusvaatimuskonfo_01
             {:nested {:path "search_terms.hakutiedot"
                       :query {:bool {:filter
                                      [{:nested
                                        {:path "search_terms.hakutiedot.hakuajat"
                                         :query
                                         {:bool {:should
                                                 [{:bool
                                                   {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                                  {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                           {:gt "2019-06-01T01:01"}}}]}}}}
                                       {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                               "pohjakoulutusvaatimuskonfo_01"}}]}}}}
             :pohjakoulutusvaatimuskonfo_02
             {:nested {:path "search_terms.hakutiedot"
                       :query {:bool {:filter
                                      [{:nested
                                        {:path "search_terms.hakutiedot.hakuajat"
                                         :query
                                         {:bool {:should
                                                 [{:bool
                                                   {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                                  {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                           {:gt "2019-06-01T01:01"}}}]}}}}
                                       {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                               "pohjakoulutusvaatimuskonfo_02"}}]}}}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :maakunta
          {:filters {:filters {:maakunta_01 {:term {:search_terms.sijainti.keyword "maakunta_01"}}
                               :maakunta_02 {:term {:search_terms.sijainti.keyword "maakunta_02"}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :koulutustyyppitaso2
          {:filters {:filters {:koulutustyyppi_01 {:term {:search_terms.koulutustyypit.keyword
                                                          "koulutustyyppi_01"}}
                               :koulutustyyppi_02 {:term {:search_terms.koulutustyypit.keyword
                                                          "koulutustyyppi_02"}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :hakutapa
          {:filters
           {:filters
            {:hakutapa_01
             {:nested {:path "search_terms.hakutiedot"
                       :query {:bool
                               {:filter
                                [{:nested
                                  {:path "search_terms.hakutiedot.hakuajat"
                                   :query
                                   {:bool {:should
                                           [{:bool {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                            {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                     {:gt "2019-06-01T01:01"}}}]}}}}
                                 {:term {:search_terms.hakutiedot.hakutapa "hakutapa_01"}}]}}}}
             :hakutapa_02
             {:nested {:path "search_terms.hakutiedot"
                       :query {:bool
                               {:filter
                                [{:nested
                                  {:path "search_terms.hakutiedot.hakuajat"
                                   :query
                                   {:bool {:should
                                           [{:bool {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                            {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                     {:gt "2019-06-01T01:01"}}}]}}}}
                                 {:term {:search_terms.hakutiedot.hakutapa "hakutapa_02"}}]}}}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :opetustapa {:filters {:filters
                                 {:opetuspaikkakk_01 {:term {:search_terms.opetustavat.keyword
                                                             "opetuspaikkakk_01"}}
                                  :opetuspaikkakk_02 {:term {:search_terms.opetustavat.keyword
                                                             "opetuspaikkakk_02"}}}}
                       :aggs {:real_hits {:reverse_nested {}}}}
          :opetuskieli {:filters {:filters {:oppilaitoksenopetuskieli_01
                                            {:term {:search_terms.opetuskielet.keyword
                                                    "oppilaitoksenopetuskieli_01"}}
                                            :oppilaitoksenopetuskieli_02
                                            {:term {:search_terms.opetuskielet.keyword
                                                    "oppilaitoksenopetuskieli_02"}}}}
                        :aggs {:real_hits {:reverse_nested {}}}}
          :hakukaynnissa
          {:filters
           {:filters
            {:hakukaynnissa
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
                                "2020-01-01T01:01"}}}]}}]}}}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :valintatapa
          {:filters
           {:filters
            {:valintatapajono_01
             {:nested {:path "search_terms.hakutiedot"
                       :query
                       {:bool
                        {:filter
                         [{:nested
                           {:path "search_terms.hakutiedot.hakuajat"
                            :query {:bool {:should
                                           [{:bool {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                            {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                     {:gt "2019-06-01T01:01"}}}]}}}}
                          {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_01"}}]}}}}
             :valintatapajono_02
             {:nested
              {:path "search_terms.hakutiedot"
               :query {:bool
                       {:filter
                        [{:nested {:path "search_terms.hakutiedot.hakuajat"
                                   :query
                                   {:bool {:should
                                           [{:bool {:must_not
                                                    {:exists
                                                     {:field
                                                      "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                            {:range {:search_terms.hakutiedot.hakuajat.paattyy
                                                     {:gt "2019-06-01T01:01"}}}]}}}}
                         {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_02"}}]}}}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :koulutustyyppi
          {:filters
           {:filters
            {:amm-osaamisala {:term {:search_terms.koulutustyypit.keyword "amm-osaamisala"}}
             :kandi {:term {:search_terms.koulutustyypit.keyword "kandi"}}
             :amk-ylempi {:term {:search_terms.koulutustyypit.keyword "amk-ylempi"}}
             :tohtori {:term {:search_terms.koulutustyypit.keyword "tohtori"}}
             :amm-tutkinnon-osa {:term {:search_terms.koulutustyypit.keyword "amm-tutkinnon-osa"}}
             :lk {:term {:search_terms.koulutustyypit.keyword "lk"}}
             :maisteri {:term {:search_terms.koulutustyypit.keyword "maisteri"}}
             :amk {:term {:search_terms.koulutustyypit.keyword "amk"}}
             :amk-alempi {:term {:search_terms.koulutustyypit.keyword "amk-alempi"}}
             :kandi-ja-maisteri {:term {:search_terms.koulutustyypit.keyword "kandi-ja-maisteri"}}
             :yo {:term {:search_terms.koulutustyypit.keyword "yo"}}
             :amm {:term {:search_terms.koulutustyypit.keyword "amm"}}
             :amm-muu {:term {:search_terms.koulutustyypit.keyword "amm-muu"}}
             :tuva {:term {:search_terms.koulutustyypit.keyword "tuva"}}
             :tuva-normal {:term {:search_terms.koulutustyypit.keyword "tuva-normal"}}
             :tuva-erityisopetus {:term {:search_terms.koulutustyypit.keyword "tuva-erityisopetus"}}
             :telma {:term {:search_terms.koulutustyypit.keyword "telma"}}
             :vapaa-sivistystyo {:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo"}}
             :vapaa-sivistystyo-opistovuosi {:term {:search_terms.koulutustyypit.keyword
                                                    "vapaa-sivistystyo-opistovuosi"}}
             :vapaa-sivistystyo-muu {:term {:search_terms.koulutustyypit.keyword
                                            "vapaa-sivistystyo-muu"}}
             :aikuisten-perusopetus {:term {:search_terms.koulutustyypit.keyword 
                                            "aikuisten-perusopetus"}}}}
           :aggs {:real_hits {:reverse_nested {}}}}}}})))))
