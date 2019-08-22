(ns konfo-backend.index.valintaperuste-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn valintaperuste-url
  [id]
  (str "/konfo-backend/valintaperuste/" id))

(deftest valintaperuste-test

  (let [sorakuvausId      "2ff6700d-087f-4dbf-9e42-7f38948f227a"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"
        valintaperusteId2 "45d2ae02-9a5f-42ef-8148-47d07737927b"
        valintaperusteId3 "45d2ae02-9a5f-42ef-8148-47d077379299"]

    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu")
    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :sorakuvaus sorakuvausId)
    (fixture/add-valintaperuste-mock valintaperusteId2 :tila "tallennettu" :sorakuvaus sorakuvausId)

    (fixture/index-oids-without-related-indices {:valintaperusteet [valintaperusteId1 valintaperusteId2]})

    (testing "Get valintaperuste"
      (testing "ok"
        (let [response (get-ok (valintaperuste-url valintaperusteId1))]
          (is (= valintaperusteId1 (:id response)))))
      (testing "not found"
        (get-not-found (valintaperuste-url valintaperusteId3)))
      (testing "not julkaistu"
        (get-not-found (valintaperuste-url valintaperusteId2))))))