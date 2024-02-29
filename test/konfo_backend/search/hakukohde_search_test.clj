(ns konfo-backend.search.hakukohde-search-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [clojure.string :refer [starts-with?]]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-mock-data :refer :all]
            [cheshire.core :refer [generate-string]]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn hakukohde-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/hakukohteet" query-params))

(defn search
  [& query-params]
  (get-ok (apply hakukohde-search-url query-params)))

(defn ->bad-request-body
  [& query-params]
  (:body (get-bad-request (apply hakukohde-search-url query-params))))

(deftest test-hakukohde-search
  (testing "Hakukohde search returns bad request when kohdejoukko is missing"
    (is (= "Haun kohdejoukko puuttuu"
           (->bad-request-body))))
  (testing "Hakukohde search returns bad request with invalid kohdejoukko value"
    (is (= "Haun kohdejoukon arvo on virheellinen"
           (->bad-request-body :kohdejoukko "foofoo"))))
  (testing "Searching with haunkohdejoukko_02"
    (let [response (search :kohdejoukko "haunkohdejoukko_02")]
      (is (= 7 (count (:hits response))))
      (is (= 7 (:total response)))
      (is (= {:oid "1.2.246.562.20.0000011"
              :nimi {:fi "nimi fi" :sv "nimi sv"}
              :hakuOid "1.2.246.562.29.0000006"
              :jarjestyspaikka {:nimi {:fi "Jokin järjestyspaikka" :sv "Jokin järjestyspaikka sv"}}
              :jarjestyspaikkaHierarkiaNimi {:fi "Oppilaitos, Järjestämispaikka fi" :sv "Oppilaitos, Järjestämispaikka sv"}
              :toteutus {:oid "1.2.246.562.17.000008"}
              :koulutustyyppi "yo"
              :ammatillinenPerustutkintoErityisopetuksena nil}
             (first (:hits response))))))
  (testing "Searching with haunkohdejoukko_11"
    (let [response (search :kohdejoukko "haunkohdejoukko_11")]
      (is (= 0 (count (:hits response))))
      (is (= 0 (:total response))))))

(not
  (=
    {:oid "1.2.246.562.20.0000011",
     :nimi {:fi "nimi fi", :sv "nimi sv"},
     :hakuOid "1.2.246.562.29.0000006",
     :jarjestyspaikka {:nimi {:fi "Jokin järjestyspaikka", :sv "Jokin järjestyspaikka sv"}},
     :jarjestyspaikkaHierarkiaNimi {:fi "Oppilaitos, Järjestämispaikka fi", :sv "Oppilaitos, Järjestämispaikka sv"},
     :toteutus {:oid "1.2.246.562.17.000008"}, :koulutustyyppi "yo",
     :ammatillinenPerustutkintoErityisopetuksena nil}
    {:oid "1.2.246.562.20.0000011",
     :nimi {:fi "nimi fi", :sv "nimi sv"},
     :hakuOid "1.2.246.562.29.0000006",
     :jarjestyspaikka {:nimi {:fi "Jokin järjestyspaikka", :sv "Jokin järjestyspaikka sv"}},
     :toteutus {:oid "1.2.246.562.17.000008"},
     :koulutustyyppi "yo",
     :ammatillinenPerustutkintoErityisopetuksena nil}))
