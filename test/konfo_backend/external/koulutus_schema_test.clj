(ns konfo-backend.external.koulutus-schema-test
  (:require [clojure.test :refer :all]
            [konfo-backend.external.schema.koulutus :as k]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn koulutus-url
  [oid]
  (str "/konfo-backend/external/koulutus/" oid))

(defn toteutus-url
  [oid]
  (str "/konfo-backend/external/toteutus/" oid))

(defn hakukohde-url
  [oid]
  (str "/konfo-backend/external/hakukohde/" oid))

(deftest koulutus-schema-test
  (testing "Testing external koulutus api"
    (let [koulutusOid1 "1.2.246.562.13.000001"
          koulutusOid2 "1.2.246.562.13.000002"
          koulutusOid3 "1.2.246.562.13.000003"
          sorakuvausId      "2ff6700d-087f-4dbf-9e42-7f38948f227a"
          valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"]

      (fixture/add-koulutus-mock koulutusOid1 :tila "julkaistu" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos1)
      (fixture/add-koulutus-mock koulutusOid2 :tila "tallennettu" :nimi "Hupaisa julkaisematon koulutus" :organisaatio mocks/Oppilaitos2)

      (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu")
      (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid1 :tila "tallennettu")
      (fixture/add-toteutus-mock "1.2.246.562.17.000003" koulutusOid1 :tila "julkaistu")

      (fixture/add-haku-mock "1.2.246.562.29.000001" :tila "julkaistu")

      (fixture/add-hakukohde-mock "1.2.246.562.20.000001" "1.2.246.562.17.000001" "1.2.246.562.29.000001" :tila "julkaistu" :valintaperuste valintaperusteId1)

      (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu")
      (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :sorakuvaus sorakuvausId)

      (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2]
                                                   :toteutukset ["1.2.246.562.17.000001"]
                                                   :haut ["1.2.246.562.29.000001"]
                                                   :hakukohteet ["1.2.246.562.20.000001"]
                                                   :valintaperusteet ["fe39c85a-05de-4f92-9306-9844c5841664"]})

      (testing "Get koulutus"
        (testing "ok"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1))]
            (is (= koulutusOid1 (:oid response)))))
        (comment testing "filter julkaisemattomat toteutukset"
                 (let [response (get-ok (koulutus-url koulutusOid1))]
                   (is (= 2 (count (:toteutukset response))))))
        (testing "not found"
                 (get-not-found (koulutus-url koulutusOid3)))
        (testing "not julkaistu"
                 (get-not-found (koulutus-url koulutusOid2))))

      (testing "Get toteutus"
        (testing "ok"
          (let [response (get-ok-or-print-schema-error (toteutus-url "1.2.246.562.17.000001"))]
            (is (= "1.2.246.562.17.000001" (:oid response)))
            )
          )
        )

      (testing "Get hakukohde"
        (testing "ok"
          (let [response (get-ok-or-print-schema-error (hakukohde-url "1.2.246.562.20.000001"))]
            (is (= "1.2.246.562.20.000001" (:oid response)))
            )
          )
        )



      )))