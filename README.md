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
Kirjoitushetkellä käytössä openJDK11.

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
 :search-terms-boost    {:koulutusnimi 20
                         :toteutusNimi 6
                         :asiasanat 5
                         :tutkintonimikkeet 4
                         :ammattinimikkeet 3
                         :koulutus_organisaationimi 2
                         :toteutus_organisaationimi 1
                         :kunta 1
                         :language-default 1
                         :default 0.1}

 :swagger-ui {:syntaxHighlight false}
 :koodisto-host "virkailija.untuvaopintopolku.fi"
 }
```

### 3.2. Testien ajaminen

Testit ajetaan [Kaocha test runnerilla](https://cljdoc.org/d/lambdaisland/kaocha/1.87.1366/doc/readme).
Kaikki testit saa ajettua komentoriviltä komennolla `lein test`.

Yksittäisen testitiedoston saa ajettua lisäämällä `^:focus`-tagin testitiedoston namespace-määrittelyn alkuun, esim.
`ns ^:focus kouta-indeksoija-service.indexer.kouta-indexer-test`.

Yksittäisen testin saa ajettua lisäämällä `^:focus`-tagi ennen ajettavan testin nimeä, esim.
`deftest ^:focus index-oppilaitos-test`.

Testejä voi ajaa myös watch-modessa komennolla `lein test --watch` eli test runner ajaa testit automaattisesti uudestaan, kun koodia tai testejä muutetaan.

Testit käynnistävät Elasticsearchin docker-kontissa satunnaiseen vapaaseen porttiin.
Watch-moodissa elasticsearch käynnistetään vain kerran, kun testiajo käynnistetään, ja se pysyy taustalla käynnissä kunnes testiajo lopetetaan.

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

### 3.4.1 Kikkoja lokaaliin kehitykseen

Jos täytyy debugata tai kehittää konfo-backendin generoimia elasticsearch kyselyitä, tähän yksi keino on 
lisätä queryn generoivaan koodiin printtausta (esim. `(println (chesire.core/generate-string query))`),
laittaa konfo-backend ajoon ja tehdä haluttu request swaggerista. Sen jälkeen printatun elastic kyselyn voi kopioida haluamaansa
http clientiin, jossa kyselyn kehitystä voi jatkaa. 

Http clientin kyselyitä voi tehdä halutessaan jonkin testiympäristön
elasticia vastaa. Testiympäristön elasticien linkit löytyy Confluencesta ja autentikointia varten saat tiedon cloud-basen secretseistä

## 4. Hakurajaimet

### 4.1 Hakutietorajaimet

Koulutus- ja oppilaitos-search hitteihin tallentuu rakenteista hakutiedot-dataa, jossa tärkeimpänä sisältönä on voimassaoloaika. Nämä rajaimet vaativat paljon kompleksisempaa käsittely kuin muut rajaimet, sillä niissä täytyy ottaa mahdollisesti huomioon halutaanko nähdä vain sellaista dataa, jonka haku on käynnissä.

Asiaa pystyy parhaiten lähestymään käytännön esimerkillä:
* Koulutus (hit), jolla on toteutus, jolla on kaksi hakukohdetta
* Ensimmäisen hakukohteen hakuaika on käynnissä ja sillä on pohjakoulutusvaatimus X
* Toisen hakukohteen hakuaika ei ole vielä alkanut ja sillä on pohjakoulutusvaatimus Y
* Käyttäjä hakee rajauksella X + haku käynnissä -> pitää saada tulos
* Käyttäjä hakee rajauksella Y + haku käynnissä -> ei pidä saada tuloksia

Ym. tarpeen vuoksi hakutietokohtaiset rajaimet on toteutettu toimimaan yhdessä haku-käynnissä rajaimen kanssa niin että elastic käsittelee nämä yhteisinä ehtoina (per rajain). Käytännön tasolla siis tyyliin:

* Haku-käynnissä + pohjakoulutusvaatimus = "Koulutus-search-hitillä täytyy olla jokin hakutieto joka täyttää kaksi ehtoa - haku-käynnissä + tietty pohjakoulutusvaatimus"

## 5. Ympäristöt

### 5.1. Testiympäristöt

Testiympäristöjen swaggerit löytyvät seuraavista osoitteista:

- [untuva](https://untuvaopintopolku.fi/konfo-backend/swagger)
- [hahtuva](https://hahtuvaopintopolku.fi/konfo-backend/swagger)
- [QA eli pallero](https://testiopintopolku.fi/konfo-backend/swagger)

### 5.2. Asennus

Asennus hoituu samoilla työkaluilla kuin muidenkin OPH:n palvelujen. 
[Cloud-basen dokumentaatiosta](https://github.com/Opetushallitus/cloud-base/tree/master/docs)
ja ylläpidolta löytyy apuja.

### 5.3. Lokit

Konfo-backendin lokit löytyvät AWS:n cloudwatchista log groupista <testiympäristö>-app-konfo-backend, 
esim. hahtuva-app-konfo-backend. Lisäohjeita ylläpidolta.

## 6. Lisätietoa

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
