import '@testing-library/jest-dom/vitest';
import { beforeAll, afterEach, afterAll, vi } from 'vitest';
import { server } from '@/lib/test/mocks/server';

// Mock localStorage и sessionStorage для тестов
const storageMock = () => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString();
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
};

Object.defineProperty(global, 'localStorage', {
  value: storageMock(),
});

Object.defineProperty(global, 'sessionStorage', {
  value: storageMock(),
});

// Настройка MSW для интеграционных тестов
beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
