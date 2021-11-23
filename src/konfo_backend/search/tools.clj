(ns konfo-backend.search.tools
  (:require
    [konfo-backend.tools :refer [not-blank?]]
    [clojure.core :refer [keyword] :rename {keyword kw}]))

(defn- constraint?
  [constraints key]
  (not (empty? (key constraints))))

(defn sijainti?
  [constraints]
  (constraint? constraints :sijainti))

(defn koulutustyyppi?
  [constraints]
  (constraint? constraints :koulutustyyppi))

(defn opetuskieli?
  [constraints]
  (constraint? constraints :opetuskieli))

(defn koulutusala?
  [constraints]
  (constraint? constraints :koulutusala))

(defn opetustapa?
  [constraints]
  (constraint? constraints :opetustapa))

(defn valintatapa?
  [constraints]
  (constraint? constraints :valintatapa))

(defn hakutapa?
  [constraints]
  (constraint? constraints :hakutapa))

(defn pohjakoulutusvaatimus?
  [constraints]
  (constraint? constraints :pohjakoulutusvaatimus))

(defn haku-kaynnissa?
  [constraints]
  (true? (:hakukaynnissa constraints)))

(defn yhteishaku?
  [constraints]
  (constraint? constraints :yhteishaku))

(defn lukiolinja?
  [constraints]
  (constraint? constraints :lukiolinja))

(defn constraints?
  [constraints]
  (or (sijainti? constraints)
      (koulutustyyppi? constraints)
      (koulutusala? constraints)
      (opetuskieli? constraints)
      (opetustapa? constraints)
      (valintatapa? constraints)
      (haku-kaynnissa? constraints)
      (hakutapa? constraints)
      (yhteishaku? constraints)
      (pohjakoulutusvaatimus? constraints)))

(defn ->lng-keyword
  [str lng]
  (keyword (format str lng)))

(defn do-search?
  [keyword constraints]
  (or (not (empty keyword)) (constraints? constraints)))

(defn match-all?
  [keyword constraints]
  (not (do-search? keyword constraints)))
