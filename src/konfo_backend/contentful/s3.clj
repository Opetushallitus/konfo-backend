(ns konfo-backend.contentful.s3
  (:require
    [konfo-backend.config :refer [config]]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log])
  (:import [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.services.s3.model
            PutObjectRequest
            ObjectMetadata
            CannedAccessControlList]
           [org.joda.time DateTime]
           [com.amazonaws.regions Regions]
           [com.amazonaws.auth DefaultAWSCredentialsProviderChain]
           [com.amazonaws.auth.profile ProfileCredentialsProvider]
           (java.security MessageDigest)
           (org.apache.commons.codec.binary Hex)
           (java.util Base64)))

(defn- credentials-provider [config]
  (if-let [profile-name (get-in config [:s3 :credentials-profile])]
    (new ProfileCredentialsProvider profile-name)
    (DefaultAWSCredentialsProviderChain/getInstance)))

(defn- region [config]
  (if-let [region (get-in config [:s3 :region])]
    (Regions/fromName region)
    (log/warn "No region name configured!")))

(defn create-client []
  (when-let [region (region config)]
    (-> (AmazonS3Client/builder)
        (.withRegion region)
        (.withCredentials (credentials-provider config))
        (.build))))

(defonce md5-algorithm (MessageDigest/getInstance "MD5"))

(defn calculate-md5 [data]
  (Hex/encodeHexString
    (.digest md5-algorithm data)))

(defn put-object [ttl-hours s3-client file-key data content-type]
  (try
    (let [bucket-name (-> config :s3 :bucket-name)
          bytes       data
          metadata    (doto (ObjectMetadata.)
                            (.setHttpExpiresDate (.toDate (.plusHours (DateTime/now) ttl-hours)))
                            (.setContentType content-type)
                            (.setContentLength (alength bytes)))

          request     (->
                       (PutObjectRequest. bucket-name
                                          file-key
                                          (io/input-stream bytes)
                                          metadata)
                       (.withCannedAcl CannedAccessControlList/PublicRead))]
      (.putObject s3-client request))
    (catch Exception e
      (log/error
       (str "Unable to put object " file-key " to bucket " (-> config :s3 :bucket-name) "! " e))
      (throw e))))

(defn get-object [s3-client file-key]
  (when s3-client
        (try
          (-> (.getObject s3-client (-> config :s3 :bucket-name) file-key)
              (.getObjectContent)
              (slurp))
          (catch Exception e
            (log/error (str "Key not found " file-key "!" e))))))

(defn get-object-md5 [s3-client file-key]
  (try
    (when-let [meta-data (.getObjectMetadata s3-client (-> config :s3 :bucket-name) file-key)]
      (log/info "META: " (str (into [] (.keySet (.getRawMetadata meta-data)))))
      (.getRawMetadataValue meta-data "ETag"))
    (catch Exception e
      (log/info (str "File doesn't exist " file-key "!")))))