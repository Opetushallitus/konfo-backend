export interface Endpoint {
  url: string,
  params: string[]
}

const ENDPOINTS: Endpoint[] = [
  {url: 'konfo-backend/external/koulutus/%s', params: ['koulutusOid']},
  {url: 'konfo-backend/external/toteutus/%s', params: ['toteutusOid']}
]

export const KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET: string = 'konfo-backend/external/search/toteutukset-koulutuksittain?size=1'

export default ENDPOINTS