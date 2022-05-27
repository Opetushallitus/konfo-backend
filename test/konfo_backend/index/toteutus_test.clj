(ns konfo-backend.index.toteutus-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn toteutus-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/toteutus/" oid) [:draft false]))

(defn toteutus-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/toteutus/" oid) [:draft true]))


(deftest toteutus-test

  (let [toteutusOid1  "1.2.246.562.17.000001"
        toteutusOid4  "1.2.246.562.17.000004"
        toteutusOid5  "1.2.246.562.17.000005"
        toteutusOid6  "1.2.246.562.17.000006"
        toteutusOid99 "1.2.246.562.17.000099"
        hakukohdeOid5 "1.2.246.562.20.0000005"
        hakukohdeOid6 "1.2.246.562.20.0000006"
        hakukohdeOid7 "1.2.246.562.20.0000007"]

    (testing "Get toteutus"
      (testing "ok"
        (let [response (get-ok (toteutus-url toteutusOid1))]
          (is (= toteutusOid1 (:oid response)))
          (is (not-any? (fn [hakutieto] (some #(= hakukohdeOid7 (:hakukohdeOid %)) (:hakukohteet hakutieto))) (:hakutiedot response)))))
      (testing "get draft toteutus and hakutiedon hakukohteet when esikatselu true"
        (let [response (get-ok (toteutus-draft-url toteutusOid5))]
          (is (= toteutusOid5 (:oid response)))
          (is (some (fn [hakutieto] (some #(= hakukohdeOid5 (:hakukohdeOid %)) (:hakukohteet hakutieto))) (:hakutiedot response)))
          (is (not-any? (fn [hakutieto] (some #(= hakukohdeOid6 (:hakukohdeOid %)) (:hakukohteet hakutieto))) (:hakutiedot response)))))
      (testing "not found"
        (get-not-found (toteutus-url toteutusOid99)))
      (testing "filter arkistoitu draft even when esikatselu true"
        (get-not-found (toteutus-draft-url toteutusOid4)))
      (testing "filter not julkaistu and draft true but esikatselu false"
        (get-not-found (toteutus-draft-url toteutusOid6)))
      (testing "filter not julkaistu and draft false"
        (get-not-found (toteutus-url toteutusOid6))))))
