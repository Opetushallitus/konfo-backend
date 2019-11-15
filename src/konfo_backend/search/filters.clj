(ns konfo-backend.search.filters
  (:require
    [konfo-backend.koodisto.koodisto :as k]))

(defn- koodi->filter
  [koodi]
  (if (contains? koodi :alakoodit)
    (-> koodi
        (assoc :alakoodit (vec (map koodi->filter (:alakoodit koodi))))
        (dissoc :versio))
    (dissoc koodi :versio)))

(defn- koodisto->filters
  [koodisto]
  (map koodi->filter (-> (k/get-koodisto koodisto)
                         (:koodit))))

;TODO! Koodisto
(defn- beta-koulutustyyppi
  []
  (let [ammatillinen-koulutustyyppi? (fn [k] (or (= k "koulutustyyppi_1") (= k "koulutustyyppi_11") (= k "koulutustyyppi_12")))]
    [{:koodiUri "amm"
      :nimi {:fi "Ammatillinen koulutus"}
      :alakoodit (vec (filter #(-> % :koodiUri (ammatillinen-koulutustyyppi?)) (koodisto->filters "koulutustyyppi")))}]))

(defn hierarkia
  []
  {:opetuskieli    (koodisto->filters "oppilaitoksenopetuskieli")
   :sijainti       (koodisto->filters "maakunta")
   :koulutustyyppi (beta-koulutustyyppi) ;TODO! Koodisto
   :koulutusala    (koodisto->filters "kansallinenkoulutusluokitus2016koulutusalataso1")})