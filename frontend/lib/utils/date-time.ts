import { format, parseISO, isValid } from 'date-fns';
import { ru } from 'date-fns/locale';

// Часовые пояса для формы
export const TIMEZONE_OPTIONS = [
  { value: 'Europe/Kaliningrad', label: 'Калининград (UTC+2)' },
  { value: 'Europe/Moscow', label: 'Москва (UTC+3)' },
  { value: 'Europe/Samara', label: 'Самара (UTC+4)' },
  { value: 'Asia/Yekaterinburg', label: 'Екатеринбург (UTC+5)' },
  { value: 'Asia/Omsk', label: 'Омск (UTC+6)' },
  { value: 'Asia/Krasnoyarsk', label: 'Красноярск (UTC+7)' },
  { value: 'Asia/Irkutsk', label: 'Иркутск (UTC+8)' },
  { value: 'Asia/Yakutsk', label: 'Якутск (UTC+9)' },
  { value: 'Asia/Vladivostok', label: 'Владивосток (UTC+10)' },
  { value: 'Asia/Magadan', label: 'Магадан (UTC+11)' },
  { value: 'Asia/Kamchatka', label: 'Камчатка (UTC+12)' },
] as const;

// Форматирование даты для отображения
export function formatEventDate(isoString: string | undefined | null): string {
  if (!isoString) return '';

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return '';
    return format(date, 'd MMMM yyyy, HH:mm', { locale: ru });
  } catch {
    return '';
  }
}

// Форматирование только даты (без времени)
export function formatDateOnly(isoString: string | undefined | null): string {
  if (!isoString) return '';

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return '';
    return format(date, 'd MMMM yyyy', { locale: ru });
  } catch {
    return '';
  }
}

// Форматирование только времени
export function formatTimeOnly(isoString: string | undefined | null): string {
  if (!isoString) return '';

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return '';
    return format(date, 'HH:mm', { locale: ru });
  } catch {
    return '';
  }
}

// Получить метку часового пояса
export function getTimezoneLabel(timezone: string): string {
  const option = TIMEZONE_OPTIONS.find((tz) => tz.value === timezone);
  return option?.label ?? timezone;
}

// Объединить дату и время в ISO строку
export function combineDateAndTime(
  dateStr: string,
  timeStr: string,
  timezone: string = 'Europe/Moscow'
): string {
  if (!dateStr) return '';

  // Если время не указано, используем 00:00
  const time = timeStr || '00:00';

  // Создаём дату в указанном часовом поясе
  // Для простоты используем локальную дату (форма работает в локальном времени)
  const dateTimeStr = `${dateStr}T${time}:00`;

  try {
    const date = new Date(dateTimeStr);
    if (!isValid(date)) return '';
    return date.toISOString();
  } catch {
    return '';
  }
}

// Извлечь дату из ISO строки (YYYY-MM-DD)
export function extractDatePart(isoString: string | undefined | null): string {
  if (!isoString) return '';

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return '';
    return format(date, 'yyyy-MM-dd');
  } catch {
    return '';
  }
}

// Извлечь время из ISO строки (HH:mm)
export function extractTimePart(isoString: string | undefined | null): string {
  if (!isoString) return '';

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return '';
    return format(date, 'HH:mm');
  } catch {
    return '';
  }
}

// Проверить, что дата валидна
export function isValidDateString(dateStr: string | undefined | null): boolean {
  if (!dateStr) return false;

  try {
    const date = parseISO(dateStr);
    return isValid(date);
  } catch {
    return false;
  }
}

// Получить текущую дату в формате ISO (для default value)
export function getCurrentISODate(): string {
  return new Date().toISOString();
}

// Проверить, что дата в будущем
export function isFutureDate(isoString: string | undefined | null): boolean {
  if (!isoString) return false;

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return false;
    return date > new Date();
  } catch {
    return false;
  }
}

// Проверить, что дата в прошлом
export function isPastDate(isoString: string | undefined | null): boolean {
  if (!isoString) return false;

  try {
    const date = parseISO(isoString);
    if (!isValid(date)) return false;
    return date < new Date();
  } catch {
    return false;
  }
}
