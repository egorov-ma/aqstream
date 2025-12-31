import { test, expect } from '@playwright/test';

/**
 * E2E тесты для flow регистрации на событие
 *
 * ПРИМЕЧАНИЕ: Полноценное тестирование с API требует:
 * 1. Запущенного backend сервера с тестовыми данными
 * 2. Авторизованного пользователя
 *
 * Эти тесты проверяют UI компоненты и навигацию без реального API.
 * Интеграционные тесты с API выполняются в CI/CD pipeline.
 */
test.describe('Registration Flow', () => {
  test.describe('Auth Guard', () => {
    test('shows login prompt for unauthenticated users on event page', async ({ page }) => {
      // Мокаем ответ API для события
      await page.route('**/api/v1/public/events/*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'test-event-id',
            title: 'Тестовое событие',
            slug: 'test-event',
            status: 'PUBLISHED',
            startsAt: '2025-03-15T14:00:00.000Z',
            locationType: 'OFFLINE',
            locationAddress: 'Москва, ул. Тестовая, д. 1',
            isPublic: true,
            participantsVisibility: 'CLOSED',
            timezone: 'Europe/Moscow',
            tenantId: 'test-tenant',
            createdAt: '2024-01-01T00:00:00.000Z',
            updatedAt: '2024-01-01T00:00:00.000Z',
          }),
        });
      });

      // Мокаем типы билетов
      await page.route('**/api/v1/public/events/*/ticket-types', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'ticket-type-1',
                name: 'Стандартный',
                priceCents: 0,
                currency: 'RUB',
                soldCount: 0,
                reservedCount: 0,
                available: 100,
                sortOrder: 0,
                isActive: true,
                isSoldOut: false,
              },
            ],
            page: 0,
            size: 100,
            totalElements: 1,
            totalPages: 1,
          }),
        });
      });

      await page.goto('/events/test-event');

      // Проверяем, что показывается форма входа
      await expect(page.getByTestId('registration-form-card')).toBeVisible();
      await expect(
        page.getByText(/для регистрации на событие необходимо войти/i)
      ).toBeVisible();
    });

    test('login link preserves redirect URL', async ({ page }) => {
      await page.route('**/api/v1/public/events/*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'test-event-id',
            title: 'Тестовое событие',
            slug: 'my-event',
            status: 'PUBLISHED',
            startsAt: '2025-03-15T14:00:00.000Z',
            locationType: 'ONLINE',
            isPublic: true,
            participantsVisibility: 'CLOSED',
            timezone: 'Europe/Moscow',
            tenantId: 'test-tenant',
            createdAt: '2024-01-01T00:00:00.000Z',
            updatedAt: '2024-01-01T00:00:00.000Z',
          }),
        });
      });

      await page.route('**/api/v1/public/events/*/ticket-types', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }),
        });
      });

      await page.goto('/events/my-event');

      // Находим кнопку "Войти" и проверяем href
      const loginButton = page.getByRole('link', { name: /войти/i });
      await expect(loginButton).toHaveAttribute(
        'href',
        expect.stringContaining('/login?redirect=')
      );
      await expect(loginButton).toHaveAttribute(
        'href',
        expect.stringContaining(encodeURIComponent('/events/my-event'))
      );
    });
  });

  test.describe('Success Page', () => {
    test('displays confirmation code and event details', async ({ page }) => {
      // Мокаем событие
      await page.route('**/api/v1/public/events/test-event', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'test-event-id',
            title: 'Конференция по разработке',
            slug: 'test-event',
            status: 'PUBLISHED',
            startsAt: '2025-06-15T10:00:00.000Z',
            endsAt: '2025-06-15T18:00:00.000Z',
            locationType: 'OFFLINE',
            locationAddress: 'Москва, Красная площадь, д. 1',
            isPublic: true,
            participantsVisibility: 'CLOSED',
            timezone: 'Europe/Moscow',
            tenantId: 'test-tenant',
            createdAt: '2024-01-01T00:00:00.000Z',
            updatedAt: '2024-01-01T00:00:00.000Z',
          }),
        });
      });

      await page.goto('/events/test-event/success?code=ABC123');

      // Проверяем код подтверждения
      await expect(page.getByTestId('confirmation-code')).toHaveText('ABC123');

      // Проверяем название события
      await expect(page.getByText('Конференция по разработке')).toBeVisible();

      // Проверяем сообщение о Telegram
      await expect(
        page.getByText(/билет с qr-кодом отправлен в telegram/i)
      ).toBeVisible();

      // Проверяем кнопки
      await expect(page.getByTestId('add-to-calendar-button')).toBeVisible();
      await expect(page.getByRole('link', { name: /вернуться к событию/i })).toBeVisible();
      await expect(page.getByRole('link', { name: /мои регистрации/i })).toBeVisible();
    });

    test('redirects to event page if no code provided', async ({ page }) => {
      await page.route('**/api/v1/public/events/test-event', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'test-event-id',
            title: 'Test Event',
            slug: 'test-event',
            status: 'PUBLISHED',
            startsAt: '2025-06-15T10:00:00.000Z',
            locationType: 'ONLINE',
            isPublic: true,
            participantsVisibility: 'CLOSED',
            timezone: 'Europe/Moscow',
            tenantId: 'test-tenant',
            createdAt: '2024-01-01T00:00:00.000Z',
            updatedAt: '2024-01-01T00:00:00.000Z',
          }),
        });
      });

      // Без code параметра должен показать 404
      const response = await page.goto('/events/test-event/success');
      expect(response?.status()).toBe(404);
    });

    test('shows 404 for non-existent event', async ({ page }) => {
      await page.route('**/api/v1/public/events/non-existent', async (route) => {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ code: 'event_not_found', message: 'Событие не найдено' }),
        });
      });

      const response = await page.goto('/events/non-existent/success?code=ABC123');
      expect(response?.status()).toBe(404);
    });
  });

  test.describe('My Registrations Page', () => {
    test.beforeEach(async ({ page }) => {
      // Мокаем авторизацию
      await page.route('**/api/v1/auth/me', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'user-id',
            email: 'test@example.com',
            firstName: 'Иван',
            lastName: 'Иванов',
            emailVerified: true,
          }),
        });
      });
    });

    test('displays empty state when no registrations', async ({ page }) => {
      await page.route('**/api/v1/registrations/my*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [],
            page: 0,
            size: 10,
            totalElements: 0,
            totalPages: 0,
            hasNext: false,
            hasPrevious: false,
          }),
        });
      });

      await page.goto('/dashboard/my-registrations');

      await expect(
        page.getByText(/у вас пока нет регистраций на события/i)
      ).toBeVisible();
      await expect(page.getByRole('link', { name: /найти события/i })).toBeVisible();
    });

    test('displays registrations list', async ({ page }) => {
      await page.route('**/api/v1/registrations/my*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'reg-1',
                eventId: 'event-1',
                eventTitle: 'Конференция по React',
                eventSlug: 'react-conf',
                eventStartsAt: '2025-03-15T10:00:00.000Z',
                ticketTypeId: 'ticket-1',
                ticketTypeName: 'Стандартный',
                userId: 'user-id',
                status: 'CONFIRMED',
                confirmationCode: 'CONF123',
                firstName: 'Иван',
                email: 'test@example.com',
                createdAt: '2024-01-15T12:00:00.000Z',
              },
              {
                id: 'reg-2',
                eventId: 'event-2',
                eventTitle: 'Meetup по TypeScript',
                eventSlug: 'ts-meetup',
                eventStartsAt: '2025-04-20T18:00:00.000Z',
                ticketTypeId: 'ticket-2',
                ticketTypeName: 'VIP',
                userId: 'user-id',
                status: 'CANCELLED',
                confirmationCode: 'CANC456',
                firstName: 'Иван',
                email: 'test@example.com',
                cancelledAt: '2024-02-01T10:00:00.000Z',
                createdAt: '2024-01-20T15:00:00.000Z',
              },
            ],
            page: 0,
            size: 10,
            totalElements: 2,
            totalPages: 1,
            hasNext: false,
            hasPrevious: false,
          }),
        });
      });

      await page.goto('/dashboard/my-registrations');

      // Проверяем заголовок
      await expect(page.getByText(/мои регистрации/i)).toBeVisible();

      // Проверяем первую регистрацию
      await expect(page.getByText('Конференция по React')).toBeVisible();
      await expect(page.getByText('CONF123')).toBeVisible();
      await expect(page.getByText('Подтверждена')).toBeVisible();

      // Проверяем вторую регистрацию (отменённая)
      await expect(page.getByText('Meetup по TypeScript')).toBeVisible();
      await expect(page.getByText('CANC456')).toBeVisible();
      await expect(page.getByText('Отменена')).toBeVisible();

      // Проверяем, что кнопка отмены есть только у подтверждённой регистрации
      const cancelButtons = page.getByRole('button', { name: /отменить/i });
      await expect(cancelButtons).toHaveCount(1);
    });

    test('shows pagination for many registrations', async ({ page }) => {
      // Создаём 15 регистраций (больше PAGE_SIZE = 10)
      const registrations = Array.from({ length: 10 }, (_, i) => ({
        id: `reg-${i}`,
        eventId: `event-${i}`,
        eventTitle: `Событие ${i + 1}`,
        eventSlug: `event-${i}`,
        eventStartsAt: '2025-03-15T10:00:00.000Z',
        ticketTypeId: 'ticket-1',
        ticketTypeName: 'Стандартный',
        userId: 'user-id',
        status: 'CONFIRMED',
        confirmationCode: `CODE${i}`,
        firstName: 'Иван',
        email: 'test@example.com',
        createdAt: '2024-01-15T12:00:00.000Z',
      }));

      await page.route('**/api/v1/registrations/my*', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: registrations,
            page: 0,
            size: 10,
            totalElements: 15,
            totalPages: 2,
            hasNext: true,
            hasPrevious: false,
          }),
        });
      });

      await page.goto('/dashboard/my-registrations');

      // Проверяем, что пагинация отображается
      await expect(page.getByRole('navigation', { name: 'pagination' })).toBeVisible();
      await expect(page.getByText(/вперёд/i)).toBeVisible();
    });
  });

  test.describe('Add to Calendar Button', () => {
    test('add to calendar button is visible on success page', async ({ page }) => {
      await page.route('**/api/v1/public/events/test-event', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'test-event-id',
            title: 'Test Event',
            slug: 'test-event',
            status: 'PUBLISHED',
            startsAt: '2025-06-15T10:00:00.000Z',
            locationType: 'ONLINE',
            onlineUrl: 'https://zoom.us/meeting',
            isPublic: true,
            participantsVisibility: 'CLOSED',
            timezone: 'Europe/Moscow',
            tenantId: 'test-tenant',
            createdAt: '2024-01-01T00:00:00.000Z',
            updatedAt: '2024-01-01T00:00:00.000Z',
          }),
        });
      });

      await page.goto('/events/test-event/success?code=ABC123');

      const calendarButton = page.getByTestId('add-to-calendar-button');
      await expect(calendarButton).toBeVisible();
      await expect(calendarButton).toHaveText(/добавить в календарь/i);
    });
  });
});
