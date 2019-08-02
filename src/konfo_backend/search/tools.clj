(ns konfo-backend.search.tools
  (:require
    [konfo-backend.tools :refer [not-blank?]]
    [clojure.core :refer [keyword] :rename {keyword kw}]
    [konfo-backend.tools :refer [current-time-as-kouta-format hakuaika-kaynnissa?]]))

(defn paikkakunta?
  [constraints]
  (not-blank? (:paikkakunta constraints)))

(defn koulutustyyppi?
  [constraints]
  (not (empty? (:koulutustyyppi constraints))))

(defn vain-haku-kaynnissa?
  [constraints]
  (true? (:vainHakuKaynnissa constraints)))

(defn opetuskieli?
  [constraints]
  (not (empty? (:opetuskieli constraints))))

(defn constraints?
  [constraints]
  (or (paikkakunta? constraints) (koulutustyyppi? constraints) (vain-haku-kaynnissa? constraints) (opetuskieli? constraints)))

(defn ->lng-keyword
  [str lng]
  (keyword (format str lng)))

