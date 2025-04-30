(ns konfo-backend.contentful.s3
  (:require
    [konfo-backend.config :refer [config]]
    [clojure.tools.logging :as log])
  (:import (software.amazon.awssdk.core.sync RequestBody)
           (software.amazon.awssdk.core.sync ResponseTransformer)
           (software.amazon.awssdk.services.s3 S3Client)
           (software.amazon.awssdk.services.s3.model GetObjectRequest HeadObjectRequest PutObjectRequest NoSuchKeyException ObjectCannedACL)
           (software.amazon.awssdk.regions Region)
           (software.amazon.awssdk.auth.credentials
            DefaultCredentialsProvider
            ProfileCredentialsProvider)
           (java.time Instant)
           (java.time.temporal ChronoUnit)
           (java.security MessageDigest)
           (org.apache.commons.codec.binary Hex)))

(defn- credentials-provider [config]
  (if-let [profile-name (get-in config [:s3 :credentials-profile])]
    (ProfileCredentialsProvider/create profile-name)
    (DefaultCredentialsProvider/create)))

(defn- region [config]
  (if-let [region (get-in config [:s3 :region])]
    (Region/of region)
    (log/warn "No region name configured!")))

(defn create-client []
  (when-let [region (region config)]
    (-> (S3Client/builder)
        (.region region)
        (.credentialsProvider (credentials-provider config))
        (.build))))

(defonce md5-algorithm (MessageDigest/getInstance "MD5"))

(defn calculate-md5 [data]
  (Hex/encodeHexString
    (.digest md5-algorithm data)))

(defn put-object [ttl-hours ^S3Client s3-client file-key data content-type]
  (try
    (let [bucket-name (-> config :s3 :bucket-name)
          bytes       data
          request     (-> (PutObjectRequest/builder)
                          (.bucket bucket-name)
                          (.key file-key)
                          (.expires
                            (.plus (Instant/now)
                                   (long ttl-hours)
                                   (ChronoUnit/HOURS)))
                          (.contentType content-type)
                          (.contentLength (long (alength bytes)))
                          (.acl (str (ObjectCannedACL/PUBLIC_READ)))
                          (.build))]
      (.putObject s3-client request
                  (RequestBody/fromBytes bytes)))
    (catch Exception e
      (log/error
        (str "Unable to put object " file-key
             " to bucket " (-> config :s3 :bucket-name) "! " e))
      (throw e))))

(defn get-object [^S3Client s3-client ^String file-key]
  (when (and file-key s3-client)
        (try
          (-> s3-client
              (.getObject (-> (GetObjectRequest/builder)
                              (.bucket (-> config :s3 :bucket-name))
                              (.key file-key)
                              (.build))
                          (ResponseTransformer/toBytes))
              (.asByteArray)
              (slurp))
          (catch NoSuchKeyException e
            (log/error (str "Key not found " file-key "!" e))))))

(defn get-object-md5 [s3-client file-key]
  (try
    (-> s3-client
        (.headObject (-> (HeadObjectRequest/builder)
                         (.bucket
                           (-> config :s3 :bucket-name))
                         (.key file-key)
                         (.build)))
        (.eTag))
    (catch NoSuchKeyException e
      (log/info (str "File doesn't exist " file-key "!")))))
