(ns konfo-backend.search.koulutus-kuvaukset-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.koulutus.kuvaukset :refer [select-amm-tutkinnon-osa-kuvaus]]
            [cheshire.core :as cheshire]))

(def tutkinnon-osa-data
  (cheshire/parse-string (slurp "test/resources/eperuste-tutkinnon-osa.json") true))

(defonce
 result
 {:fi
  "Opiskelija lukee rakenne- tai rakennuspiirustuksia, tunnistaa rakentamisen yleisimmät piirrosmerkinnät, tulkitsee työselityksiä, etsii työohjeista ja käyttöturvallisuustiedoitteista tarvittavia tietoja. Opiskelija vastaanottaa, siirtää ja varastoi työmaalla käytettäviä materiaaleja, toimii työmaan avustavissa tehtävissä, käyttää työmaahan ja -vaiheeseen liittyviä tavanomaisia työkaluja, toimii ryhmässä ja yhteistyössä työmaan muiden osapuolien kanssa, kehittää omia vahvuuksiaan tuottavaan toimintaan hyödyntäen työyhteisön asiantuntemusta. Opiskelija työskentelee osana ryhmää, huomioi muut ryhmän jäsenet omassa toiminnassaan, toimii työmaan ohjeiden ja määräysten mukaan, etsii ja kysyy lisätietoja, pitää kiinni sovituista asioista, suorittaa työnsä loppuun asti, viestii ja vuorovaikuttaa työyhteisössä, noudattaa työaikoja, toimii työntekijänä oikeuksiensa, etujensa ja velvollisuuksiensa mukaisesti, osaa asiakaspalvelun perusteet, tunnistaa rakennushankkeen toimijoiden, rakennuttajan, päätoteuttajan, urakoitsijan ja itsenäisen työsuorittajan vastuut ja tehtävät, tunnistaa työmaan työturvallisuus- ja luottamusmiesorganisaation. Opiskelija tunnistaa ja lajittelee työmaalla syntyvät jätteet, toimii vettä, energiaa ja materiaaleja säästävästi työssään, käyttää henkilökohtaisia suojaimia, työskentelee turvallisesti yhteisellä työpaikalla, osaa toimia hätätilanteessa ja antaa ensiapua tarvittaessa, varmistaa ennen työn aloitusta, että omaa työssä tarvittavat luvat ja pätevyydet, varmistaa ennen työtehtävän aloitusta, että töiden aloittaminen on turvallista."
  :sv
  "Studenten läser konstruktions- eller byggritningar, känner igen de vanligaste ritningssymbolerna som används inom byggverksamhet, tolkar arbetsbeskrivningar, söker nödvändig information i arbetsanvisningar och säkerhetsdatablad. Studenten tar emot, flyttar och lagrar material som används på arbetsplatsen, arbetar med assisterande uppgifter på arbetsplatsen, använder verktyg som är vanligt förekommande på en byggarbetsplats och i arbetsskeden, arbetar i grupp och samarbetar med övriga på arbetsplatsen, utvecklar de egna starka sidorna för produktiv verksamhet genom att använda sakkännedomen i arbetsgemenskapen. Studenten arbetar som en del av gruppen, tar i sin egen verksamhet de andra medlemmarna i gruppen i beaktande, följer arbetsplatsens anvisningar och bestämmelser, söker och frågar efter mera information, håller fast vid överenskommelser, slutför sitt arbete, kommunicerar och växelverkar i arbetsgemenskapen, håller arbetstider, handlar i enlighet med sina rättigheter, intressen och skyldigheter som anställd, behärskar grunderna i kundservice, känner till vilka uppgifter och vilket ansvar aktörerna i byggprojektet, byggherren, huvudentreprenören, entreprenören och ensamföretagaren har, känner till arbetsplatsens arbetarskydds- och förtroendemannaorganisation. Studenten identifierar och sorterar avfall som uppkommer på arbetsplatsen, sparar vatten, energi och material i sitt arbete, använder personlig skyddsutrustning, utför sitt arbete på ett säkert sätt på en gemensam arbetsplats, kan fungera i en nödsituation och vid behov ge första hjälpen, säkerställer innan arbetet inleds att hen har de tillstånd och behörigheter som behövs i arbetet, ser innan arbetsuppgifterna påbörjas till att det är säkert att inleda arbetet."
  :en nil})

(deftest kuvaukset-test
  (testing "Create tutkinnon osa kuvaus from ammattitaitovaatimukset"
           (let [kuvaus (select-amm-tutkinnon-osa-kuvaus tutkinnon-osa-data)]
             (is (= kuvaus result)))))
