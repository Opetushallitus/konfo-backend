(ns konfo-backend.ataru.service
  (:require [konfo-backend.ataru.client :as client]))

(defn- get-demo-allowed
  [{demo-allowed :demo-allowed}]
  demo-allowed)

(defn demo-allowed-for-hakukohde?
  [hakukohde-oid]
  (-> hakukohde-oid
    client/get-form-for-hakukohde
    get-demo-allowed))
