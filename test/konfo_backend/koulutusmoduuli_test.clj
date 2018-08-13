(ns konfo-backend.koulutusmoduuli-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [konfo-backend.search.koulutusmoduuli :refer :all]
            [clj-log.access-log]))

(facts "search.koulutusmoduuli"
       (fact "koulutusmoduuli-query"
             (koulutusmoduuli-query "keyword" "en" [] {})
             => {:bool
                 {:must
                  {:dis_max
                   {:queries
                    [{:constant_score
                      {:boost 10
                       :filter
                              {:multi_match
                               {:fields ["searchData.nimi.kieli_en"]
                                :operator "and"
                                :query "keyword"}}}}
                     {:constant_score
                      {:boost 5
                       :filter
                              {:multi_match
                               {:fields ["tutkintonimikes.nimi.kieli_en"
                                         "koulutusala.nimi.kieli_en"
                                         "tutkinto.nimi.kieli_en"]
                                :operator "and"
                                :query "keyword"}}}}
                     {:constant_score
                      {:boost 4
                       :filter
                              {:multi_match
                               {:fields ["aihees.nimi.kieli_en"
                                         "searchData.oppiaineet.kieli_en"
                                         "ammattinimikkeet.nimi.kieli_en"]
                                :operator "and"
                                :query "keyword"}}}}
                     {:constant_score
                      {:boost 2
                       :filter
                              {:multi_match
                               {:fields ["searchData.organisaatio.nimi.kieli_en"]
                                :operator "and"
                                :query "keyword"}}}}]}}
                  :must_not
                  {:range
                   {:searchData.opintopolunNayttaminenLoppuu
                    {:format "yyyy-MM-dd"
                     :lt "now"}}}}})

       (fact "koulutusmoduuli-query with constraints"
             (koulutusmoduuli-query nil "en" []
                                    {:kieli "fi,sv" :paikkakunta "testipaikkakunta" :oppilaitostyyppi "kk,ako"})
             => {:bool
                 {:must
                  {:constant_score
                   {:boost 10
                    :filter
                           {:terms
                            {:opetuskielis.uri ["fi" "sv"]}}}}
                  :must_not
                  {:range
                   {:searchData.opintopolunNayttaminenLoppuu
                    {:format "yyyy-MM-dd"
                     :lt "now"}}}}})

       (fact "koulutusmoduuli-query with oids"
             (koulutusmoduuli-query nil "en" ["oid1" "oid2" "oid3"] {})
             => {:bool
                 {:must
                  {:constant_score
                   {:boost 10
                    :filter
                           {:terms
                            {:searchData.organisaatio.oid ["oid1" "oid2" "oid3"]}}}}
                  :must_not
                  {:range
                   {:searchData.opintopolunNayttaminenLoppuu
                    {:format "yyyy-MM-dd"
                     :lt "now"}}}}}))
