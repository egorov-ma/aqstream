/**
 * Утилиты для работы с календарём (ICS формат)
 */

/**
 * Форматирует дату в формат ICS (YYYYMMDDTHHMMSSZ)
 * @param isoString - дата в ISO формате
 * @returns дата в формате ICS
 */
export function formatIcsDate(isoString: string): string {
  const date = new Date(isoString);

  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  const day = String(date.getUTCDate()).padStart(2, '0');
  const hours = String(date.getUTCHours()).padStart(2, '0');
  const minutes = String(date.getUTCMinutes()).padStart(2, '0');
  const seconds = String(date.getUTCSeconds()).padStart(2, '0');

  return `${year}${month}${day}T${hours}${minutes}${seconds}Z`;
}

/**
 * Экранирует специальные символы для ICS
 * RFC 5545: backslash, semicolon, comma должны быть экранированы
 * Переносы строк заменяются на \n
 * @param text - исходный текст
 * @returns экранированный текст
 */
export function escapeIcs(text: string): string {
  if (!text) return '';

  return text
    .replace(/\\/g, '\\\\') // Сначала экранируем backslash
    .replace(/;/g, '\\;')
    .replace(/,/g, '\\,')
    .replace(/\r?\n/g, '\\n');
}

/**
 * Генерирует содержимое ICS файла для события
 * @param options - параметры события
 * @returns содержимое ICS файла
 */
export function generateIcsContent(options: {
  id: string;
  title: string;
  startsAt: string;
  endsAt?: string | null;
  description?: string | null;
  location?: string | null;
}): string {
  const { id, title, startsAt, endsAt, description, location } = options;

  const lines: string[] = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//AqStream//Event//RU',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'BEGIN:VEVENT',
    `UID:${id}@aqstream.ru`,
    `DTSTAMP:${formatIcsDate(new Date().toISOString())}`,
    `DTSTART:${formatIcsDate(startsAt)}`,
  ];

  // Дата окончания
  if (endsAt) {
    lines.push(`DTEND:${formatIcsDate(endsAt)}`);
  }

  // Название
  lines.push(`SUMMARY:${escapeIcs(title)}`);

  // Описание (ограничиваем 500 символами)
  if (description) {
    const truncatedDescription = description.slice(0, 500);
    lines.push(`DESCRIPTION:${escapeIcs(truncatedDescription)}`);
  }

  // Место
  if (location) {
    lines.push(`LOCATION:${escapeIcs(location)}`);
  }

  lines.push('END:VEVENT', 'END:VCALENDAR');

  return lines.join('\r\n');
}

/**
 * Генерирует URL для добавления события в Google Calendar
 * @param options - параметры события
 * @returns URL для Google Calendar
 */
export function generateGoogleCalendarUrl(options: {
  title: string;
  startsAt: string;
  endsAt?: string | null;
  description?: string | null;
  location?: string | null;
}): string {
  const { title, startsAt, endsAt, description, location } = options;

  // Формат дат для Google Calendar совпадает с ICS (YYYYMMDDTHHMMSSZ)
  const startDate = formatIcsDate(startsAt);
  const endDate = formatIcsDate(endsAt ?? startsAt);

  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: title,
    dates: `${startDate}/${endDate}`,
  });

  if (description) {
    params.set('details', description.slice(0, 500));
  }

  if (location) {
    params.set('location', location);
  }

  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

/**
 * Скачивает файл с указанным содержимым
 * @param content - содержимое файла
 * @param filename - имя файла
 * @param mimeType - MIME тип файла
 */
export function downloadFile(content: string, filename: string, mimeType: string): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';

  document.body.appendChild(link);
  link.click();

  // Очистка
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
