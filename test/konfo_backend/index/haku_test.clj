(ns konfo-backend.index.haku-test
  (:require [clojure.test :refer :all]
            [konfo-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn haku-url
  [oid]
  (str "/konfo-backend/haku/" oid))

(deftest haku-test

  (let [hakuOid1      "1.2.246.562.29.000001"
        hakuOid2      "1.2.246.562.29.000002"
        hakuOid3      "1.2.246.562.29.000003"
        hakukohdeOid1 "1.2.246.562.20.000001"
        hakukohdeOid2 "1.2.246.562.20.000002"
        hakukohdeOid3 "1.2.246.562.20.000003"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"
        valintaperusteId2 "45d2ae02-9a5f-42ef-8148-47d07737927b"]

    (fixture/add-haku-mock hakuOid1 :tila "julkaistu"   :organisaatio mocks/Oppilaitos1)
    (fixture/add-haku-mock hakuOid2 :tila "tallennettu" :organisaatio mocks/Oppilaitos1)

    (fixture/add-hakukohde-mock hakukohdeOid1 "1.2.246.562.17.000001" hakuOid1 :tila "julkaistu"   :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid2 "1.2.246.562.17.000001" hakuOid1 :tila "julkaistu"   :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock hakukohdeOid3 "1.2.246.562.17.000001" hakuOid1 :tila "tallennettu" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)

    (fixture/index-oids-without-related-indices {:haut [hakuOid1 hakuOid2]})

    (testing "Get haku"
      (testing "ok"
        (let [response (get-ok (haku-url hakuOid1))]
          (is (= hakuOid1 (:oid response)))))
      (testing "filter julkaisemattomat hakukohteet"
        (let [response (get-ok (haku-url hakuOid1))]
          (is (= 2 (count (:hakukohteet response))))))
      (testing "not found"
        (get-not-found (haku-url hakuOid3)))
      (testing "not julkaistu"
        (get-not-found (haku-url hakuOid2))))))