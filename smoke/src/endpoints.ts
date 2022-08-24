export interface Endpoint {
  url: string
  params: string[]
}

const ENDPOINTS: Endpoint[] = [
  { url: 'konfo-backend/external/koulutus/%s', params: ['koulutusOid'] },
  { url: 'konfo-backend/external/toteutus/%s', params: ['toteutusOid'] },
  { url: 'konfo-backend/external/haku/%s', params: ['hakuOid'] },
  { url: 'konfo-backend/external/hakukohde/%s', params: ['hakukohdeOid'] },
  { url: 'konfo-backend/external/search/toteutukset-koulutuksittain', params: [] },
  { url: 'konfo-backend/external/search/filters', params: [] },
  { url: 'konfo-backend/external/search/filters_as_array', params: [] }
]

export const KONFO_GET_KOULUTUS_WITH_TOTEUTUKSET: string = 'konfo-backend/external/search/toteutukset-koulutuksittain?size=1'
export const KONFO_GET_KOULUTUS_WITH_HAKU_AND_HAKUKOHDE: string = 'konfo-backend/external/koulutus/%s?haut=true&hakukohteet=true'
export default ENDPOINTS
