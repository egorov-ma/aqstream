import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { printWelcomeMessage, registerVersionCommand } from './welcome';
import * as versionApi from '@/lib/api/version';

// Мокаем API модуль
vi.mock('@/lib/api/version', () => ({
  getFrontendVersion: vi.fn(() => ({
    version: '1.0.0',
    gitCommit: 'abc123',
    buildTime: '2025-01-01T00:00:00.000Z',
  })),
  fetchSystemVersion: vi.fn(() =>
    Promise.resolve({
      environment: 'development',
      timestamp: '2025-01-01T00:00:00.000Z',
      gateway: { version: '1.0.0' },
      services: {
        'user-service': { version: '1.0.0' },
        'event-service': { version: '1.0.0' },
      },
      infrastructure: {
        postgresql: '16.0',
        redis: '7.0',
        rabbitmq: '3.12',
      },
    })
  ),
}));

describe('printWelcomeMessage', () => {
  const originalWindow = global.window;
  const originalConsole = global.console;

  beforeEach(() => {
    vi.useFakeTimers();
    // Мокаем window
    global.window = {} as Window & typeof globalThis;
    // Мокаем console
    global.console = {
      ...originalConsole,
      log: vi.fn(),
      error: vi.fn(),
    } as unknown as Console;
  });

  afterEach(() => {
    vi.useRealTimers();
    global.window = originalWindow;
    global.console = originalConsole;
  });

  it('does nothing when window is undefined', () => {
    // @ts-expect-error - тестируем SSR окружение
    global.window = undefined;

    printWelcomeMessage();
    vi.advanceTimersByTime(200);

    expect(console.log).not.toHaveBeenCalled();
  });

  it('prints welcome message in browser', () => {
    printWelcomeMessage();
    vi.advanceTimersByTime(200);

    // Проверяем, что console.log был вызван несколько раз
    expect(console.log).toHaveBeenCalled();
  });

  it('prints ASCII art logo', () => {
    printWelcomeMessage();
    vi.advanceTimersByTime(200);

    // Ищем вызов с ASCII art (содержит AqStream)
    const calls = (console.log as ReturnType<typeof vi.fn>).mock.calls;
    const hasAsciiArt = calls.some((call) => {
      const message = call[0];
      return typeof message === 'string' && message.includes('AqStream');
    });

    expect(hasAsciiArt).toBe(true);
  });

  it('handles errors gracefully', () => {
    // Мокаем getFrontendVersion чтобы выбросить ошибку
    vi.mocked(versionApi.getFrontendVersion).mockImplementationOnce(() => {
      throw new Error('Test error');
    });

    printWelcomeMessage();
    vi.advanceTimersByTime(200);

    expect(console.error).toHaveBeenCalled();
  });
});

describe('registerVersionCommand', () => {
  const originalWindow = global.window;

  beforeEach(() => {
    global.window = {} as Window & typeof globalThis;
  });

  afterEach(() => {
    global.window = originalWindow;
  });

  it('does nothing when window is undefined', () => {
    // @ts-expect-error - тестируем SSR окружение
    global.window = undefined;

    registerVersionCommand();

    // Не должно быть ошибки
    expect(true).toBe(true);
  });

  it('registers AqStream object on window', () => {
    registerVersionCommand();

    expect(global.window.AqStream).toBeDefined();
    expect(typeof global.window.AqStream.versions).toBe('function');
    expect(typeof global.window.AqStream.version).toBe('function');
  });

  it('AqStream.version logs version info', () => {
    const mockConsoleLog = vi.fn();
    global.console = {
      ...console,
      log: mockConsoleLog,
    } as unknown as Console;

    registerVersionCommand();
    global.window.AqStream.version();

    expect(mockConsoleLog).toHaveBeenCalled();
  });
});
