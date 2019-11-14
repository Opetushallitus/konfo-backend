(ns konfo-backend.search.koulutus-search-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]))

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

(defn create-koulutus-metatieto
  []
  (cheshire/generate-string
    {:tyyppi "amm",
     :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso1_01#1",
                            "kansallinenkoulutusluokitus2016koulutusalataso1_02#1"]}))

(defn create-toteutus-metatieto
  []
  (cheshire/generate-string
    {:tyyppi           "amm"
     :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
     :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]
     :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_02"]}}))

(defn mock-koodistot
  [x]
  (cond
    (= "maakunta" x)                                        ["maakunta_01",
                                                             "maakunta_02"]
    (= "oppilaitoksenopetuskieli" x)                        ["oppilaitoksenopetuskieli_01",
                                                             "oppilaitoksenopetuskieli_02"]
    (= "kansallinenkoulutusluokitus2016koulutusalataso1" x) ["kansallinenkoulutusluokitus2016koulutusalataso1_01",
                                                             "kansallinenkoulutusluokitus2016koulutusalataso1_02"]
    :else []))

(deftest koulutus-search-test
  (let [punkaharjun-yliopisto    "1.2.246.562.10.000002"
        punkaharjun-toimipiste-1 "1.2.246.562.10.000003"
        punkaharjun-toimipiste-2 "1.2.246.562.10.000004"]

    (defn punkaharju-org
      [x & {:as params}]
      (mocks/create-organisaatio-hierarkia
        {:oid "1.2.246.562.10.000001"
         :nimi {:fi "Punkaharjun kunta"
                :sv "Punkaharjun kunta sv"}
         :kotipaikka "kunta_618"
         :kielet ["oppilaitoksenopetuskieli_1#1",
                  "oppilaitoksenopetuskieli_2#1" ]}
        {:oid punkaharjun-yliopisto
         :nimi {:fi "Punkaharjun yliopisto"
                :sv "Punkaharjun yliopisto sv"}
         :kotipaikka "kunta_618"
         :kielet ["oppilaitoksenopetuskieli_1#1",
                  "oppilaitoksenopetuskieli_2#1" ]}
        [{:oid punkaharjun-toimipiste-1
          :nimi {:fi "Punkaharjun yliopiston toimipiste"
                 :sv "Punkaharjun yliopiston toimipiste sv "}
          :kotipaikka "kunta_618"
          :kielet ["oppilaitoksenopetuskieli_2#1" ]},
         {:oid punkaharjun-toimipiste-2
          :nimi {:fi "Punkaharjun yliopiston Karjaan toimipiste"
                 :sv "Punkaharjun yliopiston Karjaan toimipiste sv "}
          :kotipaikka "kunta_220"
          :kielet ["oppilaitoksenopetuskieli_1#1"]}]))

    (fixture/add-koulutus-mock "1.2.246.562.13.000001" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata (create-koulutus-metatieto))
    (fixture/add-koulutus-mock "1.2.246.562.13.000002" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata (create-koulutus-metatieto))
    (fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000002" :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata (create-toteutus-metatieto))

    (fixture/index-oids-without-related-indices {:koulutukset ["1.2.246.562.13.000001" "1.2.246.562.13.000002"] :oppilaitokset [punkaharjun-yliopisto]} punkaharju-org)

    (with-redefs [konfo-backend.koodisto.koodisto/list-koodi-urit mock-koodistot]
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
            (is (= {:koulutustyyppi   {:amm 1 },
                    :opetuskieli      {:oppilaitoksenopetuskieli_01 0,
                                       :oppilaitoksenopetuskieli_02 0 },
                    :sijainti         {:maakunta_01 1,
                                       :maakunta_02 0 },
                    :koulutusalataso1 {:kansallinenkoulutusluokitus2016koulutusalataso1_01 1,
                                       :kansallinenkoulutusluokitus2016koulutusalataso1_02 1}} (:filters r)))))
        (testing "koulutustyyppi"
          (let [r (search :koulutustyyppi "amm")]
            (is (= 2 (count (:hits r))))
            (is (= {:koulutustyyppi   {:amm 2 },
                    :opetuskieli      {:oppilaitoksenopetuskieli_01 0,
                                       :oppilaitoksenopetuskieli_02 1 },
                    :sijainti         {:maakunta_01 2,
                                       :maakunta_02 0 },
                    :koulutusalataso1 {:kansallinenkoulutusluokitus2016koulutusalataso1_01 2,
                                       :kansallinenkoulutusluokitus2016koulutusalataso1_02 2}} (:filters r)))))

        (testing "opetuskieli"
          (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01")]
            (is (= 0 (count (:hits r))))
            (is (= {:koulutustyyppi   {:amm 0 },
                    :opetuskieli      {:oppilaitoksenopetuskieli_01 0,
                                       :oppilaitoksenopetuskieli_02 0 },
                    :sijainti         {:maakunta_01 0,
                                       :maakunta_02 0 },
                    :koulutusalataso1 {:kansallinenkoulutusluokitus2016koulutusalataso1_01 0,
                                       :kansallinenkoulutusluokitus2016koulutusalataso1_02 0}} (:filters r)))))

        (testing "koulutusala"
          (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01")]
            (is (= 2 (count (:hits r))))
            (is (= {:koulutustyyppi   {:amm 2},
                    :opetuskieli      {:oppilaitoksenopetuskieli_01 0,
                                       :oppilaitoksenopetuskieli_02 1},
                    :sijainti         {:maakunta_01 2,
                                       :maakunta_02 0},
                    :koulutusalataso1 {:kansallinenkoulutusluokitus2016koulutusalataso1_01 2,
                                       :kansallinenkoulutusluokitus2016koulutusalataso1_02 2}} (:filters r))))))

        (testing "Search koulutukset, get correct result"
          (let [r (search :sijainti "kunta_618")]
            (is (= {:opintojenlaajuusyksikko {:koodiUri   "opintojenlaajuusyksikko_01",
                                              :nimi  {:fi "opintojenlaajuusyksikko_01 nimi fi",
                                                      :sv "opintojenlaajuusyksikko_01 nimi sv"}},
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
                                          :opintojenlaajuus {:koodiUri  "opintojenlaajuus_01",
                                                             :nimi {:fi "opintojenlaajuus_01 nimi fi",
                                                                    :sv "opintojenlaajuus_01 nimi sv"}},
                    :koulutustyyppi "amm"})))))))

(deftest koulutus-paging-and-sorting-test
  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"
        koulutusOid5 "1.2.246.562.13.000005"]

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
        (is (= 5 (:total (get-ok (koulutus-search-url :keyword "aakkosissa" :size 2)))))))))

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

     (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6]})

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

       (comment testing "musiikkioppilaitos <-> musiikkioppilaitokset" ;TODO fix-me
         (is (= [koulutusOid5] (search-and-get-oids :keyword "musiikkioppilaitos"))))

       (testing "automaatiikka <-> automaatioinsinööri"
         (is (= [koulutusOid4] (search-and-get-oids :keyword "automaatiikka"))))

       (testing "insinööri <-> automaatioinsinööri"
         (is (= [koulutusOid4] (search-and-get-oids :keyword "insinööri")))))))