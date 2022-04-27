(ns konfo-backend.external.external-api-test
  (:require [clojure.test :refer :all]
            [konfo-backend.external.schema.koulutus :as k]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

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
    (let [koulutusOid1 "1.2.246.562.13.000003"
          koulutusOid2 "1.2.246.562.13.000004"
          lukio-Oid "1.2.246.562.13.000008"
          kkKoulutusOid "1.2.246.562.13.000009"
          toteutusOid1 "1.2.246.562.17.000001"
          toteutusOid2 "1.2.246.562.17.000002"
          toteutusOid3 "1.2.246.562.17.000003"
          toteutusOid4 "1.2.246.562.17.000004"
          lukio-toteutus-oid "1.2.246.562.17.000007"
          kkToteutusOid "1.2.246.562.17.000008"
          hakukohdeOid1 "1.2.246.562.20.0000001"
          hakukohdeOid2 "1.2.246.562.20.0000002"
          hakukohdeOid3 "1.2.246.562.20.0000003"
          hakukohdeOid4 "1.2.246.562.20.0000004"
          kkHakukohdeOid "1.2.246.562.20.0000011"
          hakuOid1 "1.2.246.562.29.0000001"
          hakuOid2 "1.2.246.562.29.0000004"
          kkHakuOid "1.2.246.562.29.0000006"
          valintaperusteId1 "31972648-ebb7-4185-ac64-31fa6b841e34"]

      (testing "Get koulutus"
        (testing "ok only koulutus"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1))]
            (is (= koulutusOid1 (:oid response)))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "only lukio koulutus"
          (let [response (get-ok-or-print-schema-error (koulutus-url lukio-Oid))]
            (is (= lukio-Oid (:oid response)))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok koulutus with toteutukset"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1 :toteutukset true))]
            (is (= koulutusOid1 (:oid response)))
            (is (= [toteutusOid1 toteutusOid2 toteutusOid3] (vec (sort (map :oid (:toteutukset response))))))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok koulutus with hakukohteet"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1 :hakukohteet true))]
            (is (= koulutusOid1 (:oid response)))
            (is (= [hakukohdeOid1 hakukohdeOid3] (vec (sort (map :oid (:hakukohteet response))))))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :haut)))))
        (testing "ok koulutus with haut"
          (let [response (get-ok-or-print-schema-error (koulutus-url koulutusOid1 :haut true))]
            (is (= koulutusOid1 (:oid response)))
            (is (= [hakuOid1] (vec (sort (distinct (map :oid (:haut response)))))))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :toteutukset)))))
        (testing "not found"
          (get-not-found (koulutus-url "1.2.246.562.13.000999")))
        (testing "not julkaistu"
          (get-not-found (koulutus-url koulutusOid2)))
        (testing "not amm"
          (get-ok-or-print-schema-error (koulutus-url kkKoulutusOid))))

      (testing "Get toteutus"
        (testing "ok only toteutus"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid1))]
            (is (= toteutusOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "only lukio toteutus"
          (let [response (get-ok-or-print-schema-error (toteutus-url lukio-toteutus-oid))]
            (is (= lukio-toteutus-oid (:oid response)))))
        (testing "ok toteutus with koulutus"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid1 :koulutus true))]
            (is (= toteutusOid1 (:oid response)))
            (is (= koulutusOid1 (get-in response [:koulutus :oid])))
            (is (false? (contains? response :hakukohteet)))
            (is (false? (contains? response :haut)))))
        (testing "ok toteutus with hakukohteet"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid1 :hakukohteet true))]
            (is (= toteutusOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (= [hakukohdeOid1] (vec (sort (map :oid (:hakukohteet response))))))
            (is (false? (contains? response :haut)))))
        (testing "ok toteutus with haut"
          (let [response (get-ok-or-print-schema-error (toteutus-url toteutusOid1 :haut true))]
            (is (= toteutusOid1 (:oid response)))
            (is (false? (contains? response :koulutus)))
            (is (false? (contains? response :hakukohteet)))
            (is (= [hakuOid1] (vec (sort (map :oid (:haut response))))))))
        (testing "not found"
          (get-not-found (toteutus-url "1.2.246.562.17.000009")))
        (testing "not julkaistu"
          (get-not-found (toteutus-url toteutusOid4)))
        (testing "not amm"
          (get-ok-or-print-schema-error (toteutus-url kkToteutusOid))))

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
            (is (= toteutusOid1 (get-in response [:toteutus :oid])))
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
          (get-not-found (hakukohde-url "1.2.246.562.20.000999")))
        (testing "not julkaistu"
          (get-not-found (hakukohde-url hakukohdeOid4)))
        (testing "not amm"
          (let [response (get-ok-or-print-schema-error (hakukohde-url kkHakukohdeOid :koulutus true :toteutus true :haku true :valintaperustekuvaus true))]
            (is (= kkHakukohdeOid (:oid response)))
            (is (= kkKoulutusOid (get-in response [:koulutus :oid])))
            (is (= kkToteutusOid (get-in response [:toteutus :oid])))
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
            (is (= [koulutusOid1] (vec (sort (distinct (map :oid (:koulutukset response)))))))
            (is (false? (contains? response :toteutukset)))
            (is (false? (contains? response :hakukohteet)))))
        (testing "ok haku with toteutukset"
          (let [response (get-ok-or-print-schema-error (haku-url hakuOid1 :toteutukset true))]
            (is (= hakuOid1 (:oid response)))
            (is (false? (contains? response :koulutukset)))
            (is (= [toteutusOid1 toteutusOid2] (vec (sort (map :oid (:toteutukset response))))))
            (is (false? (contains? response :hakukohteet)))))
        (testing "ok haku with hakukohteet"
          (let [response (get-ok-or-print-schema-error (haku-url hakuOid1 :hakukohteet true))]
            (is (= hakuOid1 (:oid response)))
            (is (false? (contains? response :koulutukset)))
            (is (false? (contains? response :toteutukset)))
            (is (= [hakukohdeOid1 hakukohdeOid2 hakukohdeOid3] (vec (sort (map :oid (:hakukohteet response))))))))
        (testing "not found"
          (get-not-found (haku-url "1.2.246.562.29.000009")))
        (testing "not julkaistu"
          (get-not-found (haku-url hakuOid2)))
        (testing "not amm"
          (let [response (get-ok-or-print-schema-error (haku-url kkHakuOid :toteutukset true :koulutukset true :hakukohteet true))]
            (is (= kkHakuOid (:oid response)))
            (is (= [kkKoulutusOid] (vec (sort (map :oid (:koulutukset response))))))
            (is (= [kkToteutusOid] (vec (sort (map :oid (:toteutukset response))))))
            (is (= [kkHakukohdeOid] (vec (sort (map :oid (:hakukohteet response))))))))))))
