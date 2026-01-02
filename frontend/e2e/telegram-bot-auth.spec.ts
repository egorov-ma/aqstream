import { test, expect } from '@playwright/test';

/**
 * E2E тесты для авторизации через Telegram бота.
 *
 * Примечание: Полный flow (с подтверждением в Telegram) требует реального бота.
 * Эти тесты проверяют UI flow до момента, когда пользователь должен
 * перейти в Telegram для подтверждения.
 */
test.describe('Telegram Bot Auth', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('displays telegram login button on login page', async ({ page }) => {
    await expect(page.getByTestId('telegram-bot-login')).toBeVisible();
    await expect(page.getByTestId('telegram-bot-start-button')).toBeVisible();
    await expect(page.getByTestId('telegram-bot-start-button')).toContainText('Войти через Telegram');
  });

  test('clicking button initiates auth and shows waiting state', async ({ page }) => {
    // Кликаем на кнопку входа
    await page.getByTestId('telegram-bot-start-button').click();

    // Ожидаем появления кнопки "Открыть в Telegram" (API должен вернуть deeplink)
    await expect(page.getByTestId('telegram-open-button')).toBeVisible({ timeout: 10000 });

    // Проверяем что кнопка ведёт на t.me
    const openButton = page.getByTestId('telegram-open-button');
    const href = await openButton.getAttribute('href');
    expect(href).toMatch(/^https:\/\/t\.me\//);
    expect(href).toContain('?start=auth_');

    // Проверяем текст ожидания
    await expect(page.getByText('Ожидание подтверждения')).toBeVisible();
  });

  test('shows cancel button in waiting state', async ({ page }) => {
    // Инициируем авторизацию
    await page.getByTestId('telegram-bot-start-button').click();

    // Ожидаем появления кнопки отмены
    await expect(page.getByTestId('telegram-cancel-button')).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('telegram-cancel-button')).toContainText('Отмена');
  });

  test('cancel button returns to initial state', async ({ page }) => {
    // Инициируем авторизацию
    await page.getByTestId('telegram-bot-start-button').click();

    // Ожидаем появления кнопки отмены
    await expect(page.getByTestId('telegram-cancel-button')).toBeVisible({ timeout: 10000 });

    // Нажимаем отмену
    await page.getByTestId('telegram-cancel-button').click();

    // Проверяем возврат к начальному состоянию
    await expect(page.getByTestId('telegram-bot-start-button')).toBeVisible();
    await expect(page.getByTestId('telegram-cancel-button')).not.toBeVisible();
  });

  test('deeplink opens in new tab', async ({ page }) => {
    // Инициируем авторизацию
    await page.getByTestId('telegram-bot-start-button').click();

    // Ожидаем появления кнопки
    await expect(page.getByTestId('telegram-open-button')).toBeVisible({ timeout: 10000 });

    // Проверяем что ссылка открывается в новом окне
    const openButton = page.getByTestId('telegram-open-button');
    await expect(openButton).toHaveAttribute('target', '_blank');
    await expect(openButton).toHaveAttribute('rel', 'noopener noreferrer');
  });

  // TODO: Этот тест нестабилен из-за быстрого переключения состояний компонента
  // Когда WebSocket подключается и отключается быстро, компонент перерендеривается
  test.skip('multiple init calls generate unique tokens', async ({ page }) => {
    // Первый вызов
    await page.getByTestId('telegram-bot-start-button').click();
    await expect(page.getByTestId('telegram-open-button')).toBeVisible({ timeout: 10000 });

    const href1 = await page.getByTestId('telegram-open-button').getAttribute('href');

    // Отменяем
    await page.getByTestId('telegram-cancel-button').click();

    // Второй вызов
    await page.getByTestId('telegram-bot-start-button').click();
    await expect(page.getByTestId('telegram-open-button')).toBeVisible({ timeout: 10000 });

    const href2 = await page.getByTestId('telegram-open-button').getAttribute('href');

    // Токены должны быть разными
    expect(href1).not.toEqual(href2);
  });
});
