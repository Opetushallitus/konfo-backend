(ns konfo-backend.external.external-search-api-test
  (:require [clojure.test :refer :all]
            [konfo-backend.external.schema.koulutus :as k]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/external/search/toteutukset-koulutuksittain" query-params))

(defn search
  [& query-params]
  (get-ok-or-print-schema-error (apply search-url query-params)))

(defn search-failed
  [& query-params]
  (get-internal-error (apply search-url query-params)))

(deftest external-search-api-test
  (let [koulutusOid1 "1.2.246.562.13.000041" ;; Järjestyksessä ensimmäinen
        koulutusOid2 "1.2.246.562.13.000011" ;; Hevosalan koulutus
        koulutusOid3 "1.2.246.562.13.000014" ;; Hevosalan osaamisala koulutus
        koulutusOid4 "1.2.246.562.13.000031" ;; yo
        koulutusOid5 "1.2.246.562.13.000009" ;; yo
        koulutusOid6 "1.2.246.562.13.000030" ;; yo
        koulutusOid8 "1.2.246.562.13.000047" ;; amk
        toteutusOid1 "1.2.246.562.17.000025" ;; Järjestyksessä ensimmäinen
        toteutusOid2 "1.2.246.562.17.000021" ;; yo
        toteutusOid3 "1.2.246.562.17.000028"] ;; amk

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Search toteutus with koulutustyyppi amm"
      (let [r (search :koulutustyyppi "amm")]
        (is (= 20 (:total r)))
        (is (= [koulutusOid1] [(:oid (first (:hits r)))]))
        (is (= [toteutusOid1] [(:toteutusOid (first (:toteutukset (first (:hits r)))))]))))
    (testing "Search toteutus with keyword"
      (let [r (search :keyword "Hevosalan")]
        (is (= 4 (:total r)))
        (is (= [koulutusOid2 koulutusOid3] (vec (map :oid (:hits r)))))))
    (testing "Search toteutus koulutustyyppi yo"
      (let [r (search :koulutustyyppi "yo")]
        (is (= 5 (:total r)))
        (is (= [koulutusOid4 koulutusOid5 koulutusOid6] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid2] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Search toteutus with keyword and koulutustyyppi amk"
      (let [r (search :keyword "Moottori" :koulutustyyppi "amk")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid8] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid3] (vec (sort (distinct (map :toteutusOid (:toteutukset (first (:hits r)))))))))))
    (testing "Nothing found"
      (is (= 0 (count (:hits (search :keyword "mummo"))))))
    (testing "Erroneous schema"
    (with-redefs-fn {#'konfo-backend.search.response/parse-external (fn [response] {:hits {:hits {:huuhaa "hiihaa"}}})}
      #(search-failed :keyword "Virheellinen" :koulutustyyppi "tuva"))))))
