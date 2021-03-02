# Konfo-backend

## 1. Palvelun tehtävä

Konfo-backend tarjoaa konfo-ui:lle rajapinnat kouta-indeksoijan dataan. Lisäksi konfo-backend tarjoaa
/external-rajapinnan opetushallituksesta riippumattomille palveluille.

## 2. Arkkitehtuuri

Konfo-backend on clojurella toteutettu ring/compojure/compojure-api web API. Palvelu on itse tilaton
ja tekee suoria kyselyitä elasticsearchin dataan, jonka kouta-indeksoija on luonut.

## 3. Kehitysympäristö

### 3.1. Esivaatimukset

Asenna haluamallasi tavalla koneellesi
1. [Clojure](https://clojure.org/guides/getting_started)
2. [Docker](https://www.docker.com/get-started) (Elasticsearchia varten)
3. [Leiningen](https://leiningen.org/) (Valitse asennus haluammallasi package managerilla)

Lisäksi tarvitset Java SDK:n (Unix pohjaisissa käyttöjärjestelmissä auttaa esim. [SDKMAN!](https://sdkman.io/)).
Katso [.travis.yml](.travis.yml) mitä versioita sovellus käyttää. Kirjoitushetkellä käytössä openJDK11.

Konfo-backendin saa konfiguroitua luomalla tiedoston `dev-configuration/konfo-backend.edn` (laitettu .gitignoreen) ja asettamalla sinne
tarvittavat arvot. Tiedostosta `dev-configuration/konfo-backend.template` näkee mitä arvoja sovellus tarvitsee toimiakseen.
Kirjoitushetken esimerkki konfigista, joka toimii lokaalilla Elasticsearchilla:

```clojure
{
 :elastic-url "http://localhost:9200"
 :elastic-timeout 120000

 :contentful-update-username "oph"
 :contentful-update-password "oph"

 :contentful-space-id "space-id"
 :contentful-access-token "access-token"
 :contentful-preview-token "preview-token"

 :dev true
 :konfo-host "http://localhost"
 }
```

### 3.2. Testien ajaminen

Testit saa ajettua komentoriviltä komennolla `lein test`

Yksittäisen testitiedoston saa ajettua `lein test <namespacen nimi>`.
Esimerkiksi `lein test konfo-backend.index.toteutus-test`

Yksittäisen testin saa ajettua `lein test :only <namespacen nimi>/<testin nimi>`.
Esimerkiksi `lein test :only konfo-backend.index.toteutus-test/toteutus-test`

Testit käynnistävät Elasticsearchin docker-kontissa satunnaiseen vapaaseen porttiin.

### 3.3. Ajaminen lokaalisti

Ennen konfo-backendin ajamista lokaalisti täytyy pyörimässä olla Elasticsearch.
Elasticsearchia kannattaa pyörittää docker-kontissa. Ohjeet tähän ja testidatan luontiin
löydät kouta-indeksoijan repon readme:stä.

Kun Elasticsearch-kontti on pyörimässä, sovelluksen saa käyntiin ajamalla projektin juuressa `lein run`

Swagger löytyy selaimella osoitteesta http://localhost:3006/konfo-backend/swagger

### 3.4. Kehitystyökalut

Suositeltava kehitysympäristö on [IntelliJ IDEA](https://www.jetbrains.com/idea/) + [Cursive plugin](https://cursive-ide.com/)
mutta sovelluksen ja testien ajamisen kannalta nämä eivät ole välttämättömiä. Jos et tee paljon clojure-kehitystä, myöskään cursive ei
ole välttämätön.

## 4. Ympäristöt

### 4.1. Testiympäristöt

Testiympäristöjen swaggerit löytyvät seuraavista osoitteista:

- [untuva](https://beta.untuvaopintopolku.fi/konfo-backend/swagger)
- [hahtuva](https://beta.hahtuvaopintopolku.fi/konfo-backend/swagger)
- [QA eli pallero](https://beta.testiopintopolku.fi/konfo-backend/swagger)

### 4.2. Asennus

Asennus hoituu samoilla työkaluilla kuin muidenkin OPH:n palvelujen. 
[Cloud-basen dokumentaatiosta](https://github.com/Opetushallitus/cloud-base/tree/master/docs)
ja ylläpidolta löytyy apuja.

### 4.3. Lokit

Konfo-backendin lokit löytyvät AWS:n cloudwatchista log groupista <testiympäristö>-app-konfo-backend, 
esim. hahtuva-app-konfo-backend. Lisäohjeita ylläpidolta.

### 4.4. Continuous integration

https://travis-ci.com/github/Opetushallitus/konfo-backend

## 5. Lisätietoa

Vanhan README:n sisältöä jonka paikkansapitävyyttä ei ole selvitetty:

---
Updater-sovelluksen voi käynnistää komennolla (testikäyttäjäntunnukset: oph oph):

`lein run-updater`

Muista lisätä paikallisesti kehittäessä `dev-configuration/konfo-backend.edn` muuttujat:
```
{
 :contentful-space-id      "............"
 :contentful-access-token  "..........................................."
 :contentful-preview-token "..........................................."

 :contentful-update-username "oph"
 :contentful-update-password "oph"
}
```
Token ja space-id löytyy contentful.com palvelusta Settings -> API keys.

## Contentful Backup

1. Luo config.json:
```
{
  "spaceId": "source space id",
  "managementToken": "destination space management token"
}
```

2. [Asenna Contentful-cli](https://www.contentful.com/developers/docs/tutorials/cli/installation/)

Import & Export [Ohjeet](https://www.contentful.com/developers/docs/tutorials/cli/import-and-export)
3. contentful space export --config config.json --include-archived true --skip-webhooks true
4. contentful space import --config config-2.json  --include-archived true --skip-webhooks true --content-file contentful-export-....json
