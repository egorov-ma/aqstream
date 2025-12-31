// Коды ошибок API аутентификации
export const AUTH_ERROR_CODES = {
  INVALID_CREDENTIALS: 'invalid_credentials',
  EMAIL_NOT_VERIFIED: 'email_not_verified',
  ACCOUNT_LOCKED: 'account_locked',
  EMAIL_ALREADY_EXISTS: 'email_already_exists',
  INVALID_TOKEN: 'invalid_token',
  TOO_MANY_REQUESTS: 'too_many_requests',
} as const;

// Маппинг кодов ошибок на сообщения
export const AUTH_ERROR_MESSAGES: Record<string, string> = {
  [AUTH_ERROR_CODES.INVALID_CREDENTIALS]: 'Неверный email или пароль',
  [AUTH_ERROR_CODES.EMAIL_NOT_VERIFIED]: 'Подтвердите email для входа',
  [AUTH_ERROR_CODES.ACCOUNT_LOCKED]: 'Аккаунт заблокирован. Попробуйте через 15 минут',
  [AUTH_ERROR_CODES.EMAIL_ALREADY_EXISTS]: 'Пользователь с таким email уже существует',
  [AUTH_ERROR_CODES.INVALID_TOKEN]: 'Ссылка недействительна или срок её действия истёк',
  [AUTH_ERROR_CODES.TOO_MANY_REQUESTS]: 'Слишком много попыток. Попробуйте позже',
};

// Получить сообщение об ошибке по коду
export function getAuthErrorMessage(code: string, fallback: string): string {
  return AUTH_ERROR_MESSAGES[code] || fallback;
}

// Коды ошибок API событий
export const EVENT_ERROR_CODES = {
  EVENT_NOT_FOUND: 'event_not_found',
  INVALID_STATUS: 'invalid_status',
  EVENT_ALREADY_PUBLISHED: 'event_already_published',
  EVENT_ALREADY_CANCELLED: 'event_already_cancelled',
  EVENT_NOT_EDITABLE: 'event_not_editable',
  DUPLICATE_SLUG: 'duplicate_slug',
} as const;

// Маппинг кодов ошибок событий на сообщения
export const EVENT_ERROR_MESSAGES: Record<string, string> = {
  [EVENT_ERROR_CODES.EVENT_NOT_FOUND]: 'Событие не найдено',
  [EVENT_ERROR_CODES.INVALID_STATUS]: 'Недопустимый статус события',
  [EVENT_ERROR_CODES.EVENT_ALREADY_PUBLISHED]: 'Событие уже опубликовано',
  [EVENT_ERROR_CODES.EVENT_ALREADY_CANCELLED]: 'Событие уже отменено',
  [EVENT_ERROR_CODES.EVENT_NOT_EDITABLE]: 'Событие нельзя редактировать',
  [EVENT_ERROR_CODES.DUPLICATE_SLUG]: 'Событие с таким URL уже существует',
};

// Получить сообщение об ошибке события по коду
export function getEventErrorMessage(code: string, fallback: string): string {
  return EVENT_ERROR_MESSAGES[code] || fallback;
}

// Коды ошибок API типов билетов
export const TICKET_TYPE_ERROR_CODES = {
  TICKET_TYPE_NOT_FOUND: 'ticket_type_not_found',
  TICKET_TYPE_HAS_REGISTRATIONS: 'ticket_type_has_registrations',
  TICKET_TYPE_SOLD_OUT: 'ticket_type_sold_out',
  INVALID_QUANTITY: 'invalid_quantity',
} as const;

// Маппинг кодов ошибок типов билетов на сообщения
export const TICKET_TYPE_ERROR_MESSAGES: Record<string, string> = {
  [TICKET_TYPE_ERROR_CODES.TICKET_TYPE_NOT_FOUND]: 'Тип билета не найден',
  [TICKET_TYPE_ERROR_CODES.TICKET_TYPE_HAS_REGISTRATIONS]: 'Есть регистрации на этот тип билета',
  [TICKET_TYPE_ERROR_CODES.TICKET_TYPE_SOLD_OUT]: 'Билеты распроданы',
  [TICKET_TYPE_ERROR_CODES.INVALID_QUANTITY]: 'Недопустимое количество билетов',
};

// Получить сообщение об ошибке типа билета по коду
export function getTicketTypeErrorMessage(code: string, fallback: string): string {
  return TICKET_TYPE_ERROR_MESSAGES[code] || fallback;
}
