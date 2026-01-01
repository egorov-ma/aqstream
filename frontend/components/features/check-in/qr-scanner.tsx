'use client';

import { useEffect, useRef, useState } from 'react';
import { Camera, CameraOff, RotateCw } from 'lucide-react';
import { Html5Qrcode } from 'html5-qrcode';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

interface QRScannerProps {
  onScan: (code: string) => void;
  isScanning?: boolean;
}

/**
 * Компонент для сканирования QR-кодов с камеры.
 */
export function QRScanner({ onScan, isScanning = true }: QRScannerProps) {
  const [isStarted, setIsStarted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [facingMode, setFacingMode] = useState<'environment' | 'user'>('environment');
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isScanning || !containerRef.current) return;

    const scanner = new Html5Qrcode('qr-reader');
    scannerRef.current = scanner;

    const startScanner = async () => {
      try {
        await scanner.start(
          { facingMode },
          {
            fps: 10,
            qrbox: { width: 250, height: 250 },
            aspectRatio: 1,
          },
          (decodedText) => {
            onScan(decodedText);
          },
          () => {
            // Игнорируем ошибки декодирования
          }
        );
        setIsStarted(true);
        setError(null);
      } catch (err) {
        console.error('Ошибка запуска сканера:', err);
        setError('Не удалось получить доступ к камере');
        setIsStarted(false);
      }
    };

    startScanner();

    return () => {
      if (scanner.isScanning) {
        scanner.stop().catch(console.error);
      }
    };
  }, [isScanning, facingMode, onScan]);

  const handleSwitchCamera = async () => {
    if (scannerRef.current?.isScanning) {
      await scannerRef.current.stop();
    }
    setFacingMode((prev) => (prev === 'environment' ? 'user' : 'environment'));
  };

  if (error) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12 text-center">
          <CameraOff className="mb-4 h-12 w-12 text-muted-foreground" />
          <p className="mb-4 text-muted-foreground">{error}</p>
          <Button onClick={() => setError(null)} variant="outline">
            <RotateCw className="mr-2 h-4 w-4" />
            Попробовать снова
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="relative">
      <div
        id="qr-reader"
        ref={containerRef}
        className="mx-auto w-full max-w-sm overflow-hidden rounded-lg"
      />

      {isStarted && (
        <div className="mt-4 flex justify-center">
          <Button variant="outline" size="sm" onClick={handleSwitchCamera}>
            <Camera className="mr-2 h-4 w-4" />
            Переключить камеру
          </Button>
        </div>
      )}

      {!isStarted && !error && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Camera className="mb-4 h-12 w-12 animate-pulse text-muted-foreground" />
            <p className="text-muted-foreground">Инициализация камеры...</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
