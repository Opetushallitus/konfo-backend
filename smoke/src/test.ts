import connectToEndpoint, 
  {KoulutusWithToteutus, getKoulutusWithToteutukset} from './util'
import DOMAINS from './domains'
import ENDPOINTS from './endpoints'

describe('Running tests', () => {
  for (let domain of DOMAINS) {

    let koulutusWithToteutus : KoulutusWithToteutus;

    const getParams = async (): Promise<KoulutusWithToteutus> => {
      if (!koulutusWithToteutus) {
        koulutusWithToteutus = await getKoulutusWithToteutukset(domain);
      }
      return koulutusWithToteutus;
    }

    describe(`testing domain ${domain}`, () => {

      for (let endpoint of ENDPOINTS) {

        it (`with endpoint ${endpoint.url}`, async () => {
          const params = await getParams()
          const status = await connectToEndpoint(domain, endpoint, params)
          expect(status).toBe(200)
        })
      }
    })
  }
})
