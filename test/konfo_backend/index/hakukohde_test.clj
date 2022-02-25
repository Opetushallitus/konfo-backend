(ns konfo-backend.index.hakukohde-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn hakukohde-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/hakukohde/" oid) [:draft false]))

(defn hakukohde-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/hakukohde/" oid) [:draft true]))

(deftest hakukohde-test
  (let [hakukohdeOid1  "1.2.246.562.20.0000001"
        hakukohdeOid2  "1.2.246.562.20.0000002"
        hakukohdeOid5  "1.2.246.562.20.0000005"
        hakukohdeOid6  "1.2.246.562.20.0000006"
        hakukohdeOid99 "1.2.246.562.20.0000099"]

    (testing "Get hakukohde"
      (testing "ok"
        (let [response (get-ok (hakukohde-url hakukohdeOid1))]
          (is (= hakukohdeOid1 (:oid response)))))
      (testing "allowed to get draft hakukohde when tallennettu and esikatselu true"
        (let [response (get-ok (hakukohde-draft-url hakukohdeOid5))]
          (is (= hakukohdeOid5 (:oid response)))))
      (testing "not allowed to get draft hakukohde when tallennettu and esikatselu false"
        (get-not-found (hakukohde-draft-url hakukohdeOid6)))
      (testing "not allowd to get hakukohde when tallennettu and esikatselu true"
        (get-not-found (hakukohde-url hakukohdeOid5)))
      (testing "not found hakukohde"
        (get-not-found (hakukohde-url hakukohdeOid99)))
      (testing "get julkaistu valintaperuste"
        (let [response (get-ok (hakukohde-url hakukohdeOid1))]
          (is (= true (contains? response :valintaperuste)))))
      (testing "filter julkaisematon valintaperuste when draft false"
        (let [response (get-ok (hakukohde-url hakukohdeOid2))]
          (is (= false (contains? response :valintaperuste)))))
      (testing "get draft valintaperuste when tallennettu and esikatselu true"
        (let [response (get-ok (hakukohde-draft-url hakukohdeOid5))]
          (is (= true (contains? response :valintaperuste))))))))
