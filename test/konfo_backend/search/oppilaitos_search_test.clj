(ns konfo-backend.search.oppilaitos-search-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn oppilaitos-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/oppilaitokset" query-params))

(defn search
  [& query-params]
  (get-ok (apply oppilaitos-search-url query-params)))

(defn create-koulutus-metatieto
  []
  (cheshire/generate-string
    {:tyyppi "amm",
     :koulutusalaKoodiUrit[ "kansallinenkoulutusluokitus2016koulutusalataso1_054#1",
                           "kansallinenkoulutusluokitus2016koulutusalataso1_055#1"]}))

(defn create-toteutus-metatieto
  []
  (cheshire/generate-string
    {:tyyppi "amm"
     :asiasanat [{:kieli "fi" :arvo "hevonen"}]
     :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]}))

(deftest oppilaitos-search-test
  (let [punkaharjun-yliopisto "1.2.246.562.10.000002"
        punkaharjun-toimipiste-1 "1.2.246.562.10.000003"
        punkaharjun-toimipiste-2 "1.2.246.562.10.000004"]

    (defn punkaharju-org
      [x & {:as params}]
      (mocks/create-organisaatio-hierarkia
        {:oid "1.2.246.562.10.000001"
         :nimi {:fi "Punkaharjun kunta" :sv "Punkaharjun kunta sv"}
         :kotipaikka "kunta_618"
         :kielet [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]}
        {:oid punkaharjun-yliopisto
         :nimi {:fi "Punkaharjun yliopisto" :sv "Punkaharjun yliopisto sv"}
         :kotipaikka "kunta_618"
         :kielet [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]}
        [{:oid punkaharjun-toimipiste-1
          :nimi {:fi "Punkaharjun yliopiston toimipiste" :sv "Punkaharjun yliopiston toimipiste sv "}
          :kotipaikka "kunta_618"
          :kielet [ "oppilaitoksenopetuskieli_2#1" ]},
         {:oid punkaharjun-toimipiste-2
          :nimi {:fi "Punkaharjun yliopiston Karjaan toimipiste" :sv "Punkaharjun yliopiston Karjaan toimipiste sv "}
          :kotipaikka "kunta_220"
          :kielet [ "oppilaitoksenopetuskieli_1#1"]}]))

    (fixture/add-koulutus-mock "1.2.246.562.13.000001" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata (create-koulutus-metatieto))
    (fixture/add-koulutus-mock "1.2.246.562.13.000002" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat punkaharjun-yliopisto :metadata (create-koulutus-metatieto))
    (fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000002" :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata (create-toteutus-metatieto))

    (fixture/index-oids-without-related-indices {:koulutukset ["1.2.246.562.13.000001" "1.2.246.562.13.000002"] :oppilaitokset [punkaharjun-yliopisto]} punkaharju-org)

    (testing "Search oppilaitokset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_618")]
          (is (= 1 (count (:hits r))))
          (is (= 1 (get-in r [:filters :sijainti :maakunta_01]))))))))