(ns konfo-backend.test-mock-data
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire])
  (:import (fi.oph.kouta.external KoutaFixtureTool$)))

(defonce KoutaFixtureTool KoutaFixtureTool$/MODULE$)

(def koulutusOid1 "1.2.246.562.13.000001")
(def koulutusOid2 "1.2.246.562.13.000002")
(def koulutusOid3 "1.2.246.562.13.000003")
(def koulutusOid4 "1.2.246.562.13.000004")
(def koulutusOid5 "1.2.246.562.13.000005")
(def koulutusOid6 "1.2.246.562.13.000006")
(def koulutusOid7 "1.2.246.562.13.000007")
(def koulutusOid8 "1.2.246.562.13.000008")
(def koulutusOid9 "1.2.246.562.13.000009")
(def koulutusOid10 "1.2.246.562.13.000010")
(def koulutusOid11 "1.2.246.562.13.000011")
(def koulutusOid12 "1.2.246.562.13.000012")
(def koulutusOid13 "1.2.246.562.13.000013")
(def koulutusOid14 "1.2.246.562.13.000014")
(def koulutusOid15 "1.2.246.562.13.000015")
(def koulutusOid16 "1.2.246.562.13.000016")
(def koulutusOid17 "1.2.246.562.13.000017")

(def oppilaitosOid1 "1.2.246.562.10.0000011")
(def oppilaitosOid2 "1.2.246.562.10.0000012")
(def oppilaitosOid3 "1.2.246.562.10.0000013")
(def oppilaitosOid4 "1.2.246.562.10.0000014")
(def oppilaitosOid5 "1.2.246.562.10.0000015")
(def oppilaitosOid6 "1.2.246.562.10.0000016")

(defonce koulutus-metatieto
  (cheshire/generate-string
    {:tyyppi "amm",
     :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso1_01#1",
                            "kansallinenkoulutusluokitus2016koulutusalataso1_02#1"]}))

(defonce lukio-koulutus-metatieto
         (cheshire/generate-string
           {:tyyppi               "lk"
            :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso1_01#1"]
            :kuvauksenNimi        {:fi "kuvaus", :sv "kuvaus sv"}}))

(defonce yo-koulutus-metatieto
  (cheshire/generate-string
    {:tyyppi               "yo"
     :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso2_01#1",
                            "kansallinenkoulutusluokitus2016koulutusalataso2_02#1"]
     :kuvauksenNimi        {:fi "kuvaus", :sv "kuvaus sv"}}))

(defonce amk-koulutus-metatieto
   (cheshire/generate-string
     {:tyyppi               "amk"
      :koulutusalaKoodiUrit ["kansallinenkoulutusluokitus2016koulutusalataso2_01#1",
                             "kansallinenkoulutusluokitus2016koulutusalataso2_02#1"]
      :kuvauksenNimi        {:fi "kuvaus", :sv "kuvaus sv"}}))

(defonce amm-tutkinnon-osa-koulutus-metadata (.ammTutkinnonOsaKoulutusMetadata KoutaFixtureTool))

(defonce amm-osaamisala-koulutus-metadata (.ammOsaamisalaKoulutusMetadata KoutaFixtureTool))

(defonce toteutus-metatieto
  (cheshire/generate-string
    {:tyyppi           "amm"
     :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
     :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]
     :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_02"]
              :opetustapaKoodiUrit ["opetuspaikkakk_02"]}}))

(defonce yo-toteutus-metatieto
   (cheshire/generate-string
     {:tyyppi           "yo"
      :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
      :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]
      :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_02"]
               :opetustapaKoodiUrit ["opetuspaikkakk_02"]
               :koulutuksenTarkkaAlkamisaika false
               :koulutuksenAlkamisvuosi 2022}}))

(defonce amk-toteutus-metatieto
   (cheshire/generate-string
     {:tyyppi           "amk"
      :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
      :ammattinimikkeet [{:kieli "fi" :arvo "ponipoika"}]
      :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_01"]
               :opetustapaKoodiUrit ["opetuspaikkakk_01"]
               :koulutuksenTarkkaAlkamisaika true
               :koulutuksenAlkamisvuosi 2019}}))

(defonce amm-tutkinnon-osa-toteutus-metadata (.ammTutkinnonOsaToteutusMetadata KoutaFixtureTool))

(defonce lukio-toteutus-metatieto (.lukioToteutusMedatada KoutaFixtureTool))

(defn mock-get-koodisto
  [x]
  (cond
    (= "maakunta" x)                                        {:id "maakunta"
                                                             :koodisto "maakunta"
                                                             :koodit [{:koodiUri "maakunta_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Kiva maakunta"}}
                                                                      {:koodiUri "maakunta_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Toinen kiva maakunta"}}]}
    (= "koulutustyyppi" x)                                  {:id "koulutustyyppi"
                                                             :koodisto "koulutustyyppi"
                                                             :koodit [{:koodiUri "koulutustyyppi_11"
                                                                       :versio 1
                                                                       :nimi {:fi "Mahtava koulutustyyppi"}}]}
    (= "opetuspaikkakk" x)                                  {:id "opetuspaikkakk"
                                                             :koodisto "opetuspaikkakk"
                                                             :koodit [{:koodiUri "opetuspaikkakk_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Mahtava opetustapa"}}
                                                                      {:koodiUri "opetuspaikkakk_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Toinen mahtava opetustapa"}}]}
    (= "valintatapajono" x)                                 {:id "valintatapajono"
                                                             :koodisto "valintatapajono"
                                                             :koodit [{:koodiUri "valintatapajono_av"
                                                                       :versio 1
                                                                       :nimi {:fi "Valintatapa yksi"}}
                                                                      {:koodiUri "valintatapajono_tv"
                                                                       :versio 1
                                                                       :nimi {:fi "Valintatapa kaksi"}}]}
    (= "hakutapa" x)                                        {:id "hakutapa"
                                                             :koodisto "hakutapa"
                                                             :koodit [{:koodiUri "hakutapa_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Hakutapa yksi"}}
                                                                      {:koodiUri "hakutapa_03"
                                                                       :versio 1
                                                                       :nimi {:fi "Hakutapa kolme"}}]}
    (= "pohjakoulutusvaatimuskonfo" x)                      {:id "pohjakoulutusvaatimuskonfo"
                                                             :koodisto "pohjakoulutusvaatimuskonfo"
                                                             :koodit [{:koodiUri "pohjakoulutusvaatimuskonfo_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Pohjakoulutusvaatimus yksi"}}
                                                                      {:koodiUri "pohjakoulutusvaatimuskonfo_am"
                                                                       :versio 1
                                                                       :nimi {:fi "Pohjakoulutusvaatimus AM"}}]}
    (= "oppilaitoksenopetuskieli" x)                        {:id "oppilaitoksenopetuskieli"
                                                             :koodisto "oppilaitoksenopetuskieli"
                                                             :koodit [{:koodiUri "oppilaitoksenopetuskieli_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Suomi"}}
                                                                      {:koodiUri "oppilaitoksenopetuskieli_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Ruotsi"}}]}
    (= "kansallinenkoulutusluokitus2016koulutusalataso1" x) {:id "kansallinenkoulutusluokitus2016koulutusalataso1"
                                                             :koodisto "kansallinenkoulutusluokitus2016koulutusalataso1"
                                                             :koodit [{:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso1_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Kiva koulutusala"}
                                                                       :alakoodit [{:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso2_01"
                                                                                    :versio 1
                                                                                    :nimi {:fi "Kiva alakoulutusala1"}}]}
                                                                      {:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso1_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Toinen kiva koulutusala"}
                                                                       :alakoodit [{:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso2_02"
                                                                                    :versio 1
                                                                                    :nimi {:fi "Kiva alakoulutusala2"}}]}]}
    (= "kansallinenkoulutusluokitus2016koulutusalataso2" x) {:id "kansallinenkoulutusluokitus2016koulutusalataso2"
                                                             :koodisto "kansallinenkoulutusluokitus2016koulutusalataso2"
                                                             :koodit [{:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso2_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Kiva taso2 koulutusala"}}
                                                                      {:koodiUri "kansallinenkoulutusluokitus2016koulutusalataso2_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Toinen kiva taso2 koulutusala"}}]}
    :else []))

(def punkaharjun-yliopisto    "1.2.246.562.10.000002")
(def punkaharjun-toimipiste-1 "1.2.246.562.10.000003")
(def punkaharjun-toimipiste-2 "1.2.246.562.10.000004")

(defonce punkaharju-org
  (mocks/create-organisaatio-hierarkia
    {:oid "1.2.246.562.10.000001"
     :nimi {:fi "Punkaharjun kunta"
            :sv "Punkaharjun kunta sv"}
     :kotipaikka "kunta_618"
     :kielet ["oppilaitoksenopetuskieli_1#1",
              "oppilaitoksenopetuskieli_2#1" ]}
    {:oid punkaharjun-yliopisto
     :nimi {:fi "Punkaharjun yliopisto"
            :sv "Punkaharjun yliopisto sv"}
     :kotipaikka "kunta_618"
     :kielet ["oppilaitoksenopetuskieli_1#1",
              "oppilaitoksenopetuskieli_2#1" ]}
    [{:oid punkaharjun-toimipiste-1
      :nimi {:fi "Punkaharjun yliopiston toimipiste"
             :sv "Punkaharjun yliopiston toimipiste sv "}
      :kotipaikka "kunta_618"
      :kielet ["oppilaitoksenopetuskieli_2#1" ]},
     {:oid punkaharjun-toimipiste-2
      :nimi {:fi "Punkaharjun yliopiston Karjaan toimipiste"
             :sv "Punkaharjun yliopiston Karjaan toimipiste sv "}
      :kotipaikka "kunta_220"
      :kielet ["oppilaitoksenopetuskieli_1#1"]}]))

(def helsingin-yliopisto  "1.2.246.562.10.000005")
(def helsingin-toimipiste "1.2.246.562.10.000006")

(defonce helsinki-org
  (mocks/create-organisaatio-hierarkia
    {:oid "1.2.246.562.10.000001"
     :nimi {:fi "Helsingin kunta" :sv "Helsingin kunta sv"}
     :kotipaikka "kunta_091"
     :kielet [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]}
    {:oid helsingin-yliopisto
     :nimi {:fi "Helsingin yliopisto" :sv "Helsingin yliopisto sv"}
     :kotipaikka "kunta_091"
     :kielet [ "oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_2#1" ]}
    [{:oid helsingin-toimipiste
      :nimi {:fi "Helsingin yliopiston toimipiste" :sv "Helsingin yliopiston toimipiste sv "}
      :kotipaikka "kunta_091"
      :kielet [ "oppilaitoksenopetuskieli_2#1" ]}]))

(defn named-oppilaitos
  [oid nimi]
  (mocks/create-organisaatio-hierarkia
    {:oid "1.2.246.562.10.000001"
     :nimi {:fi "Helsingin kunta" :sv "Helsingin kunta sv"}
     :kotipaikka "kunta_091"}
    {:oid oid
     :nimi {:fi nimi :sv (str nimi " sv")}
     :kotipaikka "kunta_091"}
    []))

(defn aakkos-orgs
  [x & {:as params}]
  (cond
    (= x oppilaitosOid1) (named-oppilaitos x "Aakkosissa ensimmäinen")
    (= x oppilaitosOid2) (named-oppilaitos x "Aakkosissa toinen")
    (= x oppilaitosOid3) (named-oppilaitos x "Aakkosissa vasta kolmas")
    (= x oppilaitosOid4) (named-oppilaitos x "Aakkosissa vasta neljäs")
    (= x oppilaitosOid5) (named-oppilaitos x "Aakkosissa viidentenä")
    (= x oppilaitosOid6) (named-oppilaitos x "Aakkosissa viimein kuudentena")))

(defn orgs
  [x & {:as params}]
  (cond
    (or (= x punkaharjun-yliopisto) (= x punkaharjun-toimipiste-1) (= x punkaharjun-toimipiste-2)) punkaharju-org
    (or (= x helsingin-yliopisto) (= x helsingin-toimipiste)) helsinki-org))
