import { test, expect } from '@playwright/test';
import { login } from './fixtures/auth';
import { testUsers, testOrganization } from './fixtures/test-data';
import { TestDataHelper, EventResponse, TicketTypeResponse, RegistrationResponse } from './fixtures/api-helpers';

test.describe('Check-in (J4)', () => {
  // Тестовые данные создаются в beforeAll через API
  let testEvent: EventResponse;
  let testTicketType: TicketTypeResponse;
  let testRegistration: RegistrationResponse;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();

    // Получаем токен owner с контекстом организации для создания событий
    const ownerToken = await TestDataHelper.getAuthTokenWithOrganization(
      context.request,
      testUsers.owner.email,
      testUsers.owner.password,
      testOrganization.id
    );
    const ownerHelper = new TestDataHelper(context.request, ownerToken);

    // Создаём событие от owner
    testEvent = await ownerHelper.createEvent(`Check-in Test ${Date.now()}`);
    testTicketType = await ownerHelper.addTicketType(testEvent.id, 'Standard Ticket', 100);
    await ownerHelper.publishEvent(testEvent.id);

    // Регистрацию создаём от user (не owner!)
    const userToken = await TestDataHelper.getAuthToken(
      context.request,
      testUsers.user.email,
      testUsers.user.password
    );
    const userHelper = new TestDataHelper(context.request, userToken);
    testRegistration = await userHelper.createRegistration(testEvent.slug, testTicketType.id, {
      firstName: 'Check-in',
      lastName: 'Test User',
    });

    await context.close();
  });

  test('owner can access check-in page', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Открыть страницу check-in события', async () => {
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Проверить заголовок страницы', async () => {
      await expect(page.getByRole('heading', { name: /check-in/i })).toBeVisible();
    });

    await test.step('Проверить наличие табов Сканер и Вручную', async () => {
      await expect(page.getByRole('tab', { name: /сканер/i })).toBeVisible();
      await expect(page.getByRole('tab', { name: /вручную/i })).toBeVisible();
    });
  });

  test('owner can switch to manual search tab', async ({ page }) => {
    await test.step('Войти и открыть страницу check-in', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Переключиться на вкладку Вручную', async () => {
      await page.getByRole('tab', { name: /вручную/i }).click();
    });

    await test.step('Проверить форму поиска', async () => {
      await expect(page.getByTestId('check-in-search-form')).toBeVisible();
      await expect(page.getByTestId('check-in-code-input')).toBeVisible();
      await expect(page.getByTestId('check-in-search-button')).toBeVisible();
    });
  });

  test('owner can search for registration by code', async ({ page }) => {
    await test.step('Войти и открыть страницу check-in', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Переключиться на вкладку Вручную', async () => {
      await page.getByRole('tab', { name: /вручную/i }).click();
      await expect(page.getByTestId('check-in-search-form')).toBeVisible();
    });

    await test.step('Ввести код подтверждения и выполнить поиск', async () => {
      await page.getByTestId('check-in-code-input').fill(testRegistration.confirmationCode);
      await page.getByTestId('check-in-search-button').click();
    });

    await test.step('Проверить карточку участника', async () => {
      await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });
      await expect(page.getByTestId('attendee-name')).toContainText(/Check-in/);
    });
  });

  test('invalid code shows error message', async ({ page }) => {
    await test.step('Войти и открыть страницу check-in', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Переключиться на вкладку Вручную', async () => {
      await page.getByRole('tab', { name: /вручную/i }).click();
    });

    await test.step('Ввести неверный код', async () => {
      await page.getByTestId('check-in-code-input').fill('INVALID1');
      await page.getByTestId('check-in-search-button').click();
    });

    await test.step('Проверить сообщение об ошибке', async () => {
      await expect(page.getByText(/не найдена|недействителен/i)).toBeVisible({
        timeout: 10000,
      });
    });
  });

  test('owner can perform check-in', async ({ page }) => {
    // Создаём новую регистрацию для этого теста, чтобы она точно не была checked-in
    let freshRegistration: RegistrationResponse;

    await test.step('Создать новую регистрацию для теста от admin', async () => {
      const context = page.context();
      // Используем admin для регистрации (user уже зарегистрирован в beforeAll)
      const adminToken = await TestDataHelper.getAuthToken(
        context.request,
        testUsers.admin.email,
        testUsers.admin.password
      );
      const adminHelper = new TestDataHelper(context.request, adminToken);
      freshRegistration = await adminHelper.createRegistration(testEvent.slug, testTicketType.id, {
        firstName: 'Fresh',
        lastName: 'Attendee',
      });
    });

    await test.step('Войти и открыть страницу check-in', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Переключиться на вкладку Вручную', async () => {
      await page.getByRole('tab', { name: /вручную/i }).click();
    });

    await test.step('Найти участника по коду', async () => {
      await page.getByTestId('check-in-code-input').fill(freshRegistration!.confirmationCode);
      await page.getByTestId('check-in-search-button').click();
      await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Проверить статус — ожидает отметки', async () => {
      await expect(page.getByTestId('attendee-status-pending')).toBeVisible();
    });

    await test.step('Выполнить check-in', async () => {
      await page.getByTestId('check-in-confirm-button').click();
    });

    await test.step('Проверить статус — отмечен', async () => {
      await expect(page.getByTestId('attendee-status-checked-in')).toBeVisible({ timeout: 10000 });
    });
  });

  test('already checked-in attendee shows appropriate message', async ({ page }) => {
    // Используем регистрацию из предыдущего теста (уже checked-in) или создаём и check-in'им новую
    let checkedInRegistration: RegistrationResponse;

    await test.step('Создать и check-in регистрацию через API от owner', async () => {
      const context = page.context();
      // Используем owner для регистрации (user и admin уже использованы)
      const ownerToken = await TestDataHelper.getAuthToken(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password
      );
      const ownerHelper = new TestDataHelper(context.request, ownerToken);

      // Создаём регистрацию
      checkedInRegistration = await ownerHelper.createRegistration(testEvent.slug, testTicketType.id, {
        firstName: 'Already',
        lastName: 'CheckedIn',
      });

      // Выполняем check-in через API (публичный эндпоинт)
      await context.request.post(
        `http://localhost:8080/api/v1/public/check-in/${checkedInRegistration.confirmationCode}`
      );
    });

    await test.step('Войти и открыть страницу check-in', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Переключиться на вкладку Вручную', async () => {
      await page.getByRole('tab', { name: /вручную/i }).click();
    });

    await test.step('Найти участника по коду', async () => {
      await page.getByTestId('check-in-code-input').fill(checkedInRegistration!.confirmationCode);
      await page.getByTestId('check-in-search-button').click();
      await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Проверить статус и сообщение', async () => {
      await expect(page.getByTestId('attendee-status-checked-in')).toBeVisible();
      await expect(page.getByText(/участник уже отмечен/i)).toBeVisible();
    });
  });

  test('can scan another code after viewing attendee', async ({ page }) => {
    await test.step('Войти и открыть страницу check-in', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${testEvent.id}/check-in`);
    });

    await test.step('Переключиться на вкладку Вручную', async () => {
      await page.getByRole('tab', { name: /вручную/i }).click();
    });

    await test.step('Найти участника по коду', async () => {
      await page.getByTestId('check-in-code-input').fill(testRegistration.confirmationCode);
      await page.getByTestId('check-in-search-button').click();
      await expect(page.getByTestId('attendee-card')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Нажать Сканировать другой код', async () => {
      await page.getByRole('button', { name: /сканировать другой код/i }).click();
    });

    await test.step('Проверить возврат к форме поиска', async () => {
      await expect(page.getByTestId('check-in-search-form')).toBeVisible();
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
