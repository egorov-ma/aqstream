'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function AuthError({ error, reset }: ErrorProps) {
  useEffect(() => {
    console.error('Ошибка авторизации:', error);
  }, [error]);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Ошибка</CardTitle>
        <CardDescription>Произошла ошибка при загрузке страницы</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-muted-foreground">
          Попробуйте обновить страницу или вернитесь позже.
        </p>
        <Button onClick={reset} className="w-full">
          Попробовать снова
        </Button>
      </CardContent>
    </Card>
  );
}
