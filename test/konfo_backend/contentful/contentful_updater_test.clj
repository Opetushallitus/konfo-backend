(ns konfo-backend.contentful.contentful-updater-test
  (:require [clojure.test :refer :all]
            [konfo-backend.contentful.contentful :as contentful]
            [konfo-backend.contentful.contentful-updater :as updater]
            [konfo-backend.contentful.s3 :as s3])
  (:import (com.google.gson Gson)
           (java.util ArrayList HashMap)))

(def pdf-key "space/abc/doc.pdf")
(def pdf-bytes (.getBytes "pdf content"))

(defn- pdf-asset []
  (doto (HashMap.)
    (.put "url" pdf-key)
    (.put "original" (str "//assets.ctfassets.net/" pdf-key))))

(defn- uploaded-keys
  "Runs asset-store-handler for one non-image asset (no md5 in url-cache)
  and returns the keys written to S3, excluding asset.json."
  [s3-md5]
  (let [writes (atom [])]
    (with-redefs [s3/get-object             (fn [_ _] nil)
                  s3/get-object-md5         (fn [_ _] s3-md5)
                  contentful/get-assets-raw (fn [_ _] (ArrayList. [(pdf-asset)]))
                  updater/fetch->image      (fn [_ _] [pdf-bytes "application/pdf"])
                  updater/store->s3         (fn [_ _ _ key _] (swap! writes conj key))]
      (updater/asset-store-handler nil nil (Gson.) (atom {}) "asset" "fi"
                                   "2000/fi/" "1000/fi/asset.json")
      (remove #(= "2000/fi/asset.json" %) @writes))))

(deftest asset-store-handler-non-image-test
  (testing "skips the upload when the file in S3 is identical"
    (is (empty? (uploaded-keys (s3/calculate-md5 pdf-bytes)))))
  (testing "uploads when the file in S3 differs"
    (is (= [pdf-key] (uploaded-keys "0123456789abcdef0123456789abcdef"))))
  (testing "uploads when the file is not in S3 yet"
    (is (= [pdf-key] (uploaded-keys nil)))))
