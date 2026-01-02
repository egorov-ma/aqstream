import { test, expect } from '@playwright/test';
import { login } from './fixtures/auth';
import { testUsers } from './fixtures/test-data';
import { TestDataHelper } from './fixtures/api-helpers';

/**
 * E2E тесты для Organization Requests (J3)
 *
 * Тестирует полный flow:
 * 1. User подаёт заявку на создание организации
 * 2. Admin видит заявку в списке
 * 3. Admin одобряет/отклоняет заявку
 * 4. User видит статус "Одобрено"/"Отклонено"
 *
 * Примечание: admin@test.local — платформенный администратор (is_admin=true)
 */
test.describe('Organization Requests - Approval Flow (J3)', () => {
  const timestamp = Date.now();
  const testOrgName = `E2E Approve Org ${timestamp}`;
  const testOrgSlug = `e2e-approve-org-${timestamp}`;

  // Cleanup pending requests before tests
  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();

    const userToken = await TestDataHelper.getAuthToken(
      context.request,
      testUsers.user.email,
      testUsers.user.password
    );
    const adminToken = await TestDataHelper.getAuthToken(
      context.request,
      testUsers.admin.email,
      testUsers.admin.password
    );

    await TestDataHelper.cleanupPendingOrganizationRequests(
      context.request,
      userToken,
      adminToken
    );

    await context.close();
  });

  test('complete approval flow: user submits → admin approves → user sees status', async ({
    page,
  }) => {
    // Шаг 1: User подаёт заявку
    await test.step('User входит и открывает страницу заявки', async () => {
      await login(page, testUsers.user);
      await page.goto('/dashboard/organization-request');
      await expect(page.getByRole('heading', { name: /заявка на организацию/i })).toBeVisible();
    });

    await test.step('User заполняет и отправляет заявку', async () => {
      await expect(page.getByTestId('org-request-form')).toBeVisible({ timeout: 10000 });

      await page.getByTestId('org-name-input').fill(testOrgName);
      await page.getByTestId('org-slug-input').fill(testOrgSlug);
      await page.getByTestId('org-description-input').fill('E2E тест: организация для одобрения');
      await page.getByTestId('org-request-submit').click();
    });

    await test.step('User видит уведомление и заявку в списке', async () => {
      await expect(page.getByText(/заявка отправлена/i)).toBeVisible({ timeout: 10000 });
      await expect(page.getByTestId('my-requests-list')).toBeVisible({ timeout: 10000 });
      await expect(page.getByText(testOrgName)).toBeVisible();
      await expect(page.getByTestId('request-status-pending')).toBeVisible();
    });

    // Шаг 2: Admin одобряет заявку
    await test.step('Admin входит и открывает страницу заявок', async () => {
      await login(page, testUsers.admin);
      await page.goto('/dashboard/admin/organization-requests');
      await expect(page.getByTestId('admin-requests-table')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Admin находит и одобряет заявку', async () => {
      // Находим строку с нашей заявкой
      const row = page.locator('tr', { hasText: testOrgName });
      await expect(row).toBeVisible({ timeout: 10000 });

      // Одобряем
      await row.getByRole('button', { name: /одобрить/i }).click();
      await expect(page.getByText(/заявка одобрена/i)).toBeVisible({ timeout: 10000 });
    });

    // Шаг 3: User видит одобренный статус
    await test.step('User видит одобренную заявку', async () => {
      await login(page, testUsers.user);
      await page.goto('/dashboard/organization-request');

      await expect(page.getByTestId('my-requests-list')).toBeVisible({ timeout: 10000 });

      // Находим конкретную карточку по названию организации
      const requestCard = page
        .getByTestId('my-requests-list')
        .locator('div')
        .filter({ hasText: testOrgName })
        .first();

      await expect(requestCard).toContainText('Одобрено');
    });
  });

  test('user becomes organizer after approval', async ({ page }) => {
    await test.step('User входит в систему', async () => {
      await login(page, testUsers.user);
    });

    await test.step('User имеет доступ к странице событий', async () => {
      await page.goto('/dashboard/events');
      // После одобрения заявки user должен иметь доступ к странице событий
      await expect(page.getByRole('heading', { name: /события|events/i })).toBeVisible({
        timeout: 10000,
      });
    });
  });
});

test.describe('Organization Requests - Rejection Flow', () => {
  const timestamp = Date.now();
  const rejectOrgName = `E2E Reject Org ${timestamp}`;
  const rejectOrgSlug = `e2e-reject-org-${timestamp}`;

  // Cleanup pending requests before tests
  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();

    const userToken = await TestDataHelper.getAuthToken(
      context.request,
      testUsers.user.email,
      testUsers.user.password
    );
    const adminToken = await TestDataHelper.getAuthToken(
      context.request,
      testUsers.admin.email,
      testUsers.admin.password
    );

    await TestDataHelper.cleanupPendingOrganizationRequests(
      context.request,
      userToken,
      adminToken
    );

    await context.close();
  });

  test('complete rejection flow: user submits → admin rejects → user sees status', async ({
    page,
  }) => {
    // Шаг 1: User подаёт заявку
    await test.step('User входит и открывает страницу заявки', async () => {
      await login(page, testUsers.user);
      await page.goto('/dashboard/organization-request');
      await expect(page.getByTestId('org-request-form')).toBeVisible({ timeout: 10000 });
    });

    await test.step('User заполняет и отправляет заявку', async () => {
      await page.getByTestId('org-name-input').fill(rejectOrgName);
      await page.getByTestId('org-slug-input').fill(rejectOrgSlug);
      await page.getByTestId('org-request-submit').click();
    });

    await test.step('User видит уведомление и заявку в списке', async () => {
      await expect(page.getByText(/заявка отправлена/i)).toBeVisible({ timeout: 10000 });
      await expect(page.getByText(rejectOrgName)).toBeVisible({ timeout: 10000 });
      await expect(page.getByTestId('request-status-pending')).toBeVisible();
    });

    // Шаг 2: Admin отклоняет заявку
    await test.step('Admin входит и открывает страницу заявок', async () => {
      await login(page, testUsers.admin);
      await page.goto('/dashboard/admin/organization-requests');
      await expect(page.getByTestId('admin-requests-table')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Admin находит и отклоняет заявку с комментарием', async () => {
      const row = page.locator('tr', { hasText: rejectOrgName });
      await expect(row).toBeVisible({ timeout: 10000 });

      await row.getByRole('button', { name: /отклонить/i }).click();
      await expect(page.getByTestId('reject-dialog')).toBeVisible();

      await page.getByTestId('reject-comment-input').fill('E2E тест: недостаточно информации');
      await page.getByTestId('reject-confirm').click();

      await expect(page.getByText(/заявка отклонена/i)).toBeVisible({ timeout: 10000 });
    });

    // Шаг 3: User видит отклонённый статус
    await test.step('User видит отклонённую заявку с комментарием', async () => {
      await login(page, testUsers.user);
      await page.goto('/dashboard/organization-request');

      await expect(page.getByTestId('my-requests-list')).toBeVisible({ timeout: 10000 });

      // Находим конкретную карточку по названию организации
      const requestCard = page
        .getByTestId('my-requests-list')
        .locator('div')
        .filter({ hasText: rejectOrgName })
        .first();

      await expect(requestCard).toContainText('Отклонено');
      await expect(requestCard).toContainText(/причина отклонения/i);
    });
  });
});

test.describe('Organization Requests - Validation', () => {
  test('form validates required fields', async ({ page }) => {
    await test.step('User входит и открывает страницу заявки', async () => {
      await login(page, testUsers.user);
      await page.goto('/dashboard/organization-request');
      await expect(page.getByTestId('org-request-form')).toBeVisible({ timeout: 10000 });
    });

    await test.step('User пытается отправить пустую форму', async () => {
      await page.getByTestId('org-request-submit').click();
    });

    await test.step('Отображаются ошибки валидации', async () => {
      await expect(page.getByText(/название должно содержать минимум/i)).toBeVisible();
    });
  });

  test('form validates slug format', async ({ page }) => {
    await test.step('User входит и открывает страницу заявки', async () => {
      await login(page, testUsers.user);
      await page.goto('/dashboard/organization-request');
      await expect(page.getByTestId('org-request-form')).toBeVisible({ timeout: 10000 });
    });

    await test.step('User заполняет название', async () => {
      await page.getByTestId('org-name-input').fill('Test Org');
    });

    await test.step('User вводит невалидный slug', async () => {
      await page.getByTestId('org-slug-input').fill('invalid slug with spaces');
      await page.getByTestId('org-request-submit').click();
    });

    await test.step('Отображается ошибка валидации slug', async () => {
      await expect(page.getByText(/slug может содержать только/i)).toBeVisible();
    });
  });

  test('admin section is not visible for regular users', async ({ page }) => {
    await test.step('User входит в систему', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Секция Администратор не отображается', async () => {
      // У обычного пользователя не должно быть секции "Администратор"
      await expect(page.getByText(/^администратор$/i)).not.toBeVisible();
    });

    await test.step('Прямой переход на admin страницу не даёт доступа', async () => {
      await page.goto('/dashboard/admin/organization-requests');
      // Ожидаем либо редирект, либо ошибку доступа
      // Проверяем что admin таблица НЕ видна
      await expect(page.getByTestId('admin-requests-table')).not.toBeVisible();
    });
  });
});

test.describe('Organization Requests - Navigation', () => {
  test('user can navigate to organization request page', async ({ page }) => {
    await test.step('User входит в систему', async () => {
      await login(page, testUsers.user);
    });

    await test.step('User открывает страницу заявки на организацию', async () => {
      await page.goto('/dashboard/organization-request');
    });

    await test.step('Страница отображается корректно', async () => {
      await expect(page.getByRole('heading', { name: /заявка на организацию/i })).toBeVisible();
      await expect(page.getByTestId('org-request-form')).toBeVisible();
    });
  });

  test('admin can access organization requests admin page', async ({ page }) => {
    await test.step('Admin входит в систему', async () => {
      await login(page, testUsers.admin);
    });

    await test.step('Admin открывает страницу заявок', async () => {
      await page.goto('/dashboard/admin/organization-requests');
    });

    await test.step('Страница загружается корректно', async () => {
      // Проверяем заголовок страницы
      await expect(page.getByRole('heading', { name: /заявки на организации/i })).toBeVisible({
        timeout: 10000,
      });

      // Должна быть либо таблица заявок, либо сообщение "Нет заявок"
      const table = page.getByTestId('admin-requests-table');
      const emptyState = page.getByText(/нет заявок на рассмотрение/i);
      await expect(table.or(emptyState)).toBeVisible({ timeout: 10000 });
    });
  });
});
