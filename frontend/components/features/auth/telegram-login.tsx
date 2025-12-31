'use client';

import { useEffect, useRef, useCallback, useState } from 'react';
import { AxiosError } from 'axios';
import { useTelegramAuth } from '@/lib/hooks/use-auth';
import type { TelegramAuthRequest, ApiError } from '@/lib/api/types';

interface TelegramLoginProps {
  className?: string;
}

// Расширяем Window для Telegram callback
declare global {
  interface Window {
    onTelegramAuth?: (user: TelegramAuthRequest) => void;
  }
}

export function TelegramLogin({ className }: TelegramLoginProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const telegramAuthMutation = useTelegramAuth();
  const [error, setError] = useState<string | null>(null);
  // Убираем @ из начала имени бота (TELEGRAM_BOT_USERNAME может содержать @)
  // Используем regex /^@/ чтобы удалить только первый символ, если это @
  const botName = process.env.NEXT_PUBLIC_TELEGRAM_BOT_NAME?.replace(/^@/, '');

  const handleAuth = useCallback(
    async (user: TelegramAuthRequest) => {
      setError(null);
      try {
        await telegramAuthMutation.mutateAsync(user);
      } catch (err) {
        if (err instanceof AxiosError && err.response?.data) {
          const apiError = err.response.data as ApiError;
          setError(apiError.message || 'Ошибка авторизации через Telegram');
        } else {
          setError('Произошла ошибка. Попробуйте позже');
        }
      }
    },
    [telegramAuthMutation]
  );

  useEffect(() => {
    if (!botName) {
      console.warn('NEXT_PUBLIC_TELEGRAM_BOT_NAME не установлен');
      return;
    }

    // Сохраняем ссылку на container для cleanup
    const container = containerRef.current;

    // Устанавливаем глобальный callback
    window.onTelegramAuth = handleAuth;

    // Создаём и добавляем скрипт Telegram виджета
    const script = document.createElement('script');
    script.src = 'https://telegram.org/js/telegram-widget.js?22';
    script.setAttribute('data-telegram-login', botName);
    script.setAttribute('data-size', 'large');
    script.setAttribute('data-radius', '8');
    script.setAttribute('data-request-access', 'write');
    script.setAttribute('data-onauth', 'onTelegramAuth(user)');
    script.async = true;

    container?.appendChild(script);

    return () => {
      // Очистка
      window.onTelegramAuth = undefined;
      if (container) {
        container.innerHTML = '';
      }
    };
  }, [botName, handleAuth]);

  if (!botName) {
    return null;
  }

  return (
    <div className={className}>
      <div ref={containerRef} data-testid="telegram-login-widget">
        {telegramAuthMutation.isPending && (
          <div className="text-sm text-muted-foreground">Авторизация...</div>
        )}
      </div>
      {error && (
        <div
          className="mt-2 text-sm text-destructive"
          data-testid="telegram-error-message"
        >
          {error}
        </div>
      )}
    </div>
  );
}
