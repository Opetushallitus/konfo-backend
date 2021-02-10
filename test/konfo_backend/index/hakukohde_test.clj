(ns konfo-backend.index.hakukohde-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn hakukohde-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/hakukohde/" oid) [:draft false]))

(defn hakukohde-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/hakukohde/" oid) [:draft true]))

(deftest hakukohde-test

  (let [hakukohdeOid1 "1.2.246.562.20.000001"
        hakukohdeOid2 "1.2.246.562.20.000002"
        hakukohdeOid3 "1.2.246.562.20.000003"
        hakukohdeOid4 "1.2.246.562.20.000004"
        hakukohdeOid5 "1.2.246.562.20.000005"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"
        valintaperusteId2 "45d2ae02-9a5f-42ef-8148-47d07737927b"]

    (fixture/add-haku-mock "1.2.246.562.29.000001" :tila "julkaistu" :organisaatio mocks/Oppilaitos1)

    (fixture/add-koulutus-mock "1.2.246.562.13.000001" :tila "julkaistu" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos1)

    (fixture/add-hakukohde-mock hakukohdeOid1 "1.2.246.562.17.000001" "1.2.246.562.29.000001" :tila "julkaistu" :esikatselu "false" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid2 "1.2.246.562.17.000001" "1.2.246.562.29.000001" :tila "julkaistu" :esikatselu "false" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId2)
    (fixture/add-hakukohde-mock hakukohdeOid3 "1.2.246.562.17.000001" "1.2.246.562.29.000001" :tila "tallennettu" :esikatselu "false" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid5 "1.2.246.562.17.000001" "1.2.246.562.29.000001" :tila "tallennettu" :esikatselu "true" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId2)

    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :esikatselu "false")
    (fixture/add-valintaperuste-mock valintaperusteId2 :tila "tallennettu" :esikatselu "false")

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000001")
    (fixture/add-koulutus-mock "1.2.246.562.13.000001")
    (fixture/add-haku-mock "1.2.246.562.29.000001")

    (fixture/index-oids-without-related-indices {:hakukohteet [hakukohdeOid1 hakukohdeOid2 hakukohdeOid3 hakukohdeOid5]})

    (testing "Get haku"
      (testing "ok"
        (let [response (get-ok (hakukohde-url hakukohdeOid1))]
          (is (= hakukohdeOid1 (:oid response)))))
      (testing "allowed to get draft hakukohde when tallennettu and esikatselu true"
        (let [response (get-ok (hakukohde-draft-url hakukohdeOid5))]
          (is (= hakukohdeOid5 (:oid response)))))
      (testing "not filter julkaistu valintaperuste"
        (let [response (get-ok (hakukohde-url hakukohdeOid1))]
          (is (= true (contains? response :valintaperuste)))))
      (testing "filter julkaisematon valintaperuste"
        (let [response (get-ok (hakukohde-url hakukohdeOid2))]
          (is (= false (contains? response :valintaperuste) ))))
      (testing "not found"
        (get-not-found (hakukohde-url hakukohdeOid4)))
      (testing "not julkaistu"
        (get-not-found (hakukohde-url hakukohdeOid3))))))
