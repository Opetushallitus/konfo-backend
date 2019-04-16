(ns konfo-backend.index.eperuste-test
  (:require [clojure.test :refer :all]
            [clj-test-utils.elasticsearch-mock-utils :as utils]
            [konfo-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(defn eperuste-url
  [id]
  (str "/konfo-backend/eperuste/" id))

(defn kuvaus-url
  [koulutuskoodi-uri]
  (str "/konfo-backend/kuvaus/" koulutuskoodi-uri))

(defonce mocked-search-response
  {:hits {:total 1,
          :hits [{:_source {:koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                                  :en "Further vocational qualification for Commercial Divers"},
                                           :koulutuskoodiUri "koulutus_355201"}],
                                           :id 3536456,
                                           :kuvaus {:fi "kuvaus fi"
                                                    :sv "kuvaus sv"}}}]}})
(deftest eperuste-test
  (testing "Get eperuste kuvaus"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [x y & z] mocked-search-response)]
      (let [response (get-ok (kuvaus-url "koulutus_355201#1"))]
        (is (= response {:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                :en "Further vocational qualification for Commercial Divers"},
                         :koulutuskoodiUri "koulutus_355201",
                         :id 3536456,
                         :kuvaus {:fi "kuvaus fi"
                                  :sv "kuvaus sv"}})))))

  (testing "Get valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y z] {:found true :_source {:id 3536456 :tila "valmis"}})]
      (let [response (get-ok (eperuste-url 3536456))]
        (is (= response {:id 3536456 :tila "valmis"})))))

  (testing "Don't get not valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y z] {:found true :_source {:id 3536456 :tila "luonnos"}})]
      (get-not-found (eperuste-url 3536456)))))