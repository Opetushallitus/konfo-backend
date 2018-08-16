(ns konfo-backend.koulutus-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.toteutus :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(deftest koulutus-haettavissa-test
  (testing "Koulutus haettavissa (hakukohde)"
    (is (not (true? (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1512220000001}}]}))))
    (is (true? (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1712220000001}}]}))))

  (testing "Koulutus haettavissa (haku)"
    (is (not (true? (haettavissa {:haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}]}]}))))
    (is (true? (haettavissa {:haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}, {:alkuPvm 1512220000000 :loppuPvm 1712220000001}]}]}))))

  (testing "Koulutus haettavissa (hakukohteen hakuaika over haku)"
    (is (not (true? (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1512220000001}}]
                                  :haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}, {:alkuPvm 1512220000000 :loppuPvm 1712220000001}]}]}))))
    (is (true? (haettavissa {:hakukohteet [{:hakuaika {:alkuPvm 1512220000000 :loppuPvm 1712220000001}}]
                             :haut [{:hakuaikas [{:alkuPvm 1512220000000 :loppuPvm 1512220000001}]}]})))))
