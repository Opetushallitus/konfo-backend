(ns konfo-backend.search.oppilaitos-tarjonta-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [konfo-backend.search.search-test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn jarjestajat-search-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/search/oppilaitos/" oid "/tarjonta") query-params))

(defn search
  [oid & query-params]
  (get-ok (apply jarjestajat-search-url oid query-params)))

(defn ->bad-request-body
  [oid & query-params]
  (:body (get-bad-request (apply jarjestajat-search-url oid query-params))))

(deftest koulutus-jarjestajat-test
  (fixture/add-koulutus-mock "1.2.246.562.13.000001" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto)
  (fixture/add-koulutus-mock "1.2.246.562.13.000002" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000002" :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000002" "1.2.246.562.13.000001" :tila "julkaistu" :nimi "Mersukoulutus" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock "1.2.246.562.17.000003" "1.2.246.562.13.000001" :tila "julkaistu" :nimi "Audikoulutus" :tarjoajat helsingin-toimipiste :metadata toteutus-metatieto)

  (fixture/index-oids-without-related-indices {:koulutukset ["1.2.246.562.13.000001" "1.2.246.562.13.000002"] :oppilaitokset [punkaharjun-yliopisto, helsingin-yliopisto]} orgs)

  (testing "Search oppilaitoksen tarjonta with bad requests:"
    (testing "Invalid lng"
      (is (= "Virheellinen kieli")      (->bad-request-body punkaharjun-yliopisto :lng "foo")))
    (testing "Invalid sort"
      (is (= "Virheellinen järjestys")  (->bad-request-body punkaharjun-yliopisto :sort "foo"))))

  (testing "Sorting and paging tarjonta"
    (testing "asc order"
      (let [r (search punkaharjun-yliopisto :tuleva false :sort "asc")]
        (is (= 2 (:total r)))
        (is (= "1.2.246.562.17.000002" (:toteutusOid (first (:hits r)))))
        (is (= "1.2.246.562.17.000001" (:toteutusOid (second (:hits r)))))))
    (testing "desc order"
      (let [r (search punkaharjun-yliopisto :tuleva false :sort "desc")]
        (is (= 2 (:total r)))
        (is (= "1.2.246.562.17.000001" (:toteutusOid (first (:hits r)))))
        (is (= "1.2.246.562.17.000002" (:toteutusOid (second (:hits r)))))))
    (testing "paging"
      (let [r (search punkaharjun-yliopisto :tuleva false :page 2 :size 1)]
        (is (= 2 (:total r)))
        (is (= 1 (count (:hits r))))
        (is (= "1.2.246.562.17.000001" (:toteutusOid (first (:hits r))))))))

  (testing "Get oppilaitoksen tarjonta"
    (testing "no järjestäjiä"
      (let [r (search "1.2.246.562.10.000666")]
        (is (= 0 (:total r)))
        (is (= [] (:hits r)))))
    (testing "nykyinen"
      (let [r (search helsingin-yliopisto :tuleva false)]
        (is (= 1 (:total r)))
        (is (= {:maksunMaara nil,
                :kuvaus { },
                :koulutusOid "1.2.246.562.13.000001"
                :toteutusOid "1.2.246.562.17.000003",
                :opetusajat [ ],
                :nimi {:fi "Audikoulutus fi",
                       :sv "Audikoulutus sv"},
                :onkoMaksullinen false,
                :kunnat [ {:koodiUri "kunta_091",
                           :nimi {:fi "kunta_091 nimi fi",
                                  :sv "kunta_091 nimi sv"}} ],
                :tutkintonimikkeet nil,
                :koulutustyyppi "amm"} (first (:hits r))))))

    (testing "tulevat"
      (let [r (search helsingin-yliopisto :tuleva true)]
        (is (= 1 (:total r)))
        (is (= {:koulutustyypit [{:koodiUri "koulutustyyppi_01",
                                  :nimi {:fi "koulutustyyppi_01 nimi fi",
                                         :sv "koulutustyyppi_01 nimi sv"}},
                                 {:koodiUri "koulutustyyppi_02",
                                  :nimi {:fi "koulutustyyppi_02 nimi fi",
                                         :sv "koulutustyyppi_02 nimi sv"}}],
                :kuvaus {},
                :opintojenLaajuusyksikko {:koodiUri "opintojenlaajuusyksikko_01",
                                          :nimi {:fi "opintojenlaajuusyksikko_01 nimi fi",
                                                 :sv "opintojenlaajuusyksikko_01 nimi sv"}},
                :opintojenLaajuus {:koodiUri "opintojenlaajuus_01",
                                   :nimi {:fi "opintojenlaajuus_01 nimi fi",
                                          :sv "opintojenlaajuus_01 nimi sv"}},
                :koulutusOid "1.2.246.562.13.000002"
                :nimi {:fi "Hevosalan koulutus fi",
                       :sv "Hevosalan koulutus sv"},
                :kunnat [ {:koodiUri "kunta_091",
                           :nimi {:fi "kunta_091 nimi fi",
                                  :sv "kunta_091 nimi sv"}} ],
                :tutkintonimikkeet [{:koodiUri "tutkintonimikkeet_01",
                                     :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                            :sv "tutkintonimikkeet_01 nimi sv"}},
                                    {:koodiUri "tutkintonimikkeet_02",
                                     :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                            :sv "tutkintonimikkeet_02 nimi sv"}} ],
                :koulutustyyppi "amm"} (first (:hits r))))))))