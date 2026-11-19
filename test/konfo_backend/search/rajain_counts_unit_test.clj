(ns konfo-backend.search.rajain-counts-unit-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.rajain-counts]))

(def create-toimipiste-counts #'konfo-backend.search.rajain-counts/create-toimipiste-counts)

(def ylempi {:oid "1.2.246.562.10.1" :nimi {:fi "Ylempi"}})
(def alempi {:oid "1.2.246.562.10.2" :nimi {:fi "Alempi"} :parentToimipisteOid "1.2.246.562.10.1"})
(def alimmainen {:oid "1.2.246.562.10.3" :nimi {:fi "Alimmainen"} :parentToimipisteOid "1.2.246.562.10.2"})
(def erillinen {:oid "1.2.246.562.10.4" :nimi {:fi "Erillinen"}})
(def ilman-tarjontaa {:oid "1.2.246.562.10.5" :nimi {:fi "Ilman tarjontaa"}})

(defn- buckets [counts]
  (into {} (map (fn [[oid c]] [(keyword oid) {:doc_count c}]) counts)))

(deftest toimipiste-counts-test
  (testing "Alemmat toimipisteet näytetään ylimmän toimipisteensä alakoodeina, myös syvemmältä tasolta"
    (is (= {:1.2.246.562.10.1 {:nimi {:fi "Ylempi"}
                               :count 5
                               :alakoodit {:1.2.246.562.10.2 {:nimi {:fi "Alempi"} :count 3}
                                           :1.2.246.562.10.3 {:nimi {:fi "Alimmainen"} :count 1}}}
            :1.2.246.562.10.4 {:nimi {:fi "Erillinen"} :count 2}}
           (create-toimipiste-counts (buckets {"1.2.246.562.10.1" 5
                                               "1.2.246.562.10.2" 3
                                               "1.2.246.562.10.3" 1
                                               "1.2.246.562.10.4" 2})
                                     [ylempi alempi alimmainen erillinen ilman-tarjontaa]))))
  (testing "Toimipisteet ilman toteutuksia jätetään pois"
    (is (= {:1.2.246.562.10.1 {:nimi {:fi "Ylempi"} :count 1}}
           (create-toimipiste-counts (buckets {"1.2.246.562.10.1" 1 "1.2.246.562.10.2" 0})
                                     [ylempi alempi]))))
  (testing "Ilman toimipisteitä palautetaan tyhjä"
    (is (= {} (create-toimipiste-counts {} nil)))))
