(ns konfo-backend.search.koulutus-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]
            [konfo-backend.search.search-test-tools :refer :all]
            [cheshire.core :refer [generate-string]])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(intern 'clj-log.access-log 'service "konfo-backend")

(defonce KoutaFixtureTool KoutaFixtureTool$/MODULE$)

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

(defn mock-get-kuvaukset
  [x]
  [{:id 1234 :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"} :tyotehtavatJoissaVoiToimia {:fi "työtehtävät fi" :sv "työtehtävät sv"}}])

(def autoala-oid "1.2.246.562.13.000001")
(def hevosala-oid "1.2.246.562.13.000002")
(def hevostutkinnon-osa-oid "1.2.246.562.13.000003")
(def hevososaamisala-oid "1.2.246.562.13.000004")

(def ponitoteutus-oid "1.2.246.562.17.000001")
(def poniosatoteutus-oid "1.2.246.562.17.000002")

(def haku-oid "1.2.246.562.29.00000000000000000001")
(def hakukohde-oid-1 "1.2.246.562.20.00000000000000000001")
(def hakukohde-oid-2 "1.2.246.562.20.00000000000000000002")
(def valintaperuste-id "a5e88367-555b-4d9e-aa43-0904e5ea0a13")
(def sorakuvaus-id "ffa8c6cf-a962-4bb2-bf61-fe8fc741fabd")

(deftest koulutus-search-test

  (fixture/add-koulutus-mock autoala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata koulutus-metatieto)
  (fixture/add-koulutus-mock hevosala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata koulutus-metatieto)
  (fixture/add-koulutus-mock hevostutkinnon-osa-oid :koulutustyyppi "amm-tutkinnon-osa" :koulutuksetKoodiUri nil :ePerusteId nil :tila "julkaistu" :johtaaTutkintoon "false" :nimi "Hevosalan tutkinnon osa koulutus" :tarjoajat punkaharjun-yliopisto :metadata (.ammTutkinnonOsaKoulutusMetadata KoutaFixtureTool))
  (fixture/add-koulutus-mock hevososaamisala-oid :koulutustyyppi "amm-osaamisala" :tila "julkaistu" :johtaaTutkintoon "false" :nimi "Hevosalan osaamisala koulutus" :tarjoajat punkaharjun-yliopisto :metadata (.ammOsaamisalaKoulutusMetadata KoutaFixtureTool))
  (fixture/add-toteutus-mock ponitoteutus-oid hevosala-oid :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock poniosatoteutus-oid hevostutkinnon-osa-oid :tila "julkaistu" :nimi "Ponikoulu tutkinnon osa" :tarjoajat punkaharjun-toimipiste-2 :metadata (.ammTutkinnonOsaToteutusMetadata KoutaFixtureTool))

  (fixture/add-haku-mock haku-oid :tila "julkaistu" :nimi "Hevoshaku" :muokkaaja "1.2.246.562.24.62301161440")
  (fixture/add-hakukohde-mock hakukohde-oid-1 ponitoteutus-oid haku-oid :tila "julkaistu" :valintaperuste valintaperuste-id :nimi "ponikoulun hakukohde" :muokkaaja "1.2.246.562.24.62301161440" :hakuaikaAlkaa "2000-01-01T00:00")
  (fixture/add-hakukohde-mock hakukohde-oid-2 poniosatoteutus-oid haku-oid :tila "julkaistu" :valintaperuste valintaperuste-id :nimi "ponikoulun hakukohde" :muokkaaja "1.2.246.562.24.62301161440" :hakuaikaAlkaa "2000-01-01T00:00" :hakuaikaPaattyy "2000-01-02T00:00")
  (fixture/add-sorakuvaus-mock sorakuvaus-id :tila "julkaistu" :nimi "Sorakuvaus" :muokkaaja "1.2.246.562.24.62301161440")
  (fixture/add-valintaperuste-mock valintaperuste-id :tila "julkaistu" :nimi "Valintaperuste" :muokkaaja "1.2.246.562.24.62301161440")

  (fixture/index-oids-without-related-indices {:koulutukset [autoala-oid hevosala-oid hevostutkinnon-osa-oid hevososaamisala-oid] :oppilaitokset [punkaharjun-yliopisto]} (fn [x & {:as params}] punkaharju-org))

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto mock-get-koodisto
                konfo-backend.index.eperuste/get-kuvaukset-by-eperuste-ids mock-get-kuvaukset]
    (testing "Search koulutukset with bad requests:"
      (testing "Invalid lng"
        (is (= "Virheellinen kieli")      (->bad-request-body :sijainti "kunta_618" :lng "foo")))
      (testing "Invalid sort"
        (is (= "Virheellinen järjestys")  (->bad-request-body :sijainti "kunta_618" :order "foo")))
      (testing "Too short keyword"
        (is (= "Hakusana on liian lyhyt") (->bad-request-body :sijainti "kunta_618" :keyword "fo"))))

    (testing "Search all koulutukset"
      (let [r (search :sort "name" :order "asc")]
        (is (= 4 (count (:hits r))))
        (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
        (is (= 4 (get-in r [:filters :maakunta :maakunta_01 :count])))
        (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
        (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
        (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
        (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
        (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
        (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
        (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
        (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
        (is (= 2 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
        ;NOTE: fixturen with-mocked-indexing mäppää pohjakoulutusvaatimuksessa kaikki koutakoodit -> konfo_am koodeiksi
        (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
        (is (= 2 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

    (testing "Search koulutukset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_618" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= "1.2.246.562.13.000001" (:oid (first (:hits r)))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 0 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 0 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 0 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi])))))

      (testing "multiple sijainti"
        (let [r (search :sijainti "%20kunta_618%20,%20kunta_220" :sort "name" :order "asc")]
          (is (= 4 (count (:hits r))))))

      (testing "koulutustyyppi amm"
        (let [r (search :koulutustyyppi "amm" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 1 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 1 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "koulutustyyppi amm-osaamisala"
        (let [r (search :koulutustyyppi "amm-osaamisala" :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 0 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 0 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 0 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "koulutustyyppi amm-tutkinnon-osa"
        (let [r (search :koulutustyyppi "amm-tutkinnon-osa" :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 0 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 1 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 1 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "koulutustyyppi amm-muu"
        (let [r (search :koulutustyyppi "amm-muu" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-osaamisala :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :amm-muu :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 0 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 1 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 1 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "opetuskieli"
        (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01" :sort "name" :order "asc")]
          (is (= 0 (count (:hits r))))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 0 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 0 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 0 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 0 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 0 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "koulutusala"
        (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01" :sort "name" :order "asc")]
          (is (= 4 (count (:hits r))))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 4 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 2 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 2 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

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
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 1 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 1 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "valintatapa"
        (let [r (search :valintatapa "valintatapajono_av" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 2 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 2 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "hakukaynnissa"
        (let [r (search :hakukaynnissa true :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 1 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 1 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 1 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "hakutapa"
        (let [r (search :hakutapa "hakutapa_03" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 2 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 2 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count]))))))

      (testing "pohjakoulutusvaatimus"
        (let [r (search :pohjakoulutusvaatimus "pohjakoulutusvaatimuskonfo_am" :sort "name" :order "asc")]
          (is (= 2 (count (:hits r))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_av :count])))
          (is (= 2 (get-in r [:filters :valintatapa :valintatapajono_tv :count])))
          (is (= 1 (get-in r [:filters :hakukaynnissa :count])))
          (is (= 0 (get-in r [:filters :hakutapa :hakutapa_01 :count])))
          (is (= 2 (get-in r [:filters :hakutapa :hakutapa_03 :count])))
          (is (= 0 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_01 :count])))
          (is (= 2 (get-in r [:filters :pohjakoulutusvaatimus :pohjakoulutusvaatimuskonfo_am :count])))))

      (testing "Search koulutukset, get correct result"
        (let [r (search :sijainti "kunta_618" :sort "name" :order "asc")]
          (is (= {:opintojenLaajuusyksikko {:koodiUri   "opintojenlaajuusyksikko_6",
                                            :nimi  {:fi "opintojenlaajuusyksikko_6 nimi fi",
                                                    :sv "opintojenlaajuusyksikko_6 nimi sv"}},
                  :kuvaus {:fi "osaaminen fi" :sv "osaaminen sv"},
                  :teemakuva "https://testi.fi/koulutus-teemakuva/oid/kuva.jpg",
                  :nimi {:fi "Autoalan koulutus fi",
                         :sv "Autoalan koulutus sv"},
                  :oid "1.2.246.562.13.000001",
                  :kielivalinta [ "fi", "sv" ],
                  :koulutukset [{ :koodiUri  "koulutus_371101#1",
                                  :nimi {:fi "koulutus_371101#1 nimi fi",
                                         :sv "koulutus_371101#1 nimi sv"}}],
                  :tutkintonimikkeet [ {:koodiUri  "tutkintonimikkeet_01",
                                        :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                               :sv "tutkintonimikkeet_01 nimi sv" }},
                                       {:koodiUri  "tutkintonimikkeet_02",
                                        :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                               :sv "tutkintonimikkeet_02 nimi sv"}} ],
                  :opintojenLaajuusNumero 150,
                  :opintojenLaajuus {:koodiUri  "opintojenlaajuus_150",
                                     :nimi {:fi "opintojenlaajuus_150 nimi fi",
                                            :sv "opintojenlaajuus_150 nimi sv"}},
                  :eperuste 1234,
                  :koulutustyyppi "amm"} (dissoc (first (:hits r)) :_score)))))))

(deftest koulutus-paging-and-sorting-test

  (fixture/add-koulutus-mock koulutusOid1 :nimi "Aakkosissa ensimmäinen")
  (fixture/add-koulutus-mock koulutusOid2 :nimi "Aakkosissa toinen")
  (fixture/add-koulutus-mock koulutusOid3 :nimi "Aakkosissa vasta kolmas")
  (fixture/add-koulutus-mock koulutusOid4 :nimi "Aakkosissa vasta neljäs")
  (fixture/add-koulutus-mock koulutusOid5 :nimi "Aakkosissa viidentenä")

  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5]})

  (testing "Koulutus search ordering"
    (testing "by default order"
      (let [hits (:hits (search :keyword "aakkosissa"))]
        (is (= 5 (count hits)))
        (is (>= (:_score (nth hits 0))
                (:_score (nth hits 1))
                (:_score (nth hits 2))
                (:_score (nth hits 3))
                (:_score (nth hits 4))))))
    (testing "order by name asc"
      (is (= [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc"))))
    (testing "order by name desc"
      (is (= [koulutusOid5 koulutusOid4 koulutusOid3 koulutusOid2 koulutusOid1] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "desc")))))

  (testing "Koulutus search paging"
    (testing "returns first page by default"
      (is (= [koulutusOid1 koulutusOid2] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc" :size 2))))
    (testing "returns last page"
      (is (= [koulutusOid5] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc" :size 2 :page 3))))
    (testing "returns correct total count"
      (is (= 5 (:total (get-ok (koulutus-search-url :keyword "aakkosissa" :sort "name" :order "asc" :size 2))))))))

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
      (is (= [koulutusOid12 koulutusOid8] (search-and-get-oids :sort "name" :order "asc"  :keyword "maalari"))))

    (testing "puhtaus <-> Puhtaus- ja kiinteistöpalveluala (EI puhevammaisten)"
      (is (= [koulutusOid9] (search-and-get-oids :sort "name" :order "asc"  :keyword "puhtaus"))))

    (testing "palvelu <-> Puhtaus- ja kiinteistöpalveluala"
      (is (= [koulutusOid9] (search-and-get-oids :sort "name" :order "asc"  :keyword "palvelu"))))

    (testing "ammattitutkinto <-> EI ammattioppilaitos tai ammattikorkeakoulu"
      (is (= [koulutusOid14 koulutusOid10 koulutusOid9 koulutusOid15] (search-and-get-oids :sort "name" :order "asc"  :keyword "ammattitutkinto"))))

    (comment testing "sosiaaliala <-> sosiaali- ja terveysala" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid6] (search-and-get-oids :sort "name" :order "asc"  :keyword "sosiaaliala"))))

    (testing "terveys <-> sosiaali- ja terveysala"
      (is (= [koulutusOid6] (search-and-get-oids :sort "name" :order "asc"  :keyword "terveys"))))

    (testing "musiikkioppilaitos <-> musiikkioppilaitokset"
      (is (= [koulutusOid5] (search-and-get-oids :sort "name" :order "asc"  :keyword "musiikkioppilaitos"))))

    (testing "auto <-> automaatiotekniikka/automaatioinsinööri"
      (is (= [koulutusOid12 koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "auto"))))

    (testing "automaatio <-> automaatiotekniikka/automaatioinsinööri, EI autoalan perustutkintoa"
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "automaatio"))))

    (testing "humanismi <-> humanistinen"
      (is (= [koulutusOid2] (search-and-get-oids :sort "name" :order "asc"  :keyword "humanismi"))))

    (testing "lääketiede <-> lääketieteen"
      (is (= [koulutusOid1] (search-and-get-oids :sort "name" :order "asc"  :keyword "lääketiede"))))

    (testing "muusikko <-> muusikon koulutus"
      (is (= [koulutusOid5] (search-and-get-oids :sort "name" :order "asc"  :keyword "muusikko"))))

    (testing "musiikki <-> musiikkioppilaitokset"
      (is (= [koulutusOid5] (search-and-get-oids :sort "name" :order "asc"  :keyword "musiikki"))))

    (testing "musikko <-> muusikko"
      (is (= [koulutusOid5] (search-and-get-oids :sort "name" :order "asc"  :keyword "musiikki"))))

    (testing "insinööri <-> automaatioinsinööri"
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "insinööri"))))

    (testing "insinöö <-> automaatioinsinööri"
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "insinööri"))))

    (testing "tekniikka <-> automaatiotekniikka"
      (is (= [koulutusOid4 koulutusOid15] (search-and-get-oids :sort "name" :order "asc"  :keyword "tekniikka"))))

    (testing "muusikon koulutus <-> EI muita koulutuksia"
      (is (= [koulutusOid5] (search-and-get-oids :sort "name" :order "asc"  :keyword "muusikon%20koulutus"))))

    (testing "Maanmittausalan perustutkinto <-> EI muita perustutkintoja"
      (is (= [koulutusOid7] (search-and-get-oids :sort "name" :order "asc"  :keyword "maanmittausalan%20perustutkinto"))))

    (testing "perustutkinto maanmittaus <-> EI muita perustutkintoja"
      (is (= [koulutusOid7] (search-and-get-oids :sort "name" :order "asc"  :keyword "perustutkinto%20maanmittaus"))))

    (testing "Maanmittaus perus <-> maanmittausalan perustutkinto"
      (is (= [koulutusOid7] (search-and-get-oids :sort "name" :order "asc"  :keyword "maanmittauS%20peruS"))))

    (testing "tietojenkäsittelytiede <-> tietojenkäsittelytieteen"
      (is (= [koulutusOid3] (search-and-get-oids :sort "name" :order "asc"  :keyword "tietojenkäsittelytiede"))))

    (comment testing "automaatiikka <-> automaatioinsinööri"  ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "automaatiikka"))))

    (testing "hius <-> Hius- ja kauneudenhoitoalan perustutkinto"
      (is (= [koulutusOid11] (search-and-get-oids :sort "name" :order "asc"  :keyword "hius"))))

    (testing "kauneudenhoito <-> Hius- ja kauneudenhoitoalan perustutkinto"
      (is (= [koulutusOid11] (search-and-get-oids :sort "name" :order "asc"  :keyword "kauneudenhoito"))))

    (comment testing "hoito <-> Eläintenhoidon ammattitutkinto sekä Hius- ja kauneudenhoitoalan perustutkinto"  ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid14 koulutusOid11] (search-and-get-oids :sort "name" :order "asc"  :keyword "hoito"))))

    (testing "psykologia <-> Psykologi"
      (is (= [koulutusOid2] (search-and-get-oids :sort "name" :order "asc"  :keyword "psykologia"))))

    (testing "lääke <-> lääketieteen"
      (is (= [koulutusOid1] (search-and-get-oids :sort "name" :order "asc"  :keyword "lääke"))))

    (testing "ylemp <-> ylempi (AMK)"
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "ylemp"))))

    (testing "amk <-> ylempi (AMK)"
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "amk"))))

    (testing "psygologia <-> psykologia"
      (is (= [koulutusOid2] (search-and-get-oids :sort "name" :order "asc"  :keyword "psygologia"))))

    (testing "perus <-> kaikki perustutkinnot"
      (is (= [koulutusOid12 koulutusOid13 koulutusOid17 koulutusOid11 koulutusOid7 koulutusOid8 koulutusOid6 koulutusOid16] (search-and-get-oids :sort "name" :order "asc"  :keyword "perus"))))

    (testing "perustutkinto <-> kaikki perustutkinnot"
      (is (= [koulutusOid12 koulutusOid13 koulutusOid17 koulutusOid11 koulutusOid7 koulutusOid8 koulutusOid6 koulutusOid16] (search-and-get-oids :sort "name" :order "asc"  :keyword "perustutkinto"))))

    (comment testing "teknikko <-> tekniikka"  ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid4] (search-and-get-oids :sort "name" :order "asc"  :keyword "teknikko"))))

    (comment testing "tiede <-> lääketiede ja tietojenkäsittelytiede" ;Ei toimi enää, kun on haluttu lisätä haun tarkkuutta
      (is (= [koulutusOid1 koulutusOid3] (search-and-get-oids :sort "name" :order "asc"  :keyword "tiede"))))

    (testing "eläin <-> eläintenhoito EI elintarviketta"
      (is (= [koulutusOid14] (search-and-get-oids :sort "name" :order "asc"  :keyword "eläin"))))

    (testing "eläinten <-> eläintenhoito EI tieto- ja viestintätekniikan"
      (is (= [koulutusOid14] (search-and-get-oids :sort "name" :order "asc"  :keyword "eläinten"))))

    (testing "merimies <-> merimies EI esimiestä"
      (is (= [koulutusOid8] (search-and-get-oids :sort "name" :order "asc"  :keyword "merimies"))))

    (testing "sosiaali <-> ei tanssialaa"
      (is (= [koulutusOid6] (search-and-get-oids :sort "name" :order "asc"  :keyword "sosiaali"))))

    (testing "ensihoitaja <-> ei eläinten- eikä hevostenhoitajaa"
      (is (= [] (search-and-get-oids :sort "name" :order "asc"  :keyword "ensihoitaja"))))

    (testing "seppä <-> seppä"
      (is (= [koulutusOid17] (search-and-get-oids :sort "name" :order "asc"  :keyword "seppä"))))

    (testing "tie <-> lääketiede ja tietojenkäsittelytiede"
      (is (= [koulutusOid1 koulutusOid15 koulutusOid3] (search-and-get-oids :sort "name" :order "asc"  :keyword "tie"))))

    (comment testing "haluan opiskella lääkäriksi <-> lääkäri"
             (is (= [koulutusOid1] (search-and-get-oids :sort "name" :order "asc"  :keyword "haluan%20opiskella%20lääkäriksi"))))

    (comment testing "musiikin opiskelu <-> muusikon koulutus"
             (is (= [koulutusOid5] (search-and-get-oids :sort "name" :order "asc"  :keyword "musiikin%20opiskelu"))))

    (comment testing "haluan opiskella psykologiaa <-> psykologi"
             (is (= [koulutusOid2] (search-and-get-oids :sort "name" :order "asc"  :keyword "haluan%20opiskella%20psykologiaa"))))))
