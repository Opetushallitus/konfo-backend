(ns konfo-backend.tools-test
  (:require [clojure.test :refer :all]
            [konfo-backend.tools :refer :all]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [konfo-backend.util.haku-auki :refer :all])
  (:import (org.joda.time DateTime DateTimeUtils)))

(def formatter (format/with-zone (format/formatter "yyyy-MM-dd'T'HH:mm:ss") (time/time-zone-for-id "Europe/Helsinki")))

; Muunnetaan lokaali timestamp UTC-millisekunneiksi, jotta voidaan väärentää järjestelmän kello olemaan
; UTC-ajassa antamalla lokaali timestamp
(defn local-timestamp-to-utc-millis [timestamp]
  (coerce/to-long (time/to-time-zone (format/parse formatter timestamp) time/utc)))

(deftest tools-test
  (testing "haku-auki should be false one second before alkaa"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (DateTimeUtils/setCurrentMillisFixed (local-timestamp-to-utc-millis "2020-01-01T11:59:59"))
      (is (false? (:hakuAuki (with-is-haku-auki hakuajat))))))
          
  (testing "haku-auki should be true one second after alkaa"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (DateTimeUtils/setCurrentMillisFixed (local-timestamp-to-utc-millis "2020-01-01T12:00:01"))
      (is (true? (:hakuAuki (with-is-haku-auki hakuajat))))))
  
  (testing "haku-auki should be true one second before paattyy"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (DateTimeUtils/setCurrentMillisFixed (local-timestamp-to-utc-millis "2020-01-02T11:59:59"))
      (is (true? (:hakuAuki (with-is-haku-auki hakuajat))))))
  
  (testing "haku-auki should be false one second after paattyy"
    (let [hakuajat {:alkaa "2020-01-01T12:00" :paattyy "2020-01-02T12:00"}]
      (DateTimeUtils/setCurrentMillisFixed (local-timestamp-to-utc-millis "2020-01-02T12:00:01"))
      (is (false? (:hakuAuki (with-is-haku-auki hakuajat))))))
)