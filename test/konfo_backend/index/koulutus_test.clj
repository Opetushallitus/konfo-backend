(ns konfo-backend.index.koulutus-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn koulutus-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/koulutus/" oid) [:draft false]))

(defn koulutus-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/koulutus/" oid) [:draft true]))

(deftest koulutus-test

  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"
        sorakuvausId "2ff6700d-087f-4dbf-9e42-7f38948f227a"]

    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu")
    (fixture/add-koulutus-mock koulutusOid1 :tila "julkaistu" :esikatselu "false" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos1 :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid2 :tila "tallennettu" :esikatselu "false" :nimi "Hupaisa julkaisematon koulutus" :organisaatio mocks/Oppilaitos2 :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid4 :tila "tallennettu" :esikatselu "true" :nimi "Esikatselu sallittu" :organisaatio mocks/Oppilaitos2 :sorakuvausId sorakuvausId)

    (fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid1 :tila "julkaistu")
    (fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid1 :tila "tallennettu")
    (fixture/add-toteutus-mock "1.2.246.562.17.000003" koulutusOid1 :tila "julkaistu")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid4]})

    (testing "Get koulutus"
      (testing "ok"
        (let [response (get-ok (koulutus-url koulutusOid1))]
          (is (= koulutusOid1 (:oid response)))))
      (testing "allowed to get tallennettu when esikatselu true"
        (let [response (get-ok (koulutus-draft-url koulutusOid4))]
          (is (= koulutusOid4 (:oid response)))))
      (testing "filter julkaisemattomat toteutukset"
        (let [response (get-ok (koulutus-url koulutusOid1))]
          (is (= 2 (count (:toteutukset response))))))
      (testing "not found"
        (get-not-found (koulutus-url koulutusOid3)))
      (testing "filter not julkaistu draft when esikatselu false"
        (get-not-found (koulutus-draft-url koulutusOid2))))))