(ns konfo-backend.koulutus-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [konfo-backend.search.koulutus :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(facts "Koulutus"
  (fact "haettavissa (hakukohde)"
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1512220000001}}]})
        => nil?
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1712220000001}}]})
        => true)

  (fact "haettavissa (haku)"
    (haettavissa {:haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}]}]})
        => nil?
    (haettavissa {:haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}, {:alkuPvm 1512220000000 :loppuPvm 1712220000001}]}]})
        => true)

  (fact "haettavissa (hakukohteen hakuaika over haku)"
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1512220000001}}]
                                  :haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}, {:alkuPvm 1512220000000 :loppuPvm 1712220000001}]}]})
        => nil?
    (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1712220000001}}]
                             :haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}]}]}))
       => true)
