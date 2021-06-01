(ns konfo-backend.search.koulutus-jarjestajat-search-test
  (:require [clojure.test :refer :all]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :each fixture/mock-indexing-fixture)

(defn jarjestajat-search-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/search/koulutus/" oid "/jarjestajat") query-params))

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

(def haku-oid "1.2.246.562.29.00000000000000000001")
(def hakukohde-oid "1.2.246.562.20.00000000000000000001")

(def sorakuvaus-id "2ff6700d-087f-4dbf-9e42-7f38948f227a")

(deftest koulutus-jarjestajat-test
  (fixture/add-koulutus-mock autoala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-koulutus-mock hevosala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :tarjoajat (str punkaharjun-yliopisto "," helsingin-yliopisto) :metadata koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-toteutus-mock ponikoulu-oid hevosala-oid :tila "julkaistu" :nimi "Ponikoulu" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock mersukoulu-oid autoala-oid :tila "julkaistu" :nimi "Mersukoulutus" :tarjoajat punkaharjun-toimipiste-2 :metadata toteutus-metatieto)
  (fixture/add-toteutus-mock audikoulu-oid autoala-oid :tila "julkaistu" :nimi "Audikoulutus" :tarjoajat helsingin-toimipiste :metadata amk-toteutus-metatieto)

  (fixture/add-sorakuvaus-mock sorakuvaus-id :tila "julkaistu")

  (fixture/add-haku-mock haku-oid :tila "julkaistu" :nimi "Hevoshaku" :muokkaaja "1.2.246.562.24.62301161440")
  (fixture/add-hakukohde-mock hakukohde-oid ponikoulu-oid haku-oid :tila "julkaistu" :nimi "ponikoulun hakukohde" :muokkaaja "1.2.246.562.24.62301161440" :hakuaikaAlkaa "2000-01-01T00:00")

  (fixture/index-oids-without-related-indices {:koulutukset [autoala-oid hevosala-oid] :oppilaitokset [punkaharjun-yliopisto, helsingin-yliopisto]} orgs)

  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto mock-get-koodisto]
    (testing "Search koulutuksen järjestäjät with bad requests:"
      (testing "Invalid lng"
        (is (= "Virheellinen kieli") (->bad-request-body autoala-oid :lng "foo")))
      (testing "Invalid order"
        (is (= "Virheellinen järjestys") (->bad-request-body autoala-oid :order "foo"))))

    (testing "Sorting and paging järjestäjät"
      (testing "asc order"
        (let [r (search autoala-oid :tuleva false :order "asc")]
          (is (= 2 (:total r)))
          (is (= audikoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= mersukoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "desc order"
        (let [r (search autoala-oid :tuleva false :order "desc")]
          (is (= 2 (:total r)))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= audikoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "paging"
        (let [r (search autoala-oid :tuleva false :page 2 :size 1)]
          (is (= 2 (:total r)))
          (is (= 1 (count (:hits r))))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r))))))))

    (testing "Filtering järjestäjät"
      (testing "Can filter by sijainti"
        (let [r (search autoala-oid :tuleva false :order "asc" :sijainti "kunta_220")]
          (is (= 1 (:total r)))
          (is (= mersukoulu-oid (:toteutusOid (first (:hits r)))))))
      (testing "All filterts must match"
        (let [r (search autoala-oid :tuleva false :order "asc" :sijainti "kunta_220" :opetuskieli "oppilaitoksenopetuskieli_03")]
          (is (= 0 (:total r)))))
      (testing "Can filter by opetuskieli"
        (let [r (search autoala-oid :tuleva false :order "asc" :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 1 (:total r)))
          (is (= audikoulu-oid (:toteutusOid (first (:hits r)))))))
      (testing "Can filter by opetustapa"
        (let [r (search autoala-oid :tuleva false :order "asc" :opetustapa "opetuspaikkakk_01")]
          (is (= 1 (:total r)))
          (is (= audikoulu-oid (:toteutusOid (first (:hits r))))))))

    (testing "Filter counts"
      (testing "Without any filters"
        (let [r (search autoala-oid :tuleva false :order "asc")]
          (is (= 2 (:total r)))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))))
      (testing "Filtering reduces counts"
        (let [r (search autoala-oid :tuleva false :order "asc" :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 1 (:total r)))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count]))))))

    (testing "Get koulutuksen järjestäjät"
      (testing "no järjestäjiä"
        (let [r (search "1.2.246.562.13.000009")]
          (is (= 0 (:total r)))
          (is (= [] (:hits r)))))
      (testing "nykyiset"
        (let [r (search hevosala-oid :tuleva false)]
          (is (= 1 (:total r)))
          (is (= {:maksunMaara nil,
                  :kuvaus {},
                  :toteutusOid ponikoulu-oid,
                  :opetusajat [],
                  :nimi {:fi "Punkaharjun yliopisto",
                         :sv "Punkaharjun yliopisto sv"},
                  :oppilaitosOid "1.2.246.562.10.000002",
                  :maksullisuustyyppi nil,
                  :kunnat [{:koodiUri "kunta_220",
                            :nimi {:fi "kunta_220 nimi fi",
                                   :sv "kunta_220 nimi sv"}}],
                  :tutkintonimikkeet [{:koodiUri "tutkintonimikkeet_01",
                                       :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                              :sv "tutkintonimikkeet_01 nimi sv"}},
                                      {:koodiUri "tutkintonimikkeet_02",
                                       :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                              :sv "tutkintonimikkeet_02 nimi sv"}}],
                  :oppilaitosTila nil,
                  :opetuskielet ["oppilaitoksenopetuskieli_02"]
                  :toteutusNimi {:fi "Ponikoulu fi", :sv "Ponikoulu sv"},
                  :koulutustyyppi "yo"
                  :hakukaynnissa true} (first (:hits r))))))

      (testing "tulevat"
        (let [r (search hevosala-oid :tuleva true)]
          (is (= 1 (:total r)))
          (is (= {:oppilaitosOid "1.2.246.562.10.000005",
                  :nimi {:fi "Helsingin yliopisto",
                         :sv "Helsingin yliopisto sv"},
                  :oppilaitosTila nil,
                  :koulutustyyppi "yo",
                  :opetuskielet [],
                  :kunnat [{:koodiUri "kunta_091",
                            :nimi {:fi "kunta_091 nimi fi",
                                   :sv "kunta_091 nimi sv"}}],
                  :kuvaus {}
                  :hakukaynnissa nil} (first (:hits r)))))))))

(deftest koulutus-jarjestajat-test-no-jarjestajia
  (fixture/add-koulutus-mock autoala-oid :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :tarjoajat "" :organisaatio "1.2.246.562.10.00000000001" :metadata koulutus-metatieto :sorakuvausId sorakuvaus-id)
  (fixture/add-sorakuvaus-mock sorakuvaus-id :tila "julkaistu")

  (fixture/index-oids-without-related-indices {:koulutukset [autoala-oid]}, orgs)

  (testing "Get koulutuksen järjestäjät"
    (testing "no järjestäjiä"
      (let [r (search autoala-oid)]
        (is (= 0 (:total r)))
        (is (= [] (:hits r)))))))
