(ns konfo-backend.external.external-api-test
  (:require [clojure.test :refer :all]
            [konfo-backend.external.schema.koulutus :as k]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.search-test-tools :refer [yo-koulutus-metatieto yo-toteutus-metatieto]]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn koulutus-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/external/koulutus/" oid) query-params))

(defn toteutus-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/external/toteutus/" oid) query-params))

(defn hakukohde-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/external/hakukohde/" oid) query-params))

(defn haku-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/external/haku/" oid) query-params))

(deftest external-api-test
  (testing "Testing external apis"
    (let [koulutusOid1   "1.2.246.562.13.000001"
          koulutusOid2   "1.2.246.562.13.000002"
          kkKoulutusOid  "1.2.246.562.13.000099"
          toteutusOid1   "1.2.246.562.17.000001"
          toteutusOid2   "1.2.246.562.17.000002"
          toteutusOid3   "1.2.246.562.17.000003"
          kkToteutusOid  "1.2.246.562.17.000099"
          hakukohdeOid1  "1.2.246.562.20.000001"
          hakukohdeOid2  "1.2.246.562.20.000002"
          kkHakukohdeOid "1.2.246.562.20.000099"
          hakuOid1       "1.2.246.562.29.000001"
          hakuOid2       "1.2.246.562.29.000002"
          kkHakuOid      "1.2.246.562.29.000099"
          kkSorakuvausId     "2ff6700d-087f-4dbf-9e42-7f38948f227a"
          sorakuvausId       "a5e88367-555b-4d9e-aa43-0904e5ea0a13"
          valintaperusteId1  "2d0651b7-cdd3-463b-80d9-303a60d9616c"
          valintaperusteId2  "45d2ae02-9a5f-42ef-8148-47d07737927b"
          kkValintaperusteId "ffa8c6cf-a962-4bb2-bf61-fe8fc741fabd"]

      (fixture/add-koulutus-mock koulutusOid1 :tila "julkaistu" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos1)
      (fixture/add-koulutus-mock koulutusOid2 :tila "tallennettu" :nimi "Hupaisa julkaisematon koulutus" :organisaatio mocks/Oppilaitos2)
      (fixture/add-koulutus-mock kkKoulutusOid :tila "julkaistu" :koulutustyyppi "yo" :nimi "YO koulutus" :organisaatio mocks/Oppilaitos2 :metadata yo-koulutus-metatieto)

      (fixture/add-toteutus-mock toteutusOid1 koulutusOid1 :tila "julkaistu")
      (fixture/add-toteutus-mock toteutusOid2 koulutusOid1 :tila "tallennettu")
      (fixture/add-toteutus-mock toteutusOid3 koulutusOid1 :tila "julkaistu")
      (fixture/add-toteutus-mock kkToteutusOid kkKoulutusOid :tila "julkaistu" :metadata (slurp "test/resources/toteutus-metadata.json"))

      (fixture/add-haku-mock hakuOid1 :tila "julkaistu")
      (fixture/add-haku-mock hakuOid2 :tila "tallennettu")
      (fixture/add-haku-mock kkHakuOid :tila "julkaistu")

      (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid3 hakuOid1 :tila "julkaistu" :valintaperuste valintaperusteId1)
      (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid3 hakuOid2 :tila "tallennettu" :valintaperuste valintaperusteId2)
      (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid3 hakuOid2 :tila "tallennettu" :valintaperuste valintaperusteId2)
      (fixture/add-hakukohde-mock kkHakukohdeOid kkToteutusOid kkHakuOid :tila "julkaistu" :valintaperuste valintaperusteId2)

      (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu")
      ;(fixture/add-sorakuvaus-mock kkSorakuvausId :tila "julkaistu" :koulutustyyppi "yo" :metadata "{}")
      (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :sorakuvaus sorakuvausId)
      (fixture/add-valintaperuste-mock valintaperusteId2 :tila "tallennettu" :sorakuvaus sorakuvausId)
      ;(fixture/add-valintaperuste-mock kkValintaperusteId :tila "julkaistu" :koulutustyyppi "yo" :sorakuvaus kkSorakuvausId :metadata "{}")

      (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 kkKoulutusOid]
                                                   :toteutukset [toteutusOid1 toteutusOid2 toteutusOid3 kkToteutusOid]
                                                   :haut [hakuOid1 hakuOid2 kkHakuOid]
                                                   :hakukohteet [hakukohdeOid1 hakukohdeOid2 kkHakukohdeOid]
                                                   :valintaperusteet [valintaperusteId1 valintaperusteId2]})

      (testing "Get koulutus"
        (testing "ok only koulutus"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1))]
            (is (= koulutusOid1 (:oid response)))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok koulutus with toteutukset"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1 :toteutukset true))]
            (is (= koulutusOid1 (:oid response)))
            (is (= [toteutusOid1 toteutusOid3] (vec (sort (map :oid (:toteutukset response))))))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok koulutus with hakukohteet"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1 :hakukohteet true))]
            (is (= koulutusOid1 (:oid response)))
            (is (= [hakukohdeOid1] (vec (sort (map :oid (:hakukohteet response))))))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :haut)))))
        (testing "ok koulutus with haut"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1 :haut true))]
            (is (= koulutusOid1 (:oid response)))
            (is (= [hakuOid1] (vec (sort (map :oid (:haut response))))))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :toteutukset)))))
        (testing "not found"
          (get-not-found (koulutus-url "1.2.246.562.13.000009")))
        (testing "not julkaistu"
          (get-not-found (koulutus-url koulutusOid2)))
        (testing "not amm"
          (get-not-found (koulutus-url kkKoulutusOid))))

      (testing "Get toteutus"
        (testing "ok only toteutus"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid3))]
            (is (= toteutusOid3 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok toteutus with koulutus"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid3 :koulutus true))]
            (is (= toteutusOid3 (:oid response)))
            (is (= koulutusOid1 (get-in response [:koulutus :oid])))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok toteutus with hakukohteet"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid3 :hakukohteet true))]
            (is (= toteutusOid3 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (= [hakukohdeOid1] (vec (sort (map :oid (:hakukohteet response))))))
            (is (false? (contains? response :haut)))))
        (testing "ok toteutus with haut"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid3 :haut true))]
            (is (= toteutusOid3 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :hakukohteet)))
            (is (= [hakuOid1] (vec (sort (map :oid (:haut response))))))))
        (testing "not found"
          (get-not-found (toteutus-url "1.2.246.562.17.000009")))
        (testing "not julkaistu"
          (get-not-found (toteutus-url toteutusOid2)))
        (testing "not amm"
          (get-not-found (toteutus-url kkToteutusOid))))

      (testing "Get hakukohde"
        (testing "ok only hakukohde"
          (let [response (get-ok-or-print-schema-error (hakukohde-url hakukohdeOid1))]
            (is (= hakukohdeOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :toteutus)))
            (is (false? (contains? response :haku)))
            (is (false? (contains? response :valintaperustekuvaus)))))
        (testing "ok hakukohde with koulutus"
          (let [response (get-ok-or-print-schema-error (hakukohde-url hakukohdeOid1 :koulutus true))]
            (is (= hakukohdeOid1 (:oid response)))
            (is (= koulutusOid1 (get-in response [:koulutus :oid])))
            (is (false? (contains? response :toteutus)))
            (is (false? (contains? response :haku)))
            (is (false? (contains? response :valintaperustekuvaus)))))
        (testing "ok hakukohde with toteutus"
          (let [response (get-ok-or-print-schema-error (hakukohde-url hakukohdeOid1 :toteutus true))]
            (is (= hakukohdeOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (= toteutusOid3 (get-in response [:toteutus :oid])))
            (is (false? (contains? response :haku)))
            (is (false? (contains? response :valintaperustekuvaus)))))
        (testing "ok hakukohde with haku"
          (let [response (get-ok-or-print-schema-error (hakukohde-url hakukohdeOid1 :haku true))]
            (is (= hakukohdeOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :toteutus)))
            (is (= hakuOid1 (get-in response [:haku :oid])))
            (is (false? (contains? response :valintaperustekuvaus)))))
        (testing "ok hakukohde with valintaperustekuvaus"
          (let [response (get-ok-or-print-schema-error (hakukohde-url hakukohdeOid1 :valintaperustekuvaus true))]
            (is (= hakukohdeOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :toteutus)))
            (is (false? (contains? response :haku)))
            (is (= valintaperusteId1 (get-in response [:valintaperustekuvaus :id])))))
        (testing "not found"
          (get-not-found (hakukohde-url "1.2.246.562.20.000009")))
        (testing "not julkaistu"
          (get-not-found (hakukohde-url hakukohdeOid2)))
        (testing "not amm"
          (let [response (get-ok-or-print-schema-error (hakukohde-url kkHakukohdeOid :koulutus true :toteutus true :haku true :valintaperustekuvaus true))]
            (is (= kkHakukohdeOid (:oid response)))
            (is (nil? (get-in response [:koulutus :oid])))
            (is (nil? (get-in response [:toteutus :oid])))
            (is (= kkHakuOid (get-in response [:haku :oid])))
            (is (nil? (get-in response [:valintaperustekuvaus :id]))))))

      (testing "Get haku"
        (testing "ok only haku"
          (let [response (get-ok-or-print-schema-error (haku-url hakuOid1))]
            (is (= hakuOid1 (:oid response)))
            (is (false? (contains? response :koulutukset)))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :hakukohteet)))))
        (testing "ok haku with koulutukset"
          (let [response (get-ok-or-print-schema-error (haku-url hakuOid1 :koulutukset true))]
            (is (= hakuOid1 (:oid response)))
            (is (= [koulutusOid1] (vec (sort (map :oid (:koulutukset response))))))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :hakukohteet)))))
        (testing "ok haku with toteutukset"
          (let [response (get-ok-or-print-schema-error (haku-url hakuOid1 :toteutukset true))]
            (is (= hakuOid1 (:oid response)))
            (is (false? (contains? response :koulutukset)))
            (is (= [toteutusOid3] (vec (sort (map :oid (:toteutukset response))))))
            (is (false? (contains? response :hakukohteet)))))
        (testing "ok haku with hakukohteet"
          (let [response (get-ok-or-print-schema-error (haku-url hakuOid1 :hakukohteet true))]
            (is (= hakuOid1 (:oid response)))
            (is (false? (contains? response :koulutukset)))
            (is (false? (contains? response :toteutukset)))
            (is (= [hakukohdeOid1] (vec (sort (map :oid (:hakukohteet response))))))))
        (testing "not found"
          (get-not-found (haku-url "1.2.246.562.29.000009")))
        (testing "not julkaistu"
          (get-not-found (haku-url hakuOid2)))
        (testing "not amm"
          (let [response (get-ok-or-print-schema-error (haku-url kkHakuOid :toteutukset true :koulutukset true :hakukohteet true))]
            (is (= kkHakuOid (:oid response)))
            (is (= [] (response :koulutukset)))
            (is (= [] (response :toteutukset)))
            (is (= [kkHakukohdeOid] (vec (sort (map :oid (:hakukohteet response))))))))))))