(ns konfo-backend.search.search-test-tools
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-indeksoija-service.fixture.external-services :as mocks]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.search.koulutus.search :refer [index]]
            [cheshire.core :as cheshire]))

(def koulutusOid1 "1.2.246.562.13.000001")
(def koulutusOid2 "1.2.246.562.13.000002")
(def koulutusOid3 "1.2.246.562.13.000003")
(def koulutusOid4 "1.2.246.562.13.000004")
(def koulutusOid5 "1.2.246.562.13.000005")
(def koulutusOid6 "1.2.246.562.13.000006")

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

(defonce toteutus-metatieto
  (cheshire/generate-string
    {:tyyppi           "amm"
     :asiasanat        [{:kieli "fi" :arvo "hevonen"}]
     :ammattinimikkeet [{:kieli "fi" :arvo "ponityttö"}]
     :opetus {:opetuskieliKoodiUrit ["oppilaitoksenopetuskieli_02"]}}))

(defn mock-list-koodi-urit
  [x]
  (cond
    (= "maakunta" x)                                        ["maakunta_01",
                                                             "maakunta_02"]
    (= "oppilaitoksenopetuskieli" x)                        ["oppilaitoksenopetuskieli_01",
                                                             "oppilaitoksenopetuskieli_02"]
    (= "kansallinenkoulutusluokitus2016koulutusalataso1" x) ["kansallinenkoulutusluokitus2016koulutusalataso1_01",
                                                             "kansallinenkoulutusluokitus2016koulutusalataso1_02"]
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