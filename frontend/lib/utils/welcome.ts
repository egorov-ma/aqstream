import { getFrontendVersion, fetchSystemVersion } from '@/lib/api/version';

/**
 * ASCII art логотип AqStream.
 */
const AQSTREAM_ASCII = `
   ___       _____ _
  / _ \\     /  ___| |
 / /_\\ \\ __ \\ \`--.| |_ _ __ ___  __ _ _ __ ___
 |  _  |/ _\\ \`--. \\ __| '__/ _ \\/ _\` | '_ \` _ \\
 | | | | (_| /\\__/ / |_| | |  __/ (_| | | | | | |
 \\_| |_/\\__, \\____/ \\__|_|  \\___|\\__,_|_| |_| |_|
           | |
           |_|
`;

/**
 * Форматирует дату для отображения в консоли.
 */
function formatDate(dateString: string): string {
  try {
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateString;
  }
}

/**
 * Выводит приветственное сообщение в консоль браузера.
 */
export function printWelcomeMessage(): void {
  if (typeof window === 'undefined') return;

  const version = getFrontendVersion();

  // ASCII art логотип
  console.log('%c' + AQSTREAM_ASCII, 'color: #3b82f6; font-family: monospace;');

  // Название платформы
  console.log(
    '%cAqStream Platform',
    'color: #3b82f6; font-size: 16px; font-weight: bold;'
  );

  // Информация о фронтенде
  console.log(
    `%cФронтенд: v${version.version} (${version.gitCommit})`,
    'color: #6b7280; font-size: 12px;'
  );

  console.log(
    `%cСборка: ${formatDate(version.buildTime)}`,
    'color: #6b7280; font-size: 12px;'
  );

  // Подсказка для получения версий
  console.log(
    '%c\nДля информации о версиях сервисов: AqStream.versions()',
    'color: #10b981; font-size: 11px;'
  );
}

/**
 * Выводит таблицу версий всех сервисов.
 */
async function printVersionsTable(): Promise<void> {
  console.log('%cЗагрузка версий сервисов...', 'color: #6b7280;');

  try {
    const data = await fetchSystemVersion();
    const frontend = getFrontendVersion();

    // Формируем данные для таблицы
    const tableData: Record<string, string> = {
      Frontend: frontend.version,
      Gateway: data.gateway?.version || 'N/A',
    };

    // Добавляем сервисы
    if (data.services) {
      Object.entries(data.services).forEach(([name, service]) => {
        const displayName = name
          .split('-')
          .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
          .join(' ');
        tableData[displayName] = service?.version || 'N/A';
      });
    }

    // Добавляем инфраструктуру
    if (data.infrastructure) {
      if (data.infrastructure.postgresql) {
        tableData['PostgreSQL'] = data.infrastructure.postgresql;
      }
      if (data.infrastructure.redis) {
        tableData['Redis'] = data.infrastructure.redis;
      }
      if (data.infrastructure.rabbitmq) {
        tableData['RabbitMQ'] = data.infrastructure.rabbitmq;
      }
    }

    console.table(tableData);

    // Дополнительная информация
    console.log(
      `%cОкружение: ${data.environment}`,
      'color: #6b7280; font-size: 11px;'
    );
    console.log(
      `%cTimestamp: ${formatDate(data.timestamp)}`,
      'color: #6b7280; font-size: 11px;'
    );
  } catch (error) {
    console.error('%cНе удалось загрузить версии сервисов', 'color: #ef4444;');
    console.error(error);
  }
}

/**
 * Интерфейс для глобального объекта AqStream.
 */
interface AqStreamGlobal {
  versions: () => Promise<void>;
  version: () => void;
}

/**
 * Расширение типа Window для AqStream.
 */
declare global {
  interface Window {
    AqStream: AqStreamGlobal;
  }
}

/**
 * Регистрирует глобальную команду AqStream.versions() в консоли.
 */
export function registerVersionCommand(): void {
  if (typeof window === 'undefined') return;

  window.AqStream = {
    versions: printVersionsTable,
    version: () => {
      const version = getFrontendVersion();
      console.log(
        `%cAqStream Frontend v${version.version}`,
        'color: #3b82f6; font-weight: bold;'
      );
      console.log(`Git: ${version.gitCommit}`);
      console.log(`Build: ${formatDate(version.buildTime)}`);
    },
  };
}
