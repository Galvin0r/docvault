import { expect, test, type Page } from '@playwright/test';

type Fixture = {
  alice: { login: string; email: string; password: string };
  bob: { login: string; email: string; password: string };
  publicDocumentId: number;
  privateDocumentId: number;
};

let fixture: Fixture;

test.beforeEach(async ({ request }) => {
  let lastError = '';
  for (let attempt = 0; attempt < 20; attempt++) {
    const response = await request.post('http://127.0.0.1:8080/api/e2e/reset');
    if (response.ok()) {
      fixture = await response.json();
      return;
    }
    lastError = `Fixture reset failed with ${response.status()}: ${await response.text()}`;
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error(lastError);
});

test.describe('DocVault end-to-end flows', () => {
  test('guest searches public documents and opens a document preview', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Documents' })).toBeVisible();
    await expect(page.getByText('Search public documents. Sign in to see private and shared results.')).toBeVisible();
    await expect(page.getByText('Solar Clinic Backup Power')).toBeVisible();

    await page.getByLabel('Content').fill('clinic');
    await expect(page.getByText('1 result')).toBeVisible();
    await expect(page.getByText('Battery cabinets keep the')).toBeVisible();

    await page.getByRole('button', { name: 'Open document' }).click();

    await expect(page).toHaveURL(new RegExp(`/document/${fixture.publicDocumentId}$`));
    await expect(page.getByRole('heading', { name: 'Solar Clinic Backup Power' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Content Preview' })).toBeVisible();
    await expect(page.getByText('The backup inverter is tested every Friday morning.')).toBeVisible();
  });

  test('login opens the authenticated workspace', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page.getByText('Username or email is required')).toBeVisible();
    await expect(page.getByText('Password is required')).toBeVisible();

    await page.getByLabel('Username or email').fill(fixture.alice.login);
    await page.getByLabel('Password').fill('wrong-password');
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('Incorrect username/email or password.')).toBeVisible();

    await page.getByLabel('Password').fill(fixture.alice.password);
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByText('All accessible')).toBeVisible();
    await expect(page.getByText('Search public documents, your own library, and documents shared with you.')).toBeVisible();
    await expect(page.getByText('Legal Archive Retention')).toBeVisible();
  });

  test('owner shares a private document and the target user can find it through search ACLs', async ({ page }) => {
    await login(page, fixture.alice.login, fixture.alice.password);

    await page.goto(`/document/${fixture.privateDocumentId}`);

    await expect(page.getByRole('heading', { name: 'Legal Archive Retention' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Sharing' })).toBeVisible();
    await expect(page.getByText('No users shared')).toBeVisible();

    await page.getByLabel('User login or email').fill(fixture.bob.login);
    await page.getByRole('button', { name: 'Share user' }).click();
    await expect(page.getByText(fixture.bob.login, { exact: true })).toBeVisible();

    await page.context().clearCookies();
    await login(page, fixture.bob.login, fixture.bob.password);

    await page.getByLabel('Content').fill('archive');
    await expect(page.getByText('Legal Archive Retention')).toBeVisible({ timeout: 10_000 });
  });

  test('authenticated user browses groups and profile data', async ({ page }) => {
    await login(page, fixture.alice.login, fixture.alice.password);

    await page.goto('/groups');
    await expect(page.getByRole('heading', { name: 'Groups' })).toBeVisible();
    await expect(page.getByText('Research Team')).toBeVisible();

    await page.getByLabel('Group name').fill('legal');
    await expect(page.getByText('Legal Archive')).toBeVisible();
    await expect(page.getByText('Research Team')).toBeHidden();

    await page.goto('/user/alice');
    await expect(page.getByText('Username')).toBeVisible();
    await expect(page.getByText('alice', { exact: true })).toBeVisible();
    await expect(page.getByText('alice@example.test')).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Groups' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Documents' })).toBeVisible();
  });
});

async function login(page: Page, login: string, password: string) {
  await page.goto('/login');
  await page.getByLabel('Username or email').fill(login);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/$/);
}
