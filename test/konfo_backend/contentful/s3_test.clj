(ns konfo-backend.contentful.s3-test
  (:require [clojure.test :refer :all]
            [konfo-backend.contentful.s3 :as s3]))

(deftest etag->md5-test
  (testing "strips the quotes S3 puts around the ETag"
    (is (= "d41d8cd98f00b204e9800998ecf8427e"
           (s3/etag->md5 "\"d41d8cd98f00b204e9800998ecf8427e\""))))
  (testing "leaves an unquoted value unchanged"
    (is (= "d41d8cd98f00b204e9800998ecf8427e"
           (s3/etag->md5 "d41d8cd98f00b204e9800998ecf8427e"))))
  (testing "returns nil when the object has no ETag"
    (is (nil? (s3/etag->md5 nil))))
  (testing "a quoted ETag equals the MD5 calculated from the same bytes"
    (let [data (.getBytes "konfo")
          md5  (s3/calculate-md5 data)]
      (is (= md5 (s3/etag->md5 (str "\"" md5 "\"")))))))
