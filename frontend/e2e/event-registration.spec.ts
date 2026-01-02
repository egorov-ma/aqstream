import { test, expect } from '@playwright/test';
import { login } from './fixtures/auth';
import { testUsers, testOrganization } from './fixtures/test-data';
import { TestDataHelper, EventResponse } from './fixtures/api-helpers';

test.describe('Event Registration (J2)', () => {
  // Тестовые данные создаются в beforeAll
  let testEvent: EventResponse;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();

    const token = await TestDataHelper.getAuthTokenWithOrganization(
      context.request,
      testUsers.owner.email,
      testUsers.owner.password,
      testOrganization.id
    );
    const helper = new TestDataHelper(context.request, token);

    // Создаём публичное событие для тестов регистрации
    testEvent = await helper.createEvent(`E2E Registration Event ${Date.now()}`);
    await helper.addTicketType(testEvent.id, 'Free Ticket', 100);
    await helper.publishEvent(testEvent.id);

    await context.close();
  });

  test('user can view public event page', async ({ page }) => {
    await test.step('Открыть публичную страницу события', async () => {
      await page.goto(`/events/${testEvent.slug}`);
    });

    await test.step('Проверить заголовок события', async () => {
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    });

    await test.step('Проверить карточку регистрации', async () => {
      await expect(page.getByTestId('registration-form-card')).toBeVisible();
    });
  });

  test('authenticated user can register for event', async ({ page }) => {
    // Создаём отдельное событие для этого теста, чтобы избежать дубликата регистрации
    let freshEvent: EventResponse;

    await test.step('Создать событие для регистрации', async () => {
      const context = page.context();
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);

      freshEvent = await helper.createEvent(`E2E Fresh Event ${Date.now()}`);
      await helper.addTicketType(freshEvent.id, 'Standard', 50);
      await helper.publishEvent(freshEvent.id);
    });

    await test.step('Войти как user', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${freshEvent!.slug}`);
      await expect(page.getByTestId('registration-form')).toBeVisible();
    });

    await test.step('Проверить предзаполненные поля', async () => {
      await expect(page.getByTestId('firstName-input')).toHaveValue(testUsers.user.firstName);
      await expect(page.getByTestId('email-input')).toHaveValue(testUsers.user.email);
    });

    await test.step('Выбрать тип билета', async () => {
      await page.getByTestId('ticket-type-card').first().click();
    });

    await test.step('Отправить регистрацию', async () => {
      await page.getByTestId('registration-submit').click();
    });

    await test.step('Проверить редирект на success страницу', async () => {
      await expect(page).toHaveURL(/\/success/, { timeout: 15000 });
      await expect(page.getByTestId('registration-success-card')).toBeVisible();
    });
  });

  test('success page shows confirmation code', async ({ page }) => {
    // Создаём событие для этого теста
    let freshEvent: EventResponse;

    await test.step('Создать событие для регистрации', async () => {
      const context = page.context();
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);

      freshEvent = await helper.createEvent(`E2E Success Page ${Date.now()}`);
      await helper.addTicketType(freshEvent.id, 'Standard', 50);
      await helper.publishEvent(freshEvent.id);
    });

    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${freshEvent!.slug}`);
      await expect(page.getByTestId('registration-form')).toBeVisible();
    });

    await test.step('Выбрать тип билета и зарегистрироваться', async () => {
      await page.getByTestId('ticket-type-card').first().click();
      await page.getByTestId('registration-submit').click();
    });

    await test.step('Проверить success страницу', async () => {
      await expect(page).toHaveURL(/\/success/, { timeout: 15000 });
      await expect(page.getByTestId('registration-success-card')).toBeVisible();
    });

    await test.step('Проверить код подтверждения', async () => {
      await expect(page.getByTestId('confirmation-code')).toBeVisible();
      const codeText = await page.getByTestId('confirmation-code').textContent();
      expect(codeText).toMatch(/[A-Z0-9]{8}/);
    });
  });

  test('unauthenticated user sees login prompt', async ({ page }) => {
    await test.step('Открыть страницу события без авторизации', async () => {
      await page.goto(`/events/${testEvent.slug}`);
    });

    await test.step('Проверить наличие карточки регистрации', async () => {
      await expect(page.getByTestId('registration-form-card')).toBeVisible();
    });

    await test.step('Проверить отсутствие формы регистрации', async () => {
      await expect(page.getByTestId('registration-form')).not.toBeVisible();
    });

    await test.step('Проверить наличие кнопки входа', async () => {
      await expect(
        page.getByTestId('registration-form-card').getByRole('link', { name: /войти/i })
      ).toBeVisible();
    });
  });

  test('registration form validates required fields', async ({ page }) => {
    await test.step('Войти как user', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${testEvent.slug}`);
      await expect(page.getByTestId('registration-form')).toBeVisible();
    });

    await test.step('Очистить обязательные поля', async () => {
      await page.getByTestId('firstName-input').clear();
      await page.getByTestId('email-input').clear();
    });

    await test.step('Попытаться отправить форму', async () => {
      await page.getByTestId('registration-submit').click();
    });

    await test.step('Проверить, что остались на странице (не прошла валидация)', async () => {
      await expect(page).toHaveURL(new RegExp(`/events/${testEvent.slug}`));
    });
  });

  test('user cannot register twice for same event', async ({ page }) => {
    // Создаём событие и регистрацию через API
    let freshEvent: EventResponse;

    await test.step('Создать событие через owner', async () => {
      const context = page.context();
      const ownerToken = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const ownerHelper = new TestDataHelper(context.request, ownerToken);

      freshEvent = await ownerHelper.createEvent(`E2E Double Reg ${Date.now()}`);
      await ownerHelper.addTicketType(freshEvent.id, 'Standard', 50);
      await ownerHelper.publishEvent(freshEvent.id);
    });

    await test.step('Создать первую регистрацию от имени user', async () => {
      const context = page.context();
      // Важно: регистрация создаётся с токеном user, а не owner
      const userToken = await TestDataHelper.getAuthToken(
        context.request,
        testUsers.user.email,
        testUsers.user.password
      );
      const userHelper = new TestDataHelper(context.request, userToken);

      // Получаем типы билетов через публичный API
      const ticketTypesResponse = await context.request.get(
        `http://localhost:8080/api/v1/public/events/${freshEvent.slug}/ticket-types`
      );
      const ticketTypes = await ticketTypesResponse.json();

      await userHelper.createRegistration(freshEvent.slug, ticketTypes[0].id, {
        firstName: testUsers.user.firstName,
        lastName: testUsers.user.lastName,
        email: testUsers.user.email,
      });
    });

    await test.step('Войти как user', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${freshEvent!.slug}`);
      // Ждём загрузки страницы
      await page.waitForLoadState('networkidle');
      // Дополнительное ожидание для React hydration
      await page.waitForTimeout(500);
    });

    await test.step('Проверить, что повторная регистрация заблокирована', async () => {
      // Ждём заголовок события как индикатор загрузки страницы
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible({ timeout: 10000 });

      // Проверяем наличие формы регистрации
      const form = page.getByTestId('registration-form');
      const submitButton = page.getByTestId('registration-submit');

      // Ждём появления формы (должна быть видна даже для зарегистрированных)
      await expect(form).toBeVisible({ timeout: 10000 });

      // Выбираем тип билета
      await page.getByTestId('ticket-type-card').first().click();

      // Отправляем форму
      await submitButton.click();

      // После отправки должна появиться ошибка о дублирующей регистрации
      // Или редирект на success page (если API вернул успех, что не должно произойти)
      const errorMessage = page.getByText(/уже зарегистрированы|уже есть регистрация/i);
      const successIndicator = page.getByText(/регистрация успешна/i);

      // Ждём одного из двух: ошибку (ожидаемо) или успех (неожиданно)
      await expect(errorMessage.or(successIndicator)).toBeVisible({ timeout: 15000 });

      // Проверяем, что появилась именно ошибка
      const hasError = await errorMessage.isVisible();
      expect(hasError).toBe(true);
    });
  });

  // Cleanup после всех тестов
  test.afterAll(async ({ browser }) => {
    if (!testEvent?.id) return;

    const context = await browser.newContext();
    try {
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);
      await helper.deleteEvent(testEvent.id).catch(() => {});
    } finally {
      await context.close();
    }
  });
});
