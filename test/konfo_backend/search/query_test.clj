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

(deftest oppilaitos-aggregations-test
  (testing
   "Aggregations"
   (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit (fn [x] [(str x "_01")
                                                                          (str x "_02")])
                 konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")
                 konfo-backend.index.haku/list-yhteishaut
                 (fn [] ["1.2.246.562.29.00000000000000000001"])]
     (is
      (=
       (hakutulos-aggregations {})
       {:hits_aggregation
        {:nested {:path "search_terms"}
         :aggs
         {:koulutusala {:filters {:filters
                                  {:kansallinenkoulutusluokitus2016koulutusalataso1_01
                                   {:bool {:filter [{:term {:search_terms.koulutusalat.keyword
                                           "kansallinenkoulutusluokitus2016koulutusalataso1_01"}}]}}
                                   :kansallinenkoulutusluokitus2016koulutusalataso1_02
                                   {:bool {:filter [{:term {:search_terms.koulutusalat.keyword
                                           "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}]}}}}
                        :aggs {:real_hits {:reverse_nested {}}}}
          :yhteishaku
          {:filters {:filters
                     {:1.2.246.562.29.00000000000000000001
                      {:bool {:filter [{:nested
                                        {:path "search_terms.hakutiedot"
                                         :query
                                         {:bool {:filter
                                                 {:term {:search_terms.hakutiedot.yhteishakuOid
                                                         "1.2.246.562.29.00000000000000000001"}}}}}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :koulutusalataso2
          {:filters {:filters {:kansallinenkoulutusluokitus2016koulutusalataso2_01
                               {:bool {:filter [{:term {:search_terms.koulutusalat.keyword
                                       "kansallinenkoulutusluokitus2016koulutusalataso2_01"}}]}}
                               :kansallinenkoulutusluokitus2016koulutusalataso2_02
                               {:bool {:filter [{:term {:search_terms.koulutusalat.keyword
                                       "kansallinenkoulutusluokitus2016koulutusalataso2_02"}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :kunta {:filters {:filters {:kunta_01 {:bool {:filter [{:term {:search_terms.sijainti.keyword "kunta_01"}}]}}
                                      :kunta_02 {:bool {:filter [{:term {:search_terms.sijainti.keyword
                                                        "kunta_02"}}]}}}}
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
          :koulutustyyppitaso2
          {:filters
           {:filters
            {:koulutustyyppi_01
             {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_01"}}]}}
             :koulutustyyppi_02
             {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_02"}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :hakutapa
          {:filters
           {:filters
            {:hakutapa_01
             {:bool {:filter
                     [{:nested {:path "search_terms.hakutiedot"
                               :query {:bool
                                       {:filter
                                        {:term {:search_terms.hakutiedot.hakutapa "hakutapa_01"}}}}}}]
                     }}
             :hakutapa_02
             {:bool {:filter
                     [{:nested {:path "search_terms.hakutiedot"
                                :query {:bool
                                        {:filter
                                         {:term {:search_terms.hakutiedot.hakutapa "hakutapa_02"}}}}}}]} }}}
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
                              "2020-01-01T01:01"}}}]}}]}}}}]}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :jotpa
          {:filters
           {:filters
            {:jotpa
             {:bool
              {:filter [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :tyovoimakoulutus
          {:filters
           {:filters
            {:tyovoimakoulutus
             {:bool
              {:filter [{:bool {:should [{:term {:search_terms.isTyovoimakoulutus true}}]}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}
          :taydennyskoulutus
          {:filters
           {:filters
            {:taydennyskoulutus
             {:bool
              {:filter [{:bool {:should [{:term {:search_terms.isTaydennyskoulutus true}}]}}]}}}}
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
             :erikoistumiskoulutus {:bool {:filter [{:term {:search_terms.koulutustyypit.keyword "erikoistumiskoulutus"}}]}}}}
           :aggs {:real_hits {:reverse_nested {}}}}}}}))))

  (testing
    "aggregations with selected filters"
    (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit (fn [x] [(str x "_01")
                                                                           (str x "_02")])
                  konfo-backend.tools/current-time-as-kouta-format (fn [] "2020-01-01T01:01")
                  konfo-backend.index.haku/list-yhteishaut
                  (fn [] ["1.2.246.562.29.00000000000000000001"])]
      (is
        (=
          (hakutulos-aggregations {:jotpa true})
          {:hits_aggregation
           {:nested {:path "search_terms"}
            :aggs
            {:koulutusala
             {:filters
              {:filters
               {:kansallinenkoulutusluokitus2016koulutusalataso1_01
                {:bool
                 {:filter
                  [{:term {:search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kansallinenkoulutusluokitus2016koulutusalataso1_02
                {:bool
                 {:filter
                  [{:term {:search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso1_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
                           :aggs {:real_hits {:reverse_nested {}}}}
             :yhteishaku
             {:filters {:filters
                        {:1.2.246.562.29.00000000000000000001
                         {:bool {:filter
                                 [{:nested
                                   {:path "search_terms.hakutiedot"
                                    :query
                                    {:bool {:filter
                                            {:term {:search_terms.hakutiedot.yhteishakuOid
                                                     "1.2.246.562.29.00000000000000000001"}}}}}}
                                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :koulutusalataso2
             {:filters
              {:filters
               {:kansallinenkoulutusluokitus2016koulutusalataso2_01
                {:bool
                 {:filter
                  [{:term {:search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kansallinenkoulutusluokitus2016koulutusalataso2_02
                {:bool
                 {:filter
                  [{:term {:search_terms.koulutusalat.keyword "kansallinenkoulutusluokitus2016koulutusalataso2_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :kunta
             {:filters
              {:filters
               {:kunta_01
                {:bool
                 {:filter
                  [{:term {:search_terms.sijainti.keyword "kunta_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kunta_02
                {:bool
                 {:filter
                  [{:term {:search_terms.sijainti.keyword "kunta_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
                     :aggs {:real_hits {:reverse_nested {}}}}
             :pohjakoulutusvaatimus
             {:filters
              {:filters
               {:pohjakoulutusvaatimuskonfo_01
                {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
                                           :query {:bool {:filter
                                                          {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                                  "pohjakoulutusvaatimuskonfo_01"}}}}}}
                                 {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :pohjakoulutusvaatimuskonfo_02
                {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
                                           :query {:bool {:filter
                                                          {:term {:search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                                  "pohjakoulutusvaatimuskonfo_02"}}}}}}
                                 {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :maakunta
             {:filters
              {:filters
               {:maakunta_01
                {:bool
                 {:filter
                  [{:term {:search_terms.sijainti.keyword "maakunta_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :maakunta_02 {:bool
                 {:filter
                  [{:term {:search_terms.sijainti.keyword "maakunta_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :koulutustyyppitaso2
             {:filters
              {:filters
               {:koulutustyyppi_01
                {:bool
                 {:filter
                  [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :koulutustyyppi_02
                {:bool
                 {:filter
                  [{:term {:search_terms.koulutustyypit.keyword "koulutustyyppi_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :hakutapa
             {:filters
              {:filters
               {:hakutapa_01
                {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
                                           :query {:bool
                                                   {:filter
                                                    {:term {:search_terms.hakutiedot.hakutapa "hakutapa_01"}}}}}}
                                 {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :hakutapa_02
                {:bool {:filter [{:nested {:path "search_terms.hakutiedot"
                                           :query {:bool
                                                   {:filter
                                                    {:term {:search_terms.hakutiedot.hakutapa "hakutapa_02"}}}}}}
                                 {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :opetustapa
             {:filters
              {:filters
               {:opetuspaikkakk_01
                {:bool
                 {:filter
                  [{:term {:search_terms.opetustavat.keyword "opetuspaikkakk_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :opetuspaikkakk_02
                {:bool
                 {:filter
                  [{:term {:search_terms.opetustavat.keyword "opetuspaikkakk_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :opetuskieli
             {:filters
              {:filters
               {:oppilaitoksenopetuskieli_01
                {:bool
                 {:filter
                  [{:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_01"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :oppilaitoksenopetuskieli_02
                {:bool
                 {:filter
                  [{:term {:search_terms.opetuskielet.keyword "oppilaitoksenopetuskieli_02"}}
                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :hakukaynnissa
             {:filters
              {:filters
               {:hakukaynnissa
                {:bool
                 {:filter
                  [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}
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
                                 "2020-01-01T01:01"}}}]}}]}}}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :jotpa
             {:filters
              {:filters
               {:jotpa
                {:bool
                 {:filter [{:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
            :tyovoimakoulutus
              {:filters
              {:filters
                {:tyovoimakoulutus
                {:bool
                  {:filter [{:bool {:should [{:term {:search_terms.isTyovoimakoulutus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
            :taydennyskoulutus
              {:filters
              {:filters
                {:taydennyskoulutus
                {:bool
                  {:filter [
                    {:bool {:should [{:term {:search_terms.isTaydennyskoulutus true}}]}}]}}}}
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
                         {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :valintatapajono_02
                {:bool {:filter
                        [{:nested
                          {:path "search_terms.hakutiedot"
                           :query {:bool
                                   {:filter
                                    {:term {:search_terms.hakutiedot.valintatavat "valintatapajono_02"}}}}}}
                         {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}
             :koulutustyyppi
             {:filters
              {:filters
               {:amm-osaamisala {:bool {:filter
                                        [{:term {:search_terms.koulutustyypit.keyword "amm-osaamisala"}}
                                         {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kandi {:bool {:filter
                               [{:term {:search_terms.koulutustyypit.keyword "kandi"}}
                                {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amk-ylempi {:bool {:filter
                                    [{:term {:search_terms.koulutustyypit.keyword "amk-ylempi"}}
                                     {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :tohtori {:bool {:filter
                                 [{:term {:search_terms.koulutustyypit.keyword "tohtori"}}
                                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amm-tutkinnon-osa {:bool {:filter
                                           [{:term {:search_terms.koulutustyypit.keyword "amm-tutkinnon-osa"}}
                                            {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :lk {:bool {:filter
                            [{:term {:search_terms.koulutustyypit.keyword "lk"}}
                             {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :maisteri {:bool {:filter
                                  [{:term {:search_terms.koulutustyypit.keyword "maisteri"}}
                                   {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amk {:bool {:filter
                             [{:term {:search_terms.koulutustyypit.keyword "amk"}}
                              {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amk-muu {:bool {:filter
                                 [{:term {:search_terms.koulutustyypit.keyword "amk-muu"}}
                                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amk-alempi {:bool {:filter
                                    [{:term {:search_terms.koulutustyypit.keyword "amk-alempi"}}
                                     {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kandi-ja-maisteri {:bool {:filter
                                           [{:term {:search_terms.koulutustyypit.keyword "kandi-ja-maisteri"}}
                                            {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :yo {:bool {:filter
                            [{:term {:search_terms.koulutustyypit.keyword "yo"}}
                             {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amm {:bool {:filter
                             [{:term {:search_terms.koulutustyypit.keyword "amm"}}
                              {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amm-muu {:bool {:filter
                                 [{:term {:search_terms.koulutustyypit.keyword "amm-muu"}}
                                  {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :tuva {:bool {:filter
                              [{:term {:search_terms.koulutustyypit.keyword "tuva"}}
                               {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :tuva-normal {:bool {:filter
                                     [{:term {:search_terms.koulutustyypit.keyword "tuva-normal"}}
                                      {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :tuva-erityisopetus {:bool {:filter
                                            [{:term {:search_terms.koulutustyypit.keyword "tuva-erityisopetus"}}
                                             {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :telma {:bool {:filter
                               [{:term {:search_terms.koulutustyypit.keyword "telma"}}
                                {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :amm-ope-erityisope-ja-opo {:bool {:filter
                                                   [{:term {:search_terms.koulutustyypit.keyword "amm-ope-erityisope-ja-opo"}}
                                                    {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :ope-pedag-opinnot {:bool {:filter
                                           [{:term {:search_terms.koulutustyypit.keyword "ope-pedag-opinnot"}}
                                            {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :vapaa-sivistystyo {:bool {:filter
                                           [{:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo"}}
                                            {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :vapaa-sivistystyo-opistovuosi {:bool {:filter
                                                       [{:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo-opistovuosi"}}
                                                        {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :vapaa-sivistystyo-muu {:bool {:filter
                                               [{:term {:search_terms.koulutustyypit.keyword "vapaa-sivistystyo-muu"}}
                                                {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :aikuisten-perusopetus {:bool {:filter
                                               [{:term {:search_terms.koulutustyypit.keyword "aikuisten-perusopetus"}}
                                                {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kk-opintojakso {:bool {:filter
                                        [{:term {:search_terms.koulutustyypit.keyword "kk-opintojakso"}}
                                         {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :kk-opintokokonaisuus {:bool {:filter
                                              [{:term {:search_terms.koulutustyypit.keyword "kk-opintokokonaisuus"}}
                                               {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :erikoislaakari {:bool {:filter
                                        [{:term {:search_terms.koulutustyypit.keyword "erikoislaakari"}}
                                         {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}
                :erikoistumiskoulutus {:bool {:filter
                                        [{:term {:search_terms.koulutustyypit.keyword "erikoistumiskoulutus"}}
                                         {:bool {:should [{:term {:search_terms.hasJotpaRahoitus true}}]}}]}}}}
              :aggs {:real_hits {:reverse_nested {}}}}}}}))))
  )
