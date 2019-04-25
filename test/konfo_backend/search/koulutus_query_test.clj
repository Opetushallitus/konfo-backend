(ns konfo-backend.search.koulutus-query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.koulutus.query :refer [create-query] :rename {create-query koulutus-query}]))

(deftest koulutus-query-test
  (with-redefs [konfo-backend.tools/current-time-as-kouta-format (fn [] "2019-04-11T10:45")]
    (testing "Elasticsearch query format"
      (testing "is correct with all constraints expect opetuskieli if vainHakuKaynnissa=false"
        (is (= (koulutus-query "arkeologia" "en" {:paikkakunta "kerava" :koulutustyyppi "yo" :vainHakuKaynnissa false})
               {:bool {
                       :should [{:nested {
                                          :path "toteutukset",
                                          :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet"
                                                                 "toteutukset.metadata.asiasanat"
                                                                 "toteutukset.hakuOnKaynnissa"
                                                                 "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi"
                                                                 "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]},
                                          :score_mode "max",
                                          :query {
                                                  :function_score {
                                                                   :query {
                                                                           :bool {
                                                                                  :must {
                                                                                         :multi_match {
                                                                                                       :query "arkeologia",
                                                                                                       :fields ["toteutukset.koulutus.nimi.en"
                                                                                                                "toteutukset.nimi.en"
                                                                                                                "toteutukset.metadata.ammattinimikkeet.en"
                                                                                                                "toteutukset.metadata.asiasanat.en"]
                                                                                                       }
                                                                                         },
                                                                                  :filter [{:term {:toteutukset.kielivalinta "en"}}
                                                                                           {:term {:toteutukset.tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                                           {:term {:toteutukset.koulutus.koulutustyyppi.keyword "yo"}}]
                                                                                  }
                                                                           },
                                                                   :boost_mode "replace",
                                                                   :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}
                                {:constant_score {:boost 1,
                                                  :filter {
                                                           :bool {
                                                                  :must_not {:exists {:field "toteutukset"}},
                                                                  :must {:match {:nimi.en "arkeologia"}},
                                                                  :filter [{:term {:kielivalinta "en"}}
                                                                           {:term {:tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                           {:term {:koulutustyyppi.keyword "yo"}}]
                                                                  }
                                                           }}}]}})))

      (testing "is correct with all constraints if vainHakuKaynnissa=true"
        (is (= (koulutus-query "arkeologia" "en" {:paikkakunta "kerava" :opetuskieli "oppilaitoksenopetuskieli_2" :koulutustyyppi "yo" :vainHakuKaynnissa true})
               {:nested {
                         :path "toteutukset",
                         :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet"
                                                "toteutukset.metadata.asiasanat"
                                                "toteutukset.hakuOnKaynnissa"
                                                "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi"
                                                "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]},
                         :score_mode "max",
                         :query {
                                 :function_score {
                                                  :query {
                                                          :bool {:filter [{:term {:toteutukset.kielivalinta "en"}}
                                                                          {:term {:toteutukset.tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                          {:term {:toteutukset.koulutus.koulutustyyppi.keyword "yo"}}
                                                                          {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword "oppilaitoksenopetuskieli_2#*"}}
                                                                          {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}]
                                                                 :must {
                                                                        :multi_match {
                                                                                      :query "arkeologia",
                                                                                      :fields ["toteutukset.koulutus.nimi.en"
                                                                                               "toteutukset.nimi.en"
                                                                                               "toteutukset.metadata.ammattinimikkeet.en"
                                                                                               "toteutukset.metadata.asiasanat.en"]
                                                                                      }
                                                                        }
                                                                 }
                                                          },
                                                  :boost_mode "replace",
                                                  :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}})))

      (testing "is correct without any constraints"
        (is (= (koulutus-query "arkeologia" "en" {})
               {:bool {
                       :should [{:nested {
                                          :path "toteutukset",
                                          :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet"
                                                                 "toteutukset.metadata.asiasanat"
                                                                 "toteutukset.hakuOnKaynnissa"
                                                                 "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi"
                                                                 "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]},
                                          :score_mode "max",
                                          :query {
                                                  :function_score {
                                                                   :query {
                                                                           :bool {
                                                                                  :filter [{:term {:toteutukset.kielivalinta "en"}}],
                                                                                  :must {
                                                                                         :multi_match {
                                                                                                       :query "arkeologia",
                                                                                                       :fields ["toteutukset.koulutus.nimi.en"
                                                                                                                "toteutukset.nimi.en"
                                                                                                                "toteutukset.metadata.ammattinimikkeet.en"
                                                                                                                "toteutukset.metadata.asiasanat.en"]
                                                                                                       }
                                                                                         }
                                                                                  }
                                                                           },
                                                                   :boost_mode "replace",
                                                                   :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}
                                {:constant_score {:boost 1,
                                                  :filter {
                                                           :bool {
                                                                  :must_not {:exists {:field "toteutukset"}},
                                                                  :filter [{:term {:kielivalinta "en"}}],
                                                                  :must {:match {:nimi.en "arkeologia"}}
                                                                  }
                                                           }}}]}})))

      (testing "is correct without keyword and opetuskieli"
        (is (= (koulutus-query nil "en" {:paikkakunta "kerava" :koulutustyyppi "yo"})
               {:bool {
                       :should [{:nested {
                                          :path "toteutukset",
                                          :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet"
                                                                 "toteutukset.metadata.asiasanat"
                                                                 "toteutukset.hakuOnKaynnissa"
                                                                 "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi"
                                                                 "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]},
                                          :score_mode "max",
                                          :query {
                                                  :function_score {
                                                                   :query {
                                                                           :bool {
                                                                                  :filter [{:term {:toteutukset.kielivalinta "en"}}
                                                                                           {:term {:toteutukset.tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                                           {:term {:toteutukset.koulutus.koulutustyyppi.keyword "yo"}}]
                                                                                  }
                                                                           },
                                                                   :boost_mode "replace",
                                                                   :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}
                                {:constant_score {:boost 1,
                                                  :filter {
                                                           :bool {
                                                                  :must_not {:exists {:field "toteutukset"}},
                                                                  :filter [{:term {:kielivalinta "en"}}
                                                                           {:term {:tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                           {:term {:koulutustyyppi.keyword "yo"}}]
                                                                  }
                                                           }}}]}})))

      (testing "is correct without keyword or filters in koulutus"
        (is (= (koulutus-query nil "en" {:opetuskieli "oppilaitoksenopetuskieli_2"})
               {:nested {
                         :path "toteutukset",
                         :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet"
                                                "toteutukset.metadata.asiasanat"
                                                "toteutukset.hakuOnKaynnissa"
                                                "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi"
                                                "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]},
                         :score_mode "max",
                         :query {
                                 :function_score {
                                                  :query {
                                                          :bool {
                                                                 :filter [{:term {:toteutukset.kielivalinta "en"}}
                                                                          {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword "oppilaitoksenopetuskieli_2#*"}}]
                                                                 }
                                                          },
                                                  :boost_mode "replace",
                                                  :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}})))

      (testing "is correct with only vainHakuKaynnissa=true"
        (is (= (koulutus-query nil "en" {:vainHakuKaynnissa true})
               {:nested {
                         :path "toteutukset",
                         :inner_hits {:_source ["toteutukset.metadata.ammattinimikkeet"
                                                "toteutukset.metadata.asiasanat"
                                                "toteutukset.hakuOnKaynnissa"
                                                "toteutukset.metadata.alemmanKorkeakoulututkinnonOsaamisalat.nimi"
                                                "toteutukset.metadata.ylemmanKorkeakoulututkinnonOsaamisalat.nimi"]},
                         :score_mode "max",
                         :query {
                                 :function_score {
                                                  :query {
                                                          :bool {
                                                                 :filter [{:term {:toteutukset.kielivalinta "en"}}
                                                                          {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}]
                                                                 }
                                                          },
                                                  :boost_mode "replace",
                                                  :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}))))))