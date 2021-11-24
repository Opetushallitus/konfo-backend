(ns konfo-backend.search.oppilaitos-search-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-tools :refer [debug-pretty]]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]
            [konfo-backend.test-mock-data :refer :all]))

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

(def yo-sorakuvaus-id "2ff6700d-087f-4dbf-9e42-7f38948f3333")
(def sorakuvaus-id "2ff6700d-087f-4dbf-9e42-7f38948f227a")

(deftest oppilaitos-search-test

  (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "amm-tutkinnon-osa" :koulutuksetKoodiUri nil :ePerusteId nil :tila "julkaistu" :johtaaTutkintoon "false" :nimi "Hevosalan tutkinnon osa koulutus" :tarjoajat punkaharjun-yliopisto :metadata amm-tutkinnon-osa-koulutus-metadata :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "amm-osaamisala" :tila "julkaistu" :johtaaTutkintoon "false" :nimi "Hevosalan osaamisala koulutus" :tarjoajat punkaharjun-yliopisto :metadata amm-osaamisala-koulutus-metadata :sorakuvausId sorakuvaus-id)
  (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid2 :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid2 :tila "julkaistu" :nimi "Ponikoulu helsingissä" :tarjoajat helsingin-toimipiste :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000003" koulutusOid3 :tila "julkaistu" :nimi "Ponikoulu tutkinnon osa" :tarjoajat punkaharjun-toimipiste-2 :metadata amm-tutkinnon-osa-toteutus-metadata)

  (fixture/add-sorakuvaus-mock sorakuvaus-id :tila "julkaistu")

  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4] :oppilaitokset [punkaharjun-yliopisto helsingin-yliopisto]} orgs)

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Search oppilaitokset with bad requests:"
      (testing "Invalid lng"
        (is (= "Virheellinen kieli")      (->bad-request-body :sijainti "kunta_618" :lng "foo")))
      (testing "Invalid sort"
        (is (= "Virheellinen järjestys")  (->bad-request-body :sijainti "kunta_618" :sort "foo")))
      (testing "Too short keyword"
        (is (= "Hakusana on liian lyhyt") (->bad-request-body :sijainti "kunta_618" :keyword "fo"))))

    (testing "Search all oppilaitokset"
      (let [r (search :sort "name" :order "asc")]
        (is (= 2 (count (:hits r))))
        (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
        (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
        (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
        (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
        (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
        (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

    (testing "Search oppilaitokset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_618" :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= punkaharjun-yliopisto (:oid (first (:hits r)))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi])))))

      (testing "multiple sijainti"
        (let [r (search :sijainti "%20kunta_618%20,%20kunta_091" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))))

      (testing "koulutustyyppi amm"
        (let [r (search :koulutustyyppi "amm" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "koulutustyyppi amm-osaamisala"
        (let [r (search :koulutustyyppi "amm-osaamisala" :sort "name" :order "asc")]
          ;(debug-pretty r)
          (is (= 1 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "koulutustyyppi amm-tutkinnon-osa"
        (let [r (search :koulutustyyppi "amm-tutkinnon-osa" :sort "name" :order "asc")]
          ;(debug-pretty r)
          (is (= 1 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "koulutustyyppi amm-muu"
        (let [r (search :koulutustyyppi "amm-muu" :sort "name" :order "asc")]
          ;(debug-pretty r)
          (is (= 1 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "opetuskieli"
        (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01" :sort "name" :order "asc")]
          (is (= 0 (count (:hits r))))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 0 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 0 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "koulutusala"
        (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "opetustapa"
        (let [r (search :opetustapa "opetuspaikkakk_02" :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

      (testing "Search oppilaitokset, get correct result"
        (let [r (search :sijainti "kunta_618" :sort "name" :order "asc")]
          (is (= {:nimi {:fi "Punkaharjun yliopisto",
                         :sv "Punkaharjun yliopisto sv"},
                  :koulutusohjelmia 2,
                  :paikkakunnat [{:koodiUri "kunta_618",
                                  :nimi {:fi "kunta_618 nimi fi",
                                         :sv "kunta_618 nimi sv"}},
                                 {:koodiUri "kunta_091",
                                  :nimi {:fi "kunta_091 nimi fi",
                                         :sv "kunta_091 nimi sv"}}],
                  :oid "1.2.246.562.10.000002"} (dissoc (first (:hits r)) :_score))))))))

(deftest oppilaitos-paging-and-sorting-test

  (fixture/add-koulutus-mock koulutusOid1 :tarjoajat oppilaitosOid1 :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid2 :tarjoajat oppilaitosOid2 :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid3 :tarjoajat oppilaitosOid3 :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid4 :tarjoajat oppilaitosOid4 :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid5 :tarjoajat oppilaitosOid5 :sorakuvausId sorakuvaus-id)

  (fixture/add-sorakuvaus-mock sorakuvaus-id :tila "julkaistu")

  (fixture/index-oids-without-related-indices {:koulutukset   [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5]
                                               :oppilaitokset [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5]} aakkos-orgs)

  (testing "Oppilaitos search ordering"
    (testing "by default order"
      (let [hits (:hits (search :keyword "aakkosissa"))]
        (is (= 5 (count hits)))
        (is (>= (:_score (nth hits 0))
                (:_score (nth hits 1))
                (:_score (nth hits 2))
                (:_score (nth hits 3))
                (:_score (nth hits 4))))))
    (testing "order by name asc"
      (is (= [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc"))))
    (testing "order by name desc"
      (is (= [oppilaitosOid5 oppilaitosOid4 oppilaitosOid3 oppilaitosOid2 oppilaitosOid1]  (search-and-get-oids :keyword "aakkosissa"  :sort "name" :order "desc")))))

  (testing "Oppilaitos search paging"
    (testing "returns first page by default"
      (is (= [oppilaitosOid1 oppilaitosOid2] (search-and-get-oids :keyword "aakkosissa" :size 2 :sort "name" :order "asc"))))
    (testing "returns last page"
      (is (= [oppilaitosOid5] (search-and-get-oids :keyword "aakkosissa" :size 2 :page 3 :sort "name" :order "asc"))))
    (testing "returns correct total count"
      (is (= 5 (:total (get-ok (oppilaitos-search-url :keyword "aakkosissa" :size 2 :sort "name" :order "asc"))))))))

(deftest oppilaitos-keyword-search

  (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid1 :nimi "Lääketieteen koulutus" :metadata yo-koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid2 :nimi "Humanistinen koulutus" :metadata yo-koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "yo"  :tila "julkaistu" :tarjoajat oppilaitosOid3 :nimi "Tietojenkäsittelytieteen koulutus" :metadata yo-koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid4 :nimi "Automaatiotekniikka" :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid5 :koulutustyyppi "amm" :tila "julkaistu" :tarjoajat oppilaitosOid5 :nimi "Muusikon koulutus" :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock koulutusOid6 :koulutustyyppi "amm" :tile "julkaistu" :tarjoajat oppilaitosOid6 :nimi "Sosiaali- ja terveysalan perustutkinto" :sorakuvausId sorakuvaus-id)

  (fixture/add-sorakuvaus-mock yo-sorakuvaus-id :tila "julkaistu" :koulutustyyppi "yo")
  (fixture/add-sorakuvaus-mock sorakuvaus-id :tila "julkaistu")

  (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu" :tarjoajat oppilaitosOid1 :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "lääkäri"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid2 :tila "julkaistu" :tarjoajat oppilaitosOid2 :metadata (cheshire/generate-string {:tyyppi "yo" :ammattinimikkeet [{:kieli "fi" :arvo "psykologi"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000004" koulutusOid4 :tila "julkaistu" :tarjoajat oppilaitosOid4 :metadata (cheshire/generate-string {:tyyppi "amm" :ammattinimikkeet [{:kieli "fi" :arvo "automaatioinsinööri"}]}))
  (fixture/add-toteutus-mock "1.2.246.562.17.000005" koulutusOid5 :tila "julkaistu" :tarjoajat oppilaitosOid5 :metadata (cheshire/generate-string {:tyyppi "amm" :asiasanat [{:kieli "fi" :arvo "musiikkioppilaitokset"}]}))

  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5 koulutusOid6]
                                               :oppilaitokset [oppilaitosOid1 oppilaitosOid2 oppilaitosOid3 oppilaitosOid4 oppilaitosOid5 oppilaitosOid6]} aakkos-orgs)

  (testing "Searching with keyword"
    (comment testing "lääketiede <-> lääketieteen" ;Ei toimi enää multi_match-queryllä.
      (is (= [oppilaitosOid1] (search-and-get-oids :sort "name" :order "asc" :keyword "lääketiede"))))

    (testing "lääkäri <-> lääketieteen"
             (is (= [oppilaitosOid1] (search-and-get-oids :sort "name" :order "asc" :keyword "lääkäri"))))

    (comment testing "haluan opiskella lääkäriksi <-> lääkäri"
             (is (= [oppilaitosOid1] (search-and-get-oids :sort "name" :order "asc" :keyword "haluan%20opiskella%20lääkäriksi"))))

    (comment testing "musiikin opiskelu <-> muusikon koulutus"
             (is (= [oppilaitosOid5] (search-and-get-oids :sort "name" :order "asc" :keyword "musiikin%20opiskelu"))))

    (comment testing "humanismi <-> humanistinen" ;Ei toimi enää multi_match-queryllä.
      (is (= [oppilaitosOid2] (search-and-get-oids :sort "name" :order "asc" :keyword "humanismi"))))

    (testing "humanisti <-> humanistinen"
      (is (= [oppilaitosOid2] (search-and-get-oids :sort "name" :order "asc" :keyword "humanisti"))))

    (comment testing "haluan opiskella psykologiaa <-> psykologi"
             (is (= [oppilaitosOid2] (search-and-get-oids :sort "name" :order "asc" :keyword "haluan%20opiskella%20psykologiaa"))))

    (comment testing "sosiaaliala <-> sosiaali- ja terveysala" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
             (is (= [oppilaitosOid6] (search-and-get-oids :sort "name" :order "asc" :keyword "sosiaaliala"))))

    (comment testing "tietojenkäsittelytiede <-> tietojenkäsittelytieteen" ;Ei toimi enää multi_match-queryllä.
      (is (= [oppilaitosOid3] (search-and-get-oids :sort "name" :order "asc" :keyword "tietojenkäsittelytiede"))))

    (testing "tietojenkäsittely <-> tietojenkäsittelytieteen"
      (is (= [oppilaitosOid3] (search-and-get-oids :sort "name" :order "asc" :keyword "tietojenkäsittely"))))

    (comment testing "musiikkioppilaitos <-> musiikkioppilaitokset" ;Ei toimi enää multi_match-queryllä.
      (is (= [oppilaitosOid5] (search-and-get-oids :sort "name" :order "asc" :keyword "musiikkioppilaitos"))))

    (testing "musiikki <-> musiikkioppilaitokset"
      (is (= [oppilaitosOid5] (search-and-get-oids :sort "name" :order "asc" :keyword "musiikki"))))

    (comment testing "automaatiikka <-> automaatioinsinööri" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
             (is (= [oppilaitosOid4] (search-and-get-oids :sort "name" :order "asc" :keyword "automaatiikka"))))

    (testing "auto"
      (is (= [oppilaitosOid4] (search-and-get-oids :sort "name" :order "asc" :keyword "auto"))))

    (testing "muusikon koulutus"
      (is (= [oppilaitosOid5] (search-and-get-oids :sort "name" :order "asc" :keyword "muusikon%20koulutus"))))

    (comment testing "insinööri <-> automaatioinsinööri"
             (is (= [oppilaitosOid4] (search-and-get-oids :sort "name" :order "asc" :keyword "insinööri"))))))
