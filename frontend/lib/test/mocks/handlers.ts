import { http, HttpResponse } from 'msw';
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  Event,
  EventStatus,
  TicketType,
  CreateEventRequest,
  CreateTicketTypeRequest,
} from '@/lib/api/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Тестовые данные пользователя
const mockUser = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  email: 'test@example.com',
  firstName: 'Иван',
  lastName: 'Иванов',
  avatarUrl: null,
  telegramId: null,
  emailVerified: true,
  createdAt: new Date().toISOString(),
};

const mockAuthResponse: LoginResponse = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  user: mockUser,
};

// Тестовые данные событий
const mockEvent: Event = {
  id: 'event-1',
  tenantId: 'org-1',
  title: 'Тестовое событие',
  slug: 'testovoe-sobytie',
  description: 'Описание тестового события',
  startsAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(), // +7 дней
  endsAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000 + 3600000).toISOString(),
  timezone: 'Europe/Moscow',
  locationType: 'ONLINE',
  onlineUrl: 'https://zoom.us/j/123456',
  status: 'DRAFT',
  isPublic: false,
  participantsVisibility: 'CLOSED',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

const mockPublishedEvent: Event = {
  ...mockEvent,
  id: 'event-2',
  slug: 'opublikovannoe-sobytie',
  title: 'Опубликованное событие',
  status: 'PUBLISHED',
  isPublic: true,
};

const mockEvents: Event[] = [mockEvent, mockPublishedEvent];

// Тестовые данные типов билетов
const mockTicketType: TicketType = {
  id: 'tt-1',
  eventId: 'event-1',
  name: 'Стандартный билет',
  description: 'Базовый доступ к мероприятию',
  priceCents: 0,
  currency: 'RUB',
  quantity: 100,
  soldCount: 10,
  reservedCount: 0,
  isActive: true,
  isSoldOut: false,
  sortOrder: 0,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

const mockTicketTypes: TicketType[] = [mockTicketType];

export const handlers = [
  // Login - успешный сценарий
  http.post(`${API_URL}/api/v1/auth/login`, async ({ request }) => {
    const body = (await request.json()) as LoginRequest;

    // Симуляция неверных credentials
    if (body.email === 'wrong@example.com') {
      return HttpResponse.json(
        { code: 'invalid_credentials', message: 'Неверный email или пароль' },
        { status: 401 }
      );
    }

    // Симуляция заблокированного аккаунта
    if (body.email === 'locked@example.com') {
      return HttpResponse.json(
        { code: 'account_locked', message: 'Аккаунт заблокирован. Попробуйте через 15 минут' },
        { status: 403 }
      );
    }

    return HttpResponse.json(mockAuthResponse);
  }),

  // Register - успешный сценарий
  http.post(`${API_URL}/api/v1/auth/register`, async ({ request }) => {
    const body = (await request.json()) as RegisterRequest;

    // Симуляция существующего email
    if (body.email === 'exists@example.com') {
      return HttpResponse.json(
        { code: 'email_already_exists', message: 'Пользователь с таким email уже существует' },
        { status: 409 }
      );
    }

    return HttpResponse.json(
      {
        ...mockAuthResponse,
        user: { ...mockUser, email: body.email, firstName: body.firstName },
      },
      { status: 201 }
    );
  }),

  // Forgot password - всегда успех
  http.post(`${API_URL}/api/v1/auth/forgot-password`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Reset password
  http.post(`${API_URL}/api/v1/auth/reset-password`, async ({ request }) => {
    const body = (await request.json()) as { token: string; newPassword: string };

    // Симуляция невалидного токена
    if (body.token === 'invalid-token') {
      return HttpResponse.json(
        { code: 'invalid_token', message: 'Ссылка недействительна или срок её действия истёк' },
        { status: 400 }
      );
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // Telegram auth
  http.post(`${API_URL}/api/v1/auth/telegram`, () => {
    return HttpResponse.json(mockAuthResponse);
  }),

  // Logout
  http.post(`${API_URL}/api/v1/auth/logout`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Get current user
  http.get(`${API_URL}/api/v1/users/me`, () => {
    return HttpResponse.json(mockUser);
  }),

  // === Events ===

  // Get events list
  http.get(`${API_URL}/api/v1/events`, ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get('status') as EventStatus | null;
    const search = url.searchParams.get('search');

    let filtered = [...mockEvents];

    if (status) {
      filtered = filtered.filter((e) => e.status === status);
    }

    if (search) {
      filtered = filtered.filter((e) =>
        e.title.toLowerCase().includes(search.toLowerCase())
      );
    }

    return HttpResponse.json({
      content: filtered,
      totalElements: filtered.length,
      totalPages: 1,
      page: 0,
      size: 20,
    });
  }),

  // Get single event
  http.get(`${API_URL}/api/v1/events/:id`, ({ params }) => {
    const event = mockEvents.find((e) => e.id === params.id);

    if (!event) {
      return HttpResponse.json(
        { code: 'event_not_found', message: 'Событие не найдено' },
        { status: 404 }
      );
    }

    return HttpResponse.json(event);
  }),

  // Create event
  http.post(`${API_URL}/api/v1/events`, async ({ request }) => {
    const body = (await request.json()) as CreateEventRequest;
    const timestamp = Date.now();

    const newEvent: Event = {
      id: `event-${timestamp}`,
      tenantId: 'org-1',
      title: body.title,
      slug: `event-${timestamp}`,
      description: body.description,
      startsAt: body.startsAt,
      endsAt: body.endsAt,
      timezone: body.timezone ?? 'Europe/Moscow',
      locationType: body.locationType ?? 'ONLINE',
      locationAddress: body.locationAddress,
      onlineUrl: body.onlineUrl,
      maxCapacity: body.maxCapacity,
      registrationOpensAt: body.registrationOpensAt,
      registrationClosesAt: body.registrationClosesAt,
      isPublic: body.isPublic ?? false,
      participantsVisibility: body.participantsVisibility ?? 'CLOSED',
      groupId: body.groupId,
      coverImageUrl: body.coverImageUrl,
      status: 'DRAFT',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    return HttpResponse.json(newEvent, { status: 201 });
  }),

  // Update event
  http.put(`${API_URL}/api/v1/events/:id`, async ({ params, request }) => {
    const event = mockEvents.find((e) => e.id === params.id);

    if (!event) {
      return HttpResponse.json(
        { code: 'event_not_found', message: 'Событие не найдено' },
        { status: 404 }
      );
    }

    const body = (await request.json()) as Partial<Event>;
    const updatedEvent: Event = {
      ...event,
      ...body,
      updatedAt: new Date().toISOString(),
    };

    return HttpResponse.json(updatedEvent);
  }),

  // Publish event
  http.post(`${API_URL}/api/v1/events/:id/publish`, ({ params }) => {
    const event = mockEvents.find((e) => e.id === params.id);

    if (!event) {
      return HttpResponse.json(
        { code: 'event_not_found', message: 'Событие не найдено' },
        { status: 404 }
      );
    }

    if (event.status !== 'DRAFT') {
      return HttpResponse.json(
        { code: 'invalid_status', message: 'Можно опубликовать только черновик' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...event,
      status: 'PUBLISHED',
      updatedAt: new Date().toISOString(),
    });
  }),

  // Cancel event
  http.post(`${API_URL}/api/v1/events/:id/cancel`, ({ params }) => {
    const event = mockEvents.find((e) => e.id === params.id);

    if (!event) {
      return HttpResponse.json(
        { code: 'event_not_found', message: 'Событие не найдено' },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      ...event,
      status: 'CANCELLED',
      updatedAt: new Date().toISOString(),
    });
  }),

  // Delete event
  http.delete(`${API_URL}/api/v1/events/:id`, ({ params }) => {
    const event = mockEvents.find((e) => e.id === params.id);

    if (!event) {
      return HttpResponse.json(
        { code: 'event_not_found', message: 'Событие не найдено' },
        { status: 404 }
      );
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // === Ticket Types ===

  // Get ticket types for event
  http.get(`${API_URL}/api/v1/events/:eventId/ticket-types`, ({ params }) => {
    const eventTicketTypes = mockTicketTypes.filter(
      (tt) => tt.eventId === params.eventId
    );
    return HttpResponse.json(eventTicketTypes);
  }),

  // Create ticket type
  http.post(`${API_URL}/api/v1/events/:eventId/ticket-types`, async ({ params, request }) => {
    const body = (await request.json()) as CreateTicketTypeRequest;

    const newTicketType: TicketType = {
      id: `tt-${Date.now()}`,
      eventId: params.eventId as string,
      name: body.name,
      description: body.description,
      priceCents: 0,
      currency: 'RUB',
      quantity: body.quantity,
      soldCount: 0,
      reservedCount: 0,
      salesStart: body.salesStart,
      salesEnd: body.salesEnd,
      sortOrder: body.sortOrder ?? 0,
      isActive: true,
      isSoldOut: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    return HttpResponse.json(newTicketType, { status: 201 });
  }),

  // Update ticket type
  http.put(`${API_URL}/api/v1/events/:eventId/ticket-types/:id`, async ({ params, request }) => {
    const tt = mockTicketTypes.find((t) => t.id === params.id);

    if (!tt) {
      return HttpResponse.json(
        { code: 'ticket_type_not_found', message: 'Тип билета не найден' },
        { status: 404 }
      );
    }

    const body = (await request.json()) as Partial<TicketType>;
    const updatedTicketType: TicketType = {
      ...tt,
      ...body,
      updatedAt: new Date().toISOString(),
    };

    return HttpResponse.json(updatedTicketType);
  }),

  // Delete ticket type
  http.delete(`${API_URL}/api/v1/events/:eventId/ticket-types/:id`, ({ params }) => {
    const tt = mockTicketTypes.find((t) => t.id === params.id);

    if (!tt) {
      return HttpResponse.json(
        { code: 'ticket_type_not_found', message: 'Тип билета не найден' },
        { status: 404 }
      );
    }

    // Если есть проданные билеты, нельзя удалить
    if (tt.soldCount > 0) {
      return HttpResponse.json(
        { code: 'ticket_type_has_registrations', message: 'Есть регистрации на этот тип билета' },
        { status: 409 }
      );
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // Deactivate ticket type
  http.post(`${API_URL}/api/v1/events/:eventId/ticket-types/:id/deactivate`, ({ params }) => {
    const tt = mockTicketTypes.find((t) => t.id === params.id);

    if (!tt) {
      return HttpResponse.json(
        { code: 'ticket_type_not_found', message: 'Тип билета не найден' },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      ...tt,
      isActive: false,
      updatedAt: new Date().toISOString(),
    });
  }),
];
