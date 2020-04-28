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

(defonce voimassa {:voimassaoloAlkaa (- (now-in-millis) 1000),
                   :tyotehtavatJoissaVoiToimia  {:fi "työtehtävät fi" :sv "työtehtävät sv"},
                   :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"},
                   :id 3536456,
                   :koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto",
                                         :sv "Yrkesexamen för yrkesdykare",
                                         :en "Further vocational qualification for Commercial Divers"},
                                  :koulutuskoodiUri "koulutus_355201"}]})

(defonce siirtyma {:voimassaoloAlkaa  (- (now-in-millis) 10000),
                   :voimassaoloLoppuu (- (now-in-millis) 5000),
                   :siirtymaPaattyy   (+ (now-in-millis) 100000)
                   :kuvaus {:fi "kuvaus fi" :sv "kuvaus sv"},
                   :id 3536457,
                   :koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto siirtymä",
                                         :sv "Yrkesexamen för yrkesdykare siirtymä",
                                         :en "Further vocational qualification for Commercial Divers siirtymä"},
                                  :koulutuskoodiUri "koulutus_355201"}]})

(defonce tuleva {:voimassaoloAlkaa (+ (now-in-millis) 100000),
                 :kuvaus {:fi "kuvaus fi" :sv "kuvaus sv"},
                 :id 3536458,
                 :koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto tuleva",
                                       :sv "Yrkesexamen för yrkesdykare tuleva",
                                       :en "Further vocational qualification for Commercial Divers tuleva"},
                                :koulutuskoodiUri "koulutus_355201"}]})

(defonce vanha {:voimassaoloAlkaa  (- (now-in-millis) 10000),
                :voimassaoloLoppuu (- (now-in-millis) 5000),
                :kuvaus {:fi "kuvaus fi" :sv "kuvaus sv"},
                :id 3536459,
                :koulutukset [{:nimi {:fi "Ammattisukeltajan ammattitutkinto vanha",
                                      :sv "Yrkesexamen för yrkesdykare vanha",
                                      :en "Further vocational qualification for Commercial Divers vanha"},
                               :koulutuskoodiUri "koulutus_355201"}]})

(defonce mocked-response-voimassa
  {:hits {:total 2,
          :hits [{:_source voimassa},
                 {:_source siirtyma}]}})

(defonce mocked-response-siirtyma
         {:hits {:total 2,
                 :hits [{:_source vanha},
                        {:_source siirtyma}]}})

(defonce mocked-response-nothing
         {:hits {:total 2,
                 :hits [{:_source vanha},
                        {:_source tuleva}]}})

(defonce mocked-osaamisala-response
  {:hits {:total 2
          :hits [{:_source {:id 123 :suoritustapa "reformi" :osaamisala {:nimi {:fi "Osaamisala 1 nimi fi" :sv "Osaamisala 1 nimi sv"} :uri "osaamisala_1"} :teksti {:fi "Osaamisala 1 teksti fi" :sv "Osaamisala 1 teksti sv"}}},
                 {:_source {:id 124 :suoritustapa "ops" :osaamisala {:nimi {:fi "Osaamisala 2 nimi fi" :sv "Osaamisala 2 nimi sv"} :uri "osaamisala_2"} :teksti {:fi "Osaamisala 2 teksti fi" :sv "Osaamisala 2 teksti sv"}}}]}})

(deftest eperuste-test
  (testing "Get eperuste-kuvaus"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [x y & z] mocked-response-voimassa)]
      (let [response (get-ok (kuvaus-url "koulutus_355201%231"))]
        (is (= response {:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                :en "Further vocational qualification for Commercial Divers"},
                         :koulutuskoodiUri "koulutus_355201",
                         :id 3536456,
                         :tyotehtavatJoissaVoiToimia  {:fi "työtehtävät fi" :sv "työtehtävät sv"},
                         :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"}})))))

  (testing "Get siirtyma-ajalla oleva if there is no voimassa oleva kuvaus"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [x y & z] mocked-response-siirtyma)]
      (let [response (get-ok (kuvaus-url "koulutus_355201%231"))]
        (is (= (:id response) 3536457)))))

  (testing "Get nothing if there is no voimassa oleva or siirtyma-ajalla oleva kuvaus"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [x y & z] mocked-response-nothing)]
      (get-not-found (kuvaus-url "koulutus_355201%231"))))

  (testing "Get eperuste-kuvaus with osaamisalakuvaukset"
    (with-redefs [clj-elasticsearch.elastic-connect/search (fn [i y & z] (if (= "eperuste" i)
                                                                           mocked-response-voimassa
                                                                           mocked-osaamisala-response))]
      (let [response (get-ok (str (kuvaus-url "koulutus_355201%231") "?osaamisalakuvaukset=true"))]
        (is (= response {:nimi {:fi "Ammattisukeltajan ammattitutkinto", :sv "Yrkesexamen för yrkesdykare",
                                :en "Further vocational qualification for Commercial Divers"},
                         :koulutuskoodiUri "koulutus_355201",
                         :id 3536456,
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

  (testing "Get valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y] {:found true :_source {:id 3536456 :tila "valmis" :voimassaoloAlkaa  (- (now-in-millis) 10000)}})]
      (let [response (get-ok (eperuste-url 3536456))]
        (is (= (:id response) 3536456)))))

  (testing "Don't get not valmis eperuste"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y] {:found true :_source {:id 3536456 :tila "luonnos" :voimassaoloAlkaa  (- (now-in-millis) 10000)}})]
      (get-not-found (eperuste-url 3536456)))))