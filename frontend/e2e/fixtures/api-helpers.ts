import { APIRequestContext } from '@playwright/test';

const API_URL = process.env.API_URL || 'http://localhost:8080';

/**
 * Типы для работы с API
 */
export interface AuthResponse {
  accessToken: string;
  user: {
    id: string;
    email: string;
  };
}

export interface EventResponse {
  id: string;
  slug: string;
  title: string;
  status: string;
}

export interface TicketTypeResponse {
  id: string;
  name: string;
  quantity: number | null;
}

export interface RegistrationResponse {
  id: string;
  confirmationCode: string;
  status: string;
}

export interface OrganizationRequest {
  id: string;
  name: string;
  slug: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}

/**
 * Хелпер для создания тестовых данных через API.
 * Используется в beforeAll для setup изолированных тестовых данных.
 *
 * Пример использования:
 * ```typescript
 * test.beforeAll(async ({ browser }) => {
 *   const context = await browser.newContext();
 *   const token = await TestDataHelper.getAuthToken(
 *     context.request,
 *     testUsers.owner.email,
 *     testUsers.owner.password
 *   );
 *   const helper = new TestDataHelper(context.request, token);
 *   testEvent = await helper.createEvent('My Event');
 *   await context.close();
 * });
 * ```
 */
/**
 * Утилита для выполнения запросов с retry логикой
 */
async function withRetry<T>(
  fn: () => Promise<T>,
  options: { maxRetries?: number; delayMs?: number; operation?: string } = {}
): Promise<T> {
  const { maxRetries = 3, delayMs = 1000, operation = 'API call' } = options;
  let lastError: Error | null = null;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      if (attempt < maxRetries) {
        const delay = delayMs * attempt; // Exponential backoff
        console.warn(
          `${operation} failed (attempt ${attempt}/${maxRetries}), retrying in ${delay}ms...`
        );
        await new Promise((r) => setTimeout(r, delay));
      }
    }
  }

  throw lastError;
}

export class TestDataHelper {
  constructor(
    private request: APIRequestContext,
    private token: string
  ) {}

  /**
   * Получить JWT токен для API запросов (с retry)
   */
  static async getAuthToken(
    request: APIRequestContext,
    email: string,
    password: string
  ): Promise<string> {
    return withRetry(
      async () => {
        const response = await request.post(`${API_URL}/api/v1/auth/login`, {
          data: { email, password },
        });

        if (!response.ok()) {
          const body = await response.text();
          throw new Error(`Ошибка авторизации: ${response.status()} ${body}`);
        }

        const data: AuthResponse = await response.json();
        return data.accessToken;
      },
      { operation: 'getAuthToken' }
    );
  }

  /**
   * Получить токен с переключением на организацию (с retry).
   * Возвращает токен с правильным tenantId для создания событий.
   */
  static async getAuthTokenWithOrganization(
    request: APIRequestContext,
    email: string,
    password: string,
    organizationId: string
  ): Promise<string> {
    return withRetry(
      async () => {
        // Сначала логин
        const loginResponse = await request.post(`${API_URL}/api/v1/auth/login`, {
          data: { email, password },
        });

        if (!loginResponse.ok()) {
          const body = await loginResponse.text();
          throw new Error(`Ошибка авторизации: ${loginResponse.status()} ${body}`);
        }

        const loginData: AuthResponse = await loginResponse.json();

        // Переключаемся на организацию
        const switchResponse = await request.post(
          `${API_URL}/api/v1/organizations/${organizationId}/switch`,
          {
            headers: { Authorization: `Bearer ${loginData.accessToken}` },
          }
        );

        if (!switchResponse.ok()) {
          const body = await switchResponse.text();
          throw new Error(`Ошибка переключения организации: ${switchResponse.status()} ${body}`);
        }

        const switchData = await switchResponse.json();
        return switchData.accessToken;
      },
      { operation: 'getAuthTokenWithOrganization' }
    );
  }

  /**
   * Получить информацию о текущем пользователе
   */
  async getCurrentUser(): Promise<{ id: string; email: string }> {
    const response = await this.request.get(`${API_URL}/api/v1/users/me`, {
      headers: this.authHeaders(),
    });

    if (!response.ok()) {
      const body = await response.text();
      throw new Error(`Ошибка получения пользователя: ${response.status()} ${body}`);
    }

    return response.json();
  }

  /**
   * Создать событие через API (с retry)
   */
  async createEvent(title: string): Promise<EventResponse> {
    const slug = this.generateSlug(title);
    // Дата начала: 7 дней от текущего момента
    const startsAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const headers = this.authHeaders();

    return withRetry(
      async () => {
        const response = await this.request.post(`${API_URL}/api/v1/events`, {
          headers,
          data: {
            title,
            slug,
            startsAt,
            timezone: 'Europe/Moscow',
            isPublic: true,
            locationType: 'OFFLINE',
            locationAddress: 'Тестовый адрес',
          },
        });

        if (!response.ok()) {
          const body = await response.text();
          throw new Error(`Ошибка создания события: ${response.status()} ${body}`);
        }

        return response.json();
      },
      { operation: 'createEvent' }
    );
  }

  /**
   * Опубликовать событие (с retry)
   */
  async publishEvent(eventId: string): Promise<EventResponse> {
    const headers = this.authHeaders();

    return withRetry(
      async () => {
        const response = await this.request.post(`${API_URL}/api/v1/events/${eventId}/publish`, {
          headers,
        });

        if (!response.ok()) {
          const body = await response.text();
          throw new Error(`Ошибка публикации события: ${response.status()} ${body}`);
        }

        return response.json();
      },
      { operation: 'publishEvent' }
    );
  }

  /**
   * Добавить тип билета к событию (с retry)
   */
  async addTicketType(
    eventId: string,
    name: string,
    quantity: number
  ): Promise<TicketTypeResponse> {
    const headers = this.authHeaders();

    return withRetry(
      async () => {
        const response = await this.request.post(`${API_URL}/api/v1/events/${eventId}/ticket-types`, {
          headers,
          data: {
            name,
            quantity,
            sortOrder: 0,
          },
        });

        if (!response.ok()) {
          const body = await response.text();
          throw new Error(`Ошибка создания типа билета: ${response.status()} ${body}`);
        }

        return response.json();
      },
      { operation: 'addTicketType' }
    );
  }

  /**
   * Создать регистрацию на публичное событие (по slug) (с retry)
   */
  async createRegistration(
    eventSlug: string,
    ticketTypeId: string,
    overrides?: {
      firstName?: string;
      lastName?: string;
      email?: string;
    }
  ): Promise<RegistrationResponse> {
    const uniqueId = crypto.randomUUID().slice(0, 8);
    const headers = this.authHeaders();

    return withRetry(
      async () => {
        const response = await this.request.post(
          `${API_URL}/api/v1/public/events/${eventSlug}/registrations`,
          {
            headers,
            data: {
              ticketTypeId,
              firstName: overrides?.firstName || 'E2E',
              lastName: overrides?.lastName || 'Test',
              email: overrides?.email || `e2e-${uniqueId}@test.local`,
            },
          }
        );

        if (!response.ok()) {
          const body = await response.text();
          throw new Error(`Ошибка создания регистрации: ${response.status()} ${body}`);
        }

        return response.json();
      },
      { operation: 'createRegistration' }
    );
  }

  /**
   * Получить информацию о регистрации по confirmation code
   */
  async getCheckInInfo(
    confirmationCode: string
  ): Promise<{
    registrationId: string;
    isCheckedIn: boolean;
    status: string;
  }> {
    const response = await this.request.get(
      `${API_URL}/api/v1/public/check-in/${confirmationCode}`,
      {
        headers: this.authHeaders(),
      }
    );

    if (!response.ok()) {
      const body = await response.text();
      throw new Error(`Ошибка получения информации о check-in: ${response.status()} ${body}`);
    }

    return response.json();
  }

  /**
   * Получить публичное событие по slug
   */
  async getPublicEvent(slug: string): Promise<EventResponse> {
    const response = await this.request.get(`${API_URL}/api/v1/public/events/${slug}`);

    if (!response.ok()) {
      const body = await response.text();
      throw new Error(`Ошибка получения события: ${response.status()} ${body}`);
    }

    return response.json();
  }

  /**
   * Получить типы билетов публичного события
   */
  async getPublicTicketTypes(slug: string): Promise<TicketTypeResponse[]> {
    const response = await this.request.get(`${API_URL}/api/v1/public/events/${slug}/ticket-types`);

    if (!response.ok()) {
      const body = await response.text();
      throw new Error(`Ошибка получения типов билетов: ${response.status()} ${body}`);
    }

    return response.json();
  }

  /**
   * Найти существующее публичное событие или вернуть null
   */
  async findPublishedEvent(): Promise<EventResponse | null> {
    const response = await this.request.get(`${API_URL}/api/v1/public/events`, {
      params: { page: 0, size: 1 },
    });

    if (!response.ok()) {
      return null;
    }

    const data = await response.json();
    if (data.data && data.data.length > 0) {
      return data.data[0];
    }

    return null;
  }

  /**
   * Удалить событие (для cleanup)
   */
  async deleteEvent(eventId: string): Promise<void> {
    const response = await this.request.delete(`${API_URL}/api/v1/events/${eventId}`, {
      headers: this.authHeaders(),
    });

    if (!response.ok() && response.status() !== 404) {
      const body = await response.text();
      throw new Error(`Ошибка удаления события: ${response.status()} ${body}`);
    }
  }

  /**
   * Отменить регистрацию (для cleanup)
   */
  async cancelRegistration(registrationId: string): Promise<void> {
    const response = await this.request.delete(
      `${API_URL}/api/v1/registrations/${registrationId}`,
      {
        headers: this.authHeaders(),
      }
    );

    if (!response.ok() && response.status() !== 404) {
      const body = await response.text();
      throw new Error(`Ошибка отмены регистрации: ${response.status()} ${body}`);
    }
  }

  /**
   * Получить свои заявки на организацию
   */
  async getMyOrganizationRequests(): Promise<OrganizationRequest[]> {
    const response = await this.request.get(`${API_URL}/api/v1/organization-requests/my`, {
      headers: this.authHeaders(),
    });

    if (!response.ok()) {
      const body = await response.text();
      throw new Error(`Ошибка получения заявок: ${response.status()} ${body}`);
    }

    return response.json();
  }

  /**
   * Отклонить заявку на организацию (только для admin)
   */
  async rejectOrganizationRequest(requestId: string, comment: string): Promise<void> {
    const response = await this.request.post(
      `${API_URL}/api/v1/organization-requests/${requestId}/reject`,
      {
        headers: this.authHeaders(),
        data: { comment },
      }
    );

    if (!response.ok()) {
      const body = await response.text();
      throw new Error(`Ошибка отклонения заявки: ${response.status()} ${body}`);
    }
  }

  /**
   * Очистить все pending заявки пользователя (требует admin токен)
   * @param userToken - токен пользователя для получения его заявок
   * @param adminToken - токен админа для отклонения заявок
   */
  static async cleanupPendingOrganizationRequests(
    request: APIRequestContext,
    userToken: string,
    adminToken: string
  ): Promise<void> {
    // Получаем заявки пользователя
    const userHelper = new TestDataHelper(request, userToken);
    const requests = await userHelper.getMyOrganizationRequests();

    // Отклоняем все PENDING заявки через admin
    const adminHelper = new TestDataHelper(request, adminToken);
    for (const req of requests) {
      if (req.status === 'PENDING') {
        await adminHelper.rejectOrganizationRequest(req.id, 'E2E cleanup');
      }
    }
  }

  /**
   * Генерация заголовков авторизации
   */
  private authHeaders(): Record<string, string> {
    return {
      Authorization: `Bearer ${this.token}`,
      'Content-Type': 'application/json',
    };
  }

  /**
   * Генерация уникального slug из названия (UUID для уникальности)
   */
  private generateSlug(title: string): string {
    const uniqueId = crypto.randomUUID().slice(0, 12);
    const base = title
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-')
      .substring(0, 30);
    return `${base}-${uniqueId}`;
  }
}

/**
 * Генерация уникальных тестовых данных (UUID для уникальности)
 */
export function createTestContext() {
  const uniqueId = crypto.randomUUID().slice(0, 8);

  return {
    eventTitle: `E2E Event ${uniqueId}`,
    eventSlug: `e2e-event-${uniqueId}`,
    orgName: `E2E Org ${uniqueId}`,
    orgSlug: `e2e-org-${uniqueId}`,
    email: `e2e-${uniqueId}@test.local`,
  };
}
