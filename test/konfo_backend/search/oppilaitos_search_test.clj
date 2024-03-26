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
        (is (= 12 (count (:hits r))))
        (is (= 5 (get-in r [:filters :koulutustyyppi :aikuisten-perusopetus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :taiteen-perusopetus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :vaativan-tuen-koulutukset :alakoodit :tuva-erityisopetus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :vaativan-tuen-koulutukset :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :valmentavat-koulutukset :alakoodit :tuva-normal :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :valmentavat-koulutukset :alakoodit :telma :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :valmentavat-koulutukset :alakoodit :vapaa-sivistystyo-opistovuosi :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :valmentavat-koulutukset :count])))
        (is (= 5 (get-in r [:filters :koulutustyyppi :amm :alakoodit :muu-amm-tutkinto :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-muu :count])))
        (is (= 6 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi :lk :count])))

        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-alempi :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-ylempi :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amm-ope-erityisope-ja-opo :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-opintojakso-avoin :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-opintojakso :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-opintokokonaisuus-avoin :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-opintokokonaisuus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :alakoodit :amk-erikoistumiskoulutus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amk :count])))

        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :kandi :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :kandi-ja-maisteri :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :maisteri :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :tohtori :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :yo-opintojakso-avoin :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :yo-opintojakso :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :yo-opintokokonaisuus-avoin :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :yo-opintokokonaisuus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :ope-pedag-opinnot :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :erikoislaakari :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :alakoodit :yo-erikoistumiskoulutus :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :yo :count])))

        (is (= 0 (get-in r [:filters :koulutustyyppi :vapaa-sivistystyo-muu :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :muu :count])))

        (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 6 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))
        (is (= 12 (get-in r [:filters :maakunta :maakunta_01 :count])))
        (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
        (is (= 1 (get-in r [:filters :opetustapa :opetuspaikkakk_01 :count])))
        (is (= 6 (get-in r [:filters :opetustapa :opetuspaikkakk_02 :count])))
        (is (= 5 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 5 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

    (testing "Search oppilaitokset, filter with..."
      (testing "sijainti"
        (let [r (search :sijainti "kunta_220" :sort "name" :order "asc")]
          (is (= 1 (count (:hits r))))
          (is (= punkaharjun-yliopisto (:oid (last (:hits r)))))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :muu-amm-tutkinto :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-osaamisala :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-tutkinnon-osa :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-muu :count])))
          (is (= 2 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 12 (get-in r [:filters :maakunta :maakunta_01 :count])))
          (is (= 0 (get-in r [:filters :maakunta :maakunta_02 :count])))
          (is (= "Kiva maakunta" (get-in r [:filters :maakunta :maakunta_01 :nimi :fi]))))))

    (testing "multiple sijainti"
      (let [r (search :sijainti " kunta_618 , kunta_091" :sort "name" :order "asc")]
        (is (= 10 (count (:hits r))))))

    (testing "koulutustyyppi amm"
      (let [r (search :koulutustyyppi "amm" :sort "name" :order "asc")]
        (is (= 5 (count (:hits r))))
        (is (= 5 (get-in r [:filters :koulutustyyppi :amm :alakoodit :muu-amm-tutkinto :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-tutkinnon-osa :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-muu :count])))
        (is (= 6 (get-in r [:filters :koulutustyyppi :amm :count])))))

    (testing "koulutustyyppi amm-osaamisala"
      (let [r (search :koulutustyyppi "amm-osaamisala" :sort "name" :order "asc")]
          ;; (debug-pretty r)
        (is (= 1 (count (:hits r))))
        (is (= 6 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-osaamisala :count])))))

    (testing "koulutustyyppi amm-tutkinnon-osa"
      (let [r (search :koulutustyyppi "amm-tutkinnon-osa" :sort "name" :order "asc")]
          ;(debug-pretty r)
        (is (= 0 (count (:hits r))))
        (is (= 6 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-osaamisala :count])))
        (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-tutkinnon-osa :count])))))

    (testing "koulutustyyppi amm-muu"
      (let [r (search :koulutustyyppi "amm-muu" :sort "name" :order "asc")]
          ;(debug-pretty r)
        (is (= 0 (count (:hits r))))
          (is (= 6 (get-in r [:filters :koulutustyyppi :amm :count])))
          (is (= 1 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-osaamisala :count])))
          (is (= 0 (get-in r [:filters :koulutustyyppi :amm :alakoodit :amm-muu :count])))))

    (testing "opetuskieli"
      (let [r (search :opetuskieli "oppilaitoksenopetuskieli_01" :sort "name" :order "asc")]
        (is (= 1 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 1 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_01 :count])))
        (is (= 6 (get-in r [:filters :opetuskieli :oppilaitoksenopetuskieli_02 :count])))))

    (testing "koulutusala"
      (let [r (search :koulutusala "kansallinenkoulutusluokitus2016koulutusalataso1_01" :sort "name" :order "asc")]
        (is (= 5 (count (:hits r))))
        (is (= 6 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 5 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_01 :count])))
        (is (= 5 (get-in r [:filters :koulutusala :kansallinenkoulutusluokitus2016koulutusalataso1_02 :count])))))

    (testing "opetustapa"
      (let [r (search :opetustapa "opetuspaikkakk_02" :sort "name" :order "asc")]
        (is (= 6 (count (:hits r))))
        (is (= 1 (get-in r [:filters :koulutustyyppi :amm :count])))
        (is (= 5 (get-in r [:filters :koulutustyyppi :aikuisten-perusopetus :count])))
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
                :oid "1.2.246.562.10.00101010105"} (dissoc (last (:hits r)) :_score)))))
    (testing "Totalcount of a category, get correct number"
      (let [r (konfo-backend.search.rajain-counts/generate-default-rajain-counts {:muu-amm-tutkinto 1,
                                                                                  :amm-osaamisala 2,
                                                                                  :amm-tutkinnon-osa 3,
                                                                                  :amm-muu 4})]
        (is (= 10 (get-in r [:koulutustyyppi :amm :count])))))))

(deftest oppilaitos-paging-and-sorting-test

  (def aakkostus-oppilaitos-oid1 "1.2.246.562.10.0000011")
  (def aakkostus-oppilaitos-oid2 "1.2.246.562.10.0000012")
  (def aakkostus-oppilaitos-oid3 "1.2.246.562.10.0000013")
  (def aakkostus-oppilaitos-oid4 "1.2.246.562.10.0000014")
  (def aakkostus-oppilaitos-oid5 "1.2.246.562.10.0000015")

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
(def jokin-jarjestyspaikka "1.2.246.562.10.67476956288")

(deftest oppilaitos-keyword-search

  (testing "Searching with keyword"

    (testing "lääkäri <-> lääketieteen"
      (is (= [oppilaitos-oid1] (search-and-get-oids :sort "name" :order "asc" :keyword "lääkäri"))))

    (testing "humanisti <-> humanistinen"
      (is (= [oppilaitos-oid2] (search-and-get-oids :sort "name" :order "asc" :keyword "humanisti"))))

    (testing "musiikkioppilaitos <-> musiikkioppilaitokset"
      (is (= [oppilaitos-oid5] (search-and-get-oids :sort "name" :order "asc" :keyword "musiikkioppilaitos"))))

    (testing "auto"
      (is (= [jokin-jarjestyspaikka oppilaitos-oid4] (search-and-get-oids :sort "name" :order "asc" :keyword "auto"))))

    (testing "muusikon koulutus"
      (is (= [oppilaitos-oid5] (search-and-get-oids :sort "name" :order "asc" :keyword "muusikon koulutus"))))

    (testing "kunta_220 nimi"
      ; TODO: Lisätään dumppeihin kuntien oikeat nimet, jotta voidaan tehdä realistisempia testihakuja
      (is (= 12 (count (search-and-get-oids :sort "name" :order "asc" :keyword "kunta_220")))))))
