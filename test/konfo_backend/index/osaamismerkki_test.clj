(ns konfo-backend.index.osaamismerkki-test
  (:require [clojure.test :refer :all]
            [konfo-backend.index.osaamismerkki :refer [parse-osaamismerkki-kuvaus parse-osaamismerkki-kuvaus-items]]))

(deftest parse-osaamismerkki-kuvaus-items-test
  (testing "should parse osaamistavoitteet with one tavoite"
    (let [osaamistavoitteet [{:osaamistavoite
                              {:_tunniste "057dfabf-25cd-4b64-800c-e74b2714e5e3"
                               :fi "osaa hakea tietoa digitaalisista ympäristöistä"
                               :sv "kan söka information i digitala miljöer"
                               :_id "9204226"}
                              :id 9203075}]]
      (is (= {:fi "osaa hakea tietoa digitaalisista ympäristöistä."
              :sv "kan söka information i digitala miljöer."}
             (parse-osaamismerkki-kuvaus-items osaamistavoitteet :osaamistavoite)))))

  (testing "should parse osaamistavoitteet with two tavoite"
    (let [osaamistavoitteet [{:osaamistavoite
                              {:_tunniste "057dfabf-25cd-4b64-800c-e74b2714e5e3"
                               :fi "osaa hakea tietoa digitaalisista ympäristöistä"
                               :sv "kan söka information i digitala miljöer"
                               :_id "9204226"}
                              :id 9203075}
                             {:osaamistavoite
                              {:_tunniste "aa44b884-d542-4020-bff5-a1a3ccadc705"
                               :fi "osaa arvioida tietolähteiden ja tiedon luotettavuutta"
                               :sv "kan bedöma hur tillförlitliga informationskällorna och informationen är"
                               :_id "9204227"}
                              :id 9203076}]]
      (is (= {:fi "osaa hakea tietoa digitaalisista ympäristöistä, osaa arvioida tietolähteiden ja tiedon luotettavuutta."
              :sv "kan söka information i digitala miljöer, kan bedöma hur tillförlitliga informationskällorna och informationen är."}
             (parse-osaamismerkki-kuvaus-items osaamistavoitteet :osaamistavoite)))))

  (testing "should parse arviointikriteerit with two kriteeri"
    (let [arviointikriteerit [{:arviointikriteeri
                               {:_tunniste "a04cb8ee-9462-4409-8668-45defe1fa60e"
                                :fi "nimeää erilaisia hakulähteitä ja -tapoja tiedonhaussa"
                                :sv "ger exempel på olika informationskällor och metoder vid informationssökning"
                                :_id "9204221"}
                               :id 9203142}
                              {:arviointikriteeri
                               {:_tunniste "9ba3cea4-f188-45c1-a019-bee840aa6adb"
                                :fi "käyttää hakutermejä tietolähteiden ja tiedon etsimisessä"
                                :sv "använder söktermer vid sökning av informationskällor och information"
                                :_id "9204222"}
                               :id 9203143}
                              {:arviointikriteeri
                               {:_tunniste "c2d969eb-4659-40ee-92cc-4129c01dcd36"
                                :fi "kertoo perustellen tietolähteiden ja tiedon luotettavuudesta"
                                :sv "beskriver och motiverar informationskällans och informationens tillförlitlighet"
                                :_id "9204223"}
                               :id 9203144}]]
      (is (= {:fi "nimeää erilaisia hakulähteitä ja -tapoja tiedonhaussa, käyttää hakutermejä tietolähteiden ja tiedon etsimisessä, kertoo perustellen tietolähteiden ja tiedon luotettavuudesta."
              :sv "ger exempel på olika informationskällor och metoder vid informationssökning, använder söktermer vid sökning av informationskällor och information, beskriver och motiverar informationskällans och informationens tillförlitlighet."}
             (parse-osaamismerkki-kuvaus-items arviointikriteerit :arviointikriteeri)))))

  (testing "should return empty map when arviointikriteerit list is empty"
    (is (= {}
           (parse-osaamismerkki-kuvaus-items [] :arviointikriteeri))))

  (testing "should return empty map when arviointikriteerit is nil"
    (is (= {}
           (parse-osaamismerkki-kuvaus-items nil :arviointikriteeri)))))

(deftest parse-osaamismerkki-kuvaus-test
  (testing "should parse kuvaus with only osaamistavoitteet"
    (let [osaamismerkki-data {:id 9203131
                              :koodiUri "osaamismerkit_1022"
                              :osaamistavoitteet [{:osaamistavoite
                                                   {:_tunniste "057dfabf-25cd-4b64-800c-e74b2714e5e3"
                                                    :fi "osaa hakea tietoa digitaalisista ympäristöistä"
                                                    :sv "kan söka information i digitala miljöer"
                                                    :_id "9204226"}
                                                   :id 9203075}]}]
      (is (= {:osaamistavoitteet
              {:fi "osaa hakea tietoa digitaalisista ympäristöistä."
               :sv "kan söka information i digitala miljöer."}
              :arviointikriteerit {}}
             (parse-osaamismerkki-kuvaus osaamismerkki-data)))))

  (testing "should parse kuvaus with osaamistavoitteet and arviointikriteerit"
    (let [osaamismerkki-data {:id 9203131
                              :koodiUri "osaamismerkit_1022"
                              :osaamistavoitteet [{:osaamistavoite
                                                   {:_tunniste "057dfabf-25cd-4b64-800c-e74b2714e5e3"
                                                    :fi "osaa hakea tietoa digitaalisista ympäristöistä"
                                                    :sv "kan söka information i digitala miljöer"
                                                    :_id "9204226"}
                                                   :id 9203075}]
                              :arviointikriteerit [{:arviointikriteeri
                                                    {:_tunniste "a04cb8ee-9462-4409-8668-45defe1fa60e"
                                                     :fi "nimeää erilaisia hakulähteitä ja -tapoja tiedonhaussa"
                                                     :sv "ger exempel på olika informationskällor och metoder vid informationssökning"
                                                     :_id "9204221"}
                                                    :id 9203142}
                                                   {:arviointikriteeri
                                                    {:_tunniste "9ba3cea4-f188-45c1-a019-bee840aa6adb"
                                                     :fi "käyttää hakutermejä tietolähteiden ja tiedon etsimisessä"
                                                     :sv "använder söktermer vid sökning av informationskällor och information"
                                                     :_id "9204222"}
                                                    :id 9203143}
                                                   {:arviointikriteeri
                                                    {:_tunniste "c2d969eb-4659-40ee-92cc-4129c01dcd36"
                                                     :fi "kertoo perustellen tietolähteiden ja tiedon luotettavuudesta"
                                                     :sv "beskriver och motiverar informationskällans och informationens tillförlitlighet"
                                                     :_id "9204223"}
                                                    :id 9203144}]}]
      (is (= {:osaamistavoitteet
              {:fi "osaa hakea tietoa digitaalisista ympäristöistä."
               :sv "kan söka information i digitala miljöer."}
              :arviointikriteerit
              {:fi "nimeää erilaisia hakulähteitä ja -tapoja tiedonhaussa, käyttää hakutermejä tietolähteiden ja tiedon etsimisessä, kertoo perustellen tietolähteiden ja tiedon luotettavuudesta."
               :sv "ger exempel på olika informationskällor och metoder vid informationssökning, använder söktermer vid sökning av informationskällor och information, beskriver och motiverar informationskällans och informationens tillförlitlighet."}}
             (parse-osaamismerkki-kuvaus osaamismerkki-data))))))
