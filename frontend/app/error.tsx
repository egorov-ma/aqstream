'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function Error({ error, reset }: ErrorProps) {
  useEffect(() => {
    // Логируем ошибку в систему мониторинга
    console.error('Произошла ошибка:', error);
  }, [error]);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <div className="text-center">
        <h1 className="mb-4 text-2xl font-bold">Что-то пошло не так</h1>
        <p className="mb-8 text-muted-foreground">
          Произошла непредвиденная ошибка. Попробуйте обновить страницу.
        </p>
        <Button size="lg" onClick={reset}>
          Попробовать снова
        </Button>
      </div>
    </main>
  );
}
