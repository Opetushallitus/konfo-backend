(ns konfo-backend.index.toteutus-test
  (:require [clojure.test :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :as utils]
            [konfo-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each utils/mock-embedded-elasticsearch-fixture fixture/mock-indexing-fixture)

(defn toteutus-url
  [oid]
  (str "/konfo-backend/toteutus/" oid))

(deftest toteutus-test

  (let [toteutusOid1  "1.2.246.562.17.000001"
        toteutusOid2  "1.2.246.562.17.000002"
        toteutusOid3  "1.2.246.562.17.000003"
        hakukohdeOid1 "1.2.246.562.20.000001"
        hakukohdeOid2 "1.2.246.562.20.000002"
        hakukohdeOid3 "1.2.246.562.20.000003"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"
        valintaperusteId2 "45d2ae02-9a5f-42ef-8148-47d07737927b"]

    (fixture/add-toteutus-mock toteutusOid1 "1.2.246.562.13.000001" :tila "julkaistu"   :nimi "Hauska toteutus"                :organisaatio mocks/Oppilaitos1)
    (fixture/add-toteutus-mock toteutusOid2 "1.2.246.562.13.000001" :tila "tallennettu" :nimi "Hupaisa julkaisematon toteutus" :organisaatio mocks/Oppilaitos2)

    (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid1 "1.2.246.562.29.000001" :tila "julkaistu"   :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid1 "1.2.246.562.29.000001" :tila "julkaistu"   :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock hakukohdeOid3 toteutusOid1 "1.2.246.562.29.000001" :tila "tallennettu" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)

    (fixture/index-oids-without-related-indices {:toteutukset [toteutusOid1 toteutusOid2]})

    (testing "Get toteutus"
      (testing "ok"
        (let [response (get-ok (toteutus-url toteutusOid1))]
          (is (= toteutusOid1 (:oid response)))))
      (testing "filter julkaisemattomat hakukohteet"
        (let [response (get-ok (toteutus-url toteutusOid1))]
          (is (= 2 (count (:hakukohteet response))))))
      (testing "not found"
        (get-not-found (toteutus-url toteutusOid3)))
      (testing "not julkaistu"
        (get-not-found (toteutus-url toteutusOid2))))))