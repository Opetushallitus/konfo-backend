(ns konfo-backend.index.osaamismerkki-test
  (:require [clojure.test :refer :all]
            [konfo-backend.index.osaamismerkki :refer [parse-osaamismerkki-eperustedata parse-osaamismerkki-kuvaus-items parse-kuvaukset]]))

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

(deftest parse-osaamismerkki-eperustedata-test
  (testing "should parse kuvaus with only osaamistavoitteet"
    (let [osaamismerkki-data {:_id "osaamismerkit_1018"
                              :_source
                              {:osaamistavoitteet [{:osaamistavoite
                                                    {:_tunniste "057dfabf-25cd-4b64-800c-e74b2714e5e3"
                                                     :fi "osaa hakea tietoa digitaalisista ympäristöistä"
                                                     :sv "kan söka information i digitala miljöer"
                                                     :_id "9204226"}
                                                    :id 9203075}]}}]
      (is (= {:osaamismerkit_1018
              {:kuvaus {:osaamistavoitteet
                        {:fi "osaa hakea tietoa digitaalisista ympäristöistä."
                         :sv "kan söka information i digitala miljöer."}
                        :arviointikriteerit {}}
               :kuvake nil}}
             (parse-osaamismerkki-eperustedata osaamismerkki-data)))))

  (testing "should parse kuvaus with osaamistavoitteet and arviointikriteerit and liite image"
    (let [osaamismerkki-data {:_index "osaamismerkki-20-05-2024-at-11.37.57.477"
                              :_id "osaamismerkit_1018"
                              :_score 1.0 :_ignored ["kategoria.liite.binarydata.keyword"]
                              :_source {:osaamistavoitteet [{:osaamistavoite
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
                                                              :id 9203144}]
                                        :kategoria {:nimi
                                                    {:_tunniste "6d20f392-f411-4e85-9d00-559411a6e4d7"
                                                     :fi "Digitaidot"
                                                     :sv "Digital kompetens"
                                                     :_id "9202528"}
                                                    :id 9202623
                                                    :muokattu 1707992127262
                                                    :kuvaus nil
                                                    :liite {:nimi "digitaidot_eitekstia.png"
                                                            :binarydata "iVBORw0KGgoA"
                                                            :mime "image/png"
                                                            :id "ff78de54-0090-484f-87ce-802ea6c70156"}}}}]

      (is (= {:osaamismerkit_1018
              {:kuvaus {:osaamistavoitteet
                        {:fi "osaa hakea tietoa digitaalisista ympäristöistä."
                         :sv "kan söka information i digitala miljöer."}
                        :arviointikriteerit
                        {:fi "nimeää erilaisia hakulähteitä ja -tapoja tiedonhaussa, käyttää hakutermejä tietolähteiden ja tiedon etsimisessä, kertoo perustellen tietolähteiden ja tiedon luotettavuudesta."
                         :sv "ger exempel på olika informationskällor och metoder vid informationssökning, använder söktermer vid sökning av informationskällor och information, beskriver och motiverar informationskällans och informationens tillförlitlighet."}}
               :kuvake {:nimi "digitaidot_eitekstia.png"
                        :binarydata "iVBORw0KGgoA"
                        :mime "image/png"
                        :id "ff78de54-0090-484f-87ce-802ea6c70156"}}}
             (parse-osaamismerkki-eperustedata osaamismerkki-data))))))

(deftest parse-kuvaukset-test
  (testing "should parse kuvaukset with only one osaamismerkki"
    (let [data {:took 618
                :timed_out false
                :_shards {:total 1
                          :successful 1
                          :skipped 0
                          :failed 0}
                :hits {:total {:value 2 :relation "eq"}
                       :max_score 1.0
                       :hits [{:_index "osaamismerkki-20-05-2024-at-11.37.57.477"
                               :_id "osaamismerkit_1018"
                               :_score 1.0 :_ignored ["kategoria.liite.binarydata.keyword"]
                               :_source {:osaamistavoitteet [{:osaamistavoite {:_tunniste "b054d3d6-349e-40e1-aafd-9b7c2672751a"
                                                                               :fi "osaa toimia rakentavasti vuorovaikutuksessa viestintätilanteessa"
                                                                               :sv "kan handla konstruktivt i samverkan med andra i kommunikationssituationer" :_id 9204159}
                                                              :id 9203070}]
                                         :kategoria {:nimi {:_tunniste "b23c4750-f06b-4a15-9f45-7433512a76a8"
                                                            :fi "Vuorovaikutus- ja työhyvinvointitaidot" :sv "Färdigheter i kommunikation och arbetshälsa"
                                                            :_id 9202527}
                                                     :id 9202622
                                                     :muokattu 1710842670880
                                                     :kuvaus nil
                                                     :liite {:nimi "vuorovaikutus_ja_tyohyvinvointitaidot_eitekstia.png"
                                                             :binarydata "iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAYAAACtWK6eAAAACXBIWXMAAAjgAAAI4AGfo+EVAAAgAElEQVR4nGL8//8/wyigLkhnTBdgYGAwYGBgUIBiGJ8B"
                                                             :mime "image/png"
                                                             :id "64eeceb8-1263-47f7-b5f5-b695c32c44ca"}}
                                         :arviointikriteerit [{:arviointikriteeri {:_tunniste "d06762c2-1a95-4d59-8f9a-7e6bb7d60fd4"
                                                                                   :fi "kuuntelee, osallistuu keskusteluun ja kommentoi tarkoituksenmukaisesti, toiset huomioiden ja rakentavasti" :sv "lyssnar, deltar i diskussionen och kommenterar på ett ändamålsenligt och konstruktivt sätt och tar andra i beaktande" :_id 9204155}
                                                               :id 9203019}
                                                              {:arviointikriteeri {:_tunniste "5bbf5323-e263-4c42-8220-4545730f4135" :fi "kertoo oman mielipiteensä rakentavasti ja perustelee sen" :sv "uttrycker och motiverar sin åsikt på ett konstruktivt sätt" :_id 9204156} :id 9203060}]}}
                                ;; {:_index "osaamismerkki-20-05-2024-at-11.37.57.477"
                                ;;  :_id "osaamismerkit_1025"
                                ;;  :_score 1.0
                                ;;  :_ignored ["kategoria.liite.binarydata.keyword"]
                                ;;  :_source {:osaamistavoitteet [{:osaamistavoite {:_tunniste "6c8835ea-e8bc-4325-a700-83a650798497"
                                ;;                                                  :fi "osaa tunnistaa digitaalisten työkalujen ja laitteiden vaikutuksia omaan hyvinvointiinsa"
                                ;;                                                  :sv "känner till hur digitala verktyg och utrustning inverkar på det egna välbefinnandet"
                                ;;                                                  :_id 9205304}
                                ;;                                 :id 9203211}
                                ;;                                {:osaamistavoite {:_tunniste "9b5136ab-81e4-498f-8827-3b7c8e5ca72e"
                                ;;                                                  :fi "osaa tunnistaa digitalisaation ympäristövaikutuksia"
                                ;;                                                  :sv "känner till hur digitaliseringen inverkar på miljön"
                                ;;                                                  :_id 9204291} :id 9203212}]
                                ;;            :kategoria {:nimi {:_tunniste "6d20f392-f411-4e85-9d00-559411a6e4d7"
                                ;;                               :fi "Digitaidot"
                                ;;                               :sv "Digital kompetens"
                                ;;                               :_id 9202528}
                                ;;                        :id 9202623
                                ;;                        :muokattu 1707992127262
                                ;;                        :kuvaus nil
                                ;;                        :liite {:nimi "digitaidot_eitekstia.png"
                                ;;                                :binarydata "iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAYAAACtWK6eAAAACXBIWXMAAAjgAAAI4AGfo+EVAAAgAElEQVR4nGL8//8/wyigLkhnTBdgYGA"
                                ;;                                :mime "image/png"
                                ;;                                :id "ff78de54-0090-484f-87ce-802ea6c70156"}}
                                ;;            :arviointikriteerit [{:arviointikriteeri {:_tunniste "bf435530-3147-42dc-b4df-115755614dbf"
                                ;;                                                      :fi "luettelee digitaalisten laitteiden ja median aiheuttamia fyysisiä ja psyykkisiä haittoja"
                                ;;                                                      :sv "ger exempel på hur digital utrustning och medier inverkar negativt på det fysiska och psykiska välbefinnandet" :_id 9204274} :id 9203203}
                                ;;                                 {:arviointikriteeri {:_tunniste "21370df3-9e9b-4af4-8a72-669a46be6e52"
                                ;;                                                      :fi "nimeää tapoja, joilla digitaalisia ympäristöjä voi hyödyntää hyvinvoinnin parantamiseksi"
                                ;;                                                      :sv "ger exempel på olika sätt att använda digitala miljöer för att förbättra det egna välbefinnandet" :_id 9204275} :id 9203204}
                                ;;                                 {:arviointikriteeri {:_tunniste "c5e5b261-9415-419c-9a4c-e5d3ab929c56"
                                ;;                                                      :fi "kertoo digitalisaation ja digilaitteiden ympäristövaikutuksista"
                                ;;                                                      :sv "beskriver hur digitaliseringen och digital utrustning inverkar på miljön" :_id 9204276} :id 9203205}
                                ;;                                 {:arviointikriteeri {:_tunniste "b130a30f-5e24-4eeb-b974-0fe78832c3d9"
                                ;;                                                      :fi "nimeää tapoja, joilla voi parantaa digitalisaatioon liittyvää kuluttamiskäyttäytymistä"
                                ;;                                                      :sv "ger exempel på olika sätt att förbättra konsumentbeteendet gällande digitaliseringen" :_id 9204277} :id 9203206}]}}
                              ]}}
          result {:osaamismerkit_1018
                  {:kuvaus
                   {:osaamistavoitteet {:fi "osaa toimia rakentavasti vuorovaikutuksessa viestintätilanteessa."
                                        :sv "kan handla konstruktivt i samverkan med andra i kommunikationssituationer."}
                    :arviointikriteerit {:fi "kuuntelee, osallistuu keskusteluun ja kommentoi tarkoituksenmukaisesti, toiset huomioiden ja rakentavasti, kertoo oman mielipiteensä rakentavasti ja perustelee sen."
                                         :sv "lyssnar, deltar i diskussionen och kommenterar på ett ändamålsenligt och konstruktivt sätt och tar andra i beaktande, uttrycker och motiverar sin åsikt på ett konstruktivt sätt."}}
                   :kuvake {:nimi "vuorovaikutus_ja_tyohyvinvointitaidot_eitekstia.png"
                            :binarydata "iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAYAAACtWK6eAAAACXBIWXMAAAjgAAAI4AGfo+EVAAAgAElEQVR4nGL8//8/wyigLkhnTBdgYGAwYGBgUIBiGJ8B"
                            :mime "image/png"
                            :id "64eeceb8-1263-47f7-b5f5-b695c32c44ca"}}}]
      (is (= result (parse-kuvaukset data))))))
