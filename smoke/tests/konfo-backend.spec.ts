import { test, expect } from '@playwright/test'
import connectToEndpoint, 
  {KoulutusWithToteutus, getKoulutusWithToteutukset} from '../src/util'
import DOMAINS from '../src/domains'
import ENDPOINTS from '../src/endpoints'

test.describe('konfo external api', () => {
  for (let domain of DOMAINS) {

    let koulutusWithToteutus : KoulutusWithToteutus;

    const getParams = async (): Promise<KoulutusWithToteutus> => {
      if (!koulutusWithToteutus) {
        koulutusWithToteutus = await getKoulutusWithToteutukset(domain);
      }
      return koulutusWithToteutus
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
