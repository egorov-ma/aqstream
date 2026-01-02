import { test, expect } from '@playwright/test';
import { login, logout } from './fixtures/auth';
import { testUsers, testOrganization } from './fixtures/test-data';
import { TestDataHelper, EventResponse } from './fixtures/api-helpers';

test.describe('Event Management (J1)', () => {
  // Тестовые данные для просмотра публичных событий
  let publicEvent: EventResponse;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();

    // Получаем токен owner с контекстом организации для создания событий
    const token = await TestDataHelper.getAuthTokenWithOrganization(
      context.request,
      testUsers.owner.email,
      testUsers.owner.password,
      testOrganization.id
    );
    const helper = new TestDataHelper(context.request, token);

    // Создаём публичное событие для тестов
    publicEvent = await helper.createEvent(`E2E Public Event ${Date.now()}`);
    await helper.addTicketType(publicEvent.id, 'Standard', 100);
    await helper.publishEvent(publicEvent.id);

    await context.close();
  });

  test('owner can login', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Проверить заголовок дашборда', async () => {
      await expect(page.getByRole('heading', { name: /обзор/i })).toBeVisible();
    });
  });

  test('owner can navigate to create event page', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Нажать на ссылку создания события', async () => {
      await page.getByRole('link', { name: /создать событие/i }).click();
    });

    await test.step('Проверить URL и заголовок', async () => {
      await expect(page).toHaveURL(/\/dashboard\/events\/new/);
      await expect(page.getByRole('heading', { name: /создание события/i })).toBeVisible();
    });

    await test.step('Проверить наличие формы', async () => {
      await expect(page.getByTestId('event-form')).toBeVisible();
    });
  });

  test('owner can create and publish an event', async ({ page }) => {
    const eventTitle = `E2E New Event ${Date.now()}`;

    await test.step('Войти и открыть форму создания события', async () => {
      await login(page, testUsers.owner);
      await page.goto('/dashboard/events/new');
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
      await page.getByLabel(/ссылка на трансляцию/i).fill('https://meet.example.com/test');
    });

    await test.step('Включить публичное событие', async () => {
      const publicSwitch = page.getByRole('switch', { name: /публичное событие/i });
      await publicSwitch.click();
      await expect(publicSwitch).toBeChecked();
    });

    await test.step('Добавить тип билета', async () => {
      await page.getByTestId('ticket-type-add-button').click();
      await page.getByTestId('ticket-type-name-0').fill('Free Admission');
    });

    await test.step('Опубликовать событие', async () => {
      await page.getByTestId('event-publish-submit').click();
    });

    await test.step('Проверить успешную публикацию', async () => {
      await expect(page.getByTestId('event-status-published')).toBeVisible({ timeout: 15000 });
    });
  });

  test('published event is accessible via public API', async ({ page }) => {
    await test.step('Запросить публичные события через API', async () => {
      // Запрашиваем с достаточным size чтобы найти наше событие
      const response = await page.request.get(
        'http://localhost:8080/api/v1/public/events?size=100'
      );
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      const events = data.data || [];
      expect(events.length).toBeGreaterThan(0);
    });

    await test.step('Проверить наличие созданного события', async () => {
      // Используем конкретный slug созданного события
      const response = await page.request.get(
        `http://localhost:8080/api/v1/public/events/${publicEvent.slug}`
      );
      expect(response.ok()).toBeTruthy();

      const event = await response.json();
      expect(event.title).toContain('E2E');
      expect(event.slug).toBe(publicEvent.slug);
    });
  });

  test('public event page is accessible', async ({ page }) => {
    await test.step('Открыть публичную страницу события', async () => {
      await page.goto(`/events/${publicEvent.slug}`);
    });

    await test.step('Проверить заголовок события', async () => {
      await expect(page.getByRole('heading', { level: 1 })).toContainText(/E2E/i);
    });

    await test.step('Проверить наличие формы регистрации', async () => {
      await expect(page.getByTestId('registration-form-card')).toBeVisible();
    });
  });

  test('owner can logout', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Выйти из аккаунта', async () => {
      await logout(page);
    });

    await test.step('Проверить редирект на страницу входа', async () => {
      await expect(page).toHaveURL('/login');
    });
  });

  // Cleanup после всех тестов
  test.afterAll(async ({ browser }) => {
    if (!publicEvent?.id) return;

    const context = await browser.newContext();
    try {
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);
      await helper.deleteEvent(publicEvent.id).catch(() => {});
    } finally {
      await context.close();
    }
  });
});

test.describe('Event Cancellation (J5)', () => {
  // Каждый тест создаёт своё событие для изоляции
  let cancellableEvent: EventResponse;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext();

    const token = await TestDataHelper.getAuthTokenWithOrganization(
      context.request,
      testUsers.owner.email,
      testUsers.owner.password,
      testOrganization.id
    );
    const helper = new TestDataHelper(context.request, token);

    // Создаём событие для тестов отмены
    cancellableEvent = await helper.createEvent(`E2E Cancel Event ${Date.now()}`);
    await helper.addTicketType(cancellableEvent.id, 'Standard', 100);
    await helper.publishEvent(cancellableEvent.id);

    await context.close();
  });

  test('owner can see cancel option for published event', async ({ page }) => {
    await test.step('Войти как owner', async () => {
      await login(page, testUsers.owner);
    });

    await test.step('Открыть страницу события', async () => {
      await page.goto(`/dashboard/events/${cancellableEvent.id}`);
      await expect(page.getByTestId('event-status-published')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Открыть меню действий', async () => {
      await page.getByRole('button', { name: /действия/i }).click();
    });

    await test.step('Проверить наличие опции отмены', async () => {
      await expect(page.getByTestId('event-cancel-menu-item')).toBeVisible();
    });
  });

  test('cancel dialog shows correctly', async ({ page }) => {
    await test.step('Войти и открыть страницу события', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${cancellableEvent.id}`);
      await expect(page.getByTestId('event-status-published')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Открыть диалог отмены', async () => {
      await page.getByRole('button', { name: /действия/i }).click();
      await page.getByTestId('event-cancel-menu-item').click();
    });

    await test.step('Проверить содержимое диалога', async () => {
      await expect(page.getByRole('alertdialog')).toBeVisible();
      await expect(page.getByText(/отменить событие\?/i)).toBeVisible();
      await expect(page.getByText(/все регистрации будут отменены/i)).toBeVisible();
    });

    await test.step('Проверить элементы формы', async () => {
      await expect(page.getByTestId('cancel-reason-input')).toBeVisible();
      await expect(page.getByTestId('cancel-dialog-back')).toBeVisible();
      await expect(page.getByTestId('cancel-dialog-confirm')).toBeVisible();
    });
  });

  test('owner can cancel an event with reason', async ({ page }) => {
    // Создаём отдельное событие для этого теста
    let eventToCancel: EventResponse;

    await test.step('Создать событие для отмены', async () => {
      const context = page.context();
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);

      eventToCancel = await helper.createEvent(`E2E To Cancel ${Date.now()}`);
      await helper.addTicketType(eventToCancel.id, 'Standard', 50);
      await helper.publishEvent(eventToCancel.id);
    });

    await test.step('Войти и открыть страницу события', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${eventToCancel!.id}`);
      await expect(page.getByTestId('event-status-published')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Открыть диалог отмены', async () => {
      await page.getByRole('button', { name: /действия/i }).click();
      await page.getByTestId('event-cancel-menu-item').click();
    });

    await test.step('Заполнить причину и подтвердить', async () => {
      await page.getByTestId('cancel-reason-input').fill('E2E тест: событие отменено');
      await page.getByTestId('cancel-dialog-confirm').click();
    });

    await test.step('Проверить статус — Отменено', async () => {
      await expect(page.getByTestId('event-status-cancelled')).toBeVisible({ timeout: 10000 });
    });
  });

  test('cancelled event cannot be cancelled again', async ({ page }) => {
    // Создаём и сразу отменяем событие через API
    let cancelledEvent: EventResponse;

    await test.step('Создать и отменить событие через API', async () => {
      const context = page.context();
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);

      cancelledEvent = await helper.createEvent(`E2E Already Cancelled ${Date.now()}`);
      await helper.addTicketType(cancelledEvent.id, 'Standard', 50);
      await helper.publishEvent(cancelledEvent.id);

      // Отменяем через API
      await context.request.post(`http://localhost:8080/api/v1/events/${cancelledEvent.id}/cancel`, {
        headers: { Authorization: `Bearer ${token}` },
        data: { reason: 'API cancel' },
      });
    });

    await test.step('Войти и открыть страницу отменённого события', async () => {
      await login(page, testUsers.owner);
      await page.goto(`/dashboard/events/${cancelledEvent!.id}`);
      await expect(page.getByTestId('event-status-cancelled')).toBeVisible({ timeout: 10000 });
    });

    await test.step('Открыть меню действий', async () => {
      await page.getByRole('button', { name: /действия/i }).click();
    });

    await test.step('Проверить отсутствие опции отмены', async () => {
      await expect(page.getByTestId('event-cancel-menu-item')).not.toBeVisible();
    });
  });

  // Cleanup после всех тестов
  test.afterAll(async ({ browser }) => {
    if (!cancellableEvent?.id) return;

    const context = await browser.newContext();
    try {
      const token = await TestDataHelper.getAuthTokenWithOrganization(
        context.request,
        testUsers.owner.email,
        testUsers.owner.password,
        testOrganization.id
      );
      const helper = new TestDataHelper(context.request, token);
      await helper.deleteEvent(cancellableEvent.id).catch(() => {});
    } finally {
      await context.close();
    }
  });
});
