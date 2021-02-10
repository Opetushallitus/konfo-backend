(ns konfo-backend.index.valintaperuste-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn valintaperuste-url
  [id]
  (apply url-with-query-params (str "/konfo-backend/valintaperuste/" id) [:draft false]))

(defn valintaperuste-draft-url
  [id]
  (apply url-with-query-params (str "/konfo-backend/valintaperuste/" id) [:draft true]))

(deftest valintaperuste-test

  (let [sorakuvausId      "2ff6700d-087f-4dbf-9e42-7f38948f227a"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"
        valintaperusteId2 "45d2ae02-9a5f-42ef-8148-47d07737927b"
        valintaperusteId3 "45d2ae02-9a5f-42ef-8148-47d077379299"
        valintaperusteId4 "45d2ae02-9a5f-42ef-8148-47d077379777"]

    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu")
    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :esikatselu "false" :sorakuvaus sorakuvausId)
    (fixture/add-valintaperuste-mock valintaperusteId2 :tila "tallennettu" :esikatselu "false" :sorakuvaus sorakuvausId)
    (fixture/add-valintaperuste-mock valintaperusteId4 :tila "tallennettu" :esikatselu "true" :sorakuvaus sorakuvausId)

    (fixture/index-oids-without-related-indices {:valintaperusteet [valintaperusteId1 valintaperusteId2 valintaperusteId4]})

    (testing "Get valintaperuste"
      (testing "ok"
        (let [response (get-ok (valintaperuste-url valintaperusteId1))]
          (is (= valintaperusteId1 (:id response)))))
      (testing "get draft valintaperuste when esikatselu true"
        (let [response (get-ok (valintaperuste-draft-url valintaperusteId4))]
          (is (= valintaperusteId4 (:id response)))))
      (testing "not found"
        (get-not-found (valintaperuste-url valintaperusteId3)))
      (testing "filter not julkaistu and draft false"
        (get-not-found (valintaperuste-url valintaperusteId2))))))