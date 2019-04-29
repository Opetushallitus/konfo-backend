(ns konfo-backend.index.eperuste-test
  (:require [clojure.test :refer :all]
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

(defonce mocked-osaamisala-response
  {:hits {:total 2
          :hits [{:_source {:id 123 :osaamisala {:nimi {:fi "Osaamisala 1 nimi fi" :sv "Osaamisala 1 nimi sv"} :uri "osaamisala_1"} :teksti {:fi "Osaamisala 1 teksti fi" :sv "Osaamisala 1 teksti sv"}}},
                 {:_source {:id 124 :osaamisala {:nimi {:fi "Osaamisala 2 nimi fi" :sv "Osaamisala 2 nimi sv"} :uri "osaamisala_2"} :teksti {:fi "Osaamisala 2 teksti fi" :sv "Osaamisala 2 teksti sv"}}}]}})

(deftest eperuste-test
  (testing "Get eperuste-kuvaus"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [x y & z] mocked-search-response)]
      (let [response (get-ok (kuvaus-url "koulutus_355201%231"))]
        (is (= response {:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                :en "Further vocational qualification for Commercial Divers"},
                         :koulutuskoodiUri "koulutus_355201",
                         :id 3536456,
                         :kuvaus {:fi "kuvaus fi"
                                  :sv "kuvaus sv"}})))))

  (testing "Get eperuste-kuvaus with osaamisalakuvaukset"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [i y & z] (if (= "eperuste" i)
                                                                           mocked-search-response
                                                                           mocked-osaamisala-response))]
      (let [response (get-ok (str (kuvaus-url "koulutus_355201%231") "?osaamisalakuvaukset=true"))]
        (is (= response {:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                :en "Further vocational qualification for Commercial Divers"},
                         :koulutuskoodiUri "koulutus_355201",
                         :id 3536456,
                         :kuvaus {:fi "kuvaus fi"
                                  :sv "kuvaus sv"}
                         :osaamisalat [{:nimi {:fi "Osaamisala 1 nimi fi" :sv "Osaamisala 1 nimi sv"}
                                        :osaamisalakoodiUri "osaamisala_1"
                                        :id 123
                                        :kuvaus {:fi "Osaamisala 1 teksti fi" :sv "Osaamisala 1 teksti sv"}},
                                       {:nimi {:fi "Osaamisala 2 nimi fi" :sv "Osaamisala 2 nimi sv"}
                                        :osaamisalakoodiUri "osaamisala_2"
                                        :id 124
                                        :kuvaus {:fi "Osaamisala 2 teksti fi" :sv "Osaamisala 2 teksti sv"}}]})))))

  (testing "Get valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y z] {:found true :_source {:id 3536456 :tila "valmis"}})]
      (let [response (get-ok (eperuste-url 3536456))]
        (is (= response {:id 3536456 :tila "valmis"})))))

  (testing "Don't get not valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y z] {:found true :_source {:id 3536456 :tila "luonnos"}})]
      (get-not-found (eperuste-url 3536456)))))