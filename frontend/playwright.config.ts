import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright конфигурация для E2E тестов
 *
 * Настройка workers:
 *   PW_WORKERS=2 pnpm test:e2e  # Локально с 2 workers
 *   PW_WORKERS=1 pnpm test:e2e  # Последовательно
 *
 * Allure отчёт:
 *   pnpm exec allure serve allure-results
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  // 1 retry локально для обнаружения flaky, 2 в CI
  retries: process.env.CI ? 2 : 1,
  // Настраиваемое количество workers через PW_WORKERS env
  // В CI используем 2 workers для стабильности (меньше race conditions)
  workers: parseInt(process.env.PW_WORKERS || '') || (process.env.CI ? 2 : undefined),
  // Reporters: html для просмотра, list для консоли, allure для отчётов, json для анализа
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
    ['allure-playwright'],
    ['json', { outputFile: 'test-results/results.json' }],
  ],
  // Таймауты: увеличены для CI
  timeout: process.env.CI ? 90_000 : 60_000,
  expect: {
    // Увеличен timeout для assertion (SSR + API latency)
    timeout: process.env.CI ? 20_000 : 10_000,
  },
  use: {
    baseURL: 'http://localhost:3000',
    // Trace записывается при retry для отладки
    trace: 'on-first-retry',
    // Screenshot и video только при падении
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    // Увеличенные таймауты для навигации и действий
    navigationTimeout: process.env.CI ? 30_000 : 15_000,
    actionTimeout: process.env.CI ? 15_000 : 10_000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: process.env.CI ? 180_000 : 120_000,
  },
});
