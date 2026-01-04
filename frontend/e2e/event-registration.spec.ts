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

    await test.step('Проверить упрощённую форму (без личных данных)', async () => {
      // Поля firstName/lastName/email отсутствуют (данные автозаполняются на backend)
      await expect(page.getByTestId('firstName-input')).not.toBeVisible();
      await expect(page.getByTestId('lastName-input')).not.toBeVisible();
      await expect(page.getByTestId('email-input')).not.toBeVisible();
      // Selector билетов присутствует
      await expect(page.getByTestId('ticket-type-card').first()).toBeVisible();
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

  test('success page shows add to calendar button with dropdown', async ({ page }) => {
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

      freshEvent = await helper.createEvent(`E2E Calendar Button ${Date.now()}`);
      await helper.addTicketType(freshEvent.id, 'Standard', 50);
      await helper.publishEvent(freshEvent.id);
    });

    await test.step('Войти как admin', async () => {
      await login(page, testUsers.admin);
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

    await test.step('Проверить кнопку добавления в календарь', async () => {
      await expect(page.getByTestId('add-to-calendar-button')).toBeVisible();
    });

    await test.step('Открыть dropdown и проверить опции календарей', async () => {
      await page.getByTestId('add-to-calendar-button').click();

      // Проверяем все три опции календарей
      await expect(page.getByTestId('add-to-calendar-google')).toBeVisible();
      await expect(page.getByTestId('add-to-calendar-apple')).toBeVisible();
      await expect(page.getByTestId('add-to-calendar-outlook')).toBeVisible();
    });
  });

  test('unauthenticated user sees ticket list and login prompt', async ({ page }) => {
    await test.step('Открыть страницу события без авторизации', async () => {
      await page.goto(`/events/${testEvent.slug}`);
    });

    await test.step('Проверить наличие карточки регистрации', async () => {
      await expect(page.getByTestId('registration-form-card')).toBeVisible();
    });

    await test.step('Проверить список доступных билетов', async () => {
      await expect(page.getByTestId('ticket-type-list')).toBeVisible();
      const firstCard = page.getByTestId('ticket-type-card').first();
      await expect(firstCard).toBeVisible();
      // Проверяем что карточки disabled (cursor-not-allowed)
      await expect(firstCard).toHaveClass(/cursor-not-allowed/);
    });

    await test.step('Проверить отсутствие формы регистрации', async () => {
      await expect(page.getByTestId('registration-form')).not.toBeVisible();
    });

    await test.step('Проверить наличие кнопок входа и регистрации', async () => {
      await expect(page.getByTestId('login-button')).toBeVisible();
      await expect(page.getByTestId('register-button')).toBeVisible();
    });
  });

  test('registration form validates required ticketTypeId', async ({ page }) => {
    await test.step('Войти как user', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${testEvent.slug}`);
      await expect(page.getByTestId('registration-form')).toBeVisible();
    });

    await test.step('Попытаться отправить форму без выбора билета', async () => {
      // Не выбираем тип билета (ticketTypeId остаётся пустым)
      await page.getByTestId('registration-submit').click();
    });

    await test.step('Проверить, что остались на странице (валидация не прошла)', async () => {
      await expect(page).toHaveURL(new RegExp(`/events/${testEvent.slug}`));
      // Должна быть видна ошибка валидации
      await expect(page.getByText(/выберите тип билета/i)).toBeVisible();
    });
  });

  test('registered user sees ticket card instead of form', async ({ page }) => {
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

      freshEvent = await ownerHelper.createEvent(`E2E Ticket Card ${Date.now()}`);
      await ownerHelper.addTicketType(freshEvent.id, 'Standard', 50);
      await ownerHelper.publishEvent(freshEvent.id);
    });

    await test.step('Создать регистрацию от имени user', async () => {
      const context = page.context();
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

      // Создаём регистрацию
      await userHelper.createRegistration(freshEvent.slug, ticketTypes[0].id, {});
    });

    await test.step('Войти как user', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${freshEvent!.slug}`);
      await page.waitForLoadState('networkidle');
    });

    await test.step('Проверить отображение карточки билета вместо формы', async () => {
      // Карточка билета должна быть видна
      await expect(page.getByTestId('registration-ticket-card')).toBeVisible({ timeout: 10000 });

      // Проверяем элементы карточки
      await expect(page.getByText('Вы зарегистрированы')).toBeVisible();
      await expect(page.getByTestId('ticket-confirmation-code')).toBeVisible();
      await expect(page.getByTestId('ticket-type-name')).toBeVisible();

      // Форма регистрации НЕ должна быть видна
      await expect(page.getByTestId('registration-form')).not.toBeVisible();
    });
  });

  test('user can cancel registration from ticket card', async ({ page }) => {
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

      freshEvent = await ownerHelper.createEvent(`E2E Cancel Reg ${Date.now()}`);
      await ownerHelper.addTicketType(freshEvent.id, 'Standard', 50);
      await ownerHelper.publishEvent(freshEvent.id);
    });

    await test.step('Создать регистрацию от имени user', async () => {
      const context = page.context();
      const userToken = await TestDataHelper.getAuthToken(
        context.request,
        testUsers.user.email,
        testUsers.user.password
      );
      const userHelper = new TestDataHelper(context.request, userToken);

      const ticketTypesResponse = await context.request.get(
        `http://localhost:8080/api/v1/public/events/${freshEvent.slug}/ticket-types`
      );
      const ticketTypes = await ticketTypesResponse.json();
      await userHelper.createRegistration(freshEvent.slug, ticketTypes[0].id, {});
    });

    await test.step('Войти как user', async () => {
      await login(page, testUsers.user);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/events/${freshEvent!.slug}`);
      await page.waitForLoadState('networkidle');
    });

    await test.step('Отменить регистрацию через карточку билета', async () => {
      // Проверяем наличие карточки билета
      await expect(page.getByTestId('registration-ticket-card')).toBeVisible({ timeout: 10000 });

      // Нажимаем "Отменить регистрацию"
      await page.getByTestId('cancel-registration-show-button').click();

      // Подтверждаем отмену
      await expect(page.getByText(/Вы уверены/)).toBeVisible();
      await page.getByTestId('cancel-registration-confirm-button').click();

      // Проверяем toast сообщение об успехе
      await expect(page.getByText('Регистрация отменена')).toBeVisible({ timeout: 10000 });

      // Ждём обновления страницы
      await page.waitForTimeout(1000);

      // Форма регистрации должна вернуться
      await expect(page.getByTestId('registration-form')).toBeVisible({ timeout: 10000 });

      // Карточка билета должна исчезнуть
      await expect(page.getByTestId('registration-ticket-card')).not.toBeVisible();
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
