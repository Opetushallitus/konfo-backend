(ns konfo-backend.search.koulutus-jarjestajat-search-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [starts-with?]]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn jarjestajat-search-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/search/koulutus/" oid "/jarjestajat") query-params))

(defn search
  [oid & query-params]
  (get-ok (apply jarjestajat-search-url oid query-params)))

(defn ->bad-request-body
  [oid & query-params]
  (:body (get-bad-request (apply jarjestajat-search-url oid query-params))))

(def traktoriala-oid "1.2.246.562.13.000010")
(def hevosala-oid "1.2.246.562.13.000011")
(def puutarha-koulutus-oid "1.2.246.562.13.000049")
(def tuva-erityisopetuksena-oid "1.2.246.562.13.000050")

(def ponikoulu-oid  "1.2.246.562.17.000010")
(def valtrakoulu-oid "1.2.246.562.17.000011")
(def massikkakoulu-oid  "1.2.246.562.17.000012")
(def puutarha-ala-toteutus-erityisopetuksena-oid "1.2.246.562.17.000030")
(def tuva-toteutus-erityisopetuksena-oid "1.2.246.562.17.000031")

(def haku-oid "1.2.246.562.29.0000001")
(def hakukohde-oid "1.2.246.562.20.0000008")

(def sorakuvaus-id "2ff6700d-087f-4dbf-9e42-7f38948f227a")

(deftest koulutus-jarjestajat-test
  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Search koulutuksen järjestäjät with bad requests:"
      (testing "Invalid lng"
        (is (starts-with? (->bad-request-body traktoriala-oid :lng "foo") "Virheellinen kieli")))
      (testing "Invalid order"
        (is (starts-with? (->bad-request-body traktoriala-oid :order "foo") "Virheellinen järjestys"))))

    (testing "Sorting and paging järjestäjät"
      (testing "asc order"
        (let [r (search traktoriala-oid :tuleva false :order "asc")]
          (is (= 2 (:total r)))
          (is (= massikkakoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= valtrakoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "desc order"
        (let [r (search traktoriala-oid :tuleva false :order "desc")]
          (is (= 2 (:total r)))
          (is (= valtrakoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= massikkakoulu-oid (:toteutusOid (second (:hits r)))))))
      (testing "paging"
        (let [r (search traktoriala-oid :tuleva false :page 2 :size 1)]
          (is (= 2 (:total r)))
          (is (= 1 (count (:hits r))))
          (is (= valtrakoulu-oid (:toteutusOid (first (:hits r))))))))

    (testing "Filtering järjestäjät"
      (testing "Can filter by sijainti"
        (let [r (search traktoriala-oid :tuleva false :order "asc" :sijainti "kunta_220")]
          (is (= 1 (:total r)))
          (is (= valtrakoulu-oid (:toteutusOid (first (:hits r)))))))
      (testing "All filterts must match"
        (let [r (search traktoriala-oid :tuleva false :order "asc" :sijainti "kunta_220" :opetuskieli "oppilaitoksenopetuskieli_03")]
          (is (= 0 (:total r)))))
      (testing "Can filter by opetuskieli"
        (let [r (search traktoriala-oid :tuleva false :order "asc" :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 1 (:total r)))
          (is (= massikkakoulu-oid (:toteutusOid (first (:hits r)))))))
      (testing "Can filter by opetustapa"
        (let [r (search traktoriala-oid :tuleva false :order "asc" :opetustapa "opetuspaikkakk_01")]
          (is (= 1 (:total r)))
          (is (= massikkakoulu-oid (:toteutusOid (first (:hits r)))))))
      (testing "returns amm perustutkinto vaativana erityisenä tukena"
        (let [r (search puutarha-koulutus-oid :tuleva false :order "asc" :amm_erityisopetus true)
              hits (:hits r)]
          (is (= 1 (:total r)))
          (is (= 1 (count hits)))
          (is (= puutarha-ala-toteutus-erityisopetuksena-oid (:toteutusOid (first hits))))))
      (testing "Can filter by tuva vaativana erityisenä tukena"
        (let [r (search tuva-erityisopetuksena-oid :tuleva false :order "asc" :tuva_erityisopetus true)
              hits (:hits r)]
          (is (= 1 (:total r)))
          (is (= 1 (count hits)))
          (is (= tuva-toteutus-erityisopetuksena-oid (:toteutusOid (first hits))))))
      )

    (testing "Filter counts"
      (testing "Without any filters"
        (let [r (search traktoriala-oid :tuleva false :order "asc")]
          (is (= 2 (:total r)))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 2 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 1 (get-in r [:filters :oppilaitos :1.2.246.562.10.000002 :count])))))
      (testing "Filtering reduces counts"
        (let [r (search traktoriala-oid :tuleva false :order "asc" :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 1 (:total r)))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 1 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count]))))))

    (testing "Get koulutuksen järjestäjät"
      (testing "no järjestäjiä"
        (let [r (search "1.2.246.562.13.000999")]
          (is (= 0 (:total r)))
          (is (= {} (get-in r [:filters :oppilaitos])))
          (is (= [] (:hits r)))))
      (testing "nykyiset"
        (let [r (search hevosala-oid :tuleva false)]
          (is (= 1 (:total r)))
          (is (match?
               {:koulutusOid "1.2.246.562.13.000011"
                ;:maksunMaara nil,
                :kuvaus {},
                :toteutusOid ponikoulu-oid,
                :opetusajat [],
                :nimi {:fi "Punkaharjun yliopisto",
                       :sv "Punkaharjun yliopisto sv"},
                :oppilaitosOid "1.2.246.562.10.000002",
                :suunniteltuKestoKuukausina 0,
                ;:maksullisuustyyppi nil,
                :kunnat [{:koodiUri "kunta_220",
                          :nimi {:fi "kunta_220 nimi fi",
                                 :sv "kunta_220 nimi sv"}}],
                :tutkintonimikkeet [{:koodiUri "tutkintonimikkeet_01",
                                     :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                            :sv "tutkintonimikkeet_01 nimi sv"}},
                                    {:koodiUri "tutkintonimikkeet_02",
                                     :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                            :sv "tutkintonimikkeet_02 nimi sv"}}],
                ;:jarjestetaanErityisopetuksena nil,
                :ammatillinenPerustutkintoErityisopetuksena false,
                :jarjestaaUrheilijanAmmKoulutusta true,
                ;:oppilaitosTila nil,
                :opetuskielet ["oppilaitoksenopetuskieli_02"]
                :toteutusNimi {:fi "Ponikoulu fi", :sv "Ponikoulu sv"},
                :koulutustyyppi "amm"
                :hakuAuki true}
               (first (:hits r))))))

      (testing "tulevat"
        (let [r (search hevosala-oid :tuleva true)]
          (is (= 1 (:total r)))
          (is (match?
               {:koulutusOid "1.2.246.562.13.000011"
                :oppilaitosOid "1.2.246.562.10.39218317368",
                :nimi {:fi "Helsingin yliopisto",
                       :sv "Helsingfors universitet"
                       :en "University of Helsinki"},
                  ;:oppilaitosTila nil,
                :koulutustyyppi "yo",
                  ;:opetuskielet [],
                :kunnat [{:koodiUri "kunta_091",
                          :nimi {:fi "kunta_091 nimi fi",
                                 :sv "kunta_091 nimi sv"}}],
                :hakuAuki false
                :kuvaus {}}
               (first (:hits r)))))))))

(def traktoriala-oid2 "1.2.246.562.13.000012")
(deftest koulutus-jarjestajat-test-no-jarjestajia
  (testing "Get koulutuksen järjestäjät"
    (testing "no järjestäjiä"
      (let [r (search traktoriala-oid2)]
        (is (= 0 (:total r)))
        (is (= [] (:hits r)))))))
