(ns konfo-backend.util.rest
  (:refer-clojure :exclude [get])
  (:require [clj-http.client :as client]
            [clojure.string :refer [upper-case]]
            [cheshire.core :as cheshire]
            [clojure.walk :as walk]
            [clojure.tools.logging :as log]))

(defn add-headers [options]
  (let [caller-id "1.2.246.562.10.00000000001.konfo-backend"]
    (-> options
        (assoc-in [:headers "Caller-id"] caller-id)
        (assoc-in [:headers "CSRF"] caller-id)
        (assoc-in [:cookies "CSRF"] {:value caller-id :path "/"}))))

(defn get [url opts]
  (let [options (add-headers opts)]
    (log/info "Making GET call to " url " with options: " options)
    (client/get url options)))

(defn put [url opts]
  (let [options (add-headers opts)]
    (log/info "Making PUT call to " url " with options: " options)
    (client/put url options)))

(defn post [url opts]
  (let [options (add-headers opts)]
    (log/info "Making POST call to " url " with options: " options)
    (client/post url options)))

(defn request [opts]
  (-> opts
      add-headers
      client/request))

(defn handle-error
  [url method-name response]
  (let [status (:status response)
        body (walk/keywordize-keys (cheshire/parse-stream (clojure.java.io/reader (:body response))))]
    (case status
      200 body
      404 (do (log/warn "Got " status " from " method-name ": " url " with body " body) nil)
      nil (do (log/error "Got " status " from " method-name ": " url " with error: " (if (instance? Exception response) (.getMessage response) response)) nil)
      (do (log/error "Got " status " from " method-name ": " url " with response " response) nil))))

(defn ->json-body-with-error-handling
  [url method opts]
  (let [method-name (upper-case (str method))
        f (case method :post post :put put :get get)]
    (log/debug method-name " => " url)
    (let [response (f url (merge opts {:throw-exceptions false :as :stream}))]
      (handle-error url method-name response))))

(defn get->json-body
  ([url query-params]
   (->json-body-with-error-handling url :get {:query-params query-params}))
  ([url]
   (get->json-body url {})))

(defn post->json-body
  ([url body content-type]
   (->json-body-with-error-handling url :post {:body body :content-type (keyword content-type)}))
  ([url body]
   (post->json-body url body :json)))
