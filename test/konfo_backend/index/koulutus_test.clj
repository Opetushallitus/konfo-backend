(ns konfo-backend.index.koulutus-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn koulutus-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/koulutus/" oid) [:draft false]))

(defn koulutus-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/koulutus/" oid) [:draft true]))

(deftest koulutus-test

  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid5 "1.2.246.562.13.000005"
        koulutusOid6 "1.2.246.562.13.000006"
        koulutusOid99 "1.2.246.562.13.000099"]

    (testing "Get koulutus"
      (testing "ok"
        (let [response (get-ok (koulutus-url koulutusOid1))]
          (is (= koulutusOid1 (:oid response)))))
      (testing "allowed to get tallennettu when esikatselu true"
        (let [response (get-ok (koulutus-draft-url koulutusOid5))]
          (is (= koulutusOid5 (:oid response)))))
      (testing "filter julkaisemattomat toteutukset"
        (let [response (get-ok (koulutus-url koulutusOid3))]
          (is (= 3 (count (:toteutukset response))))))
      (testing "not found"
        (get-not-found (koulutus-url koulutusOid99)))
      (testing "filter not julkaistu draft when esikatselu false"
        (get-not-found (koulutus-draft-url koulutusOid6))))))