(ns konfo-backend.palaute.palaute
  (:require
   [cheshire.core :as cheshire]
   [konfo-backend.config :refer [config]]
   [clojure.tools.logging :as log]
   [konfo-backend.palaute.sqs :as sqs]))


(defn build-palaute-request
  [feedback]
  {:stars      (:stars feedback)
   :feedback   (:feedback feedback)
   :user-agent (:user-agent feedback)
   :created-at (.getTime (java.util.Date.))
   :data       {}
   :key        "konfo"})

(defn send-feedback
  [amazon-sqs feedback]
  (try
    (log/info "Sending feedback to Palautepalvelu" feedback)
    (sqs/send-message amazon-sqs
      (-> config :s3 :feedback-queue)
      (-> feedback
        (build-palaute-request)
        (cheshire/generate-string)))
    (catch Exception e
      (log/warn (str "Feedback didn't go through: " (str (type e)) (str e) (.getMessage e))))))