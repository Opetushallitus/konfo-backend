(ns konfo-backend.palaute.sqs
  (:require
   [konfo-backend.config :refer [config]]
   [clojure.tools.logging :as log])
  (:import com.amazonaws.services.sqs.AmazonSQSClientBuilder
           [com.amazonaws.services.sqs.model
            ReceiveMessageRequest
            SendMessageRequest
            DeleteMessageBatchRequestEntry]))

(definterface IBody
  (^String sendMessage [req]))

(deftype AWSFakeSQSClient []
  IBody
  (sendMessage [this request]
    (prn request)))

(defn create-sqs-client []
  (if (:dev config)
    (AWSFakeSQSClient.)
    (-> (AmazonSQSClientBuilder/standard)
      (.withRegion (:region (:s3 config)))
      .build)))

(defn send-message [amazon-sqs queue-url message-body]
  {:pre [(some? amazon-sqs)
         (some? queue-url)
         (some? message-body)]}
  (log/info (str "Sending feedback with:" (str (type amazon-sqs)) (str queue-url)))
  (.sendMessage amazon-sqs
    (doto
     (-> (new SendMessageRequest)
         (.withQueueUrl queue-url))
      (.setMessageBody (str message-body)))))
