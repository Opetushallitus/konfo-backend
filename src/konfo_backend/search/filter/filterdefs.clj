(ns konfo-backend.search.filter.filterdefs
  (:require
    [konfo-backend.search.filter.query-tools :refer [keyword-terms-query hakutieto-query single-tyoelama-boolean-query make-combined-boolean-filter-query lukiolinjat-and-osaamisala-filters]]
    [konfo-backend.search.tools :refer [hakuaika-filter-query]]
    [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(def koulutustyyppi
  {:id :koulutustyyppi :make-query #(keyword-terms-query "koulutustyypit" %)})

(def sijainti
  {:id :sijainti :make-query #(keyword-terms-query "sijainti" %)})

(def opetuskieli
  {:id :opetuskieli :make-query #(keyword-terms-query "opetuskielet" %)})

(def koulutusala
  {:id :koulutusala :make-query #(keyword-terms-query "koulutusalat" %)})

(def opetustapa
  {:id :opetustapa :make-query #(keyword-terms-query "opetustavat" %)})

(def valintatapa
  {:id :valintatapa :make-query #(hakutieto-query "hakutiedot" "valintatavat" %)})

(def hakutapa
  {:id :hakutapa :make-query #(hakutieto-query "hakutiedot" "hakutapa" %)})

(def jotpa
  {:id :jotpa :make-query #(single-tyoelama-boolean-query "hasJotpaRahoitus")})

(def tyovoimakoulutus
  {:id :tyovoimakoulutus :make-query #(single-tyoelama-boolean-query "isTyovoimakoulutus")})

(def taydennyskoulutus
  {:id :taydennyskoulutus :make-query #(single-tyoelama-boolean-query "isTaydennyskoulutus")})

(def yhteishaku
  {:id :yhteishaku :make-query #(hakutieto-query "hakutiedot" "yhteishakuOid" %)})

(def pohjakoulutusvaatimus
  {:id :pohjakoulutusvaatimus :make-query #(hakutieto-query "hakutiedot" "pohjakoulutusvaatimukset" %)})

(def oppilaitos
  {:id :oppilaitos :make-query #(keyword-terms-query "oppilaitosOid" %)})

(def lukiopainotukset
  {:id :lukiopainotukset :make-query #(keyword-terms-query "lukiopainotukset" %)})

(def lukiolinjaterityinenkoulutustehtava
  {:id :lukiolinjaterityinenkoulutustehtava :make-query #(keyword-terms-query "lukiolinjaterityinenkoulutustehtava" %)})

(def osaamisala
  {:id :osaamisala :make-query #(keyword-terms-query "osaamisala" %)})

(def filter-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakutapa yhteishaku pohjakoulutusvaatimus oppilaitos])

(def jarjestaja-filter-definitions
  [sijainti opetuskieli koulutusala opetustapa lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala oppilaitos])

(def oppilaitos-filter-definitions
  [koulutustyyppi sijainti opetuskieli koulutusala opetustapa])

(def tyoelama-sub-filters
  [jotpa tyovoimakoulutus taydennyskoulutus])

(def hakukaynnissa-filter
  {:make-query (fn [constraints current-time] (when (true? (:hakukaynnissa constraints))(hakuaika-filter-query current-time)))})

(def combined-tyoelama-filter
  {:make-query #(make-combined-boolean-filter-query % tyoelama-sub-filters)})

(def combined-jarjestaja-filters
  {:make-query #(lukiolinjat-and-osaamisala-filters %)})