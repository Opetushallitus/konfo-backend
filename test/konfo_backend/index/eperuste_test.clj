(ns konfo-backend.index.eperuste-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(defn eperuste-url
  [id]
  (str "/konfo-backend/eperuste/" id))

(defn kuvaus-url
  [eperuste-id]
  (str "/konfo-backend/kuvaus/" eperuste-id))

(defonce voimassa {:tila "valmis",
                   :voimassaoloAlkaa (- (now-in-millis) 1000),
                   :tyotehtavatJoissaVoiToimia  {:fi "työtehtävät fi" :sv "työtehtävät sv"},
                   :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"},
                   :id 3536456,
                   :koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto",
                                         :sv "Yrkesexamen för yrkesdykare",
                                         :en "Further vocational qualification for Commercial Divers"},
                                  :koulutuskoodiUri "koulutus_355201"}]})

(defonce mocked-response-voimassa
  {:found true,
   :_source voimassa})

(defonce mocked-osaamisala-response
  {:hits {:total 2
          :hits [{:_source {:id 123 :suoritustapa "reformi" :osaamisala {:nimi {:fi "Osaamisala 1 nimi fi" :sv "Osaamisala 1 nimi sv"} :uri "osaamisala_1"} :teksti {:fi "Osaamisala 1 teksti fi" :sv "Osaamisala 1 teksti sv"}}},
                 {:_source {:id 124 :suoritustapa "ops" :osaamisala {:nimi {:fi "Osaamisala 2 nimi fi" :sv "Osaamisala 2 nimi sv"} :uri "osaamisala_2"} :teksti {:fi "Osaamisala 2 teksti fi" :sv "Osaamisala 2 teksti sv"}}}]}})

(deftest eperuste-test
  (testing "Get eperuste-kuvaus"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y] mocked-response-voimassa)]
      (let [response (get-ok (kuvaus-url "3536456"))]
        (is (= response {:id 3536456,
                         :tyotehtavatJoissaVoiToimia  {:fi "työtehtävät fi" :sv "työtehtävät sv"},
                         :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"}})))))

  (testing "Get eperuste-kuvaus with osaamisalakuvaukset"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y] mocked-response-voimassa)
                  clj-elasticsearch.elastic-connect/search (fn [i y & z] mocked-osaamisala-response)]
      (let [response (get-ok (str (kuvaus-url "koulutus_355201%231") "?osaamisalakuvaukset=true"))]
        (is (= response {:id 3536456,
                         :tyotehtavatJoissaVoiToimia  {:fi "työtehtävät fi" :sv "työtehtävät sv"},
                         :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"},
                         :osaamisalat [{:nimi {:fi "Osaamisala 1 nimi fi" :sv "Osaamisala 1 nimi sv"}
                                        :osaamisalakoodiUri "osaamisala_1"
                                        :id 123
                                        :suoritustapa "reformi"
                                        :kuvaus {:fi "Osaamisala 1 teksti fi" :sv "Osaamisala 1 teksti sv"}},
                                       {:nimi {:fi "Osaamisala 2 nimi fi" :sv "Osaamisala 2 nimi sv"}
                                        :osaamisalakoodiUri "osaamisala_2"
                                        :id 124
                                        :suoritustapa "ops"
                                        :kuvaus {:fi "Osaamisala 2 teksti fi" :sv "Osaamisala 2 teksti sv"}}]})))))

  (testing "Don't get not valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y] {:found true :_source {:id 3536456 :tila "luonnos" :voimassaoloAlkaa  (- (now-in-millis) 10000)}})]
      (get-not-found (eperuste-url 3536456)))))