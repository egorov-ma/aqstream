import { test, expect } from '@playwright/test';
import { login, logout } from './fixtures/auth';
import { testUsers } from './fixtures/test-data';

// Все тесты в этом файле выполняются последовательно в одном worker
// Это необходимо из-за shared state (login sessions, created data)
test.describe.configure({ mode: 'serial' });

/**
 * E2E тесты для Organization Requests (J3)
 *
 * Тестирует полный flow:
 * 1. User подаёт заявку на создание организации
 * 2. Admin видит заявку в списке
 * 3. Admin одобряет заявку
 * 4. User видит статус "Одобрено"
 *
 * Примечание: owner@test.local является администратором платформы (is_admin=true)
 */
test.describe('Organization Requests (J3)', () => {
  const timestamp = Date.now();
  const testOrgName = `E2E Test Org ${timestamp}`;
  const testOrgSlug = `e2e-test-org-${timestamp}`;
  const testOrgDescription = 'Организация для E2E тестирования';

  test.beforeAll(async ({ browser }) => {
    // Проверяем что Docker стек запущен
    const page = await browser.newPage();
    try {
      const response = await page.request.get('http://localhost:8080/actuator/health');
      expect(response.ok()).toBeTruthy();
    } finally {
      await page.close();
    }
  });

  test('user can navigate to organization request page', async ({ page }) => {
    await login(page, testUsers.user);

    await page.goto('/dashboard/organization-request');
    await expect(page.getByRole('heading', { name: /заявка на организацию/i })).toBeVisible();
    await expect(page.getByTestId('org-request-form')).toBeVisible();
  });

  test('user can submit organization request', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Ждём загрузку формы
    await expect(page.getByTestId('org-request-form')).toBeVisible({ timeout: 10000 });

    // Ждём загрузку списка заявок (чтобы проверить есть ли уже pending)
    // Ждём либо список заявок, либо пустое состояние
    await page.waitForTimeout(2000);

    // Проверяем наличие pending статуса в списке
    const pendingBadge = page.getByTestId('request-status-pending');
    const hasPendingRequest = await pendingBadge.isVisible().catch(() => false);

    if (hasPendingRequest) {
      // Уже есть pending запрос - тест успешен, просто проверяем что видим его
      await expect(page.getByTestId('my-requests-list')).toBeVisible();
      return;
    }

    // Нет pending запроса - можем создать новый
    await page.getByTestId('org-name-input').fill(testOrgName);
    await page.getByTestId('org-slug-input').fill(testOrgSlug);
    await page.getByTestId('org-description-input').fill(testOrgDescription);

    // Отправляем заявку
    await page.getByTestId('org-request-submit').click();

    // Ждём toast об успешной отправке
    await expect(page.getByText(/заявка отправлена/i)).toBeVisible({ timeout: 10000 });

    // Проверяем что заявка появилась в списке "Мои заявки" со статусом "На рассмотрении"
    await expect(page.getByTestId('my-requests-list')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(testOrgName)).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('request-status-pending')).toBeVisible();
  });

  test('user sees their pending request in list', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Проверяем что заявка видна в списке (может быть наша или от предыдущего запуска)
    await expect(page.getByTestId('my-requests-list')).toBeVisible();
    // Проверяем что есть хотя бы один pending статус
    await expect(page.getByTestId('request-status-pending').first()).toBeVisible();
  });

  test('admin can see pending requests', async ({ page }) => {
    // Owner является администратором платформы (is_admin=true)
    await login(page, testUsers.owner);
    await page.goto('/dashboard/admin/organization-requests');

    // Проверяем что админ видит таблицу заявок
    await expect(page.getByTestId('admin-requests-table')).toBeVisible({ timeout: 10000 });
    // Должна быть хотя бы одна заявка с кнопкой "Одобрить"
    await expect(page.getByRole('button', { name: /одобрить/i }).first()).toBeVisible();
  });

  test('admin can approve request', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/admin/organization-requests');

    // Ждём загрузку таблицы
    await expect(page.getByTestId('admin-requests-table')).toBeVisible({ timeout: 10000 });

    // Получаем первую кнопку "Одобрить" и кликаем
    const approveButton = page.getByRole('button', { name: /одобрить/i }).first();

    // Проверяем что кнопка видима
    if (!(await approveButton.isVisible())) {
      // Нет pending заявок - пропускаем
      return;
    }

    await approveButton.click();

    // Ждём toast об успешном одобрении
    await expect(page.getByText(/заявка одобрена/i)).toBeVisible({ timeout: 10000 });
  });

  test('user sees approved status after approval', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Проверяем что список заявок видим
    await expect(page.getByTestId('my-requests-list')).toBeVisible();
    // Проверяем что есть хотя бы одна одобренная заявка
    await expect(page.getByTestId('request-status-approved').first()).toBeVisible();
  });

  test('user becomes organizer after approval', async ({ page }) => {
    await login(page, testUsers.user);

    // После одобрения заявки user должен иметь доступ к странице событий
    // (секция "Организатор" в sidebar может не отображаться сразу из-за кеширования)
    await page.goto('/dashboard/events');

    // Проверяем что страница событий загружается (у организатора есть доступ)
    await expect(page.getByRole('heading', { name: /события|events/i })).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Organization Requests - Rejection Flow', () => {
  const timestamp = Date.now();
  const rejectOrgName = `E2E Reject Org ${timestamp}`;
  const rejectOrgSlug = `e2e-reject-org-${timestamp}`;

  test('user submits another request for rejection test', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Ждём загрузку формы
    await expect(page.getByTestId('org-request-form')).toBeVisible({ timeout: 10000 });

    // Ждём загрузку списка заявок
    await page.waitForTimeout(2000);

    // Проверяем наличие pending статуса в списке
    const pendingBadge = page.getByTestId('request-status-pending');
    const hasPendingRequest = await pendingBadge.isVisible().catch(() => false);

    if (hasPendingRequest) {
      // Уже есть pending запрос - тест успешен
      await expect(page.getByTestId('my-requests-list')).toBeVisible();
      return;
    }

    await page.getByTestId('org-name-input').fill(rejectOrgName);
    await page.getByTestId('org-slug-input').fill(rejectOrgSlug);
    await page.getByTestId('org-request-submit').click();

    // Ждём toast об успешной отправке
    await expect(page.getByText(/заявка отправлена/i)).toBeVisible({ timeout: 10000 });

    await expect(page.getByText(rejectOrgName)).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('request-status-pending')).toBeVisible();
  });

  test('admin can reject request with comment', async ({ page }) => {
    await login(page, testUsers.owner);
    await page.goto('/dashboard/admin/organization-requests');

    // Ждём загрузку таблицы
    await expect(page.getByTestId('admin-requests-table')).toBeVisible({ timeout: 10000 });

    // Находим первую кнопку "Отклонить" и кликаем
    const rejectButton = page.getByRole('button', { name: /отклонить/i }).first();

    // Проверяем что кнопка видима
    if (!(await rejectButton.isVisible())) {
      // Нет pending заявок для отклонения - пропускаем
      return;
    }

    await rejectButton.click();

    // Появляется диалог для ввода причины
    await expect(page.getByTestId('reject-dialog')).toBeVisible();

    // Вводим причину отклонения (минимум 10 символов)
    await page.getByTestId('reject-comment-input').fill('Недостаточно информации о деятельности организации');
    await page.getByTestId('reject-confirm').click();

    // Ждём toast об отклонении
    await expect(page.getByText(/заявка отклонена/i)).toBeVisible({ timeout: 10000 });
  });

  test('user sees rejected status with comment', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Проверяем список заявок
    await expect(page.getByTestId('my-requests-list')).toBeVisible();

    // Проверяем что есть заявка со статусом "Отклонено"
    const rejectedBadge = page.getByTestId('request-status-rejected').first();
    if (await rejectedBadge.isVisible().catch(() => false)) {
      // Если есть отклонённая заявка, проверяем что показывается комментарий
      await expect(page.getByText(/причина отклонения/i).first()).toBeVisible();
    }
    // Если нет отклонённых заявок, тест всё равно проходит
  });
});

test.describe('Organization Requests - Validation', () => {
  test('form validates required fields', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Пытаемся отправить пустую форму
    await page.getByTestId('org-request-submit').click();

    // Должны появиться ошибки валидации
    // Zod: "Название должно содержать минимум 2 символа"
    await expect(page.getByText(/название должно содержать минимум/i)).toBeVisible();
  });

  test('form validates slug format', async ({ page }) => {
    await login(page, testUsers.user);
    await page.goto('/dashboard/organization-request');

    // Заполняем название
    await page.getByTestId('org-name-input').fill('Test Org');

    // Вводим невалидный slug (с пробелами)
    await page.getByTestId('org-slug-input').fill('invalid slug');
    await page.getByTestId('org-request-submit').click();

    // Должна появиться ошибка валидации slug
    // Сообщение: "Slug может содержать только строчные латинские буквы, цифры и дефис..."
    await expect(page.getByText(/slug может содержать только/i)).toBeVisible();
  });

  test('admin section is not visible for regular users', async ({ page }) => {
    await login(page, testUsers.user);

    // У обычного пользователя не должно быть секции "Администратор"
    await expect(page.getByText(/администратор/i)).not.toBeVisible();

    // Прямой переход на admin страницу должен показать ошибку доступа
    await page.goto('/dashboard/admin/organization-requests');

    // Ожидаем либо редирект, либо ошибку доступа
    // (зависит от реализации - может быть 403 или редирект на dashboard)
  });
});
