import connectToEndpoint from './util'
import DOMAINS from './domains'
import ENDPOINTS from './endpoints'

describe('Running tests', () => {
  for (let endpoint of ENDPOINTS) {

    describe(`testing endpoint ${endpoint}`, () => {

      for (let domain of DOMAINS) {

        it (`with domain domain ${domain}`, async () => {
          const status = await connectToEndpoint(domain, endpoint)
          expect(status).toBe(200)
        })
      }
    })
  }
})
