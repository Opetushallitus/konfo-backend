(ns konfo-backend.search.koulutus-query-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.koulutus-search :refer [koulutus-query]]))

(deftest koulutus-query-tes
  (with-redefs [konfo-backend.tools/current-time-as-iso-local-date-time-string (fn [] "2019-04-11T10:45")]
    (testing "Elasticsearch query format"
      (testing "is correct with all constraints if vainHakuKaynnissa=false"
        (is (= (koulutus-query "arkeologia" "en" {:paikkakunta "kerava" :opetuskieli "sv" :koulutustyyppi "yo" :vainHakuKaynnissa false})
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
                                                                                           {:term {:toteutukset.koulutus.koulutustyyppi.keyword "yo"}}
                                                                                           {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword "kieli_sv#*"}}]
                                                                                  }
                                                                           },
                                                                   :boost_mode "replace",
                                                                   :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}
                                {:constant_score {
                                                  :filter {
                                                           :bool {
                                                                  :must_not {:exists {:field "toteutukset"}},
                                                                  :must {:match {:nimi.en "arkeologia"}},
                                                                  :filter [{:term {:kielivalinta "en"}}
                                                                           {:term {:tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                           {:term {:koulutustyyppi.keyword "yo"}}]
                                                                  }
                                                           },
                                                  :boost 1}}]}})))

      (testing "is correct with all constraints if vainHakuKaynnissa=true"
        (is (= (koulutus-query "arkeologia" "en" {:paikkakunta "kerava" :opetuskieli "sv" :koulutustyyppi "yo" :vainHakuKaynnissa true})
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
                                                                          {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}
                                                                          {:term {:toteutukset.koulutus.koulutustyyppi.keyword "yo"}}
                                                                          {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword "kieli_sv#*"}}]
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
                                                                                  :must {
                                                                                         :multi_match {
                                                                                                       :query "arkeologia",
                                                                                                       :fields ["toteutukset.koulutus.nimi.en"
                                                                                                                "toteutukset.nimi.en"
                                                                                                                "toteutukset.metadata.ammattinimikkeet.en"
                                                                                                                "toteutukset.metadata.asiasanat.en"]
                                                                                                       }
                                                                                         },
                                                                                  :filter [{:term {:toteutukset.kielivalinta "en"}}]
                                                                                  }
                                                                           },
                                                                   :boost_mode "replace",
                                                                   :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}
                                {:constant_score {
                                                  :filter {
                                                           :bool {
                                                                  :must_not {:exists {:field "toteutukset"}},
                                                                  :must {:match {:nimi.en "arkeologia"}},
                                                                  :filter [{:term {:kielivalinta "en"}}]
                                                                  }
                                                           },
                                                  :boost 1}}]}})))

      (testing "is correct without keyword"
        (is (= (koulutus-query nil "en" {:paikkakunta "kerava" :opetuskieli "sv" :koulutustyyppi "yo"})
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
                                                                                           {:term {:toteutukset.koulutus.koulutustyyppi.keyword "yo"}}
                                                                                           {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword "kieli_sv#*"}}]
                                                                                  }
                                                                           },
                                                                   :boost_mode "replace",
                                                                   :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}}
                                {:constant_score {
                                                  :filter {
                                                           :bool {
                                                                  :must_not {:exists {:field "toteutukset"}},
                                                                  :filter [{:term {:kielivalinta "en"}}
                                                                           {:term {:tarjoajat.paikkakunta.nimi.en.keyword "kerava"}}
                                                                           {:term {:koulutustyyppi.keyword "yo"}}]
                                                                  }
                                                           },
                                                  :boost 1}}]}})))

      (testing "is correct without keyword or filters in koulutus"
        (is (= (koulutus-query nil "en" {:opetuskieli "sv"})
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
                                                                          {:wildcard {:toteutukset.metadata.opetus.opetuskieli.koodiUri.keyword "kieli_sv#*"}}]
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
                                                  :functions [{:filter {:term {:toteutukset.hakuOnKaynnissa {:value "2019-04-11T10:45"}}}, :weight 100}]}}}})))
      )))