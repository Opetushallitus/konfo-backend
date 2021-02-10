(ns konfo-backend.index.oppilaitos-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn oppilaitos-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/oppilaitos/" oid) [:draft false]))

(defn oppilaitos-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/oppilaitos/" oid) [:draft true]))

(deftest oppilaitos-test

  (let [oppilaitosOid1 "1.2.246.562.10.00101010101"
        oppilaitosOid2 "1.2.246.562.10.00101010102"
        oppilaitosOid3 "1.2.246.562.10.00101010103"
        oppilaitosOid4 "1.2.246.562.10.00101010104"
        oppilaitoksenOsaOid1 "1.2.246.562.10.001010101011"
        oppilaitoksenOsaOid2 "1.2.246.562.10.001010101012"
        oppilaitoksenOsaOid3 "1.2.246.562.10.001010101021"
        oppilaitoksenOsaOid4 "1.2.246.562.10.001010101022"]

    (fixture/add-oppilaitos-mock oppilaitosOid1 :tila "julkaistu" :esikatselu "false" :organisaatio oppilaitosOid1)
    (fixture/add-oppilaitos-mock oppilaitosOid2 :tila "tallennettu" :esikatselu "false" :organisaatio oppilaitosOid2)
    (fixture/add-oppilaitos-mock oppilaitosOid4 :tila "tallennettu" :esikatselu "true" :organisaatio oppilaitosOid4)

    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid1 oppilaitosOid1 :tila "julkaistu" :esikatselu "false" :organisaatio oppilaitoksenOsaOid1)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid2 oppilaitosOid1 :tila "arkistoitu" :esikatselu "false" :organisaatio oppilaitoksenOsaOid2)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid3 oppilaitosOid2 :tila "julkaistu" :esikatselu "false" :organisaatio oppilaitoksenOsaOid3)
    (fixture/add-oppilaitoksen-osa-mock oppilaitoksenOsaOid4 oppilaitosOid2 :tila "tallennettu" :esikatselu "false" :organisaatio oppilaitoksenOsaOid4)

    (fixture/index-oppilaitokset [oppilaitosOid1 oppilaitosOid2 oppilaitosOid4])

    (defn find-osa
      [response oid]
      (first (filter #(= oid (:oid %)) (:osat response))))

    (testing "Get oppilaitos"
      (testing "ok"
        (let [response (get-ok (oppilaitos-url oppilaitosOid1))]
          (is (= oppilaitosOid1 (:oid response)))
          (is (= 0 (:koulutusohjelmia response)))))
      (testing "allowed to get oppilaitos when tallennettu and esikatselu true"
        (let [response (get-ok (oppilaitos-draft-url oppilaitosOid4))]
          (is (= oppilaitosOid4 (:oid response)))
          (is (true? (contains? response :oppilaitos)))))
      (testing "not found"
        (get-not-found (oppilaitos-url oppilaitosOid3)))
      (testing "filter not julkaistu and not esikatselu oppilaitos in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid2))]
          (is (= oppilaitosOid2 (:oid response)))
          (is (false? (contains? response :oppilaitos)))
          (is (contains? (find-osa response oppilaitoksenOsaOid3) :oppilaitoksenOsa))
          (is (false? (contains? (find-osa response oppilaitoksenOsaOid4) :oppilaitoksenOsa)))))
      (testing "filter tallennettu but not esikatselu true oppilaitos in Kouta"
        (let [response (get-ok (oppilaitos-draft-url oppilaitosOid2))]
          (is (= oppilaitosOid2 (:oid response)))
          (is (false? (contains? response :oppilaitos)))))
      (testing "filter tallennettu and esikatselu true but draft false oppilaitos in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid4))]
          (is (= oppilaitosOid4 (:oid response)))
          (is (false? (contains? response :oppilaitos)))))
      (testing "filter not julkaistu and not esikatselu oppilaitoksen osa in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid1))]
          (is (= oppilaitosOid1 (:oid response)))
          (is (contains? response :oppilaitos))
          (is (contains? (find-osa response oppilaitoksenOsaOid1) :oppilaitoksenOsa))
          (is (false? (contains? (find-osa response oppilaitoksenOsaOid2) :oppilaitoksenOsa))))))))