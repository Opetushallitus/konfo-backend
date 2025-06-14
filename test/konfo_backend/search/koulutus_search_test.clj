(ns konfo-backend.search.koulutus-search-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [clojure.string :refer [starts-with?]]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-mock-data :refer :all]
            [cheshire.core :refer [generate-string]]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn koulutus-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/koulutukset" query-params))

(defn search
  [& query-params]
  (get-ok (apply koulutus-search-url query-params)))

(defn search-and-get-oids
  [& query-params]
  (vec (map :oid (:hits (apply search query-params)))))

(defn ->bad-request-body
  [& query-params]
  (:body (get-bad-request (apply koulutus-search-url query-params))))

(defn mock-get-kuvaukset
  [x]
  [{:id 1234 :suorittaneenOsaaminen {:fi "osaaminen fi" :sv "osaaminen sv"} :tyotehtavatJoissaVoiToimia {:fi "työtehtävät fi" :sv "työtehtävät sv"}}])

(def yo-oid "1.2.246.562.13.000005")
(def amk-oid "1.2.246.562.13.000006")

(def haku-oid-1 "1.2.246.562.29.0000001")
(def haku-oid-2 "1.2.246.562.29.0000002")

(deftest koulutus-search-test
  (set-fixed-time "2021-01-01T04:00:00")
  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto
                konfo-backend.index.eperuste/get-kuvaukset-by-eperuste-ids mock-get-kuvaukset
                konfo-backend.index.eperuste/get-tutkinnon-osa-kuvaukset-by-eperuste-ids mock-get-kuvaukset]
    (testing "Search koulutukset with bad requests:"
      (testing "Invalid lng"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :lng "foo") "Virheellinen kieli")))
      (testing "Invalid sort"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :order "foo") "Virheellinen järjestys")))
      (testing "Too short keyword"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :keyword "fo") "Hakusana on liian lyhyt"))))

    (testing "Search all koulutukset"
      (let [r (search :sort "name" :order "asc")]
        (is (match? 40 (:total r)))
        (is (match? 20 (count (:hits r))))
        (is (match? {:koulutustyyppi {:amm {:count 24}
                                      :amk {:count 0}
                                      :yo {:count 0}
                                      :lk {:count 2}
                                      :aikuisten-perusopetus {:count 5}
                                      :vaativan-tuen-koulutukset {:alakoodit {:koulutustyyppi_4 {:count 1}
                                                                              :tuva-erityisopetus {:count 1}}
                                                                  :count 2} }
                     :opetuskieli {:oppilaitoksenopetuskieli_01 {:count 1}
                                   :oppilaitoksenopetuskieli_02 {:count 7}}
                     :maakunta {:maakunta_01 {:count 39}
                                :maakunta_02 {:count 0}}
                     :opetustapa {:opetuspaikkakk_01 {:count 1}
                                  :opetuspaikkakk_02 {:count 7}}
                     :koulutusala {:kansallinenkoulutusluokitus2016koulutusalataso1_01
                                   {:count 23}
                                   :kansallinenkoulutusluokitus2016koulutusalataso1_02
                                   {:count 23}}
                     :valintatapa {:valintatapajono_av {:count 3}
                                   :valintatapajono_tv {:count 3}}
                     :hakukaynnissa {:count 1}
                     :hakutapa {:hakutapa_01 {:count 2}
                                :hakutapa_03 {:count 3}}
                     :pohjakoulutusvaatimus {:pohjakoulutusvaatimuskonfo_01 {:count 0}
                                             :pohjakoulutusvaatimuskonfo_am {:count 4}}
                     :koulutuksenkestokuukausina {:count 23, :max 46.0}
                     :maksullisuus {:maksullisuustyyppi
                                    {:maksuton {:count 2}
                                     :maksullinen {:count 5}
                                     :lukuvuosimaksu {:count 2}}
                                    :maksunmaara {:count 5
                                                  :max 200.5}
                                    :lukuvuosimaksunmaara {:count 2
                                                           :max 500.0}
                                    :apuraha {:count 2}}}
                    (:filters r)))))

    (testing "Search koulutukset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_618" :sort "name" :order "asc")]
          (is (= 18 (count (:hits r))))
          (is (= "1.2.246.562.13.000020" (:oid (first (:hits r)))))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi]))))))

    (testing "multiple sijainti"
      (let [r (search :sijainti " kunta_618 , kunta_220" :sort "name" :order "asc")]
        (is (match? 21 (:total r)))
        (is (match? 20 (count (:hits r))))))

    (testing "koulutustyyppi amm-osaamisala"
      (let [r (search :koulutustyyppi "amm-osaamisala" :sort "name" :order "asc")]
        (is (match? 1 (count (:hits r))))
        (is (match? {:koulutustyyppi
                     {:amm {:count 24
                            :alakoodit {:muu-amm-tutkinto {:count 21}
                                        :amm-osaamisala {:count 1}
                                        :amm-tutkinnon-osa {:count 1}
                                        :amm-muu {:count 1}}}}}
                    (:filters r)))))

    (testing "koulutustyyppi amm-tutkinnon-osa"
      (let [r (search :koulutustyyppi "amm-tutkinnon-osa" :sort "name" :order "asc")]
        (is (match? 1 (count (:hits r))))
        (is (match? {:koulutustyyppi
                     {:amm {:count 24
                            :alakoodit {:muu-amm-tutkinto {:count 21}
                                        :amm-osaamisala {:count 1}
                                        :amm-tutkinnon-osa {:count 1}
                                        :amm-muu {:count 1}}}}}
                    (:filters r)))))

    (testing "koulutustyyppi amm-muu"
      (let [r (search :koulutustyyppi "amm-muu" :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (match? {:koulutustyyppi
                     {:amm {:count 24
                            :alakoodit {:muu-amm-tutkinto {:count 21}
                                        :amm-osaamisala {:count 1}
                                        :amm-tutkinnon-osa {:count 1}
                                        :amm-muu {:count 1}}}}}
                    (:filters r)))))

    (testing "koulutustyyppi yo"
      (let [r (search :koulutustyyppi "yo" :sort "name" :order "asc")]
        (is (= 5 (count (:hits r))))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :count])))))

    (testing "koulutustyyppi amk"
      (let [r (search :koulutustyyppi "amk" :sort "name" :order "asc")]
        (is (= 2 (count (:hits r))))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :count])))))

    (testing "koulutustyyppi lukio"
      (let [r (search :koulutustyyppi "lk" :sort "name" :order "asc")]
        (is (= 2 (count (:hits r))))
        (is (= 2 (get-in r [:filters :koulutustyyppi :lk :count])))))

    (testing "koulutustyyppi osaamismerkki"
      (let [r (search :koulutustyyppi "vapaa-sivistystyo-osaamismerkki" :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :vapaa-sivistystyo :alakoodit :vapaa-sivistystyo-osaamismerkki :count])))))

    (testing "koulutustyyppi vaativan erityisen tuen koulutukset"
      (let [r (search :koulutustyyppi "koulutustyyppi_4,tuva-erityisopetus,vaativan-tuen-koulutukset" :sort "name" :order "asc")
            hits (:hits r) ]
        (is (= 2 (count hits)))
        (is (= "1.2.246.562.13.000049" (:oid (first hits))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :vaativan-tuen-koulutukset :alakoodit :tuva-erityisopetus :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi :vaativan-tuen-koulutukset :alakoodit :koulutustyyppi_4 :count])))))

    (testing "opetuskieli"
      (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01" :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "koulutusala"
      (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01" :sort "name" :order "asc")]
        (is (= 20 (count (:hits r))))))

    (testing "opetustapa"
      (let [r (search :opetustapa "opetuspaikkakk_02" :sort "name" :order "asc")]
        (is (= 7 (count (:hits r))))
        (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 5 (get-in r [:filters :koulutustyyppi :aikuisten-perusopetus :count])))))

    (testing "valintatapa"
      (let [r (search :valintatapa "valintatapajono_av" :sort "name" :order "asc")]
        (is (= 3 (count (:hits r))))
        (is (= 3 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "hakukaynnissa"
      (let [r (search :hakukaynnissa true :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "hakutapa"
      (let [r (search :hakutapa "hakutapa_03" :sort "name" :order "asc")]
        (is (= 3 (count (:hits r))))
        (is (= 3 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "hakukäynnissä ja hakutapa_01"
      (let [r (search :hakukaynnissa true :hakutapa "hakutapa_01" :sort "name" :order "asc")]
        (is (= 0 (count (:hits r))))))

    (testing "hakukäynnissä ja hakutapa_03"
      (let [r (search :hakukaynnissa true :hakutapa "hakutapa_03" :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))))

    (testing "yhteishaku"
      (let [r (search :yhteishaku haku-oid-2 :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "yhteishaku-no-results"
      (let [r (search :yhteishaku haku-oid-1 :sort "name" :order "asc")]
        (is (= 0 (count (:hits r))))))

    (testing "pohjakoulutusvaatimus"
      (let [r (search :pohjakoulutusvaatimus "pohjakoulutusvaatimuskonfo_am" :sort "name" :order "asc")]
        (is (= 4 (count (:hits r))))
        (is (= 3 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "Search koulutukset, get correct result"
      (let [r (search :sijainti "kunta_220" :koulutustyyppi "amm" :sort "name" :order "asc")]
        (is (match? {:opintojenLaajuusyksikko {:koodiUri   "opintojenlaajuusyksikko_6",
                                               :nimi  {:fi "opintojenlaajuusyksikko_6 nimi fi",
                                                       :sv "opintojenlaajuusyksikko_6 nimi sv"}},
                     :kuvaus {:fi "osaaminen fi" :sv "osaaminen sv"},
                     :teemakuva "https://testi.fi/koulutus-teemakuva/oid/kuva.jpg",
                     :nimi {:fi "Hevosalan koulutus fi",
                            :sv "Hevosalan koulutus sv"},
                     :oid "1.2.246.562.13.000011",
                     :opintojenLaajuusNumeroMin nil,
                     :toteutustenTarjoajat {:nimi {:fi "Punkaharjun yliopisto",
                                                   :sv "Punkaharjun yliopisto sv"}, :count 1}
                     :opintojenLaajuusNumeroMax nil,
                     :kielivalinta ["fi", "sv"],
                     :koulutukset [{:koodiUri  "koulutus_371101#1",
                                    :nimi {:fi "koulutus_371101#1 nimi fi",
                                           :sv "koulutus_371101#1 nimi sv"}}],
                     :tutkintonimikkeet [{:koodiUri  "tutkintonimikkeet_01",
                                          :nimi {:fi "tutkintonimikkeet_01 nimi fi",
                                                 :sv "tutkintonimikkeet_01 nimi sv"}},
                                         {:koodiUri  "tutkintonimikkeet_02",
                                          :nimi {:fi "tutkintonimikkeet_02 nimi fi",
                                                 :sv "tutkintonimikkeet_02 nimi sv"}}],
                     :opintojenLaajuusNumero 150,
                     :isAvoinKorkeakoulutus nil,
                     :opintojenLaajuus {:koodiUri  "opintojenlaajuus_150",
                                        :nimi {:fi "opintojenlaajuus_150 nimi fi",
                                               :sv "opintojenlaajuus_150 nimi sv"}},
                     :eperuste 1234,
                     :koulutustyyppi "amm"} (dissoc (first (:hits r)) :_score)))))))

(def aakkostus-koulutus-oid1 "1.2.246.562.13.000020")
(def aakkostus-koulutus-oid2 "1.2.246.562.13.000021")
(def aakkostus-koulutus-oid3 "1.2.246.562.13.000022")
(def aakkostus-koulutus-oid4 "1.2.246.562.13.000023")
(def aakkostus-koulutus-oid5 "1.2.246.562.13.000024")

(deftest koulutus-paging-and-sorting-test

  (testing "Koulutus search ordering"
    (testing "by default order"
      (let [hits (:hits (search :keyword "aakkosissa"))]
        (is (= 5 (count hits)))
        (is (>= (:_score (nth hits 0))
                (:_score (nth hits 1))
                (:_score (nth hits 2))
                (:_score (nth hits 3))
                (:_score (nth hits 4))))))
    (testing "order by name asc"
      (is (= [aakkostus-koulutus-oid1 aakkostus-koulutus-oid2 aakkostus-koulutus-oid3 aakkostus-koulutus-oid4 aakkostus-koulutus-oid5]
             (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc"))))
    (testing "order by name desc"
      (is (= [aakkostus-koulutus-oid5 aakkostus-koulutus-oid4 aakkostus-koulutus-oid3 aakkostus-koulutus-oid2 aakkostus-koulutus-oid1]
             (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "desc")))))

  (testing "Koulutus search paging"
    (testing "returns first page by default"
      (is (= [aakkostus-koulutus-oid1 aakkostus-koulutus-oid2] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc" :size 2))))
    (testing "returns last page"
      (is (= [aakkostus-koulutus-oid5] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc" :size 2 :page 3))))
    (testing "returns correct total count"
      (is (= 5 (:total (get-ok (koulutus-search-url :keyword "aakkosissa" :sort "name" :order "asc" :size 2))))))))

(def keyword-koulutus-oid1 "1.2.246.562.13.000030")
(def keyword-koulutus-oid2 "1.2.246.562.13.000031")
(def keyword-koulutus-oid3 "1.2.246.562.13.000032")
(def keyword-koulutus-oid4 "1.2.246.562.13.000033")
(def keyword-koulutus-oid5 "1.2.246.562.13.000034")
(def keyword-koulutus-oid6 "1.2.246.562.13.000035")
(def keyword-koulutus-oid7 "1.2.246.562.13.000036")
(def keyword-koulutus-oid8 "1.2.246.562.13.000037")
(def keyword-koulutus-oid9 "1.2.246.562.13.000038")
(def keyword-koulutus-oid10 "1.2.246.562.13.000039")
(def keyword-koulutus-oid11 "1.2.246.562.13.000040")
(def keyword-koulutus-oid12 "1.2.246.562.13.000041")
(def keyword-koulutus-oid13 "1.2.246.562.13.000042")
(def keyword-koulutus-oid14 "1.2.246.562.13.000043")
(def keyword-koulutus-oid15 "1.2.246.562.13.000044")
(def keyword-koulutus-oid16 "1.2.246.562.13.000045")
(def keyword-koulutus-oid17 "1.2.246.562.13.000046")
(def keyword-koulutus-oid18 "1.2.246.562.13.000047")
;(def traktorialan-koulutus-oid "1.2.246.562.13.000010")
;(def hevosalan-koulutus-oid "1.2.246.562.13.000011")
;(def hevosalan-osaamisala-oid "1.2.246.562.13.000014")

(deftest koulutus-keyword-search
  (with-redefs [konfo-backend.index.eperuste/get-tutkinnon-osa-kuvaukset-by-eperuste-ids mock-get-kuvaukset]

    (testing "Searching with keyword"

      (testing "maalari <-> Pintakäsittelyala (EI maanmittausala)"
        (is (= [keyword-koulutus-oid12 keyword-koulutus-oid8] (search-and-get-oids :sort "name" :order "asc" :keyword "maalari"))))

      (testing "puhtaus <-> Puhtaus- ja kiinteistöpalveluala (EI puhevammaisten)"
        (is (= [keyword-koulutus-oid9] (search-and-get-oids :sort "name" :order "asc" :keyword "puhtaus"))))

      (testing "palvelu <-> Puhtaus- ja kiinteistöpalveluala"
        (is (= [keyword-koulutus-oid9] (search-and-get-oids :sort "name" :order "asc" :keyword "palvelu"))))

      (testing "ammattitutkinto <-> EI ammattioppilaitos tai ammattikorkeakoulu"
        (is (= [keyword-koulutus-oid14 keyword-koulutus-oid10 keyword-koulutus-oid9 keyword-koulutus-oid15] (search-and-get-oids :sort "name" :order "asc" :keyword "ammattitutkinto"))))

      (testing "terveys <-> sosiaali- ja terveysala"
        (is (= [keyword-koulutus-oid6] (search-and-get-oids :sort "name" :order "asc" :keyword "terveys"))))

      (testing "auto <-> automaatiotekniikka/automaatioinsinööri"
        (is (= [keyword-koulutus-oid12 keyword-koulutus-oid4] (search-and-get-oids :sort "name" :order "asc" :keyword "auto"))))

      (testing "automaatio <-> automaatiotekniikka/automaatioinsinööri, EI autoalan perustutkintoa"
        (is (= [keyword-koulutus-oid4] (search-and-get-oids :sort "name" :order "asc" :keyword "automaatio"))))

      (testing "humanisti <-> humanistinen"
        (is (= [keyword-koulutus-oid2] (search-and-get-oids :sort "name" :order "asc" :keyword "humanisti"))))

      (testing "lääkäri <-> lääketieteen"
        (is (= [keyword-koulutus-oid1] (search-and-get-oids :sort "name" :order "asc" :keyword "lääkäri"))))

      (testing "muusikko <-> muusikon koulutus"
        (is (= [keyword-koulutus-oid5] (search-and-get-oids :sort "name" :order "asc" :keyword "muusikko"))))

      (testing "tekniikka <-> automaatiotekniikka"
        (is (= [keyword-koulutus-oid4 keyword-koulutus-oid15] (search-and-get-oids :sort "name" :order "asc" :keyword "tekniikka"))))

      (testing "muusikon koulutus <-> EI muita koulutuksia"
        (is (= [keyword-koulutus-oid5] (search-and-get-oids :sort "name" :order "asc" :keyword "muusikon koulutus"))))

      (testing "Maanmittausalan perustutkinto <-> EI muita perustutkintoja"
        (is (= [keyword-koulutus-oid7] (search-and-get-oids :sort "name" :order "asc" :keyword "maanmittausalan perustutkinto"))))

      (testing "perustutkinto maanmittaus <-> EI muita perustutkintoja"
        (is (= [keyword-koulutus-oid7] (search-and-get-oids :sort "name" :order "asc" :keyword "perustutkinto maanmittaus"))))

      (testing "Maanmittaus perus <-> maanmittausalan perustutkinto"
        (is (= [keyword-koulutus-oid7] (search-and-get-oids :sort "name" :order "asc" :keyword "maanmittauS peruS"))))

      (testing "tietojenkäsittely <-> tietojenkäsittelytieteen"
        (is (= [keyword-koulutus-oid3] (search-and-get-oids :sort "name" :order "asc" :keyword "tietojenkäsittely"))))

      (testing "hius <-> Hius- ja kauneudenhoitoalan perustutkinto"
        (is (= [keyword-koulutus-oid11] (search-and-get-oids :sort "name" :order "asc" :keyword "hius"))))

      (testing "kauneudenhoito <-> Hius- ja kauneudenhoitoalan perustutkinto"
        (is (= [keyword-koulutus-oid11] (search-and-get-oids :sort "name" :order "asc" :keyword "kauneudenhoito"))))

      (testing "psykologia <-> Psykologi"
        (is (= [keyword-koulutus-oid2] (search-and-get-oids :sort "name" :order "asc" :keyword "psykologia"))))

      (testing "lääke <-> lääketieteen"
        (is (= [keyword-koulutus-oid1] (search-and-get-oids :sort "name" :order "asc" :keyword "lääke"))))

      (testing "amk <-> ylempi (AMK)"
        (is (= [keyword-koulutus-oid4] (search-and-get-oids :sort "name" :order "asc" :keyword "amk"))))

      (testing "psykologi <-> psykologia"
        (is (= [keyword-koulutus-oid2] (search-and-get-oids :sort "name" :order "asc" :keyword "psykologi"))))

      (testing "perus <-> kaikki perustutkinnot"
        (is (= [keyword-koulutus-oid12 keyword-koulutus-oid13 keyword-koulutus-oid17 keyword-koulutus-oid11 keyword-koulutus-oid7 keyword-koulutus-oid18 keyword-koulutus-oid8 keyword-koulutus-oid6 keyword-koulutus-oid16] (search-and-get-oids :sort "name" :order "asc" :keyword "perus"))))

      (testing "perustutkinto <-> kaikki perustutkinnot"
        (is (= [keyword-koulutus-oid12 keyword-koulutus-oid13 keyword-koulutus-oid17 keyword-koulutus-oid11 keyword-koulutus-oid7 keyword-koulutus-oid18 keyword-koulutus-oid8 keyword-koulutus-oid6 keyword-koulutus-oid16] (search-and-get-oids :sort "name" :order "asc" :keyword "perustutkinto"))))

      (testing "eläin <-> eläintenhoito EI elintarviketta"
        (is (= [keyword-koulutus-oid14] (search-and-get-oids :sort "name" :order "asc" :keyword "eläin"))))

      (testing "eläinten <-> eläintenhoito EI tieto- ja viestintätekniikan"
        (is (= [keyword-koulutus-oid14] (search-and-get-oids :sort "name" :order "asc" :keyword "eläinten"))))

      (testing "merimies <-> merimies EI esimiestä"
        (is (= [keyword-koulutus-oid8] (search-and-get-oids :sort "name" :order "asc" :keyword "merimies"))))

      (testing "sosiaali <-> ei tanssialaa"
        (is (= [keyword-koulutus-oid6] (search-and-get-oids :sort "name" :order "asc" :keyword "sosiaali"))))

      (testing "ensihoitaja <-> ei eläinten- eikä hevostenhoitajaa"
        (is (= [] (search-and-get-oids :sort "name" :order "asc" :keyword "ensihoitaja"))))

      (testing "seppä <-> seppä"
        (is (= [keyword-koulutus-oid17] (search-and-get-oids :sort "name" :order "asc" :keyword "seppä"))))

      (testing "kunta_220 nimi"
        ; Ennen kunta-tiedon lisäämistä keyword-hakuun löydettiin vain yksi. Koska dumpeissa on nyt kuntien nimet tyyliin "kunta_220 nimi fi"
        ; on hyvin vaikea tehdä hakua, joka kohdistuisi juuri siihen kenttään ("kunta_220" pilkotaan osiin "kunta" ja "220", joista "220" ilmeisesti hylätään)
        ; TODO: Lisätään dumppeihin kuntien oikeat nimet, jotta voidaan tehdä realistisempia testihakuja
        (is (= 20 (count (search-and-get-oids :sort "name" :order "asc" :keyword "kunta_220"))))))))
