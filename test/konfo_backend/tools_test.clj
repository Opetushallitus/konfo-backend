(ns konfo-backend.tools-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer [parse-test-time]]
            [konfo-backend.util.time :as time]
            [konfo-backend.util.haku-auki :refer [with-is-haku-auki]]))

(deftest tools-test
  (testing "haku-auki should be false one second before alkaa"
    (with-redefs [time/current-date-time #(parse-test-time "2020-01-01T11:59:59")]
      (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
        (is (false? (:hakuAuki (with-is-haku-auki hakuajat)))))))

  (testing "haku-auki should be true one second after alkaa"
    (with-redefs [time/current-date-time #(parse-test-time "2020-01-01T12:00:01")]
      (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
        (is (true? (:hakuAuki (with-is-haku-auki hakuajat)))))))

  (testing "haku-auki should be true one second before paattyy"
    (with-redefs [time/current-date-time #(parse-test-time "2020-01-02T11:59:59")]
      (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
        (is (true? (:hakuAuki (with-is-haku-auki hakuajat)))))))

  (testing "haku-auki should be false one second after paattyy"
    (with-redefs [time/current-date-time #(parse-test-time "2020-01-02T12:00:01")]
      (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
        (is (false? (:hakuAuki (with-is-haku-auki hakuajat))))))))
