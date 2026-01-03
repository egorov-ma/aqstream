import { test, expect } from '@playwright/test';
import { login } from './fixtures/auth';
import { testUsers, testOrganization } from './fixtures/test-data';
import { TestDataHelper, EventResponse } from './fixtures/api-helpers';

/**
 * E2E тесты для контроля доступа к созданию событий.
 *
 * Согласно docs/business/role-model.md:
 * - Обычный пользователь НЕ может создавать события
 * - Только OWNER или MODERATOR организации может создавать события
 * - Платформенный администратор (ADMIN) может создавать события для любой организации
 */
test.describe('Event Creation Access Control', () => {
  test('regular user cannot access create event page', async ({ page }) => {
    await test.step('Войти как обычный пользователь', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Попытаться открыть страницу создания события', async () => {
      await page.goto('/dashboard/events/new');
    });

    await test.step('Проверить редирект на дашборд', async () => {
      // PermissionGuard должен редиректить пользователя без прав
      await expect(page).toHaveURL('/dashboard');
    });
  });

  test('regular user does not see create event link in navigation', async ({ page }) => {
    await test.step('Войти как обычный пользователь', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Перейти на страницу событий', async () => {
      await page.goto('/dashboard/events');
    });

    await test.step('Проверить отсутствие кнопки создания события', async () => {
      // Ссылка на создание события не должна быть видна
      await expect(page.getByRole('link', { name: /создать событие/i })).not.toBeVisible();
    });
  });

  test('owner can access create event page', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Открыть страницу создания события', async () => {
      await page.goto('/dashboard/events/new');
    });

    await test.step('Проверить доступность формы', async () => {
      await expect(page.getByTestId('event-form')).toBeVisible();
      await expect(page.getByRole('heading', { name: /создание события/i })).toBeVisible();
    });
  });

  test('owner does not see organization selector', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Открыть страницу создания события', async () => {
      await page.goto('/dashboard/events/new');
    });

    await test.step('Проверить отсутствие dropdown организаций', async () => {
      // Owner создаёт события для своей организации, dropdown не нужен
      await expect(page.getByTestId('event-organization-select')).not.toBeVisible();
    });
  });

  test('admin sees organization selector', async ({ page }) => {
    await test.step('Войти как admin', async () => {
      await login(page, testUsers.admin);
    });

    await test.step('Открыть страницу создания события', async () => {
      await page.goto('/dashboard/events/new');
    });

    await test.step('Проверить наличие dropdown организаций', async () => {
      // Admin может выбрать организацию для создания события
      await expect(page.getByTestId('event-organization-select')).toBeVisible();
    });
  });

  test('admin can create event for specific organization', async ({ page }) => {
    const eventTitle = `E2E Admin Event ${Date.now()}`;

    await test.step('Войти как admin', async () => {
      await login(page, testUsers.admin);
    });

    await test.step('Открыть страницу создания события', async () => {
      await page.goto('/dashboard/events/new');
    });

    await test.step('Выбрать организацию', async () => {
      await page.getByTestId('event-organization-select').click();
      // Выбираем тестовую организацию
      await page.getByRole('option', { name: testOrganization.name }).click();
    });

    await test.step('Заполнить название события', async () => {
      await page.getByTestId('event-title-input').fill(eventTitle);
    });

    await test.step('Выбрать дату начала', async () => {
      await page.getByTestId('event-starts-at-picker-button').click();
      await expect(page.locator('[data-slot="calendar"]')).toBeVisible();
      await page.getByRole('button', { name: /next month/i }).click();
      await page.getByRole('gridcell', { name: '15' }).click();
    });

    await test.step('Заполнить ссылку на трансляцию', async () => {
      await page.getByLabel(/ссылка на трансляцию/i).fill('https://meet.example.com/admin-test');
    });

    await test.step('Сохранить черновик', async () => {
      await page.getByTestId('event-submit').click();
    });

    await test.step('Проверить успешное создание', async () => {
      // Должен редирект на страницу события
      await expect(page).toHaveURL(/\/dashboard\/events\/[a-f0-9-]+/);
      await expect(page.getByTestId('event-status-draft')).toBeVisible({ timeout: 15000 });
    });
  });
});

test.describe('Event API Access Control', () => {
  test('unauthenticated request to create event returns 401', async ({ request }) => {
    await test.step('Отправить запрос без аутентификации', async () => {
      const response = await request.post('http://localhost:8080/api/v1/events', {
        data: {
          title: 'Test Event',
          startsAt: new Date(Date.now() + 86400000).toISOString(),
        },
      });

      expect(response.status()).toBe(401);
    });
  });

  test('regular user cannot create event via API', async ({ browser }) => {
    await test.step('Получить токен обычного пользователя', async () => {
      const context = await browser.newContext();

      // Логин как обычный пользователь (без организации)
      const loginResponse = await context.request.post('http://localhost:8080/api/v1/auth/login', {
        data: {
          email: testUsers.user.email,
          password: testUsers.user.password,
        },
      });
      expect(loginResponse.ok()).toBeTruthy();

      const { accessToken } = await loginResponse.json();

      // Попытка создать событие
      const createResponse = await context.request.post('http://localhost:8080/api/v1/events', {
        headers: { Authorization: `Bearer ${accessToken}` },
        data: {
          title: 'Unauthorized Event',
          startsAt: new Date(Date.now() + 86400000).toISOString(),
        },
      });

      // Должен вернуться 403 — нет прав
      expect(createResponse.status()).toBe(403);

      await context.close();
    });
  });

  test('owner can create event via API', async ({ browser }) => {
    let createdEventId: string | undefined;

    await test.step('Создать событие от имени owner', async () => {
      const context = await browser.newContext();

      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );

      const createResponse = await context.request.post('http://localhost:8080/api/v1/events', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          title: `API Test Event ${Date.now()}`,
          startsAt: new Date(Date.now() + 86400000).toISOString(),
          locationType: 'ONLINE',
          onlineUrl: 'https://meet.example.com/test',
        },
      });

      expect(createResponse.ok()).toBeTruthy();
      const event = await createResponse.json();
      createdEventId = event.id;

      expect(event.title).toContain('API Test Event');
      expect(event.status).toBe('DRAFT');

      // Cleanup
      if (createdEventId) {
        await context.request.delete(`http://localhost:8080/api/v1/events/${createdEventId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
      }

      await context.close();
    });
  });

  test('admin can create event for any organization via API', async ({ browser }) => {
    let createdEventId: string | undefined;

    await test.step('Создать событие от имени admin для организации', async () => {
      const context = await browser.newContext();

      // Логин как admin
      const loginResponse = await context.request.post('http://localhost:8080/api/v1/auth/login', {
        data: {
          email: testUsers.admin.email,
          password: testUsers.admin.password,
        },
      });
      expect(loginResponse.ok()).toBeTruthy();

      const { accessToken } = await loginResponse.json();

      // Создать событие с указанием organizationId
      const createResponse = await context.request.post('http://localhost:8080/api/v1/events', {
        headers: { Authorization: `Bearer ${accessToken}` },
        data: {
          title: `Admin API Event ${Date.now()}`,
          startsAt: new Date(Date.now() + 86400000).toISOString(),
          locationType: 'ONLINE',
          onlineUrl: 'https://meet.example.com/admin-test',
          organizationId: testOrganization.id,
        },
      });

      expect(createResponse.ok()).toBeTruthy();
      const event = await createResponse.json();
      createdEventId = event.id;

      expect(event.title).toContain('Admin API Event');
      expect(event.status).toBe('DRAFT');

      // Cleanup — нужно переключиться на организацию для удаления
      const switchResponse = await context.request.post(
        `http://localhost:8080/api/v1/organizations/${testOrganization.id}/switch`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );

      if (switchResponse.ok() && createdEventId) {
        const { accessToken: orgToken } = await switchResponse.json();
        await context.request.delete(`http://localhost:8080/api/v1/events/${createdEventId}`, {
          headers: { Authorization: `Bearer ${orgToken}` },
        });
      }

      await context.close();
    });
  });

  test('admin without organizationId gets 403', async ({ browser }) => {
    await test.step('Попытка создать событие без organizationId', async () => {
      const context = await browser.newContext();

      // Логин как admin
      const loginResponse = await context.request.post('http://localhost:8080/api/v1/auth/login', {
        data: {
          email: testUsers.admin.email,
          password: testUsers.admin.password,
        },
      });
      expect(loginResponse.ok()).toBeTruthy();

      const { accessToken } = await loginResponse.json();

      // Попытка создать событие БЕЗ organizationId (админ не входит в организацию)
      const createResponse = await context.request.post('http://localhost:8080/api/v1/events', {
        headers: { Authorization: `Bearer ${accessToken}` },
        data: {
          title: `Admin No Org Event ${Date.now()}`,
          startsAt: new Date(Date.now() + 86400000).toISOString(),
          // organizationId НЕ указан
        },
      });

      // Должен вернуться 403 — нет tenantId в JWT и нет organizationId в запросе
      expect(createResponse.status()).toBe(403);

      await context.close();
    });
  });
});
