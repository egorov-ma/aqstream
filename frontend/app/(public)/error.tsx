'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function PublicError({ error, reset }: ErrorProps) {
  useEffect(() => {
    console.error('Ошибка на публичной странице:', error);
  }, [error]);

  return (
    <div className="container py-12">
      <div className="mx-auto max-w-md text-center">
        <h2 className="mb-4 text-xl font-semibold">Что-то пошло не так</h2>
        <p className="mb-8 text-muted-foreground">
          Не удалось загрузить страницу. Попробуйте обновить или вернитесь на главную.
        </p>
        <div className="flex justify-center gap-4">
          <Button onClick={reset}>Попробовать снова</Button>
          <Button variant="outline" asChild>
            <Link href="/">На главную</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
