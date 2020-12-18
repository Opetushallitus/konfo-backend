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

(def autoala-oid "1.2.246.562.13.000001")
(def hevosala-oid "1.2.246.562.13.000002")

(def ponikoulu-oid "1.2.246.562.17.000001")
(def mersukoulu-oid "1.2.246.562.17.000002")
(def audikoulu-oid "1.2.246.562.17.000003")

(deftest oppilaitos-tarjonta-test
  (fixture/add-koulutus-mock autoala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto)
  (fixture/add-koulutus-mock hevosala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto)
  (fixture/add-toteutus-mock ponikoulu-oid hevosala-oid :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock mersukoulu-oid autoala-oid :tila "julkaistu" :nimi "Mersukoulutus" :tarjoajat punkaharjun-toimipiste-2 :metadata amk-toteutus-metatieto)
  (fixture/add-toteutus-mock audikoulu-oid autoala-oid :tila "julkaistu" :nimi "Audikoulutus" :tarjoajat helsingin-toimipiste :metadata toteutus-metatieto :teemakuva "https://example.com/kuva.jpg")

  (fixture/index-oids-without-related-indices {:koulutukset [autoala-oid hevosala-oid] :oppilaitokset [punkaharjun-yliopisto, helsingin-yliopisto]} orgs)

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto mock-get-koodisto]
    (testing "Search oppilaitoksen tarjonta with bad requests:"
      (testing "Invalid lng"
        (is (= "Virheellinen kieli") (->bad-request-body punkaharjun-yliopisto :lng "foo")))
      (testing "Invalid order"
        (is (= "Virheellinen järjestys") (->bad-request-body punkaharjun-yliopisto :order "foo"))))

    (testing "Sorting and paging tarjonta"
      (testing "asc order"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc")]
          (is (= 2 (:total r)))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= ponikoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "desc order"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "desc")]
          (is (= 2 (:total r)))
          (is (= ponikoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= mersukoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "paging"
        (let [r (search punkaharjun-yliopisto :tuleva false :page 2 :size 1)]
          (is (= 2 (:total r)))
          (is (= 1 (count (:hits r))))
          (is (= ponikoulu-oid (:toteutusOid (first (:hits r))))))))

    (testing "Filtering tarjonta"
      (testing "Can filter by sijainti"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :sijainti "kunta_220")]
          (is (= 2 (:total r)))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= ponikoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "Can filter by sijainti, no match"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :sijainti "kunta_618")]
          (is (= 0 (:total r)))))
      (testing "Can filter by koulutustyyppi"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :koulutustyyppi "yo")]
          (clojure.pprint/pprint r)
          (is (= 0 (:total r)))))
      (testing "All filterts must match"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :sijainti "kunta_220" :koulutustyyppi "yo")]
          (is (= 0 (:total r)))))
      (testing "Can filter by opetuskieli"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 1 (:total r)))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r)))))))
      (testing "Can filter by opetustapa"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :opetustapa "opetuspaikkakk_01")]
          (is (= 1 (:total r)))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r))))))))

    (testing "Filter counts"
      (testing "Without any filters"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc")]
          (is (= 2 (:total r)))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 2 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))
      (testing "Filetering reduces counts"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :opetustapa "opetuspaikkakk_01")]
          (is (= 1 (:total r)))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :amm-muu :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count]))))))

    (testing "Get oppilaitoksen tarjonta"
      (testing "no tarjontaa"
        (let [r (search "1.2.246.562.10.000666")]
          (is (= 0 (:total r)))
          (is (= [] (:hits r)))))
      (testing "nykyinen"
        (let [r (search helsingin-yliopisto :tuleva false)]
          (is (= 1 (:total r)))
          (is (= {:maksunMaara nil,
                  :kuvaus {},
                  :koulutusOid autoala-oid
                  :toteutusOid audikoulu-oid,
                  :opetusajat [],
                  :nimi {:fi "Audikoulutus fi",
                         :sv "Audikoulutus sv"},
                  :onkoMaksullinen false,
                  :kunnat [{:koodiUri "kunta_091",
                            :nimi {:fi "kunta_091 nimi fi",
                                   :sv "kunta_091 nimi sv"}}],
                  :tutkintonimikkeet nil,
                  :koulutustyyppi "amm",
                  :kuva "https://example.com/kuva.jpg"} (first (:hits r))))))

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
                  :opintojenLaajuusyksikko {:koodiUri "opintojenlaajuusyksikko_6",
                                            :nimi {:fi "opintojenlaajuusyksikko_6 nimi fi",
                                                   :sv "opintojenlaajuusyksikko_6 nimi sv"}},
                  :opintojenLaajuusNumero 150,
                  :opintojenLaajuus {:koodiUri "opintojenlaajuus_150",
                                     :nimi {:fi "opintojenlaajuus_150 nimi fi",
                                            :sv "opintojenlaajuus_150 nimi sv"}},
                  :koulutusOid hevosala-oid
                  :nimi {:fi "Hevosalan koulutus fi",
                         :sv "Hevosalan koulutus sv"},
                  :kuva "https://testi.fi/koulutus-teemakuva/oid/kuva.jpg",
                  :kunnat [{:koodiUri "kunta_091",
                            :nimi {:fi "kunta_091 nimi fi",
                                   :sv "kunta_091 nimi sv"}}],
                  :tutkintonimikkeet [{:koodiUri "tutkintonimikkeet_01",
                                       :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                              :sv "tutkintonimikkeet_01 nimi sv"}},
                                      {:koodiUri "tutkintonimikkeet_02",
                                       :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                              :sv "tutkintonimikkeet_02 nimi sv"}}],
                  :koulutustyyppi "amm"} (first (:hits r)))))))))

(deftest oppilaitos-tarjonta-test-no-tarjontaa
  (fixture/index-oids-without-related-indices {:oppilaitokset [punkaharjun-yliopisto]} orgs)

  (testing "Get oppilaitoksen tarjonta"
    (testing "no tarjontaa"
      (let [r (search punkaharjun-yliopisto)]
        (is (= 0 (:total r)))
        (is (= [] (:hits r)))))))
