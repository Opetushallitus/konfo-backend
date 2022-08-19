import axios from 'axios';
import { KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET,
  KONFO_GET_KOULUTUS_WITH_HAKU_AND_HAKUKOHDE, 
  Endpoint } from './endpoints';
import { KonfoParams } from './params';

const connectToEndpoint = async (domain: string, endpoint: Endpoint, params: object): Promise<number> => {
  const url = endpoint.params.reduce(
    (a, b) => a.replace('%s', params[b]), endpoint.url); 
  const response = await axios.get(`${domain}/${url}`)
  return response.status
}

export const getKonfoParams = async (domain: string): Promise<KonfoParams> => {
  const response = await axios.get(`${domain}/${KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET}`)
  const result = response.data.hits[0];
  const koulutusOid = result.oid
  const koulutusResponse = await axios.get(`${domain}/${KONFO_GET_KOULUTUS_WITH_HAKU_AND_HAKUKOHDE.replace('%s', koulutusOid)}`)
  const hakuOid = koulutusResponse.data.haut[0].oid;
  const hakukohdeOid = koulutusResponse.data.hakukohteet[0].oid;
  return {
    domain, 
    koulutusOid, 
    toteutusOid: result.toteutukset[0].toteutusOid,
    hakuOid,
    hakukohdeOid
  }
}

export default connectToEndpoint