/**
 * Типы и утилиты для работы с версиями сервисов.
 */

/**
 * Информация о версии отдельного сервиса.
 */
export interface ServiceVersion {
  name: string;
  version: string;
  buildTime: string | null;
  gitCommit: string | null;
  gitBranch: string | null;
  gitCommitTime: string | null;
  javaVersion: string | null;
  springBootVersion: string | null;
}

/**
 * Информация о версиях инфраструктуры.
 */
export interface InfrastructureVersion {
  postgresql: string | null;
  redis: string | null;
  rabbitmq: string | null;
}

/**
 * Агрегированная информация о версиях всей системы.
 */
export interface SystemVersion {
  platform: string;
  environment: string;
  timestamp: string;
  frontend: ServiceVersion | null;
  gateway: ServiceVersion;
  services: Record<string, ServiceVersion>;
  infrastructure: InfrastructureVersion | null;
}

/**
 * Информация о версии фронтенда.
 */
export interface FrontendVersion {
  name: string;
  version: string;
  buildTime: string;
  gitCommit: string;
}

/**
 * Получает информацию о версии фронтенда из environment переменных.
 */
export function getFrontendVersion(): FrontendVersion {
  return {
    name: 'aqstream-frontend',
    version: process.env.NEXT_PUBLIC_APP_VERSION || 'unknown',
    buildTime: process.env.NEXT_PUBLIC_BUILD_TIME || new Date().toISOString(),
    gitCommit: process.env.NEXT_PUBLIC_GIT_COMMIT || 'unknown',
  };
}

/**
 * Получает агрегированные версии всех сервисов с backend.
 */
export async function fetchSystemVersion(): Promise<SystemVersion> {
  const response = await fetch('/api/v1/system/version');
  if (!response.ok) {
    throw new Error(`Ошибка получения версий: ${response.status}`);
  }
  return response.json();
}
