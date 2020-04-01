(ns konfo-backend.search.koulutus-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]
            [konfo-backend.search.search-test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn koulutus-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/koulutukset" query-params))

(defn search
  [& query-params]
  (get-ok (apply koulutus-search-url query-params)))

(defn search-and-get-oids
  [& query-params]
  (vec (map :oid (:hits (apply search query-params)))))

(defn ->bad-request-body
  [& query-params]
  (:body (get-bad-request (apply koulutus-search-url query-params))))

(deftest koulutus-search-test

  (fixture/add-koulutus-mock "1.2.246.562.13.000001" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata koulutus-metatieto)
  (fixture/add-koulutus-mock "1.2.246.562.13.000002" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata koulutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000002" :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)

  (fixture/index-oids-without-related-indices {:koulutukset ["1.2.246.562.13.000001" "1.2.246.562.13.000002"] :oppilaitokset [punkaharjun-yliopisto]} (fn [x & {:as params}] punkaharju-org))

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto mock-get-koodisto]
    (testing "Search koulutukset with bad requests:"
      (testing "Invalid lng"
        (is (= "Virheellinen kieli")      (->bad-request-body :sijainti "kunta_618" :lng "foo")))
      (testing "Invalid sort"
        (is (= "Virheellinen järjestys")  (->bad-request-body :sijainti "kunta_618" :sort "foo")))
      (testing "Too short keyword"
        (is (= "Hakusana on liian lyhyt") (->bad-request-body :sijainti "kunta_618" :keyword "fo")))
      (testing "No keyword nor filters"
        (is (= "Hakusana tai jokin rajain on pakollinen") (->bad-request-body))))

    (testing "Search koulutukset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_618")]
          (is (= 1 (count (:hits r))))
          (is (= "1.2.246.562.13.000001" (:oid (first (:hits r)))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi])))))

      (testing "multiple sijainti"
        (let [r (search :sijainti "%20kunta_618%20,%20kunta_220")]
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
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count]))))))

      (testing "Search koulutukset, get correct result"
        (let [r (search :sijainti "kunta_618")]
          (is (= {:opintojenlaajuusyksikko {:koodiUri   "opintojenlaajuusyksikko_6",
                                            :nimi  {:fi "opintojenlaajuusyksikko_6 nimi fi",
                                                    :sv "opintojenlaajuusyksikko_6 nimi sv"}},
                                            :kuvaus { },
                  :nimi {:fi "Autoalan koulutus fi",
                         :sv "Autoalan koulutus sv"},
                  :oid "1.2.246.562.13.000001",
                  :kielivalinta [ "fi", "sv" ],
                  :koulutus { :koodiUri  "koulutus_371101#1",
                              :nimi {:fi "koulutus_371101#1 nimi fi",
                                     :sv "koulutus_371101#1 nimi sv"}},
                  :tutkintonimikkeet [ {:koodiUri  "tutkintonimikkeet_01",
                                        :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                               :sv "tutkintonimikkeet_01 nimi sv" }},
                                       {:koodiUri  "tutkintonimikkeet_02",
                                        :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                               :sv "tutkintonimikkeet_02 nimi sv"}} ],
                                        :opintojenlaajuus {:koodiUri  "opintojenlaajuus_150",
                                                           :nimi {:fi "opintojenlaajuus_150 nimi fi",
                                                                  :sv "opintojenlaajuus_150 nimi sv"}},
                  :koulutustyyppi "amm"} (first (:hits r))))))))

(deftest koulutus-paging-and-sorting-test

  (fixture/add-koulutus-mock koulutusOid1 :nimi "Aakkosissa ensimmäinen")
  (fixture/add-koulutus-mock koulutusOid2 :nimi "Aakkosissa toinen")
  (fixture/add-koulutus-mock koulutusOid3 :nimi "Aakkosissa vasta kolmas")
  (fixture/add-koulutus-mock koulutusOid4 :nimi "Aakkosissa vasta neljäs")
  (fixture/add-koulutus-mock koulutusOid5 :nimi "Aakkosissa viidentenä")

  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5]})

  (testing "Koulutus search ordering"
    (testing "by default order"
      (is (= [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5] (search-and-get-oids :keyword "aakkosissa"))))
    (testing "Koulutus search ordering"
      (is (= [koulutusOid5 koulutusOid4 koulutusOid3 koulutusOid2 koulutusOid1] (search-and-get-oids :keyword "aakkosissa" :sort "desc")))))

  (testing "Koulutus search paging"
    (testing "returns first page by default"
      (is (= [koulutusOid1 koulutusOid2] (search-and-get-oids :keyword "aakkosissa" :size 2))))
    (testing "returns last page"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "aakkosissa" :size 2 :page 3))))
    (testing "returns correct total count"
      (is (= 5 (:total (get-ok (koulutus-search-url :keyword "aakkosissa" :size 2))))))))

(deftest koulutus-keyword-search

  (fixture/add-koulutus-mock koulutusOid1  :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Lääketieteen koulutus" :metadata yo-koulutus-metatieto)
  (fixture/add-koulutus-mock koulutusOid2  :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Humanistinen koulutus" :metadata yo-koulutus-metatieto)
  (fixture/add-koulutus-mock koulutusOid3  :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Tietojenkäsittelytieteen koulutus" :metadata yo-koulutus-metatieto)
  (fixture/add-koulutus-mock koulutusOid4  :koulutustyyppi "amm" :tila "julkaistu" :nimi "Automaatiotekniikka (ylempi AMK)")
  (fixture/add-koulutus-mock koulutusOid5  :koulutustyyppi "amm" :tila "julkaistu" :nimi "Muusikon koulutus")
  (fixture/add-koulutus-mock koulutusOid6  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Sosiaali- ja terveysalan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid7  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Maanmittausalan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid8  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Pintakäsittelyalan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid9  :koulutustyyppi "amm" :tile "julkaistu" :nimi "Puhtaus- ja kiinteistöpalvelualan ammattitutkinto")
  (fixture/add-koulutus-mock koulutusOid10 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Puhevammaisten tulkkauksen erikoisammattitutkinto")
  (fixture/add-koulutus-mock koulutusOid11 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Hius- ja kauneudenhoitoalan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid12 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Autoalan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid13 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Elintarvikealan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid14 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Eläintenhoidon ammattitutkinto")
  (fixture/add-koulutus-mock koulutusOid15 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Tieto- ja viestintätekniikan ammattitutkinto")
  (fixture/add-koulutus-mock koulutusOid16 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Tanssialan perustutkinto")
  (fixture/add-koulutus-mock koulutusOid17 :koulutustyyppi "amm" :tile "julkaistu" :nimi "Hevostalouden perustutkinto")

  (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "lääkäri"}, {:kieli "fi" :arvo "esimies"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid2 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "psykologi"}] :asiasanat [{:kieli "fi" :arvo "ammattikorkeakoulu"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000004" koulutusOid4 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaatioinsinööri"}] :asiasanat [{:kieli "fi" :arvo "ammattioppilaitos"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000005" koulutusOid5 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :asiasanat [{:kieli "fi" :arvo "musiikkioppilaitokset"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000006" koulutusOid8 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "maalari"}, {:kieli "fi" :arvo "merimies"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000007" koulutusOid12 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaalari"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000008" koulutusOid14 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "eläintenhoitaja"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000009" koulutusOid17 :tila "julkaistu" :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "hevostenhoitaja"}, {:kieli "fi" :arvo "seppä"}]}))


  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6
                                                             koulutusOid7 koulutusOid8 koulutusOid9 koulutusOid10 koulutusOid11 koulutusOid12
                                                             koulutusOid13 koulutusOid14 koulutusOid15 koulutusOid16 koulutusOid17]})

  (testing "Searching with keyword"

    (testing "maalari <-> Pintakäsittelyala (EI maanmittausala)"
      (is (= [koulutusOid12 koulutusOid8] (search-and-get-oids :keyword "maalari"))))

    (testing "puhtaus <-> Puhtaus- ja kiinteistöpalveluala (EI puhevammaisten)"
      (is (= [koulutusOid9] (search-and-get-oids :keyword "puhtaus"))))

    (testing "palvelu <-> Puhtaus- ja kiinteistöpalveluala"
      (is (= [koulutusOid9] (search-and-get-oids :keyword "palvelu"))))

    (testing "ammattitutkinto <-> EI ammattioppilaitos tai ammattikorkeakoulu"
      (is (= [koulutusOid14 koulutusOid10 koulutusOid9 koulutusOid15] (search-and-get-oids :keyword "ammattitutkinto"))))

    (comment testing "sosiaaliala <-> sosiaali- ja terveysala" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid6] (search-and-get-oids :keyword "sosiaaliala"))))

    (testing "terveys <-> sosiaali- ja terveysala"
      (is (= [koulutusOid6] (search-and-get-oids :keyword "terveys"))))

    (testing "musiikkioppilaitos <-> musiikkioppilaitokset"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikkioppilaitos"))))

    (testing "auto <-> automaatiotekniikka/automaatioinsinööri"
      (is (= [koulutusOid12 koulutusOid4] (search-and-get-oids :keyword "auto"))))

    (testing "automaatio <-> automaatiotekniikka/automaatioinsinööri, EI autoalan perustutkintoa"
      (is (= [koulutusOid4] (search-and-get-oids :keyword "automaatio"))))

    (testing "humanismi <-> humanistinen"
      (is (= [koulutusOid2] (search-and-get-oids :keyword "humanismi"))))

    (testing "lääketiede <-> lääketieteen"
      (is (= [koulutusOid1] (search-and-get-oids :keyword "lääketiede"))))

    (testing "muusikko <-> muusikon koulutus"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "muusikko"))))

    (testing "musiikki <-> musiikkioppilaitokset"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikki"))))

    (testing "musikko <-> muusikko"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikki"))))

    (testing "insinööri <-> automaatioinsinööri"
      (is (= [koulutusOid4] (search-and-get-oids :keyword "insinööri"))))

    (testing "insinöö <-> automaatioinsinööri"
      (is (= [koulutusOid4] (search-and-get-oids :keyword "insinööri"))))

    (testing "tekniikka <-> automaatiotekniikka"
      (is (= [koulutusOid4 koulutusOid15] (search-and-get-oids :keyword "tekniikka"))))

    (testing "muusikon koulutus <-> EI muita koulutuksia"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "muusikon%20koulutus"))))

    (testing "Maanmittausalan perustutkinto <-> EI muita perustutkintoja"
      (is (= [koulutusOid7] (search-and-get-oids :keyword "maanmittausalan%20perustutkinto"))))

    (testing "perustutkinto maanmittaus <-> EI muita perustutkintoja"
      (is (= [koulutusOid7] (search-and-get-oids :keyword "perustutkinto%20maanmittaus"))))

    (testing "Maanmittaus perus <-> maanmittausalan perustutkinto"
      (is (= [koulutusOid7] (search-and-get-oids :keyword "maanmittauS%20peruS"))))

    (testing "tietojenkäsittelytiede <-> tietojenkäsittelytieteen"
      (is (= [koulutusOid3] (search-and-get-oids :keyword "tietojenkäsittelytiede"))))

    (comment testing "automaatiikka <-> automaatioinsinööri"  ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid4] (search-and-get-oids :keyword "automaatiikka"))))

    (testing "hius <-> Hius- ja kauneudenhoitoalan perustutkinto"
      (is (= [koulutusOid11] (search-and-get-oids :keyword "hius"))))

    (testing "kauneudenhoito <-> Hius- ja kauneudenhoitoalan perustutkinto"
      (is (= [koulutusOid11] (search-and-get-oids :keyword "kauneudenhoito"))))

    (comment testing "hoito <-> Eläintenhoidon ammattitutkinto sekä Hius- ja kauneudenhoitoalan perustutkinto"  ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid14 koulutusOid11] (search-and-get-oids :keyword "hoito"))))

    (testing "psykologia <-> Psykologi"
      (is (= [koulutusOid2] (search-and-get-oids :keyword "psykologia"))))

    (testing "lääke <-> lääketieteen"
      (is (= [koulutusOid1] (search-and-get-oids :keyword "lääke"))))

    (testing "ylemp <-> ylempi (AMK)"
      (is (= [koulutusOid4] (search-and-get-oids :keyword "ylemp"))))

    (testing "amk <-> ylempi (AMK)"
      (is (= [koulutusOid4] (search-and-get-oids :keyword "amk"))))

    (testing "psygologia <-> psykologia"
      (is (= [koulutusOid2] (search-and-get-oids :keyword "psygologia"))))

    (testing "perus <-> kaikki perustutkinnot"
      (is (= [koulutusOid12 koulutusOid13 koulutusOid17 koulutusOid11 koulutusOid7 koulutusOid8 koulutusOid6 koulutusOid16] (search-and-get-oids :keyword "perus"))))

    (testing "perustutkinto <-> kaikki perustutkinnot"
      (is (= [koulutusOid12 koulutusOid13 koulutusOid17 koulutusOid11 koulutusOid7 koulutusOid8 koulutusOid6 koulutusOid16] (search-and-get-oids :keyword "perustutkinto"))))

    (comment testing "teknikko <-> tekniikka"  ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid4] (search-and-get-oids :keyword "teknikko"))))

    (comment testing "tiede <-> lääketiede ja tietojenkäsittelytiede" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid1 koulutusOid3] (search-and-get-oids :keyword "tiede"))))

    (testing "eläin <-> eläintenhoito EI elintarviketta"
      (is (= [koulutusOid14] (search-and-get-oids :keyword "eläin"))))

    (testing "eläinten <-> eläintenhoito EI tieto- ja viestintätekniikan"
      (is (= [koulutusOid14] (search-and-get-oids :keyword "eläinten"))))

    (testing "merimies <-> merimies EI esimiestä"
      (is (= [koulutusOid8] (search-and-get-oids :keyword "merimies"))))

    (testing "sosiaali <-> ei tanssialaa"
      (is (= [koulutusOid6] (search-and-get-oids :keyword "sosiaali"))))

    (testing "ensihoitaja <-> ei eläinten- eikä hevostenhoitajaa"
      (is (= [] (search-and-get-oids :keyword "ensihoitaja"))))

    (testing "seppä <-> seppä"
      (is (= [koulutusOid17] (search-and-get-oids :keyword "seppä"))))

    (testing "tie <-> lääketiede ja tietojenkäsittelytiede"
      (is (= [koulutusOid1 koulutusOid15 koulutusOid3] (search-and-get-oids :keyword "tie"))))

    (comment testing "haluan opiskella lääkäriksi <-> lääkäri"
             (is (= [koulutusOid1] (search-and-get-oids :keyword "haluan%20opiskella%20lääkäriksi"))))

    (comment testing "musiikin opiskelu <-> muusikon koulutus"
             (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikin%20opiskelu"))))

    (comment testing "haluan opiskella psykologiaa <-> psykologi"
             (is (= [koulutusOid2] (search-and-get-oids :keyword "haluan%20opiskella%20psykologiaa"))))))
