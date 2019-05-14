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
