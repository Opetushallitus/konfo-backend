(ns konfo-backend.index.haku-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(defn haku-url
  [oid]
  (str "/konfo-backend/haku/" oid))

(use-fixtures :once with-elastic-dump)

(deftest haku-test

  (let [hakuOid1      "1.2.246.562.29.0000001"
        hakuOid5      "1.2.246.562.29.0000005"
        hakuOid99     "1.2.246.562.29.0000099"]

    (testing "Get haku"
      (testing "ok"
        (let [response (get-ok (haku-url hakuOid1))]
          (is (= hakuOid1 (:oid response)))))
      (testing "filter julkaisemattomat hakukohteet"
        (let [response (get-ok (haku-url hakuOid1))]
          (is (= 5 (count (:hakukohteet response))))))
      (testing "not found"
        (get-not-found (haku-url hakuOid99)))
      (testing "not julkaistu"
        (get-not-found (haku-url hakuOid5))))))