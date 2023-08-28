(ns konfo-backend.search.oppilaitos-search-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [starts-with?]]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.tools :refer [debug-pretty]]
            [cheshire.core :as cheshire]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn oppilaitos-search-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/oppilaitokset" query-params))

(defn search
  [& query-params]
  (get-ok (apply oppilaitos-search-url query-params)))

(defn ->bad-request-body
  [& query-params]
  (:body (get-bad-request (apply oppilaitos-search-url query-params))))

(defn search-and-get-oids
  [& query-params]
  (vec (map :oid (:hits (apply search query-params)))))

(def sorakuvaus-id "2ff6700d-087f-4dbf-9e42-7f38948f227a")
(def punkaharjun-yliopisto "1.2.246.562.10.000002")

(deftest oppilaitos-search-test
  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Search oppilaitokset with bad requests:"
      (testing "Invalid lng"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :lng "foo") "Virheellinen kieli")))
      (testing "Invalid sort"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :sort "foo") "Virheellinen järjestys")))
      (testing "Too short keyword"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :keyword "fo") "Hakusana on liian lyhyt"))))

    (testing "Search all oppilaitokset"
      (let [r (search :sort "name" :order "asc")]
        (is (= 13 (count (:hits r))))
        (is (= 4 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-muu :count])))
        (is (= 5 (get-in r [:filters :koulutustyyppi-muu :aikuisten-perusopetus :count])))
        (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 6 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
        (is (= 11 (get-in r [:filters :maakunta :maakunta_01 :count])))
        (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
        (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
        (is (= 6 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
        (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

    (testing "Search oppilaitokset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_220" :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= punkaharjun-yliopisto (:oid (last (:hits r)))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-osaamisala :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-muu :count])))
          (is (= 11 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi]))))))

    (testing "multiple sijainti"
      (let [r (search :sijainti " kunta_618 , kunta_091" :sort "name" :order "asc")]
        (is (= 10 (count (:hits r))))))

    (testing "koulutustyyppi amm"
      (let [r (search :koulutustyyppi "amm" :sort "name" :order "asc")]
        (is (= 4 (count (:hits r))))
        (is (= 4 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-muu :count])))))

    (testing "koulutustyyppi amm-osaamisala"
      (let [r (search :koulutustyyppi "amm-osaamisala" :sort "name" :order "asc")]
          ;; (debug-pretty r)
        (is (= 1 (count (:hits r))))
        (is (= 4 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-muu :count])))))

    (testing "koulutustyyppi amm-tutkinnon-osa"
      (let [r (search :koulutustyyppi "amm-tutkinnon-osa" :sort "name" :order "asc")]
          ;(debug-pretty r)
        (is (= 0 (count (:hits r))))
        (is (= 4 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-muu :count])))))

    (testing "koulutustyyppi amm-muu"
      (let [r (search :koulutustyyppi "amm-muu" :sort "name" :order "asc")]
          ;(debug-pretty r)
        (is (= 0 (count (:hits r))))
        (is (= 4 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi-muu :muut-ammatilliset :alakoodit :amm-muu :count])))))

    (testing "opetuskieli"
      (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01" :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 6 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))))

    (testing "koulutusala"
      (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01" :sort "name" :order "asc")]
        (is (= 4 (count (:hits r))))
        (is (= 4 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 4 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

    (testing "opetustapa"
      (let [r (search :opetustapa "opetuspaikkakk_02" :sort "name" :order "asc")]
        (is (= 6 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 5 (get-in r [:filters :koulutustyyppi-muu :aikuisten-perusopetus :count])))
        (is (= 0 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 6 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
        (is (= 6 (get-in r [:filters :maakunta :maakunta_01 :count])))
        (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
        (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
        (is (= 6 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
        (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 1 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

    (testing "Search oppilaitokset, get correct result"
      (let [r (search :sijainti "kunta_618" :sort "name" :order "asc")]
        (is (= {:nimi {:fi "Oppilaitos fi 1.2.246.562.10.00101010105",
                       :sv "Oppilaitos sv 1.2.246.562.10.00101010105"},
                :koulutusohjelmatLkm {:kaikki 1
                                      :tutkintoonJohtavat 1
                                      :eiTutkintoonJohtavat 0}
                :paikkakunnat [{:koodiUri "kunta_618",
                                :nimi {:fi "kunta_618 nimi fi",
                                       :sv "kunta_618 nimi sv"}}],
                :oid "1.2.246.562.10.00101010105"} (dissoc (last (:hits r)) :_score)))))))

(def aakkostus-oppilaitos-oid1 "1.2.246.562.10.0000011")
(def aakkostus-oppilaitos-oid2 "1.2.246.562.10.0000012")
(def aakkostus-oppilaitos-oid3 "1.2.246.562.10.0000013")
(def aakkostus-oppilaitos-oid4 "1.2.246.562.10.0000014")
(def aakkostus-oppilaitos-oid5 "1.2.246.562.10.0000015")

(deftest oppilaitos-paging-and-sorting-test
  (testing "Oppilaitos search ordering"
    (testing "by default order"
      (let [hits (:hits (search :keyword "aakkosissa"))]
        (is (= 5 (count hits)))
        (is (>= (:_score (nth hits 0))
                (:_score (nth hits 1))
                (:_score (nth hits 2))
                (:_score (nth hits 3))
                (:_score (nth hits 4))))))
    (testing "order by name asc"
      (is (= [aakkostus-oppilaitos-oid1 aakkostus-oppilaitos-oid2 aakkostus-oppilaitos-oid3 aakkostus-oppilaitos-oid4 aakkostus-oppilaitos-oid5] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "asc"))))
    (testing "order by name desc"
      (is (= [aakkostus-oppilaitos-oid5 aakkostus-oppilaitos-oid4 aakkostus-oppilaitos-oid3 aakkostus-oppilaitos-oid2 aakkostus-oppilaitos-oid1] (search-and-get-oids :keyword "aakkosissa" :sort "name" :order "desc")))))

  (testing "Oppilaitos search paging"
    (testing "returns first page by default"
      (is (= [aakkostus-oppilaitos-oid1 aakkostus-oppilaitos-oid2] (search-and-get-oids :keyword "aakkosissa" :size 2 :sort "name" :order "asc"))))
    (testing "returns last page"
      (is (= [aakkostus-oppilaitos-oid5] (search-and-get-oids :keyword "aakkosissa" :size 2 :page 3 :sort "name" :order "asc"))))
    (testing "returns correct total count"
      (is (= 5 (:total (get-ok (oppilaitos-search-url :keyword "aakkosissa" :size 2 :sort "name" :order "asc"))))))))

(def oppilaitos-oid1 "1.2.246.562.10.00101010101")
(def oppilaitos-oid2 "1.2.246.562.10.00101010102")
(def oppilaitos-oid3 "1.2.246.562.10.00101010103")
(def oppilaitos-oid4 "1.2.246.562.10.00101010104")
(def oppilaitos-oid5 "1.2.246.562.10.00101010105")
(def oppilaitos-oid6 "1.2.246.562.10.00101010106")

(deftest oppilaitos-keyword-search

  (testing "Searching with keyword"

    (testing "lääkäri <-> lääketieteen"
      (is (= [oppilaitos-oid1] (search-and-get-oids :sort "name" :order "asc" :keyword "lääkäri"))))

    (testing "humanisti <-> humanistinen"
      (is (= [oppilaitos-oid2] (search-and-get-oids :sort "name" :order "asc" :keyword "humanisti"))))

    (testing "musiikkioppilaitos <-> musiikkioppilaitokset"
      (is (= [oppilaitos-oid5] (search-and-get-oids :sort "name" :order "asc" :keyword "musiikkioppilaitos"))))

    (testing "auto"
      (is (= [oppilaitos-oid4] (search-and-get-oids :sort "name" :order "asc" :keyword "auto"))))

    (testing "muusikon koulutus"
      (is (= [oppilaitos-oid5] (search-and-get-oids :sort "name" :order "asc" :keyword "muusikon koulutus"))))

    (testing "kunta_220 nimi"
      ; TODO: Lisätään dumppeihin kuntien oikeat nimet, jotta voidaan tehdä realistisempia testihakuja
      (is (= 11 (count (search-and-get-oids :sort "name" :order "asc" :keyword "kunta_220")))))))
