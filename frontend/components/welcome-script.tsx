'use client';

import { useEffect } from 'react';
import { printWelcomeMessage, registerVersionCommand } from '@/lib/utils/welcome';

/**
 * Компонент для инициализации welcome message в консоли.
 * Показывает приветствие и регистрирует команду AqStream.versions().
 */
export function WelcomeScript() {
  useEffect(() => {
    try {
      printWelcomeMessage();
      registerVersionCommand();
    } catch (error) {
      // Ошибки версионирования не должны ломать приложение
      console.error('[AqStream] Ошибка инициализации:', error);
    }
  }, []);

  return null;
}
