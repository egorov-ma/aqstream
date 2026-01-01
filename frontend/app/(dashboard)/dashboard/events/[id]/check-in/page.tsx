'use client';

import { useState, useCallback } from 'react';
import { useParams } from 'next/navigation';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { QrCode, Keyboard } from 'lucide-react';

import {
  QRScanner,
  ManualSearch,
  AttendeeCard,
  OfflineIndicator,
} from '@/components/features/check-in';
import { useCheckInInfo, useConfirmCheckIn, useSyncCheckIns } from '@/lib/hooks/use-check-in';

export default function CheckInPage() {
  const params = useParams();
  const eventId = params.id as string;

  const [scannedCode, setScannedCode] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'scanner' | 'manual'>('scanner');

  const { data: info, isLoading, error, refetch } = useCheckInInfo(scannedCode);
  const confirmCheckIn = useConfirmCheckIn();
  const syncCheckIns = useSyncCheckIns();

  const handleScan = useCallback((code: string) => {
    // QR-код может содержать URL, извлекаем confirmation code
    let confirmationCode = code;
    try {
      const url = new URL(code);
      const parts = url.pathname.split('/');
      confirmationCode = parts[parts.length - 1];
    } catch {
      // Не URL, используем как есть
    }
    setScannedCode(confirmationCode.toUpperCase());
  }, []);

  const handleSearch = useCallback((code: string) => {
    setScannedCode(code);
  }, []);

  const handleCheckIn = useCallback(
    (registrationId: string) => {
      if (scannedCode) {
        confirmCheckIn.mutate(scannedCode, {
          onSuccess: () => {
            refetch();
          },
        });
      }
    },
    [scannedCode, confirmCheckIn, refetch]
  );

  const handleReset = useCallback(() => {
    setScannedCode(null);
  }, []);

  const isOffline = typeof navigator !== 'undefined' && !navigator.onLine;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Check-in</h1>
          <p className="text-muted-foreground">
            Сканируйте QR-код или введите код вручную
          </p>
        </div>
        <OfflineIndicator
          onSync={() => syncCheckIns.mutate()}
          isSyncing={syncCheckIns.isPending}
        />
      </div>

      {/* Результат сканирования */}
      {scannedCode && (
        <div className="space-y-4">
          {isLoading && (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
            </div>
          )}

          {error && (
            <div className="rounded-lg border border-destructive bg-destructive/10 p-4 text-center">
              <p className="text-destructive">
                Регистрация не найдена или код недействителен
              </p>
              <button
                onClick={handleReset}
                className="mt-2 text-sm text-primary underline"
              >
                Попробовать снова
              </button>
            </div>
          )}

          {info && (
            <>
              <AttendeeCard
                registration={{
                  id: info.registrationId,
                  confirmationCode: info.confirmationCode,
                  firstName: info.firstName,
                  lastName: info.lastName,
                  email: info.email,
                  ticketTypeName: info.ticketTypeName,
                  status: info.status,
                  checkedInAt: info.checkedInAt,
                }}
                onCheckIn={handleCheckIn}
                isCheckingIn={confirmCheckIn.isPending}
                isOffline={isOffline}
              />

              <button
                onClick={handleReset}
                className="w-full text-center text-sm text-muted-foreground underline"
              >
                Сканировать другой код
              </button>
            </>
          )}
        </div>
      )}

      {/* Сканер / Поиск */}
      {!scannedCode && (
        <Tabs
          value={activeTab}
          onValueChange={(v) => setActiveTab(v as 'scanner' | 'manual')}
        >
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="scanner">
              <QrCode className="mr-2 h-4 w-4" />
              Сканер
            </TabsTrigger>
            <TabsTrigger value="manual">
              <Keyboard className="mr-2 h-4 w-4" />
              Вручную
            </TabsTrigger>
          </TabsList>

          <TabsContent value="scanner" className="mt-4">
            <QRScanner onScan={handleScan} isScanning={activeTab === 'scanner'} />
          </TabsContent>

          <TabsContent value="manual" className="mt-4">
            <ManualSearch onSearch={handleSearch} isLoading={isLoading} />
          </TabsContent>
        </Tabs>
      )}
    </div>
  );
}
