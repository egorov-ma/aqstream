'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { useTelegramBotAuthInit } from '@/lib/hooks/use-auth';
import { useAuthStore } from '@/lib/store/auth-store';
import { useOrganizationStore } from '@/lib/store/organization-store';
import type { TelegramAuthInitResponse, User } from '@/lib/api/types';
import { toast } from 'sonner';

// Иконка Telegram
function TelegramIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="currentColor"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z" />
    </svg>
  );
}

interface TelegramBotLoginProps {
  /** Callback при ошибке — для fallback на виджет */
  onError?: () => void;
}

interface WebSocketMessage {
  type: 'confirmed' | 'error';
  accessToken?: string;
  user?: User;
  error?: string;
}

export function TelegramBotLogin({ onError }: TelegramBotLoginProps) {
  const [authData, setAuthData] = useState<TelegramAuthInitResponse | null>(null);
  const [status, setStatus] = useState<'idle' | 'waiting' | 'success' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  // Ref для доступа к актуальному статусу в setTimeout (избегаем stale closure)
  const statusRef = useRef(status);
  statusRef.current = status;

  const router = useRouter();
  const queryClient = useQueryClient();
  const { login } = useAuthStore();
  const { clear: clearOrganization } = useOrganizationStore();
  const initMutation = useTelegramBotAuthInit();

  // Очистка при unmount
  useEffect(() => {
    return () => {
      wsRef.current?.close();
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  // Подключение к WebSocket
  const connectWebSocket = useCallback(
    (token: string) => {
      // Определяем URL для WebSocket
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || '';

      // Если API URL пустой, используем текущий хост
      let wsUrl: string;
      if (apiUrl) {
        const wsProtocol = apiUrl.startsWith('https') ? 'wss' : 'ws';
        const wsHost = apiUrl.replace(/^https?:\/\//, '');
        wsUrl = `${wsProtocol}://${wsHost}/ws/telegram-auth/${token}`;
      } else {
        // Fallback на текущий хост (для SSR и development)
        const wsProtocol = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws';
        const wsHost = typeof window !== 'undefined' ? window.location.host : 'localhost:8080';
        wsUrl = `${wsProtocol}://${wsHost}/ws/telegram-auth/${token}`;
      }

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('WebSocket подключён');
      };

      ws.onmessage = (event) => {
        try {
          const data: WebSocketMessage = JSON.parse(event.data);

          if (data.type === 'confirmed' && data.accessToken && data.user) {
            // Очищаем таймаут при успешной авторизации
            if (timeoutRef.current) {
              clearTimeout(timeoutRef.current);
              timeoutRef.current = null;
            }

            setStatus('success');

            // Очищаем organization store
            clearOrganization();

            // Авторизуем пользователя
            login(data.user, data.accessToken);
            queryClient.invalidateQueries({ queryKey: ['user'] });

            toast.success('Вы успешно вошли через Telegram');
            router.push('/dashboard');
          } else if (data.type === 'error') {
            setStatus('error');
            setError(data.error || 'Ошибка авторизации');
            onError?.();
          }
        } catch (e) {
          console.error('Ошибка парсинга WebSocket сообщения:', e);
        }
      };

      ws.onerror = () => {
        console.error('WebSocket ошибка');
        setStatus('error');
        setError('Не удалось подключиться к серверу');
        onError?.();
      };

      ws.onclose = () => {
        console.log('WebSocket закрыт');
      };

      // Таймаут 5 минут — после этого токен истекает
      timeoutRef.current = setTimeout(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.close();
        }
        // Используем statusRef для актуального значения (избегаем stale closure)
        if (statusRef.current === 'waiting') {
          setStatus('error');
          setError('Время ожидания истекло. Попробуйте снова.');
        }
      }, 5 * 60 * 1000);
    },
    [clearOrganization, login, onError, queryClient, router]
  );

  // Инициализация авторизации
  const handleStart = async () => {
    setError(null);
    setStatus('idle');

    try {
      const data = await initMutation.mutateAsync();
      setAuthData(data);
      setStatus('waiting');
      connectWebSocket(data.token);
    } catch (e) {
      console.error('Ошибка инициализации Telegram auth:', e);
      setStatus('error');
      setError('Не удалось начать авторизацию');
      onError?.();
    }
  };

  // Отмена ожидания
  const handleCancel = () => {
    wsRef.current?.close();
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    setStatus('idle');
    setAuthData(null);
    setError(null);
  };

  return (
    <div className="w-full" data-testid="telegram-bot-login">
      {status === 'idle' && (
        <Button
          type="button"
          variant="outline"
          className="w-full"
          onClick={handleStart}
          disabled={initMutation.isPending}
          data-testid="telegram-bot-start-button"
        >
          <TelegramIcon className="mr-2 h-5 w-5" />
          {initMutation.isPending ? 'Загрузка...' : 'Войти через Telegram'}
        </Button>
      )}

      {status === 'waiting' && authData && (
        <div className="space-y-4 text-center">
          <p className="text-sm text-muted-foreground">
            Откройте бота в Telegram и подтвердите вход
          </p>

          <Button asChild size="lg" className="w-full" data-testid="telegram-open-button">
            <a href={authData.deeplink} target="_blank" rel="noopener noreferrer">
              <TelegramIcon className="mr-2 h-5 w-5" />
              Открыть в Telegram
            </a>
          </Button>

          <p className="text-sm text-muted-foreground animate-pulse">Ожидание подтверждения...</p>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleCancel}
            data-testid="telegram-cancel-button"
          >
            Отмена
          </Button>
        </div>
      )}

      {status === 'error' && error && (
        <div className="space-y-3 text-center">
          <p className="text-sm text-destructive" data-testid="telegram-bot-error">
            {error}
          </p>
          <Button
            type="button"
            variant="outline"
            onClick={handleStart}
            disabled={initMutation.isPending}
            data-testid="telegram-retry-button"
          >
            Попробовать снова
          </Button>
        </div>
      )}

      {status === 'success' && (
        <p className="text-sm text-center text-muted-foreground">Вход выполнен, перенаправление...</p>
      )}
    </div>
  );
}
