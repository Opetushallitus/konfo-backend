(ns konfo-backend.contentful.contentful-updater-test
  (:require [clojure.test :refer :all]
            [konfo-backend.contentful.contentful :as contentful]
            [konfo-backend.contentful.contentful-updater :as updater]
            [konfo-backend.contentful.s3 :as s3])
  (:import (com.contentful.java.cda CDAAsset)
           (com.google.gson Gson)
           (java.util ArrayList HashMap)
           (org.mockito Mockito)))

(defn- asset-with-details [details]
  (let [asset (Mockito/mock CDAAsset)]
    (-> (Mockito/when (.fileField asset "details"))
        (.thenReturn details))
    asset))

(deftest contentful-image-dimensions-test
  (testing "reads width and height from file.details.image (Gson parses numbers as doubles)"
    (is (= [2000 1500]
           (updater/contentful-image-dimensions
             (asset-with-details (HashMap. {"size" 12345.0
                                            "image" (HashMap. {"width" 2000.0 "height" 1500.0})}))))))
  (testing "returns nil when details have no image dimensions"
    (is (nil? (updater/contentful-image-dimensions (asset-with-details (HashMap. {"size" 12345.0}))))))
  (testing "returns nil when the asset has no details"
    (is (nil? (updater/contentful-image-dimensions (asset-with-details nil))))))

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
                  updater/fetch->image      (fn [_] [pdf-bytes "application/pdf"])
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

(def image-url "//images.ctfassets.net/space/abc/token/kuva.png")

(deftest replace-image-links-test
  (testing "replaces a cached image link with its S3 url"
    (is (= "![kuva](space/abc/token/kuva.png.jpg)"
           (updater/replace-image-links {image-url ["space/abc/token/kuva.png.jpg"]}
                                        "entry1" (str "![kuva](" image-url ")")))))
  (testing "marks an uncached image link as broken without pointing to Contentful"
    (is (= "![kuva](space/abc/token/kuva.png#broken)"
           (updater/replace-image-links {} "entry1" (str "![kuva](" image-url ")")))))
  (testing "leaves svg and non-protocol-relative links unchanged"
    (let [md "![a](//images.ctfassets.net/x/y.svg) [b](https://example.com)"]
      (is (= md (updater/replace-image-links {} "entry1" md)))))
  (testing "returns markdown without links unchanged"
    (is (= "plain text" (updater/replace-image-links {} "entry1" "plain text")))))
