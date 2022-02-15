(ns konfo-backend.search.oppilaitos-tarjonta-search-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [starts-with?]]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn jarjestajat-search-url
  [oid & query-params]
  (apply url-with-query-params (str "/konfo-backend/search/oppilaitos/" oid "/tarjonta") query-params))

(defn search
  [oid & query-params]
  (get-ok (apply jarjestajat-search-url oid query-params)))

(defn ->bad-request-body
  [oid & query-params]
  (:body (get-bad-request (apply jarjestajat-search-url oid query-params))))

(def punkaharjun-yliopisto "1.2.246.562.10.000002")
(def helsingin-yliopisto      "1.2.246.562.10.000005")

(def traktoriala-oid "1.2.246.562.13.000010")

(def ponikoulu-oid "1.2.246.562.17.000010")
(def valtrakoulu-oid "1.2.246.562.17.000011")
(def massikkakoulu-oid "1.2.246.562.17.000012")
(def poniosatoteutus-oid "1.2.246.562.17.000014")

(deftest oppilaitos-tarjonta-test
  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Search oppilaitoksen tarjonta with bad requests:"
      (testing "Invalid lng"
        (is (starts-with? (->bad-request-body punkaharjun-yliopisto :lng "foo") "Virheellinen kieli" )))
      (testing "Invalid order"
        (is (starts-with? (->bad-request-body punkaharjun-yliopisto :order "foo") "Virheellinen järjestys"))))

    (testing "Sorting and paging tarjonta"
      (testing "asc order"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc")]
          (is (= 3 (:total r)))
          (is (= ponikoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= poniosatoteutus-oid (:toteutusOid (second (:hits r)))))
          (is (= valtrakoulu-oid (:toteutusOid (last (:hits r)))))))
      (testing "desc order"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "desc")]
          (is (= 3 (:total r)))
          (is (= valtrakoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= poniosatoteutus-oid (:toteutusOid (second (:hits r)))))
          (is (= ponikoulu-oid (:toteutusOid (last (:hits r)))))))
      (testing "paging"
        (let [r (search punkaharjun-yliopisto :tuleva false :page 2 :size 1)]
          (is (= 3 (:total r)))
          (is (= 1 (count (:hits r))))
          (is (= poniosatoteutus-oid (:toteutusOid (first (:hits r))))))))

    (testing "Filtering tarjonta"
      (testing "Can filter by sijainti"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :sijainti "kunta_091")]
          (is (= 3 (:total r)))
          (is (= ponikoulu-oid (:toteutusOid (first (:hits r)))))
          (is (= poniosatoteutus-oid (:toteutusOid (second (:hits r)))))
          (is (= valtrakoulu-oid (:toteutusOid (last (:hits r)))))))
      (testing "Can filter by sijainti, no match"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :sijainti "kunta_618")]
          (is (= 0 (:total r)))))
      (testing "Can filter by koulutustyyppi"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :koulutustyyppi "yo")]
          (is (= 0 (:total r)))))
      (testing "All filterts must match"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :sijainti "kunta_220" :koulutustyyppi "yo")]
          (is (= 0 (:total r)))))
      (testing "Can filter by opetuskieli"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :opetuskieli "oppilaitoksenopetuskieli_01")]
          (is (= 0 (:total r)))))
      (testing "Can filter by opetustapa"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :opetustapa "opetuspaikkakk_01")]
          (is (= 0 (:total r))))))

    (testing "Filter counts"
      (testing "Without any filters"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc")]
          (is (= 3 (:total r)))
          (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
          (is (= 2 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 2 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
          (is (= 3 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
          (is (= 3 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))
      (testing "Filtering reduces counts"
        (let [r (search punkaharjun-yliopisto :tuleva false :order "asc" :opetustapa "opetuspaikkakk_02")]
          (is (= 2 (:total r)))
          (is (= 0 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
          (is (= 2 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count]))))))

    (testing "Get oppilaitoksen tarjonta"
      (testing "no tarjontaa"
        (let [r (search "1.2.246.562.10.000666")]
          (is (= 0 (:total r)))
          (is (= [] (:hits r)))))
      (testing "nykyinen"
        (let [r (search helsingin-yliopisto :tuleva false)]
          (is (= 1 (:total r)))
          (is (= {:oppilaitosOid     "1.2.246.562.10.000005"
                  ;:maksunMaara nil,
                  :kuvaus            {},
                  :koulutusOid       traktoriala-oid
                  :toteutusOid       massikkakoulu-oid,
                  :nimi              {:fi "Massikkakoulutus fi",
                         :sv "Massikkakoulutus sv"},
                  ;:maksullisuustyyppi nil,
                  :kunnat            [],
                  :tutkintonimikkeet [],
                  :opetuskielet      ["oppilaitoksenopetuskieli_01"],
                  :koulutustyyppi    "amm",
                  :kuva              "https://testi.fi/toteutus-teemakuva/oid/kuva.jpg"
                  :hakukaynnissa     nil
                  :toteutusNimi      {:fi "Massikkakoulutus fi",
                                 :sv "Massikkakoulutus sv"}} (first (:hits r)))))))))

(deftest oppilaitos-tarjonta-test-no-tarjontaa
  (testing "Get oppilaitoksen tarjonta"
    (testing "no tarjontaa"
      (let [r (search "1.2.246.562.10.0000015")]
        (is (= 0 (:total r)))
        (is (= [] (:hits r)))))))
