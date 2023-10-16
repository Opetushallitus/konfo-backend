(ns konfo-backend.index.api
  (:require
   [konfo-backend.index.toteutus :as toteutus]
   [konfo-backend.index.koulutus :as koulutus]
   [konfo-backend.index.haku :as haku]
   [konfo-backend.index.hakukohde :as hakukohde]
   [konfo-backend.index.valintaperuste :as valintaperuste]
   [konfo-backend.index.oppilaitos :as oppilaitos]
   [konfo-backend.eperuste.eperuste :as eperuste]
   [konfo-backend.index.lokalisointi :as lokalisointi]
   [compojure.api.core :as c :refer [GET POST]]
   [ring.util.http-response :refer :all]
   [clj-log.access-log :refer [with-access-logging]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]
   [konfo-backend.ataru.service :as ataru]
   [konfo-backend.koodisto.koodisto :as koodisto]))

(def paths
  "|  /translation/{lng}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae käännökset
   |      description: Hae käännökset annetulla kielellä (fi/sv/en). Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: lng
   |          schema:
   |            type: string
   |          required: true
   |          description: Kieli, joko 'fi', 'sv' tai 'en'
   |          example: sv
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /koulutus/{oid}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae koulutus
   |      description: Hae koulutuksen tiedot annetulla oidilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Koulutuksen yksilöivä oid
   |          example: 1.2.246.562.13.00000000000000000001
   |        - in: query
   |          name: draft
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Näytetäänkö luonnos esikatselua varten
   |          example: false
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /toteutus/{oid}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae toteutus
   |      description: Hae toteutuksen tiedot annetulla oidilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Toteutuksen yksilöivä oid
   |          example: 1.2.246.562.17.00000000000000000001
   |        - in: query
   |          name: draft
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Näytetäänkö luonnos esikatselua varten
   |          example: false
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /haku/{oid}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae haku
   |      description: Hae haun tiedot annetulla oidilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Haun yksilöivä oid
   |          example: 1.2.246.562.29.00000000000000000001
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /hakukohde/{oid}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae hakukohde
   |      description: Hae hakukohteen tiedot annetulla oidilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Hakukohteen yksilöivä oid
   |          example: 1.2.246.562.20.00000000000000000001
   |        - in: query
   |          name: draft
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Näytetäänkö luonnos esikatselua varten
   |          example: false
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /haku/{oid}/demo:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae tieto onko haun hakulomakkeen demo käytössä
   |      description: Hae tieto haun hakulomakkeen demosta annetulla oidilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Haun yksilöivä oid
   |          example: 1.2.246.562.20.00000000000000000001
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: object
   |                properties:
   |                  demoAllowed:
   |                    type: boolean
   |                    description: Onko haun hakulomakkeen demo käytössä?
   |        '404':
   |          description: Not found
   |  /valintaperuste/{id}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae valintaperuste
   |      description: Hae valintaperusteen tiedot annetulla id:llä. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: id
   |          schema:
   |            type: string
   |            format: uuid
   |          required: true
   |          description: Valintaperusteen yksilöivä id
   |          example: 912a545e-7800-4cfa-8d1c-1ea27aba7f2f
   |        - in: query
   |          name: draft
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Näytetäänkö luonnos esikatselua varten
   |          example: false
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /oppilaitos/{oid}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae oppilaitos tai oppilaitoksen osa
   |      description: Hae oppilaitoksen tiedot annetulla oidilla, jos oppilaitosta ei löydy, haetaan oppilaitoksen osaa. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Oppilaitoksen tai oppilaitoksen osan yksilöivä oid
   |          example: 1.2.246.562.10.12345
   |        - in: query
   |          name: draft
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Näytetäänkö luonnos esikatselua varten
   |          example: false
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /oppilaitoksen-osa/{oid}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae oppilaitoksen osa
   |      description: Hae oppilaitoksen osan tiedot annetulla oidilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Oppilaitoksen osan yksilöivä oid
   |          example: 1.2.246.562.10.12345
   |        - in: query
   |          name: draft
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Näytetäänkö luonnos esikatselua varten
   |          example: false
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /eperuste/{id}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae ePeruste
   |      description: Hae ePerusteen tiedot annetulla id:llä. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: id
   |          schema:
   |            type: string
   |          required: true
   |          description: ePerusteen yksilöivä id
   |          example: 12234
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /kuvaus/{id}:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae kuvaukset ePerusteista
   |      description: Hae koulutuksen kuvaus (ja osaamisalakuvaukset) ePerusteista eperuste-id:n perusteella.
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: id
   |          schema:
   |            type: string
   |          required: true
   |          description: ePerusteen yksilöivä id
   |          example: 12234
   |        - in: query
   |          name: osaamisalakuvaukset
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko osaamisalakuvaukset
   |          example: true
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /kuvaus/{id}/tutkinnonosat:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae tutkinnon osan kuvaukset ePerusteista
   |      description: Hae tutkinnon osan kuvaus ePerusteista eperuste-id:n ja tutkinnon osan koodi-uri:n perusteella.
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: id
   |          schema:
   |            type: string
   |          required: true
   |          description: ePerusteen yksilöivä id
   |          example: 12234
   |        - in: query
   |          name: koodi-urit
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          required: false
   |          description: pilkulla erotettu lista tutkinnon osien koodi-ureja.
   |            Jos puuttuu, palautetaan kaikkien tutkinnon osien kuvaukset
   |          example: tutkinnonosat_123,tutkinnonosat_124
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /kuvaus/{id}/osaamisalat:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae osaamisalojen kuvaukset ePerusteista
   |      description: Hae osaamisalan kuvaus ePerusteista eperuste-id:n ja osaamisalan koodi-uri:n perusteella.
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: id
   |          schema:
   |            type: string
   |          required: true
   |          description: ePerusteen yksilöivä id
   |          example: 12234
   |        - in: query
   |          name: koodi-urit
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          required: false
   |          description: pilkulla erotettu lista osaamisalojen koodi-ureja.
   |            Jos puuttuu, palautetaan kaikkien osaamisalojen kuvaukset
   |          example: osaamisala_123,osaamisala_124
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /koodisto/{koodistouri}/koodit:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae koodiston koodit koodistourilla
   |      description: Hae koodit koodistosta käyttäen koodiston koodistouria
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: koodistouri
   |          schema:
   |            type: string
   |          required: true
   |          description: koodiston tunnus
   |          example: kielivalikoima
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /suosikit-hakutiedot:
   |    get:
   |      tags:
   |        - internal
   |      summary: Hae hakukohde-suosikeille tietoja
   |      description: Hae hakukohteiden tietoja suosikit-listausta varten
   |        Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: hakukohde-oids
   |          style: form
   |          explode: false
   |          schema:
   |            type: array
   |            items:
   |              type: string
   |          required: true
   |          description: Pilkulla erotettu lista hakukohteiden oideja
   |          example: kielivalikoima
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found")

(def routes
  (c/routes
   (GET "/translation/:lng" [:as request]
     :path-params [lng :- String]
     (with-access-logging request (if (not (some #{lng} ["fi" "sv" "en"]))
                                    (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
                                    (if-let [result (lokalisointi/get lng)]
                                      (ok result)
                                      (not-found "Not found")))))

   (GET "/koulutus/:oid" [:as request]
     :query-params [{draft :- Boolean false}]
     :path-params [oid :- String]
     (with-access-logging request (if-let [result (koulutus/get oid draft)]
                                    (ok result)
                                    (not-found "Not found"))))
   (GET "/toteutus/:oid" [:as request]
     :query-params [{draft :- Boolean false}]
     :path-params [oid :- String]
     (with-access-logging request (if-let [result (toteutus/get oid draft)]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/haku/:oid" [:as request]
     :path-params [oid :- String]
     (with-access-logging request (if-let [result (haku/get oid)]
                                    (ok result)
                                    (not-found "Not found"))))
   (GET "/haku/:oid/demo" [:as request]
     :path-params [oid :- String]
     (with-access-logging request (if-let [haku (haku/get oid)]
                                    (let [ataru-lomake? (= (:hakulomaketyyppi haku) "ataru")
                                          demo-allowed? (if ataru-lomake? (ataru/demo-allowed-for-haku? oid) false)]
                                      (ok {"demoAllowed" demo-allowed?}))
                                    (not-found "Not found"))))

   (GET "/hakukohde/:oid" [:as request]
     :query-params [{draft :- Boolean false}]
     :path-params [oid :- String]
     (with-access-logging request (if-let [result (hakukohde/get oid draft)]
                                    (ok result)
                                    (not-found "Not found")))) 
   
   (GET "/suosikit-hakutiedot" [:as request]
     :query-params [{draft :- Boolean false}
                    {hakukohde-oids :- String nil}]
     (with-access-logging request 
       (if-let [result (toteutus/search-by-hakukohde-oids (comma-separated-string->vec hakukohde-oids))]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/valintaperuste/:id" [:as request]
     :query-params [{draft :- Boolean false}]
     :path-params [id :- String]
     (with-access-logging request (if-let [result (valintaperuste/get id draft)]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/oppilaitos/:oid" [:as request]
     :query-params [{draft :- Boolean false}]
     :path-params [oid :- String]
     (with-access-logging request (if-let [result (oppilaitos/get oid draft)]
                                    (ok result)
                                    (if-let [osa-result (oppilaitos/get-osa oid draft)]
                                      (ok osa-result)
                                      (not-found "Not found")))))

   (GET "/oppilaitoksen-osa/:oid" [:as request]
     :query-params [{draft :- Boolean false}]
     :path-params [oid :- String]
     (with-access-logging request (if-let [result (oppilaitos/get-osa oid draft)]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/eperuste/:id" [:as request]
     :path-params [id :- String]
     (with-access-logging request (if-let [result (eperuste/get-eperuste-by-id id)]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/tutkinnonosa/:id" [:as request]
     :path-params [id :- String]
     (with-access-logging request (if-let [result (eperuste/get-tutkinnonosa-by-id id)]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/kuvaus/:id" [:as request]
     :path-params [id :- String]
     :query-params [{osaamisalakuvaukset :- Boolean false}]
     (with-access-logging request (if-let [result (eperuste/get-kuvaus-by-eperuste-id id osaamisalakuvaukset)]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/kuvaus/:id/tutkinnonosat" [:as request]
     :path-params [id :- String]
     :query-params [{koodi-urit :- String nil}]
     (with-access-logging request (if-let [result (eperuste/get-tutkinnonosa-kuvaukset id (comma-separated-string->vec koodi-urit))]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/kuvaus/:id/osaamisalat" [:as request]
     :path-params [id :- String]
     :query-params [{koodi-urit :- String nil}]
     (with-access-logging request (if-let [result (eperuste/get-osaamisala-kuvaukset id (comma-separated-string->vec koodi-urit))]
                                    (ok result)
                                    (not-found "Not found"))))

   (GET "/koodisto/:koodistouri/koodit" [:as request]
     :path-params [koodistouri :- String]
     (with-access-logging request (if-let [result (koodisto/list-koodit koodistouri)]
                                    (ok result)
                                    (not-found "Not found"))))))
