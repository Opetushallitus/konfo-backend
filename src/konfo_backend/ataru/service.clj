(ns konfo-backend.ataru.service
  (:require [konfo-backend.ataru.client :as client]))

(defn- get-demo-allowed
  [{demo-allowed :demo-allowed}]
  demo-allowed)

(defn demo-allowed-for-haku?
  [haku-oid]
  (-> haku-oid
    client/get-form-for-haku
    get-demo-allowed))
