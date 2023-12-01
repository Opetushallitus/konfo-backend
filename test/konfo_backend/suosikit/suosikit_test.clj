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

(defn suosikit-vertailu-url
  [hakukohde-oids]
  (apply url-with-query-params "/konfo-backend/suosikit-vertailu" [:hakukohde-oids hakukohde-oids]))

(deftest suosikit-test
  (set-fixed-time "2020-01-02T12:00:01")
  (let [suosikki-hakukohde-oid1  "1.2.246.562.20.0000001"]
    (testing "Get suosikit"
      (testing "ok"
        (let [response (get-ok (suosikit-url [suosikki-hakukohde-oid1]))]
          (is (match? [{:tila "julkaistu"
                        :esittely {:fi "kuvaus"
                                   :sv "kuvaus sv"}
                        :logo "https://testi.fi/oppilaitos-logo/oid/logo.png"
                        :toteutusOid "1.2.246.562.17.000001"
                        :oppilaitosNimi {:fi "Jokin järjestyspaikka"
                                         :sv "Jokin järjestyspaikka sv"}
                        :jarjestyspaikka {:nimi {:fi "Jokin järjestyspaikka"
                                                 :sv "Jokin järjestyspaikka sv"}
                                          :paikkakunta {:koodiUri "kunta_297"
                                                        :nimi {:fi "kunta_297 nimi fi"
                                                               :sv "kunta_297 nimi sv"}}
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
                                             :nimi {:fi "tutkintonimikkeet_01 nimi fi"
                                                    :sv "tutkintonimikkeet_01 nimi sv"}}
                                            {:koodiUri "tutkintonimikkeet_02"
                                             :nimi {:fi "tutkintonimikkeet_02 nimi fi"
                                                    :sv "tutkintonimikkeet_02 nimi sv"}}]
                        :hakukohdeOid "1.2.246.562.20.0000001"}]
                      response)))))
    (testing "Get suosikit vertailu"
      (testing "ok"
        (let [response (get-ok (suosikit-vertailu-url [suosikki-hakukohde-oid1]))]
          (is (match?  [{:koulutustyyppi "amm"
                         :nimi {:fi "Hakukohde fi" :sv "Hakukohde sv"}
                         :hakukohdeOid "1.2.246.562.20.0000001"
                         :toteutusOid "1.2.246.562.17.000001"
                         :hakuOid "1.2.246.562.29.0000001"
                         :logo "https://testi.fi/oppilaitos-logo/oid/logo.png"
                         :esittely {:fi "kuvaus"
                                    :sv "kuvaus sv"}
                         :oppilaitosNimi {:fi "Jokin järjestyspaikka"
                                          :sv "Jokin järjestyspaikka sv"}
                         :osoite {}
                         :opiskelijoita 100
                         :osaamisalat [{:koodi {:koodiUri "osaamisala_0001#1"
                                                :nimi {:fi "osaamisala_0001#1 nimi fi"
                                                       :sv "osaamisala_0001#1 nimi sv"}}
                                        :linkki {:fi "http://osaamisala.fi/linkki/fi"
                                                 :sv "http://osaamisala.fi/linkki/sv"}
                                        :otsikko {:fi "Katso osaamisalan tarkempi kuvaus tästä"
                                                  :sv "Katso osaamisalan tarkempi kuvaus tästä sv"}}]
                         :edellinenHaku nil
                         :jarjestaaUrheilijanAmmKoulutusta false
                         :valintakokeet
                         [{:id "f50c7536-1c50-4fa8-b13c-514877be71a0"
                           :tyyppi {:koodiUri "valintakokeentyyppi_1#1"
                                    :nimi
                                    {:fi "valintakokeentyyppi_1#1 nimi fi"
                                     :sv "valintakokeentyyppi_1#1 nimi sv"}}
                           :nimi {:fi "valintakokeen nimi fi" :sv "valintakokeen nimi sv"}
                           :metadata {:tietoja
                                      {:fi "tietoa valintakokeesta fi"
                                       :sv "tietoa valintakokeesta sv"}
                                      :vahimmaispisteet 182.1
                                      :liittyyEnnakkovalmistautumista true
                                      :ohjeetEnnakkovalmistautumiseen
                                      {:fi "Ennakko-ohjeet fi" :sv "Ennakko-ohjeet sv"}
                                      :erityisjarjestelytMahdollisia true
                                      :ohjeetErityisjarjestelyihin
                                      {:fi "Erityisvalmistelut fi" :sv "Erityisvalmistelut sv"}}
                           :tilaisuudet [{:osoite
                                          {:osoite {:fi "Kivatie 1" :sv "kivavägen 1"}
                                           :postinumero
                                           {:koodiUri "posti_04230#2"
                                            :nimi
                                            {:fi "posti_04230#2 nimi fi" :sv "posti_04230#2 nimi sv"}}}
                                          :aika
                                          {:alkaa "2023-10-11T09:49"
                                           :paattyy "2023-10-11T09:58"
                                           :formatoituPaattyy
                                           {:fi "11.10.2023 klo 09:58"
                                            :sv "11.10.2023 kl. 09:58"
                                            :en "Oct. 11, 2023 at 09:58 am UTC+3"}
                                           :formatoituAlkaa
                                           {:fi "11.10.2023 klo 09:49"
                                            :sv "11.10.2023 kl. 09:49"
                                            :en "Oct. 11, 2023 at 09:49 am UTC+3"}}
                                          :jarjestamispaikka
                                          {:fi "Järjestämispaikka fi" :sv "Järjestämispaikka sv"}
                                          :lisatietoja {:fi "lisätieto fi" :sv "lisätieto sv"}}]}]}]
                       response)))))))