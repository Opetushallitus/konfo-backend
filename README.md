# Konfo-backend

Uuden koulutusinformaation (Konfo-UI) backend-sovellus.

## Vaatimukset

Lokaalia ajoa varten tarvitaan lokaali Elasticsearch, josta löytyy indeksoitua dataa.

## Lokaali ajo

Lokaalia ajoa varten kopioi konfiguraatiotiedoston template `dev-configuration/konfo-backend.edn.template`
tiedostoksi `dev-configuration/konfo-backend.edn` ja lisää tiedostoon oikeat arvot:

```
{
    :elastic-url "http://127.0.0.1:9200"
    :elastic-timeout 120000
}
```

Sovelluksen voi käynnistää komennolla:

`lein run`

Updater-sovelluksen voi käynnistää komennolla (testikäyttäjäntunnukset: oph oph):

`lein run-updater`


## Contentful Backup

1. Luo config.json:
```
{
  "spaceId": "source space id",
  "managementToken": "destination space management token"
}
```

2. Asenna Contentful-cli: https://www.contentful.com/developers/docs/tutorials/cli/installation/

3. contentful space export --config config.json
4. contentful space import --config config-2.json --content-file contentful-export-....json

