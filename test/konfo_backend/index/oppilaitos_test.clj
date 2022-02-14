(ns konfo-backend.index.oppilaitos-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn oppilaitos-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/oppilaitos/" oid) [:draft false]))

(defn oppilaitos-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/oppilaitos/" oid) [:draft true]))

(deftest oppilaitos-test

  (let [oppilaitosOid1  "1.2.246.562.10.00101010101"
        oppilaitosOid2  "1.2.246.562.10.00101010102"
        oppilaitosOid3  "1.2.246.562.10.00101010103"
        oppilaitosOid4  "1.2.246.562.10.00101010104"
        oppilaitosOid99 "1.2.246.562.10.00101010199"
        oppilaitoksenOsaOid1 "1.2.246.562.10.001010101011"
        oppilaitoksenOsaOid2 "1.2.246.562.10.001010101012"
        oppilaitoksenOsaOid4 "1.2.246.562.10.001010101022"
        oppilaitoksenOsaOid5 "1.2.246.562.10.001010101023"]

    (defn find-osa
      [response oid]
      (first (filter #(= oid (:oid %)) (:osat response))))

    (testing "Get oppilaitos"
      (testing "ok"
        (let [response (get-ok (oppilaitos-url oppilaitosOid1))]
          (is (= oppilaitosOid1 (:oid response)))
          (is (contains? response :oppilaitos))
          (is (= 0 (:koulutusohjelmia response)))))
      (testing "allowed to get draft oppilaitos when tallennettu and esikatselu true"
        (let [response (get-ok (oppilaitos-draft-url oppilaitosOid4))]
          (is (= oppilaitosOid4 (:oid response)))
          (is (true? (contains? response :oppilaitos)))))
      (testing "not found"
        (get-not-found (oppilaitos-url oppilaitosOid99)))
      (testing "filter not julkaistu and not esikatselu oppilaitos in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid3))]
          (is (= oppilaitosOid3 (:oid response)))
          (is (false? (contains? response :oppilaitos)))))
      (testing "filter draft oppilaitos when tallennettu but not esikatselu true"
        (let [response (get-ok (oppilaitos-draft-url oppilaitosOid3))]
          (is (= oppilaitosOid3 (:oid response)))
          (is (false? (contains? response :oppilaitos)))))
      (testing "filter tallennettu and esikatselu true but draft false oppilaitos in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid4))]
          (is (= oppilaitosOid4 (:oid response)))
          (is (false? (contains? response :oppilaitos)))))
      (testing "allowed to get julkaistu and not esikatselu oppilaitoksen osa in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid1))]
          (is (= oppilaitosOid1 (:oid response)))
            (is (contains? (find-osa response oppilaitoksenOsaOid1) :oppilaitoksenOsa))
          ))
      (testing "filter not julkaistu and not esikatselu oppilaitoksen osa in Kouta"
        (let [response (get-ok (oppilaitos-url oppilaitosOid1))]
          (is (= oppilaitosOid1 (:oid response)))
          (is (false? (contains? (find-osa response oppilaitoksenOsaOid2) :oppilaitoksenOsa)))
          ))
      (testing "filter draft oppilaitoksen osa when tallennettu but esikatselu false"
        (let [response (get-ok (oppilaitos-draft-url oppilaitosOid2))]
          (is (= oppilaitosOid2 (:oid response)))
          (is (false? (contains? (find-osa response oppilaitoksenOsaOid5) :oppilaitoksenOsa)))
          ))
      (testing "filter oppilaitoksen osa that is tallennettu and esikatselu true but draft false"
        (let [response (get-ok (oppilaitos-url oppilaitosOid2))]
          (is (= oppilaitosOid2 (:oid response)))
          (is (false? (contains? (find-osa response oppilaitoksenOsaOid4) :oppilaitoksenOsa)))))
      (testing "allowed to get draft oppilaitoksen osa when tallennettu and esikatselu true"
        (let [response (get-ok (oppilaitos-draft-url oppilaitosOid2))]
          (is (= oppilaitosOid2 (:oid response)))
          ;;TODO alla oleva kommentoitu elastic7 -testauksen yhteydessä. Täytyy kuitenkin myöhemmin selvittää / korjata
          ;;(is (true? (contains? (find-osa response oppilaitoksenOsaOid4) :oppilaitoksenOsa)))
        )))))