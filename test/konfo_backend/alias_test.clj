(ns konfo-backend.alias-test
  (:require [clojure.test :refer :all]
            [konfo-backend.elastic-tools :as e]))

(defonce koulutus (atom 0))
(defonce toteutus (atom 0))
(defonce valintaperuste (atom 0))

(defn- mock-list-aliases
  []
  {:koulutus-kouta-31-03-2020-at-08.47.02.772 {:aliases {:koulutus-kouta {:is_write_index false}}},
   :koulutus-kouta-31-03-2020-at-08.47.11.960 {:aliases {:koulutus-kouta-virkailija {:is_write_index true}}},
   :toteutus-kouta-31-03-2020-at-08.47.03.648 {:aliases {:toteutus-kouta {:is_write_index false}}},
   :toteutus-kouta-31-03-2020-at-08.47.12.706 {:aliases {:toteutus-kouta-virkailija {:is_write_index true}}},
   :valintaperuste-kouta-31-03-2020-at-08.47.06.917 {:aliases {:valintaperuste-kouta {:is_write_index false},
                                                               :valintaperuste-kouta-virkailija {:is_write_index true}}}})

(defn- mock-move-read-alias-to-write-index
  [write-alias read-alias]

  (case [write-alias read-alias]
    ["koulutus-kouta-virkailija" "koulutus-kouta"] (do (swap! koulutus inc) :koulutus-kouta-31-03-2020-at-08.47.11.960)
    ["toteutus-kouta-virkailija" "toteutus-kouta"] (do (swap! toteutus inc) :toteutus-kouta-31-03-2020-at-08.47.12.706)
    ["valintaperuste-kouta-virkailija" "valintaperuste-kouta"] (do (swap! valintaperuste inc) :valintaperuste-kouta-31-03-2020-at-08.47.06.917)
    (is (= true [write-alias read-alias]))))

(deftest elastic-alias-test
  (testing "Elastic tools should sync aliases correctly"
    (with-redefs [clj-elasticsearch.elastic-connect/list-aliases mock-list-aliases
                  clj-elasticsearch.elastic-connect/move-read-alias-to-write-index mock-move-read-alias-to-write-index]
      (e/update-aliases-on-startup)
      (is (= 1 @koulutus))
      (is (= 1 @toteutus))
      (is (= 1 @valintaperuste)))))