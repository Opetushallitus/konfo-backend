import axios from 'axios'
import {
  KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET,
  KONFO_GET_KOULUTUS_WITH_HAKU_AND_HAKUKOHDE,
  Endpoint
} from './endpoints'
import { KonfoParams } from './params'

const formatUrl = (endpoint: Endpoint, params: Record<string, string>): string => {
  return endpoint.params.reduce(
    (url, param) => url.replace('%s', params[param]), endpoint.url)
}

const connectToEndpoint = async (domain: string, endpoint: Endpoint, params: Record<string, string>): Promise<number> => {
  const url = formatUrl(endpoint, params)
  const response = await axios.get(`${domain}/${url}`)
  return response.status
}

export const getKonfoParams = async (domain: string): Promise<KonfoParams> => {
  const response = await axios.get(`${domain}/${KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET}`)
  const result = response.data.hits[0]
  const koulutusOid = result.oid
  const koulutusResponse = await axios.get(`${domain}/${KONFO_GET_KOULUTUS_WITH_HAKU_AND_HAKUKOHDE.replace('%s', koulutusOid)}`)
  const hakuOid = koulutusResponse.data.haut[0].oid
  const hakukohdeOid = koulutusResponse.data.hakukohteet[0].oid
  return {
    domain,
    koulutusOid,
    toteutusOid: result.toteutukset[0].toteutusOid,
    hakuOid,
    hakukohdeOid
  }
}

export default connectToEndpoint
