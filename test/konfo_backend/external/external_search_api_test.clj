(ns konfo-backend.external.external-search-api-test
  (:require [clojure.test :refer :all]
            [konfo-backend.external.schema.koulutus :as k]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.search-test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/external/search/toteutukset-koulutuksittain" query-params))

(defn search
  [& query-params]
  (get-ok-or-print-schema-error (apply search-url query-params)))

(deftest external-search-api-test
  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"
        koulutusOid5 "1.2.246.562.13.000005"
        toteutusOid1 "1.2.246.562.17.000001"
        toteutusOid2 "1.2.246.562.17.000002"
        toteutusOid3 "1.2.246.562.17.000003"
        toteutusOid4 "1.2.246.562.17.000004"
        toteutusOid5 "1.2.246.562.17.000005"
        toteutusOid6 "1.2.246.562.17.000006"
        sorakuvausId "2ff6700d-087f-4dbf-9e42-7f38948f227a"]

  (fixture/add-koulutus-mock koulutusOid1 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Eläinkoulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto :ePerusteId "1234" :sorakuvausId sorakuvausId)
  (fixture/add-koulutus-mock koulutusOid2 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto :sorakuvausId sorakuvausId)
  (fixture/add-koulutus-mock koulutusOid3 :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevonen koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto :sorakuvausId sorakuvausId)
  (fixture/add-koulutus-mock koulutusOid4 :koulutustyyppi "yo"  :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata yo-koulutus-metatieto :sorakuvausId sorakuvausId)
  (fixture/add-koulutus-mock koulutusOid5 :koulutustyyppi "amk" :tila "julkaistu" :nimi "Ponialan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata amk-koulutus-metatieto :sorakuvausId sorakuvausId)
  (fixture/add-toteutus-mock toteutusOid1 koulutusOid2 :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock toteutusOid2 koulutusOid1 :tila "julkaistu" :nimi "Koirakoulutus" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock toteutusOid3 koulutusOid1 :tila "julkaistu" :nimi "Kissakoulutus" :tarjoajat helsingin-toimipiste :metadata toteutus-metatieto :teemakuva "https://example.com/kuva.jpg")
  (fixture/add-toteutus-mock toteutusOid4 koulutusOid3 :tila "tallennettu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock toteutusOid5 koulutusOid4 :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata yo-toteutus-metatieto)
  (fixture/add-toteutus-mock toteutusOid6 koulutusOid5 :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata yo-toteutus-metatieto)
  (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu")

  (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5] :oppilaitokset [punkaharjun-yliopisto, helsingin-yliopisto]} orgs)

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto mock-get-koodisto]
    (testing "Search toteutukset"
      (let [r (search :keyword "hevonen" :koulutustyyppi "amm")]
        (is (= 2 (:total r)))
        (is (= [koulutusOid1 koulutusOid2] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid2 toteutusOid3] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Search toteutus"
      (let [r (search :keyword "koira" :koulutustyyppi "amm")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid1] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid2] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Get correct result"
      (let [r (search :keyword "kissa" :koulutustyyppi "amm")]
        (is (= r {:total 1,
                  :hits [{:opintojenLaajuusyksikko {:koodiUri "opintojenlaajuusyksikko_6",
                                                    :nimi {:fi "opintojenlaajuusyksikko_6 nimi fi",
                                                           :sv "opintojenlaajuusyksikko_6 nimi sv"}},
                           :kuvaus nil,
                           :teemakuva "https://testi.fi/koulutus-teemakuva/oid/kuva.jpg",
                           :toteutukset [{:toteutusOid "1.2.246.562.17.000003",
                                          :toteutusNimi {:fi "Kissakoulutus fi", :sv "Kissakoulutus sv" },
                                          :oppilaitosOid "1.2.246.562.10.000005",
                                          :oppilaitosNimi {:fi "Helsingin yliopisto", :sv "Helsingin yliopisto sv"},
                                          :kunnat [{:koodiUri "kunta_091",
                                                    :nimi {:fi "kunta_091 nimi fi", :sv "kunta_091 nimi sv"}}]}],
                           :ePerusteId 1234,
                           :nimi {:fi "Eläinkoulutus fi", :sv "Eläinkoulutus sv" },
                           :oid "1.2.246.562.13.000001",
                           :kielivalinta [ "fi", "sv" ],
                           :koulutukset [{:koodiUri "koulutus_371101#1",
                                          :nimi {:fi "koulutus_371101#1 nimi fi", :sv "koulutus_371101#1 nimi sv"}}],
                           :tutkintonimikkeet [{:koodiUri "tutkintonimikkeet_01",
                                                :nimi {:fi "tutkintonimikkeet_01 nimi fi", :sv "tutkintonimikkeet_01 nimi sv"}},
                                               {:koodiUri "tutkintonimikkeet_02",
                                                :nimi {:fi "tutkintonimikkeet_02 nimi fi", :sv "tutkintonimikkeet_02 nimi sv"}}],
                           :opintojenLaajuus {:koodiUri "opintojenlaajuus_150",
                                              :nimi {:fi "opintojenlaajuus_150 nimi fi", :sv "opintojenlaajuus_150 nimi sv"}},
                           :opintojenLaajuusNumero 150,
                           :koulutustyyppi "amm"}]}))))
    (testing "Search toteutus"
      (let [r (search :keyword "Hevosalan")]
        (is (= 2 (:total r)))
        (is (= [koulutusOid2 koulutusOid4] (vec (map :oid (:hits r)))))))
    (testing "Search toteutus"
      (let [r (search :keyword "Hevosalan" :koulutustyyppi "amm")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid2] (vec (map :oid (:hits r)))))))
    (testing "Search toteutus"
      (let [r (search :keyword "Hevosalan" :koulutustyyppi "yo")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid4] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid5] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Search toteutus"
      (let [r (search :keyword "Hevonen" :koulutustyyppi "amk")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid5] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid6] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Nothing found"
      (is (= {:total 0,
              :hits []} (search :keyword "mummo")))))))
