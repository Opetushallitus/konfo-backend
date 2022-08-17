import axios from 'axios';
import { KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET, Endpoint } from './endpoints';
export interface KoulutusWithToteutus {
  toteutusOid: string
  koulutusOid: string
  domain: string
}

const connectToEndpoint = async (domain: string, endpoint: Endpoint, params: object): Promise<number> => {
  const url = endpoint.params.reduce(
    (a, b) => a.replace('%s', params[b]), endpoint.url); 
  const response = await axios.get(`${domain}/${url}`)
  return response.status
}

export const getKoulutusWithToteutukset = async (domain: string): Promise<KoulutusWithToteutus> => {
  const response = await axios.get(`${domain}/${KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET}`)
  const result = response.data.hits[0];
  return {domain, koulutusOid: result.oid, toteutusOid: result.toteutukset[0].toteutusOid}
}

export default connectToEndpoint