// Коды ошибок API аутентификации
export const AUTH_ERROR_CODES = {
  INVALID_CREDENTIALS: 'invalid_credentials',
  EMAIL_NOT_VERIFIED: 'email_not_verified',
  ACCOUNT_LOCKED: 'account_locked',
  EMAIL_ALREADY_EXISTS: 'email_already_exists',
  INVALID_TOKEN: 'invalid_token',
} as const;

// Маппинг кодов ошибок на сообщения
export const AUTH_ERROR_MESSAGES: Record<string, string> = {
  [AUTH_ERROR_CODES.INVALID_CREDENTIALS]: 'Неверный email или пароль',
  [AUTH_ERROR_CODES.EMAIL_NOT_VERIFIED]: 'Подтвердите email для входа',
  [AUTH_ERROR_CODES.ACCOUNT_LOCKED]: 'Аккаунт заблокирован. Попробуйте через 15 минут',
  [AUTH_ERROR_CODES.EMAIL_ALREADY_EXISTS]: 'Пользователь с таким email уже существует',
  [AUTH_ERROR_CODES.INVALID_TOKEN]: 'Ссылка недействительна или срок её действия истёк',
};

// Получить сообщение об ошибке по коду
export function getAuthErrorMessage(code: string, fallback: string): string {
  return AUTH_ERROR_MESSAGES[code] || fallback;
}
