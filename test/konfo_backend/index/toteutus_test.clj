(ns konfo-backend.index.toteutus-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn toteutus-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/toteutus/" oid) [:draft false]))

(defn toteutus-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/toteutus/" oid) [:draft true]))

(deftest toteutus-test

  (let [hakuOid1      "1.2.246.562.29.000001"
        koulutusOid1  "1.2.246.562.13.000001"
        toteutusOid1  "1.2.246.562.17.000001"
        toteutusOid2  "1.2.246.562.17.000002"
        toteutusOid3  "1.2.246.562.17.000003"
        hakukohdeOid1 "1.2.246.562.20.000001"
        hakukohdeOid2 "1.2.246.562.20.000002"
        hakukohdeOid3 "1.2.246.562.20.000003"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"
        valintaperusteId2 "45d2ae02-9a5f-42ef-8148-47d07737927b"]

    (fixture/add-haku-mock hakuOid1 :tila "julkaistu"   :organisaatio mocks/Oppilaitos1)

    (fixture/add-koulutus-mock koulutusOid1 :tila "julkaistu" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos1)

    (fixture/add-toteutus-mock toteutusOid1 koulutusOid1 :tila "julkaistu"   :nimi "Hauska toteutus"                :esikatselu "false" :organisaatio mocks/Oppilaitos1)
    (fixture/add-toteutus-mock toteutusOid2 koulutusOid1 :tila "tallennettu" :nimi "Hupaisa julkaisematon toteutus" :esikatselu "false" :organisaatio mocks/Oppilaitos2)

    (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid1 hakuOid1 :tila "julkaistu"   :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid1 hakuOid1 :tila "julkaistu"   :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock hakukohdeOid3 toteutusOid1 hakuOid1 :tila "tallennettu" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1] :toteutukset [toteutusOid1 toteutusOid2]})

    (testing "Get toteutus"
      (let [response (get-ok (toteutus-url toteutusOid1))]
        (testing "ok"
          (is (= toteutusOid1 (:oid response)))))
      (testing "not found"
        (get-not-found (toteutus-url toteutusOid3)))
      (testing "filter not julkaistu and draft false"
        (get-not-found (toteutus-url toteutusOid2))))))