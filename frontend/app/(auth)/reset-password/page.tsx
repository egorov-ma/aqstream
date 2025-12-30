import { Suspense } from 'react';
import Link from 'next/link';
import type { Metadata } from 'next';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ResetPasswordForm } from '@/components/features/auth';

export const metadata: Metadata = {
  title: 'Новый пароль - AqStream',
  description: 'Установите новый пароль для аккаунта AqStream',
};

export default function ResetPasswordPage() {
  return (
    <Card>
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl">Новый пароль</CardTitle>
        <CardDescription>Придумайте новый пароль для вашего аккаунта</CardDescription>
      </CardHeader>
      <CardContent>
        <Suspense fallback={<Skeleton className="h-[200px]" />}>
          <ResetPasswordForm />
        </Suspense>
      </CardContent>
      <CardFooter>
        <Link
          href="/login"
          className="w-full text-center text-sm text-muted-foreground hover:text-primary"
          data-testid="login-link"
        >
          Вернуться к входу
        </Link>
      </CardFooter>
    </Card>
  );
}
