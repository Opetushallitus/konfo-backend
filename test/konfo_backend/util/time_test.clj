(ns konfo-backend.util.time-test
  (:require [clojure.test :refer [deftest is testing]]
            [konfo-backend.util.time :as time])
  (:import (java.time LocalDate ZonedDateTime)))

(defn- breakdown
  "Palauttaa ajan pilkottuna osiin: [vuosi kk pv t m s milli vyöhyke]"
  [^ZonedDateTime time]
  [(.getYear time)
   (.getMonthValue time)
   (.getDayOfMonth time)
   (.getHour time)
   (.getMinute time)
   (.getSecond time)
   (-> time .getNano (/ 1000000) int)
   (-> time .getZone .getId)])

(deftest current-date-time
  (testing "returns time in Helsinki timezone"
    (is (= "Europe/Helsinki" (-> (time/current-date-time) .getZone .getId)))))

(deftest format-localized-date
  (testing "returns correct timestamps"
    (is (= "3.2.2026 klo 11:58" (:fi (time/format-localized-dates "2026-02-03T09:58:24.312Z"))))
    (is (= "3.2.2026 kl. 11:58" (:sv (time/format-localized-dates "2026-02-03T09:58:24.312Z"))))
    (is (= "Feb. 3, 2026 at 11:58 AM EET" (:en (time/format-localized-dates "2026-02-03T09:58:24.312Z"))))
    (is (= "Jul. 14, 2026 at 03:58 PM EEST" (:en (time/format-localized-dates "2026-07-14T12:58:24.312Z"))))))

(deftest current-date-formatted
  (testing "returns correct date"
    (with-redefs [time/current-local-date #(LocalDate/parse "2025-01-01")]
      (is (= "2025-01-01" (time/current-date-formatted))))
    (with-redefs [time/current-local-date #(LocalDate/parse "2024-12-31")]
      (is (= "2024-12-31" (time/current-date-formatted))))))

(deftest ->kouta-date-time-string
  (testing "returns correctly formatted string"
    (is (= "2025-01-01T00:58" (time/->kouta-date-time-string (ZonedDateTime/parse "2025-01-01T00:58:24.312+02:00"))))
    (is (= "2024-12-31T23:58" (time/->kouta-date-time-string (ZonedDateTime/parse "2024-12-31T23:58:24.312+02:00"))))))

(deftest kouta-date-time-string->date-time
  (testing "returns correct datetime"
    (is (= [2025 1 1 0 58 0 0 "Europe/Helsinki"] (breakdown (time/kouta-date-time-string->date-time "2025-01-01T00:58"))))
    (is (= [2024 12 31 23 58 0 0 "Europe/Helsinki"] (breakdown (time/kouta-date-time-string->date-time "2024-12-31T23:58"))))))

(deftest current-time-as-kouta-format
  (testing "returns correct date and time"
    (with-redefs [time/current-date-time #(ZonedDateTime/parse "2025-01-01T00:58:24.312+02:00")]
      (is (= "2025-01-01T00:58" (time/current-time-as-kouta-format))))
    (with-redefs [time/current-date-time #(ZonedDateTime/parse "2024-12-31T23:58:24.312+02:00")]
      (is (= "2024-12-31T23:58" (time/current-time-as-kouta-format))))))

(deftest add-days-to-kouta-date-time-string
  (testing "returns correctly formatted string"
    (is (= "2025-01-06T00:58" (time/add-days-to-kouta-date-time-string "2025-01-01T00:58" 5)))
    (is (= "2025-01-04T23:58" (time/add-days-to-kouta-date-time-string "2024-12-31T23:58" 4)))
    (is (= "2025-02-03T23:58" (time/add-days-to-kouta-date-time-string "2024-12-31T23:58" 34)))))

(deftest within
  (let [before-start (ZonedDateTime/parse "2024-12-31T20:00:00+02:00")
        start (ZonedDateTime/parse "2024-12-31T23:58:24.312+02:00")
        in-between (ZonedDateTime/parse "2025-01-01T00:27:12+02:00")
        end (ZonedDateTime/parse "2025-01-01T00:58:24.312+02:00")
        after-end (ZonedDateTime/parse "2025-01-01T12:58:24.312+02:00")]
    (testing "when end is specified"
      (is (= false (time/within? start before-start end)))
      (is (= true (time/within? start start end)))
      (is (= true (time/within? start in-between end)))
      (is (= false (time/within? start end end)))
      (is (= false (time/within? start after-end end))))
    (testing "when end is nil"
      (is (= false (time/within? start before-start nil)))
      (is (= true (time/within? start start nil)))
      (is (= true (time/within? start in-between nil)))
      (is (= true (time/within? start end nil)))
      (is (= true (time/within? start after-end nil))))))

(deftest currently-in-between?
  (let [before-start (ZonedDateTime/parse "2024-12-31T20:00:00+02:00")
        start (ZonedDateTime/parse "2024-12-31T23:58:24.312+02:00")
        in-between (ZonedDateTime/parse "2025-01-01T00:27:12+02:00")
        end (ZonedDateTime/parse "2025-01-01T00:58:24.312+02:00")
        after-end (ZonedDateTime/parse "2025-01-01T12:58:24.312+02:00")]
    (testing "before start"
      (with-redefs [time/current-date-time (constantly before-start)]
        (testing "when start and end is specified"
          (is (= false (time/currently-in-between? start end))))
        (testing "when end is nil"
          (is (= false (time/currently-in-between? start nil))))
        (testing "when start is nil"
          (is (= false (time/currently-in-between? nil nil)))
          (is (= false (time/currently-in-between? nil end))))))
    (testing "at start"
      (with-redefs [time/current-date-time (constantly start)]
        (testing "when start and end is specified"
          (is (= true (time/currently-in-between? start end))))
        (testing "when end is nil"
          (is (= true (time/currently-in-between? start nil))))
        (testing "when start is nil"
          (is (= false (time/currently-in-between? nil nil)))
          (is (= false (time/currently-in-between? nil end))))))
    (testing "between start and end"
      (with-redefs [time/current-date-time (constantly in-between)]
        (testing "when start and end is specified"
          (is (= true (time/currently-in-between? start end))))
        (testing "when end is nil"
          (is (= true (time/currently-in-between? start nil))))
        (testing "when start is nil"
          (is (= false (time/currently-in-between? nil nil)))
          (is (= false (time/currently-in-between? nil end))))))
    (testing "at end"
      (with-redefs [time/current-date-time (constantly end)]
        (testing "when start and end is specified"
          (is (= false (time/currently-in-between? start end))))
        (testing "when end is nil"
          (is (= true (time/currently-in-between? start nil))))))
    (testing "after end"
      (with-redefs [time/current-date-time (constantly after-end)]
        (testing "when start and end is specified"
          (is (= false (time/currently-in-between? start end))))))))

(deftest currently-after?
  (let [before (ZonedDateTime/parse "2024-12-31T20:00:00+02:00")
        time (ZonedDateTime/parse "2024-12-31T23:58:24.312+02:00")
        after (ZonedDateTime/parse "2025-01-01T00:27:12+02:00")]
    (testing "before time"
      (with-redefs [time/current-date-time (constantly before)]
        (testing "when time is specified"
          (is (= false (time/currently-after? time))))
        (testing "when time is nil"
          (is (= false (time/currently-after? nil))))))
    (testing "at time"
      (with-redefs [time/current-date-time (constantly time)]
        (testing "when time is specified"
          (is (= false (time/currently-after? time))))
        (testing "when time is nil"
          (is (= false (time/currently-after? nil))))))
    (testing "after time"
      (with-redefs [time/current-date-time (constantly after)]
        (testing "when time is specified"
          (is (= true (time/currently-after? time))))
        (testing "when time is nil"
          (is (= false (time/currently-after? nil))))))))


(deftest kevat-date?
  (testing "returns correct results for Helsinki times"
    (is (= true (time/kevat-date? (ZonedDateTime/parse "2025-01-01T00:00:00+02:00"))))
    (is (= false (time/kevat-date? (ZonedDateTime/parse "2025-12-31T23:59:59+02:00"))))
    (is (= true (time/kevat-date? (ZonedDateTime/parse "2025-07-31T23:59:59+03:00"))))
    (is (= false (time/kevat-date? (ZonedDateTime/parse "2025-08-01T00:00:00+03:00")))))
  (testing "returns correct results for UTC times"
    (is (= true (time/kevat-date? (ZonedDateTime/parse "2025-01-01T00:00:00Z"))))
    (is (= false (time/kevat-date? (ZonedDateTime/parse "2025-12-31T23:59:59Z"))))
    (is (= true (time/kevat-date? (ZonedDateTime/parse "2025-07-31T23:59:59Z"))))
    (is (= false (time/kevat-date? (ZonedDateTime/parse "2025-08-01T00:00:00Z"))))))

(deftest is-kevat?
  (testing "returns correct values at edges"
    (with-redefs [time/current-local-date #(LocalDate/parse "2025-01-01")]
      (is (= true (time/is-kevat?))))
    (with-redefs [time/current-local-date #(LocalDate/parse "2025-07-31")]
      (is (= true (time/is-kevat?))))
    (with-redefs [time/current-local-date #(LocalDate/parse "2025-08-01")]
      (is (= false (time/is-kevat?))))
    (with-redefs [time/current-local-date #(LocalDate/parse "2025-12-31")]
      (is (= false (time/is-kevat?))))))

(deftest current-year
  (testing "returns current year"
    (with-redefs [time/current-local-date #(LocalDate/parse "2025-01-01")]
      (is (= 2025 (time/current-year))))
    (with-redefs [time/current-local-date #(LocalDate/parse "2024-12-31")]
      (is (= 2024 (time/current-year))))))
