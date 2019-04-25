(ns konfo-backend.search.koulutus-search-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [clj-test-utils.elasticsearch-mock-utils :as utils]
            [konfo-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [cheshire.core :as cheshire]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once utils/mock-embedded-elasticsearch-fixture)
(use-fixtures :each fixture/mock-indexing-fixture)

(defn koulutus-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/koulutukset" query-params))

(defn search-and-get-oids
  [& query-params]
  (map :oid (:koulutukset (get-ok (apply koulutus-search-url query-params)))))

(deftest koulutus-search-test

  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"]

    (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "amk" :tila "julkaistu" :nimi "Hauska koulutus, jolla toteutuksia"       :organisaatio mocks/Oppilaitos1)
    (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Toinen hauska koulutus ilman toteutuksia" :organisaatio mocks/Oppilaitos1)
    (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Ikävä koulutus, jolla on koulutuksia"     :organisaatio mocks/Oppilaitos1)

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu")
    (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid1 :tila "julkaistu")
    (fixture/add-toteutus-mock "1.2.246.562.17.000003" koulutusOid3 :tila "julkaistu")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3]} 1500)

    (testing "Searching koulutukset with and without toteutukset"
      (testing "with keyword and no constraints"
        (is (= [koulutusOid1 koulutusOid2] (search-and-get-oids :keyword "hauska"))))

      (testing "with keyword of two letters"
        (get-bad-request (koulutus-search-url :keyword "ha")))

      (testing "with illegal language"
        (get-bad-request (koulutus-search-url :keyword "hauska" :lng "de")))

      (testing "with constraints and no keywords"
        (is (= [koulutusOid3 koulutusOid2] (search-and-get-oids :koulutustyyppi "yo"))))

      (testing "with both keyword and constraints"
        (is (= [koulutusOid2] (search-and-get-oids :keyword "hauska" :koulutustyyppi "yo"))))

      (testing "with non-existing language"
        (is (= [] (search-and-get-oids :keyword "hauska" :lng "en"))))

      (testing "with no keyword nor constraints"
        (get-bad-request (koulutus-search-url))))))

(deftest koulutus-constraints-test
  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"]

    (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Ammatillinen koulutus, jolla on toteutuksia"     :organisaatio mocks/Toimipiste1OfOppilaitos1 :tarjoajat mocks/Toimipiste1OfOppilaitos1)
    (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Ammatillinen koulutus, jolla ei ole toteutuksia" :organisaatio mocks/Toimipiste2OfOppilaitos1 :tarjoajat mocks/Toimipiste2OfOppilaitos1)
    (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "amk" :tila "julkaistu" :nimi "AMK-koulutus, jolla on toteutuksia"              :organisaatio mocks/Toimipiste2OfOppilaitos1 :tarjoajat mocks/Toimipiste2OfOppilaitos1)
    (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "amk" :tila "julkaistu" :nimi "AMK-koulutus, jolla ei ole toteutuksia"          :organisaatio mocks/Toimipiste1OfOppilaitos1 :tarjoajat mocks/Toimipiste1OfOppilaitos1)

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu" :organisaatio mocks/Toimipiste1OfOppilaitos1 :tarjoajat mocks/Toimipiste1OfOppilaitos1
                               :metadata (cheshire/generate-string {:tyyppi "amm" :opetus { :opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_2#1"]}}))
    (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid3 :tila "julkaistu" :organisaatio mocks/Toimipiste2OfOppilaitos1 :tarjoajat mocks/Toimipiste2OfOppilaitos1)

    (fixture/add-haku-mock "1.2.246.562.20.00001" :hakuaikaAlkaa "2019-04-04T12:00" :hakuaikaPaattyy "2100-04-04T12:00")
    (fixture/add-hakukohde-mock "1.2.246.562.29.00001" "1.2.246.562.17.000002" "1.2.246.562.20.00001" :kaytetaanHaunAikataulua "true")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4]} 1500)

    (testing "Searching koulutukset, filter with"
      (testing "paikkakunta constraint"
        (is (= [koulutusOid3 koulutusOid2] (search-and-get-oids :paikkakunta "Kunta_297%20Nimi%20Fi"))))

      (testing "opetuskieli constraint"
        (is (= [koulutusOid1] (search-and-get-oids :opetuskieli "oppilaitoksenopetuskieli_2"))))

      (testing "opetuskieli constraint"
        (is (= [] (search-and-get-oids :opetuskieli "oppilaitoksenopetuskieli_3"))))

      (testing "opetuskieli constraint with non-existing lng"
        (is (= [] (search-and-get-oids :opetuskieli "oppilaitoksenopetuskieli_2" :lng "en"))))

      (testing "opestuskieli constraint with keyword"
        (is (= [koulutusOid1] (search-and-get-oids :keyword "ammatillinen" :opetuskieli "oppilaitoksenopetuskieli_2"))))

      (testing "koulutustyyppi constraint"
        (is (= [koulutusOid3 koulutusOid4] (search-and-get-oids :koulutustyyppi "AMK"))))

      (testing "koulutustyyppi constraint with non-existing lng"
        (is (= [] (search-and-get-oids :koulutustyyppi "AMK" :lng "en"))))

      (testing "vain haku käynnissä constraint"
        (is (= [koulutusOid3] (search-and-get-oids :vainHakuKaynnissa "true"))))

      (testing "vain haku käynnissä constraint  with non-existing lng"
        (is (= [] (search-and-get-oids :vainHakuKaynnissa "true" :lng "en"))))

      (testing "multiple constraints"
        (is (= [koulutusOid1] (search-and-get-oids :koulutustyyppi "amm" :opetuskieli "oppilaitoksenopetuskieli_2"))))
      )))

(deftest koulutus-paging-and-sorting-test
  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"
        koulutusOid5 "1.2.246.562.13.000005"
        koulutusOid6 "1.2.246.562.13.000006"]

    (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Aakkosissa ensimmäinen, on toteutuksia")
    (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Aakkosissa toinen, ei ole toteutuksia")
    (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Aakkosissa vasta kolmas, haku käynnissä")
    (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "muu" :tila "julkaistu" :nimi "Aakkosissa alussa, ei johda tutkintoon" :johtaaTutkintoon "false")
    (fixture/add-koulutus-mock koulutusOid5 :koulutustyyppi "muu" :tila "julkaistu" :nimi "Aakkosissa viimeinen, ei johda tutkintoon mutta haku on käynnissä" :johtaaTutkintoon "false")
    (fixture/add-koulutus-mock koulutusOid6 :koulutustyyppi "muu" :tila "julkaistu" :nimi "Aakkosissa viimeinen, ei johda tutkintoon" :johtaaTutkintoon "false")

    (fixture/add-haku-mock "1.2.246.562.20.00001" :hakuaikaAlkaa "2019-04-04T12:00" :hakuaikaPaattyy "2100-04-04T12:00")

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu")
    (fixture/add-toteutus-mock "1.2.246.562.17.000003" koulutusOid3 :tila "julkaistu")
    (fixture/add-toteutus-mock "1.2.246.562.17.000005" koulutusOid5 :tila "julkaistu")

    (fixture/add-hakukohde-mock "1.2.246.562.29.00003" "1.2.246.562.17.000003" "1.2.246.562.20.00001" :kaytetaanHaunAikataulua "true")
    (fixture/add-hakukohde-mock "1.2.246.562.29.00005" "1.2.246.562.17.000005" "1.2.246.562.20.00001" :kaytetaanHaunAikataulua "true")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6]} 1500)

    (testing "Koulutus search ordering"
      (is (= [koulutusOid3 koulutusOid1 koulutusOid2 koulutusOid5 koulutusOid4 koulutusOid6] (search-and-get-oids :keyword "aakkosissa"))))

    (testing "Koulutus search paging"
      (testing "returns first page by default"
        (is (= [koulutusOid3 koulutusOid1 koulutusOid2 koulutusOid5] (search-and-get-oids :keyword "aakkosissa" :size 4))))

      (testing "returns last page"
        (is (= [koulutusOid4 koulutusOid6] (search-and-get-oids :keyword "aakkosissa" :size 4 :page 2))))

      (testing "returns correct total count"
        (is (= 6 (:total_count (get-ok (koulutus-search-url :keyword "aakkosissa" :size 4)))))))))

(deftest koulutus-keyword-search
  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"
        koulutusOid5 "1.2.246.562.13.000005"
        koulutusOid6 "1.2.246.562.13.000006"]

    (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Lääketieteen koulutus")
    (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Humanistinen koulutus")
    (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Tietojenkäsittelytieteen koulutus")
    (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Automaatiotekniikka")
    (fixture/add-koulutus-mock koulutusOid5 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Muusikon koulutus")
    (fixture/add-koulutus-mock koulutusOid6 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Sosiaali- ja terveysalan perustutkinto")

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "lääkäri"}]}))
    (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid2 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "psykologi"}]}))
    (fixture/add-toteutus-mock "1.2.246.562.17.000004" koulutusOid4 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaatioinsinööri"}]}))
    (fixture/add-toteutus-mock "1.2.246.562.17.000005" koulutusOid5 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :asiasanat [{:kieli "fi" :arvo "musiikkioppilaitokset"}]}))

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6]} 1500)

    (testing "Searching with keyword"
      (testing "lääketiede <-> lääketieteen"
        (is (= [koulutusOid1] (search-and-get-oids :keyword "lääketiede"))))

      (testing "haluan opiskella lääkäriksi <-> lääkäri"
        (is (= [koulutusOid1] (search-and-get-oids :keyword "haluan%20opiskella%20lääkäriksi"))))

      (testing "musiikin opiskelu <-> muusikon koulutus"
        (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikin%20opiskelu"))))

      (testing "humanismi <-> humanistinen"
        (is (= [koulutusOid2] (search-and-get-oids :keyword "humanismi"))))

      (testing "haluan opiskella psykologiaa <-> psykologi"
        (is (= [koulutusOid2] (search-and-get-oids :keyword "haluan%20opiskella%20psykologiaa"))))

      (testing "sosiaaliala <-> sosiaali- ja terveysala"
        (is (= [koulutusOid6] (search-and-get-oids :keyword "sosiaaliala"))))

      (testing "tietojenkäsittelytiede <-> tietojenkäsittelytieteen"
        (is (= [koulutusOid3] (search-and-get-oids :keyword "tietojenkäsittelytiede"))))

      (testing "musiikkioppilaitos <-> musiikkioppilaitokset"
        (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikkioppilaitos"))))

      (testing "automaatiikka <-> automaatioinsinööri"
        (is (= [koulutusOid4] (search-and-get-oids :keyword "automaatiikka"))))

      (testing "insinööri <-> automaatioinsinööri"
        (is (= [koulutusOid4] (search-and-get-oids :keyword "insinööri")))))))

(defonce mocked-search-response
         {:total 2,
          :hits [{:_source {:koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                                  :en "Further vocational qualification for Commercial Divers"},
                                           :koulutuskoodiUri "koulutus_371101"}],
                            :id 3536456,
                            :kuvaus {:fi "Ammattisukeltajan ammattitutkinnnon kuvaus fi"
                                     :sv "Ammattisukeltajan ammattitutkinnnon kuvaus sv"}}},
                 {:_source {:koulutukset [{:nimi {:fi "Ammattikalastajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                                  :en "Further vocational qualification for Commercial Divers"},
                                           :koulutuskoodiUri "koulutus_371102"}],
                            :id 3536457,
                            :kuvaus {:fi "Ammattikalastajan ammattitutkinnnon kuvaus fi"
                                     :sv "Ammattikalastajan ammattitutkinnnon kuvaus sv"}}}]})

(deftest koulutus-search-result
  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"]

    (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Hauska koulutus 1")
    (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hauska koulutus 2")
    (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hauska koulutus 3" :koulutusKoodiUri "koulutus_371102#2")

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "yo"
                                                                                                                           :ammattinimikkeet [{:kieli "fi" :arvo "kokki"}]
                                                                                                                           :asiasanat [{:kieli "fi" :arvo "pihvi"}]
                                                                                                                           :alemmanKorkeakoulututkinnonOsaamisalat [{:nimi {:fi "ravintolakokkaus"}}]
                                                                                                                           :ylemmanKorkeakoulututkinnonOsaamisalat [{:nimi {:fi "televisiokokkaus"}}]}))
    (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid1 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "yo"
                                                                                                                           :ammattinimikkeet [{:kieli "fi" :arvo "palomies"}, {:kieli "sv" :arvo "palomies sv"}]
                                                                                                                           :alemmanKorkeakoulututkinnonOsaamisalat [{:nimi {:fi "sammuttelu" :sv "sammuttelu sv"}}]
                                                                                                                           :asiasanat [{:kieli "fi" :arvo "letku"}]}))

    (fixture/add-haku-mock "1.2.246.562.20.00001" :hakuaikaAlkaa "2019-04-04T12:00" :hakuaikaPaattyy "2100-04-04T12:00")
    (fixture/add-hakukohde-mock "1.2.246.562.29.00001" "1.2.246.562.17.000001" "1.2.246.562.20.00001" :kaytetaanHaunAikataulua "true")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3]} 1500)

    (with-redefs [konfo-backend.index.eperuste/eperuste-search (fn [f & y] (f mocked-search-response))]
      (testing "koulutus search result"
        (is (= (get-ok (koulutus-search-url :keyword "hauska"))
               {:total_count 3
                :koulutukset [{:oid koulutusOid1
                               :koulutus {:koodiUri "koulutus_371101#1",
                                          :nimi { :fi "koulutus_371101#1 nimi fi", :sv "koulutus_371101#1 nimi sv" } }
                               :johtaaTutkintoon true
                               :koulutustyyppi "yo"
                               :nimi {:fi "Hauska koulutus 1 fi" :sv "Hauska koulutus 1 sv"}
                               :hakuOnKaynnissa true
                               :asiasanat [{:fi "pihvi"}, {:fi "letku"}]
                               :ammattinimikkeet [{:fi "kokki"}, {:fi "palomies"}, {:sv "palomies sv"}]
                               :ammatillisenKoulutuksenKuvaus {}
                               :ylemmanKorkeakoulututkinnonOsaamisalat [{:fi "televisiokokkaus"}]
                               :alemmanKorkeakoulututkinnonOsaamisalat [{:fi "ravintolakokkaus"}, {:fi "sammuttelu" :sv "sammuttelu sv"}]},
                              {:oid koulutusOid2
                               :koulutustyyppi "amm"
                               :koulutus {:koodiUri "koulutus_371101#1",
                                          :nimi { :fi "koulutus_371101#1 nimi fi", :sv "koulutus_371101#1 nimi sv" } }
                               :nimi {:fi "Hauska koulutus 2 fi" :sv "Hauska koulutus 2 sv"}
                               :hakuOnKaynnissa false
                               :johtaaTutkintoon true
                               :asiasanat []
                               :ammattinimikkeet []
                               :ammatillisenKoulutuksenKuvaus {:fi "Ammattisukeltajan ammattitutkinnnon kuvaus fi"
                                                               :sv "Ammattisukeltajan ammattitutkinnnon kuvaus sv"}
                               :ylemmanKorkeakoulututkinnonOsaamisalat []
                               :alemmanKorkeakoulututkinnonOsaamisalat []},
                              {:oid koulutusOid3
                               :koulutustyyppi "amm"
                               :koulutus {:koodiUri "koulutus_371102#2",
                                          :nimi { :fi "koulutus_371102#2 nimi fi", :sv "koulutus_371102#2 nimi sv" } }
                               :nimi {:fi "Hauska koulutus 3 fi" :sv "Hauska koulutus 3 sv"}
                               :hakuOnKaynnissa false
                               :johtaaTutkintoon true
                               :asiasanat []
                               :ammattinimikkeet []
                               :ammatillisenKoulutuksenKuvaus {:fi "Ammattikalastajan ammattitutkinnnon kuvaus fi"
                                                               :sv "Ammattikalastajan ammattitutkinnnon kuvaus sv"}
                               :ylemmanKorkeakoulututkinnonOsaamisalat []
                               :alemmanKorkeakoulututkinnonOsaamisalat []}]}))))))