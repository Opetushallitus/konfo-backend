(ns konfo-backend.search.oppilaitos-search-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-tools :refer [debug-pretty]]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]
            [konfo-backend.search.search-test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn oppilaitos-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/oppilaitokset" query-params))

(defn search
  [& query-params]
  (get-ok (apply oppilaitos-search-url query-params)))

(defn ->bad-request-body
  [& query-params]
  (:body (get-bad-request (apply oppilaitos-search-url query-params))))

(defn search-and-get-oids
  [& query-params]
  (vec (map :oid (:hits (apply search query-params)))))

(deftest oppilaitos-search-test

  (fixture/add-koulutus-mock "1.2.246.562.13.000001" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto)
  (fixture/add-koulutus-mock "1.2.246.562.13.000002" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata koulutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000002" :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000002" "1.2.246.562.13.000002" :tila "julkaistu" :nimi "Ponikoulu helsingissä" :tarjoajat helsingin-toimipiste :metadata toteutus-metatieto)

  (fixture/index-oids-without-related-indices {:koulutukset ["1.2.246.562.13.000001" "1.2.246.562.13.000002"] :oppilaitokset [punkaharjun-yliopisto helsingin-yliopisto]} orgs)

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto mock-get-koodisto]
    (testing "Search oppilaitokset with bad requests:"
      (testing "Invalid lng"
        (is (= "Virheellinen kieli")      (->bad-request-body :sijainti "kunta_618" :lng "foo")))
      (testing "Invalid sort"
        (is (= "Virheellinen järjestys")  (->bad-request-body :sijainti "kunta_618" :sort "foo")))
      (testing "Too short keyword"
        (is (= "Hakusana on liian lyhyt") (->bad-request-body :sijainti "kunta_618" :keyword "fo")))
      (testing "No keyword nor filters"
        (is (= "Hakusana tai jokin rajain on pakollinen") (->bad-request-body))))

    (testing "Search oppilaitokset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_618")]
          (is (= 1 (count (:hits r))))
          (is (= punkaharjun-yliopisto (:oid (first (:hits r)))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi])))))

      (testing "multiple sijainti"
        (let [r (search :sijainti "%20kunta_618%20,%20kunta_091")]
          (is (= 2 (count (:hits r))))))

      (testing "koulutustyyppi"
        (let [r (search :koulutustyyppi "amm")]
          (is (= 2 (count (:hits r))))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "opetuskieli"
        (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 0 (count (:hits r))))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 0 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "koulutusala"
        (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01")]
          (is (= 2 (count (:hits r))))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "Search oppilaitokset, get correct result"
        (let [r (search :sijainti "kunta_618")]
          (is (= {:nimi {:fi "Punkaharjun yliopisto",
                         :sv "Punkaharjun yliopisto sv"},
                  :koulutusohjelmia 2,
                  :paikkakunnat [{:koodiUri "kunta_618",
                                  :nimi {:fi "kunta_618 nimi fi",
                                         :sv "kunta_618 nimi sv"}},
                                 {:koodiUri "kunta_220",
                                  :nimi {:fi "kunta_220 nimi fi",
                                         :sv "kunta_220 nimi sv"}} ],
                  :oid "1.2.246.562.10.000002"} (first (:hits r)))))))))

(deftest oppilaitos-paging-and-sorting-test

  (fixture/add-koulutus-mock koulutusOid1 :tarjoajat oppilaitosOid1)
  (fixture/add-koulutus-mock koulutusOid2 :tarjoajat oppilaitosOid2)
  (fixture/add-koulutus-mock koulutusOid3 :tarjoajat oppilaitosOid3)
  (fixture/add-koulutus-mock koulutusOid4 :tarjoajat oppilaitosOid4)
  (fixture/add-koulutus-mock koulutusOid5 :tarjoajat oppilaitosOid5)

  (fixture/index-oids-without-related-indices {:koulutukset   [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6]
                                               :oppilaitokset [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5]} aakkos-orgs)

  (testing "Oppilaitos search ordering"
    (testing "by default order"
      (is (= [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5] (search-and-get-oids :keyword "aakkosissa"))))
    (testing "Koulutus search ordering"
      (is (= [oppilaitosOid5 oppilaitosOid4 oppilaitosOid3 oppilaitosOid2 oppilaitosOid1]  (search-and-get-oids :keyword "aakkosissa" :sort "desc")))))

  (testing "Oppilaitos search paging"
    (testing "returns first page by default"
      (is (= [oppilaitosOid1 oppilaitosOid2] (search-and-get-oids :keyword "aakkosissa" :size 2))))
    (testing "returns last page"
      (is (= [oppilaitosOid5] (search-and-get-oids :keyword "aakkosissa" :size 2 :page 3))))
    (testing "returns correct total count"
      (is (= 5 (:total (get-ok (oppilaitos-search-url :keyword "aakkosissa" :size 2))))))))

(deftest oppilaitos-keyword-search

  (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid1 :nimi "Lääketieteen koulutus")
  (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid2 :nimi "Humanistinen koulutus")
  (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid3 :nimi "Tietojenkäsittelytieteen koulutus")
  (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid4 :nimi "Automaatiotekniikka")
  (fixture/add-koulutus-mock koulutusOid5 :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid5 :nimi "Muusikon koulutus")
  (fixture/add-koulutus-mock koulutusOid6 :koulutustyyppi "amm" :tile "julkaistu" :tarjoajat oppilaitosOid6 :nimi "Sosiaali- ja terveysalan perustutkinto")

  (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu" :tarjoajat oppilaitosOid1 :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "lääkäri"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid2 :tila "julkaistu" :tarjoajat oppilaitosOid2 :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "psykologi"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000004" koulutusOid4 :tila "julkaistu" :tarjoajat oppilaitosOid4 :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaatioinsinööri"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000005" koulutusOid5 :tila "julkaistu" :tarjoajat oppilaitosOid5 :metadata (cheshire/generate-string {:tyyppi "amm" :asiasanat [{:kieli "fi" :arvo "musiikkioppilaitokset"}]}))

  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6]
                                               :oppilaitokset [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5 oppilaitosOid6]} aakkos-orgs)

  (testing "Searching with keyword"
    (testing "lääketiede <-> lääketieteen"
      (is (= [oppilaitosOid1] (search-and-get-oids :keyword "lääketiede"))))

    (comment testing "haluan opiskella lääkäriksi <-> lääkäri"
      (is (= [oppilaitosOid1] (search-and-get-oids :keyword "haluan%20opiskella%20lääkäriksi"))))

    (comment testing "musiikin opiskelu <-> muusikon koulutus"
      (is (= [oppilaitosOid5] (search-and-get-oids :keyword "musiikin%20opiskelu"))))

    (testing "humanismi <-> humanistinen"
      (is (= [oppilaitosOid2] (search-and-get-oids :keyword "humanismi"))))

    (comment testing "haluan opiskella psykologiaa <-> psykologi"
      (is (= [oppilaitosOid2] (search-and-get-oids :keyword "haluan%20opiskella%20psykologiaa"))))

    (comment testing "sosiaaliala <-> sosiaali- ja terveysala" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [oppilaitosOid6] (search-and-get-oids :keyword "sosiaaliala"))))

    (testing "tietojenkäsittelytiede <-> tietojenkäsittelytieteen"
      (is (= [oppilaitosOid3] (search-and-get-oids :keyword "tietojenkäsittelytiede"))))

    (testing "musiikkioppilaitos <-> musiikkioppilaitokset" ;TODO fix-me
             (is (= [oppilaitosOid5] (search-and-get-oids :keyword "musiikkioppilaitos"))))

    (comment testing "automaatiikka <-> automaatioinsinööri" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [oppilaitosOid4] (search-and-get-oids :keyword "automaatiikka"))))

    (testing "auto"
      (is (= [oppilaitosOid4] (search-and-get-oids :keyword "auto"))))

    (testing "muusikon koulutus"
      (is (= [oppilaitosOid5] (search-and-get-oids :keyword "muusikon%20koulutus"))))

    (comment testing "insinööri <-> automaatioinsinööri"
      (is (= [oppilaitosOid4] (search-and-get-oids :keyword "insinööri"))))))