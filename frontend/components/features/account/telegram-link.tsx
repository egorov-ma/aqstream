'use client';

import { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { MessageSquare, ExternalLink, RefreshCw, CheckCircle } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';

import { useAuthStore } from '@/lib/store/auth-store';
import { useGenerateTelegramLinkToken } from '@/lib/hooks/use-profile';

/**
 * Компонент привязки/отвязки Telegram аккаунта.
 */
export function TelegramLink() {
  const { user } = useAuthStore();
  const generateToken = useGenerateTelegramLinkToken();
  const [linkData, setLinkData] = useState<{ token: string; botLink: string } | null>(null);

  // Если Telegram уже привязан
  if (user?.telegramId) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MessageSquare className="h-5 w-5" />
            Telegram
          </CardTitle>
          <CardDescription>Ваш аккаунт привязан к Telegram</CardDescription>
        </CardHeader>
        <CardContent>
          <Alert>
            <CheckCircle className="h-4 w-4" />
            <AlertDescription>
              Telegram привязан. Вы будете получать билеты и уведомления в Telegram.
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  // Обработчик генерации токена
  const handleGenerateToken = async () => {
    const result = await generateToken.mutateAsync();
    setLinkData(result);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <MessageSquare className="h-5 w-5" />
          Привязка Telegram
        </CardTitle>
        <CardDescription>
          Привяжите Telegram для получения билетов и уведомлений
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {!linkData ? (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              После привязки Telegram вы сможете получать билеты с QR-кодами и уведомления
              о событиях прямо в мессенджер.
            </p>
            <Button
              onClick={handleGenerateToken}
              disabled={generateToken.isPending}
              className="w-full"
            >
              {generateToken.isPending ? (
                <>
                  <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                  Генерация...
                </>
              ) : (
                <>
                  <MessageSquare className="mr-2 h-4 w-4" />
                  Привязать Telegram
                </>
              )}
            </Button>
          </div>
        ) : (
          <div className="space-y-6">
            {/* QR-код */}
            <div className="flex flex-col items-center gap-4">
              <p className="text-sm text-muted-foreground text-center">
                Отсканируйте QR-код или нажмите на кнопку ниже
              </p>
              <div className="p-4 bg-white rounded-lg">
                <QRCodeSVG value={linkData.botLink} size={200} level="H" />
              </div>
            </div>

            {/* Ссылка на бота */}
            <Button variant="outline" className="w-full" asChild>
              <a href={linkData.botLink} target="_blank" rel="noopener noreferrer">
                <ExternalLink className="mr-2 h-4 w-4" />
                Открыть в Telegram
              </a>
            </Button>

            {/* Инструкция */}
            <Alert>
              <AlertDescription className="text-sm">
                После перехода в Telegram нажмите кнопку <strong>Start</strong> в боте.
                Привязка произойдёт автоматически.
              </AlertDescription>
            </Alert>

            {/* Кнопка обновления токена */}
            <Button
              variant="ghost"
              size="sm"
              onClick={handleGenerateToken}
              disabled={generateToken.isPending}
              className="w-full"
            >
              <RefreshCw className={`mr-2 h-4 w-4 ${generateToken.isPending ? 'animate-spin' : ''}`} />
              Сгенерировать новую ссылку
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
