(ns konfo-backend.search.tools
  (:require
    [konfo-backend.tools :refer [not-blank?]]
    [clojure.core :refer [keyword] :rename {keyword kw}]
    [konfo-backend.tools :refer [current-time-as-kouta-format hakuaika-kaynnissa?]]))

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

(defn constraints?
  [constraints]
  (or (sijainti? constraints) (koulutustyyppi? constraints) (koulutusala? constraints) (opetuskieli? constraints)))

(defn ->lng-keyword
  [str lng]
  (keyword (format str lng)))

(defn do-search?
  [keyword constraints]
  (or (not (empty keyword)) (constraints? constraints)))