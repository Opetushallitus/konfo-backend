(ns konfo-backend.toteutus-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.toteutus :refer :all]
            [midje.sweet :refer :all]
            [konfo-backend.toteutus :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(facts "Toteutus"
  (fact "haettavissa (hakukohde)"
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1512220000001}}]})
        => nil?
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1712220000001}}]})
        => true)

  (fact "haettavissa (haku)"
    (haettavissa {:haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}]}]})
        => nil?
    (haettavissa {:haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}, {:alkuPvm 1512220000000 :loppuPvm 1712220000001}]}]})
        => true)

  (fact "haettavissa (hakukohteen hakuaika over haku)"
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1512220000001}}]
                                  :haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}, {:alkuPvm 1512220000000 :loppuPvm 1712220000001}]}]})
        => nil?
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1712220000001}}]
                             :haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}]}]}))
       => true)

  (fact "Parse hakuajat"
    (let [hakuaika1 {:alkuPvm (- (System/currentTimeMillis) 100) :hakuaikaId 13964988 :loppuPvm (+ (System/currentTimeMillis) 10000)
                     :nimet {:kieli_fi "Hakuaika 1" :kieli_sv "Ansökningstid 1" :kieli_en "Application period 1"}}
          hakuaika2 {:alkuPvm (- (System/currentTimeMillis) 100000) :hakuaikaId 13964993 :loppuPvm (- (System/currentTimeMillis) 1000)
                     :nimet {:kieli_fi "Hakuaika 2" :kieli_sv "Ansökningstid 2" :kieli_en "Application period 2"}}
          hakuaika3 {:alkuPvm (+ (System/currentTimeMillis) 10000), :hakuaikaId 19739638, :loppuPvm (+ (System/currentTimeMillis) 100000)
                     :nimet {:kieli_fi "Hakuaika 3" :kieli_sv "Ansökningstid 3" :kieli_en "Application period 3"}}
          hakuaika1res (assoc hakuaika1 :hakuNimi {:kieli_fi "Testi fi" :kieli_sv "Testi sv" :kieli_en "Testi en"}
                                        :hakuOid "1.2.246.562.29.26435854158")
          hakuaika2res (assoc hakuaika2 :hakuNimi {:kieli_fi "Testi fi" :kieli_sv "Testi sv" :kieli_en "Testi en"}
                                        :hakuOid "1.2.246.562.29.26435854158")
          hakuaika3res (assoc hakuaika3 :hakuNimi {:kieli_fi "Testi2 fi" :kieli_sv "Testi2 sv" :kieli_en "Testi2 en"}
                                        :hakuOid "1.2.246.562.29.70576649506")]
      (parse-hakuajat [{:nimi {:kieli_fi "Testi fi" :kieli_sv "Testi sv" :kieli_en "Testi en"}
                        :oid "1.2.246.562.29.26435854158"
                        :hakuaikas [hakuaika1 hakuaika2]}
                       {:nimi {:kieli_fi "Testi2 fi" :kieli_sv "Testi2 sv" :kieli_en "Testi2 en"}
                        :oid "1.2.246.562.29.70576649506"
                        :hakuaikas [hakuaika3]}])
      => {:aktiiviset [hakuaika1res]
          :paattyneet [hakuaika2res]
          :tulevat [hakuaika3res]
          :kaikki [hakuaika1res hakuaika2res hakuaika3res]}))

  (fact "Koulutus query only keyword"
    (koulutus-query "hakusana" "fi" [] {})
      => {:bool {:must {:dis_max {:queries
                                  [{:constant_score
                                    {:filter
                                            {:multi_match
                                             {:query "hakusana" :fields
                                              ["searchData.nimi.kieli_fi"]
                                              :operator "and"
                                              }}
                                     :boost 10}}
                                   {:constant_score
                                    {:filter
                                            {:multi_match
                                             {:query "hakusana"
                                              :fields ["tutkintonimikes.nimi.kieli_fi" "koulutusala.nimi.kieli_fi" "tutkinto.nimi.kieli_fi"]
                                              :operator "and"}}
                                     :boost 5}}
                                   {:constant_score
                                    {:filter
                                     {:multi_match
                                      {:query "hakusana"
                                       :fields ["aihees.nimi.kieli_fi" "searchData.oppiaineet.kieli_fi" "ammattinimikkeet.nimi.kieli_fi"]
                                       :operator "and"}} :boost 4}}
                                   {:constant_score
                                    {:filter
                                     {:multi_match
                                      {:query "hakusana"
                                       :fields ["searchData.organisaatio.nimi.kieli_fi"]
                                       :operator "and"}} :boost 2}}]}}
                 :must_not
                       {:range
                        {:searchData.opintopolunNayttaminenLoppuu
                         {:format "yyyy-MM-dd" :lt "now"}}}
                 :filter
                       [{:match {:tila "JULKAISTU"}}
                        {:match {:searchData.haut.tila "JULKAISTU"}}] }})

  (fact "Koulutus query with constraints"
    (koulutus-query "hakusana" "fi" ["1.2.246.562.29.70576649506"] {:koulutustyyppi "ako" :kieli "fi" :paikkakunta "tampere"})
        => {:bool {:must {:dis_max {:queries
                                    [{:constant_score
                                      {:filter
                                              {:multi_match
                                               {:query "hakusana" :fields
                                                ["searchData.nimi.kieli_fi"]
                                                :operator "and"
                                                }}
                                       :boost 10}}
                                     {:constant_score
                                      {:filter
                                              {:multi_match
                                               {:query "hakusana"
                                                :fields ["tutkintonimikes.nimi.kieli_fi" "koulutusala.nimi.kieli_fi" "tutkinto.nimi.kieli_fi"]
                                                :operator "and"}}
                                       :boost 5}}
                                     {:constant_score
                                      {:filter
                                       {:multi_match
                                        {:query "hakusana"
                                         :fields ["aihees.nimi.kieli_fi" "searchData.oppiaineet.kieli_fi" "ammattinimikkeet.nimi.kieli_fi"]
                                         :operator "and"}} :boost 4}}
                                     {:constant_score
                                      {:filter
                                       {:multi_match
                                        {:query "hakusana"
                                         :fields ["searchData.organisaatio.nimi.kieli_fi"]
                                         :operator "and"}} :boost 2}}]}}
                   :must_not
                         {:range
                          {:searchData.opintopolunNayttaminenLoppuu
                           {:format "yyyy-MM-dd" :lt "now"}}}
                   :filter
                         [{:terms {:searchData.organisaatio.oid ["1.2.246.562.29.70576649506"]}}
                          {:terms {:opetuskielis.uri ["fi"]}}
                          {:terms {:searchData.tyyppi ["ako"]}}
                          {:match {:tila "JULKAISTU"}}
                          {:match {:searchData.haut.tila "JULKAISTU"}}] }})
