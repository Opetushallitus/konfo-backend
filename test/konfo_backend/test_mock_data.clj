(ns konfo-backend.test-mock-data
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]))

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
    (= "lukiopainotukset" x)                                {:id "lukiopainotukset"
                                                             :koodisto "lukiopainotukset"
                                                             :koodit [{:koodiUri "lukiopainotukset_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Lukiopainotus 1"}}
                                                                      {:koodiUri "lukiopainotukset_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Lukiopainotus 2"}}]}
    (= "lukiolinjaterityinenkoulutustehtava" x)             {:id "lukiolinjaterityinenkoulutustehtava"
                                                             :koodisto "lukiolinjaterityinenkoulutustehtava"
                                                             :koodit [{:koodiUri "lukiolinjaterityinenkoulutustehtava_01"
                                                                       :versio 1
                                                                       :nimi {:fi "Lukiolinja 1 (erityinen koulutustehtava)"}}
                                                                      {:koodiUri "lukiolinjaterityinenkoulutustehtava_02"
                                                                       :versio 1
                                                                       :nimi {:fi "Lukiolinja 2 (erityinen koulutustehtava)"}}]}
    :else []))
