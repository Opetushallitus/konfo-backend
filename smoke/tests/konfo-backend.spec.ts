import { test, expect } from '@playwright/test'
import connectToEndpoint, 
  {getKonfoParams} from '../src/util'
import { KonfoParams } from '../src/params'
import DOMAINS from '../src/domains'
import ENDPOINTS from '../src/endpoints'

test.describe('konfo external api', () => {
  for (let domain of DOMAINS) {

    let konfoParams : KonfoParams;

    const getParams = async (): Promise<KonfoParams> => {
      if (!konfoParams) {
        konfoParams = await getKonfoParams(domain);
      }
      return konfoParams
    }

    test.describe(`testing domain ${domain}`, () => {

      for (let endpoint of ENDPOINTS) {

        test(`with endpoint ${endpoint.url}`, async () => {
          const params = await getParams()
          const status = await connectToEndpoint(domain, endpoint, params)
          expect(status).toBe(200)
        })
      }
    })
  }
})
