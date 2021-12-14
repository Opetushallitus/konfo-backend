(ns konfo-backend.external.external-search-api-test
  (:require [clojure.test :refer :all]
            [konfo-backend.external.schema.koulutus :as k]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

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

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Search toteutus keyword koira koulutustyyppi amm"
      (let [r (search :koulutustyyppi "amm")]
        (is (= 3 (:total r)))
        (is (= [koulutusOid1] [(:oid (first (:hits r)))]))
        (is (= [toteutusOid2] [(:toteutusOid (first (:toteutukset (first (:hits r)))))]))))
    (testing "Search toteutus with keyword"
      (let [r (search :keyword "Hevosalan")]
        (is (= 2 (:total r)))
        (is (= [koulutusOid2 koulutusOid4] (vec (map :oid (:hits r)))))))
    (testing "Search toteutus with koulutustyyppi amm"
      (let [r (search :koulutustyyppi "amm")]
        (is (= 3 (:total r)))
        (is (= koulutusOid1 (first (sort (map :oid (:hits r))))))))
    (testing "Search toteutus koulutustyyppi yo"
      (let [r (search :koulutustyyppi "yo")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid4] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid5] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Search toteutus with keyword and koulutustyyppi amk"
      (let [r (search :keyword "Hevonen" :koulutustyyppi "amk")]
        (is (= 1 (:total r)))
        (is (= [koulutusOid5] (vec (map :oid (:hits r)))))
        (is (= [toteutusOid6] (vec (sort (map :toteutusOid (:toteutukset (first (:hits r))))))))))
    (testing "Nothing found"
      (is (= 0 (count (:hits (search :keyword "mummo"))))))
    (testing "Erroneous schema"
    (with-redefs-fn {#'konfo-backend.search.response/parse-external (fn [response] {:hits {:hits {:huuhaa "hiihaa"}}})}
      #(search-failed :keyword "Virheellinen" :koulutustyyppi "tuva"))))))
