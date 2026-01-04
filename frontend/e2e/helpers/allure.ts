import { test } from '@playwright/test';
import type { Page, Response } from '@playwright/test';

/**
 * Allure утилиты для Playwright E2E тестов.
 */

/**
 * Прикрепить скриншот к отчёту.
 */
export async function attachScreenshot(page: Page, name: string) {
  const screenshot = await page.screenshot();
  await test.info().attach(name, {
    body: screenshot,
    contentType: 'image/png',
  });
}

/**
 * Прикрепить JSON к отчёту.
 */
export async function attachJson(name: string, data: any) {
  await test.info().attach(name, {
    body: JSON.stringify(data, null, 2),
    contentType: 'application/json',
  });
}

/**
 * Прикрепить HTML к отчёту.
 */
export async function attachHtml(page: Page, name: string) {
  const html = await page.content();
  await test.info().attach(name, {
    body: html,
    contentType: 'text/html',
  });
}

/**
 * Прикрепить текст к отчёту.
 */
export async function attachText(name: string, content: string) {
  await test.info().attach(name, {
    body: content,
    contentType: 'text/plain',
  });
}

/**
 * Мониторить API запросы и прикреплять к отчёту.
 */
export async function attachApiRequests(page: Page) {
  const requests: Array<{ url: string; method: string; status: number }> = [];

  page.on('response', (response: Response) => {
    const request = response.request();
    // Только API запросы (не статика)
    if (request.url().includes('/api/')) {
      requests.push({
        url: request.url(),
        method: request.method(),
        status: response.status(),
      });
    }
  });

  // Прикрепить в конце теста
  test.afterEach(async () => {
    if (requests.length > 0) {
      await attachJson('API Requests', requests);
    }
  });
}

/**
 * Логировать и прикреплять ошибки консоли.
 */
export async function attachConsoleErrors(page: Page) {
  const errors: string[] = [];

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });

  test.afterEach(async () => {
    if (errors.length > 0) {
      await attachText('Console Errors', errors.join('\n'));
    }
  });
}

/**
 * Прикрепить localStorage и sessionStorage.
 */
export async function attachStorage(page: Page) {
  const localStorage = await page.evaluate(() => JSON.stringify(window.localStorage));
  const sessionStorage = await page.evaluate(() => JSON.stringify(window.sessionStorage));

  await attachJson('localStorage', JSON.parse(localStorage));
  await attachJson('sessionStorage', JSON.parse(sessionStorage));
}
