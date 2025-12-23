'use client';

import { useEffect } from 'react';
import { printWelcomeMessage, registerVersionCommand } from '@/lib/utils/welcome';

/**
 * Компонент для инициализации welcome message в консоли.
 * Показывает приветствие и регистрирует команду AqStream.versions().
 */
export function WelcomeScript() {
  useEffect(() => {
    // Отладка: проверяем что useEffect выполняется
    console.log('[AqStream] WelcomeScript mounted');

    try {
      // Показываем welcome message при каждой загрузке
      printWelcomeMessage();

      // Регистрируем глобальную команду
      registerVersionCommand();

      console.log('[AqStream] Welcome script initialized');
    } catch (error) {
      // Ошибки версионирования не должны ломать приложение
      console.error('[AqStream] Ошибка инициализации welcome script:', error);
    }
  }, []);

  return null;
}
