'use client';

import { useState } from 'react';
import { TelegramBotLogin } from './telegram-bot-login';
import { TelegramLogin } from './telegram-login';

interface TelegramAuthProps {
  className?: string;
}

/**
 * Компонент авторизации через Telegram.
 *
 * По умолчанию использует авторизацию через бота (TelegramBotLogin).
 * При ошибке автоматически переключается на Telegram Login Widget (fallback).
 */
export function TelegramAuth({ className }: TelegramAuthProps) {
  const [mode, setMode] = useState<'bot' | 'widget'>('bot');
  const [showFallbackHint, setShowFallbackHint] = useState(false);

  // Переключение на виджет при ошибке бота
  const handleBotError = () => {
    setMode('widget');
    setShowFallbackHint(true);
  };

  // Переключение обратно на бота
  const handleSwitchToBot = () => {
    setMode('bot');
    setShowFallbackHint(false);
  };

  return (
    <div className={className}>
      {showFallbackHint && mode === 'widget' && (
        <p className="mb-2 text-xs text-muted-foreground text-center">
          Используем альтернативный способ входа.{' '}
          <button
            type="button"
            onClick={handleSwitchToBot}
            className="text-primary hover:underline"
          >
            Попробовать через бота
          </button>
        </p>
      )}

      {mode === 'bot' ? (
        <TelegramBotLogin onError={handleBotError} />
      ) : (
        <TelegramLogin className="flex justify-center" />
      )}
    </div>
  );
}
