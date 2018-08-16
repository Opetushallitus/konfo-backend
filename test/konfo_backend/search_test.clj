(ns konfo-backend.search-test
  (:require [midje.sweet :refer :all]
            [konfo-backend.search.search :refer :all]))

(facts "Search tools"
  (fact "constraints all"
    (constraints :koulutustyyppi "ako,amm" :paikkakunta "tampere" :kieli "fi,sv")
        => {:kieli "fi,sv" :paikkakunta "tampere" :koulutustyyppi "ako,amm"})
  (fact "constraints without kieli"
      (constraints :koulutustyyppi "ako,amm" :paikkakunta "tampere")
      => {:paikkakunta "tampere" :koulutustyyppi "ako,amm"}))
