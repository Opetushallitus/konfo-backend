(ns konfo-backend.suosikit.suosikit-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [matcher-combinators.test]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn suosikit-url
  [hakukohde-oids]
  (apply url-with-query-params "/konfo-backend/suosikit" [:hakukohde-oids hakukohde-oids]))

(deftest suosikit-test
  (let [suosikki-hakukohde-oid1  "1.2.246.562.20.0000001"]
    (testing "Get suosikki"
      (testing "ok"
        (let [response (get-ok (suosikit-url [suosikki-hakukohde-oid1]))]
          (is (match?  [{:tila "julkaistu"
                         :esittely {:fi "Esittely"
                                    :sv "Esittely sv"}
                         :logo "https://testi.fi/oppilaitos-logo/oid/logo.png"
                         :toteutusOid "1.2.246.562.17.000001"
                         :jarjestyspaikka {:nimi {:fi "Jokin järjestyspaikka"
                                                  :sv "Jokin järjestyspaikka sv"}
                                           :paikkakunta {:koodiUri "kunta_297"
                                                         :nimi {:fi "kunta_297 nimi fi" :sv "kunta_297 nimi sv"}}
                                           :jarjestaaUrheilijanAmmKoulutusta true
                                           :oid "1.2.246.562.10.67476956288"}
                         :nimi {:fi "nimi fi" :sv "nimi sv"}
                         :hakuajat [{:formatoituAlkaa {:fi "11.10.2023 klo 09:49"
                                                       :sv "11.10.2023 kl. 09:49"
                                                       :en "Oct. 11, 2023 at 09:49 am UTC+3"}
                                     :formatoituPaattyy {:fi "11.10.2023 klo 09:58"
                                                         :sv "11.10.2023 kl. 09:58"
                                                         :en "Oct. 11, 2023 at 09:58 am UTC+3"}
                                     :alkaa "2023-10-11T09:49"
                                     :paattyy "2023-10-11T09:58"
                                     :hakuAuki false
                                     :hakuMennyt false}]
                         :tutkintonimikkeet [{:koodiUri "tutkintonimikkeet_01"
                                              :nimi
                                              {:fi "tutkintonimikkeet_01 nimi fi"
                                               :sv "tutkintonimikkeet_01 nimi sv"}}
                                             {:koodiUri "tutkintonimikkeet_02"
                                              :nimi
                                              {:fi "tutkintonimikkeet_02 nimi fi"
                                               :sv "tutkintonimikkeet_02 nimi sv"}}]
                         :hakukohdeOid "1.2.246.562.20.0000001"}] response)))))))