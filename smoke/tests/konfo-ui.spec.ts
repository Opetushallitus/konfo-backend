import { test, expect } from '@playwright/test';
import DOMAINS from '../src/domains';

test.describe('konfo-ui', () => {
  for (let domain of DOMAINS) {
    
    test(`${domain} has opintopolku title and redirects to konfo/fi`, async ({page}) => {
      await page.goto(domain)
      await expect(page).toHaveTitle(/Opintopolku/)
      await expect(page).toHaveURL(/.*konfo\/fi/)
    })
  }

})