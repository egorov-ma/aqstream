import { test, expect } from '@playwright/test';

test.describe('Auth Pages', () => {
  test.describe('Login Page', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/login');
    });

    test('displays login form with all elements', async ({ page }) => {
      await test.step('Проверить видимость формы', async () => {
        await expect(page.getByTestId('login-form')).toBeVisible();
      });

      await test.step('Проверить поля ввода', async () => {
        await expect(page.getByTestId('email-input')).toBeVisible();
        await expect(page.getByTestId('password-input')).toBeVisible();
      });

      await test.step('Проверить кнопку и ссылки', async () => {
        await expect(page.getByTestId('login-submit')).toBeVisible();
        await expect(page.getByTestId('forgot-password-link')).toBeVisible();
        await expect(page.getByTestId('register-link')).toBeVisible();
      });
    });

    test('shows validation errors for empty fields', async ({ page }) => {
      await test.step('Нажать кнопку входа без заполнения', async () => {
        await page.getByTestId('login-submit').click();
      });

      await test.step('Проверить ошибки валидации', async () => {
        await expect(page.getByText(/email обязателен/i)).toBeVisible();
        await expect(page.getByText(/пароль обязателен/i)).toBeVisible();
      });
    });

    // ПРИМЕЧАНИЕ: Тест на invalid email format удалён из E2E
    // Асинхронная валидация Zod + React Hook Form нестабильна в Playwright
    // Этот сценарий покрыт unit тестами в lib/validations/auth.test.ts

    test('navigates to register page', async ({ page }) => {
      await test.step('Нажать ссылку регистрации', async () => {
        await page.getByTestId('register-link').click();
      });

      await test.step('Проверить переход на страницу регистрации', async () => {
        await expect(page).toHaveURL('/register');
      });
    });

    test('navigates to forgot password page', async ({ page }) => {
      await test.step('Нажать ссылку восстановления пароля', async () => {
        await page.getByTestId('forgot-password-link').click();
      });

      await test.step('Проверить переход на страницу восстановления', async () => {
        await expect(page).toHaveURL('/forgot-password');
      });
    });
  });

  test.describe('Register Page', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/register');
    });

    test('displays registration form with all elements', async ({ page }) => {
      await test.step('Проверить видимость формы', async () => {
        await expect(page.getByTestId('register-form')).toBeVisible();
      });

      await test.step('Проверить все поля ввода', async () => {
        await expect(page.getByTestId('email-input')).toBeVisible();
        await expect(page.getByTestId('first-name-input')).toBeVisible();
        await expect(page.getByTestId('last-name-input')).toBeVisible();
        await expect(page.getByTestId('password-input')).toBeVisible();
        await expect(page.getByTestId('confirm-password-input')).toBeVisible();
      });

      await test.step('Проверить кнопку и ссылки', async () => {
        await expect(page.getByTestId('register-submit')).toBeVisible();
        await expect(page.getByTestId('login-link')).toBeVisible();
      });
    });

    test('shows validation errors for empty required fields', async ({ page }) => {
      await test.step('Нажать кнопку регистрации без заполнения', async () => {
        await page.getByTestId('register-submit').click();
      });

      await test.step('Проверить ошибки валидации', async () => {
        await expect(page.getByText(/email обязателен/i)).toBeVisible();
        await expect(page.getByText(/имя обязательно/i)).toBeVisible();
      });
    });

    test('shows validation error for short password', async ({ page }) => {
      await test.step('Заполнить форму с коротким паролем', async () => {
        await page.getByTestId('email-input').fill('test@example.com');
        await page.getByTestId('first-name-input').fill('Иван');
        await page.getByTestId('password-input').fill('pass1');
        await page.getByTestId('confirm-password-input').fill('pass1');
      });

      await test.step('Отправить форму', async () => {
        await page.getByTestId('register-submit').click();
      });

      await test.step('Проверить ошибку валидации пароля', async () => {
        await expect(
          page.getByText(/пароль должен содержать минимум 8 символов/i)
        ).toBeVisible();
      });
    });

    test('shows validation error for mismatched passwords', async ({ page }) => {
      await test.step('Заполнить форму с разными паролями', async () => {
        await page.getByTestId('email-input').fill('test@example.com');
        await page.getByTestId('first-name-input').fill('Иван');
        await page.getByTestId('password-input').fill('password123');
        await page.getByTestId('confirm-password-input').fill('different123');
      });

      await test.step('Отправить форму', async () => {
        await page.getByTestId('register-submit').click();
      });

      await test.step('Проверить ошибку несовпадения паролей', async () => {
        await expect(page.getByText(/пароли должны совпадать/i)).toBeVisible();
      });
    });

    test('navigates to login page', async ({ page }) => {
      await test.step('Нажать ссылку входа', async () => {
        await page.getByTestId('login-link').click();
      });

      await test.step('Проверить переход на страницу входа', async () => {
        await expect(page).toHaveURL('/login');
      });
    });
  });

  test.describe('Forgot Password Page', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/forgot-password');
    });

    test('displays forgot password form', async ({ page }) => {
      await test.step('Проверить видимость формы', async () => {
        await expect(page.getByTestId('forgot-password-form')).toBeVisible();
      });

      await test.step('Проверить поля и кнопки', async () => {
        await expect(page.getByTestId('email-input')).toBeVisible();
        await expect(page.getByTestId('forgot-password-submit')).toBeVisible();
        await expect(page.getByTestId('login-link')).toBeVisible();
      });
    });

    test('shows validation error for empty email', async ({ page }) => {
      await test.step('Нажать кнопку отправки без заполнения', async () => {
        await page.getByTestId('forgot-password-submit').click();
      });

      await test.step('Проверить ошибку валидации', async () => {
        await expect(page.getByText(/email обязателен/i)).toBeVisible();
      });
    });

    // ПРИМЕЧАНИЕ: Тест на invalid email format удалён из E2E
    // Асинхронная валидация Zod + React Hook Form нестабильна в Playwright
    // Этот сценарий покрыт unit тестами в lib/validations/auth.test.ts

    test('navigates back to login', async ({ page }) => {
      await test.step('Нажать ссылку входа', async () => {
        await page.getByTestId('login-link').click();
      });

      await test.step('Проверить переход на страницу входа', async () => {
        await expect(page).toHaveURL('/login');
      });
    });
  });

  test.describe('Reset Password Page', () => {
    test('shows error without token', async ({ page }) => {
      await test.step('Открыть страницу без токена', async () => {
        await page.goto('/reset-password');
      });

      await test.step('Проверить ошибку недействительной ссылки', async () => {
        await expect(page.getByTestId('api-error-message')).toContainText(
          /недействительная ссылка/i
        );
      });
    });

    test('displays reset password form with token', async ({ page }) => {
      await test.step('Открыть страницу с токеном', async () => {
        await page.goto('/reset-password?token=valid-token-123');
      });

      await test.step('Проверить видимость формы', async () => {
        await expect(page.getByTestId('reset-password-form')).toBeVisible();
      });

      await test.step('Проверить поля и кнопки', async () => {
        await expect(page.getByTestId('password-input')).toBeVisible();
        await expect(page.getByTestId('confirm-password-input')).toBeVisible();
        await expect(page.getByTestId('reset-password-submit')).toBeVisible();
      });
    });

    test('shows validation error for mismatched passwords', async ({ page }) => {
      await test.step('Открыть страницу с токеном', async () => {
        await page.goto('/reset-password?token=valid-token-123');
      });

      await test.step('Заполнить разные пароли', async () => {
        await page.getByTestId('password-input').fill('newpassword123');
        await page.getByTestId('confirm-password-input').fill('different123');
      });

      await test.step('Отправить форму', async () => {
        await page.getByTestId('reset-password-submit').click();
      });

      await test.step('Проверить ошибку несовпадения паролей', async () => {
        await expect(page.getByText(/пароли должны совпадать/i)).toBeVisible();
      });
    });
  });

  test.describe('Verify Email Sent Page', () => {
    test('displays success message', async ({ page }) => {
      await test.step('Открыть страницу подтверждения email', async () => {
        await page.goto('/verify-email-sent');
      });

      await test.step('Проверить сообщение и ссылку', async () => {
        await expect(page.getByText(/проверьте почту/i)).toBeVisible();
        await expect(page.getByTestId('login-link')).toBeVisible();
      });
    });

    test('navigates to login page', async ({ page }) => {
      await test.step('Открыть страницу подтверждения email', async () => {
        await page.goto('/verify-email-sent');
      });

      await test.step('Нажать ссылку входа', async () => {
        await page.getByTestId('login-link').click();
      });

      await test.step('Проверить переход на страницу входа', async () => {
        await expect(page).toHaveURL('/login');
      });
    });
  });

  test.describe('Navigation Flow', () => {
    test('full navigation flow between auth pages', async ({ page }) => {
      await test.step('Начать с login страницы', async () => {
        await page.goto('/login');
        await expect(page).toHaveURL('/login');
      });

      await test.step('Перейти на register', async () => {
        await page.getByTestId('register-link').click();
        await expect(page).toHaveURL('/register');
      });

      await test.step('Вернуться на login', async () => {
        await page.getByTestId('login-link').click();
        await expect(page).toHaveURL('/login');
      });

      await test.step('Перейти на forgot-password', async () => {
        await page.getByTestId('forgot-password-link').click();
        await expect(page).toHaveURL('/forgot-password');
      });

      await test.step('Вернуться на login', async () => {
        await page.getByTestId('login-link').click();
        await expect(page).toHaveURL('/login');
      });
    });
  });
});
