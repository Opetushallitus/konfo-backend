(ns konfo-backend.tools-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer [set-fixed-time]]
            [konfo-backend.tools :refer :all]
            [konfo-backend.util.haku-auki :refer :all]))

(deftest tools-test
  (testing "haku-auki should be false one second before alkaa"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (set-fixed-time "2020-01-01T11:59:59")
      (is (false? (:hakuAuki (with-is-haku-auki hakuajat))))))

  (testing "haku-auki should be true one second after alkaa"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (set-fixed-time "2020-01-01T12:00:01")
      (is (true? (:hakuAuki (with-is-haku-auki hakuajat))))))

  (testing "haku-auki should be true one second before paattyy"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (set-fixed-time "2020-01-02T11:59:59")
      (is (true? (:hakuAuki (with-is-haku-auki hakuajat))))))

  (testing "haku-auki should be false one second after paattyy"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (set-fixed-time "2020-01-02T12:00:01")
      (is (false? (:hakuAuki (with-is-haku-auki hakuajat)))))))