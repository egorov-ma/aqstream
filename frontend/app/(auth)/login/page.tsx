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
import { LoginForm, TelegramLogin } from '@/components/features/auth';

export const metadata: Metadata = {
  title: 'Вход - AqStream',
  description: 'Войдите в свой аккаунт AqStream',
};

export default function LoginPage() {
  return (
    <Card>
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl">Вход</CardTitle>
        <CardDescription>Введите email и пароль для входа в аккаунт</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <LoginForm />

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-background px-2 text-muted-foreground">или войдите через</span>
          </div>
        </div>

        <div className="flex justify-center">
          <TelegramLogin />
        </div>
      </CardContent>
      <CardFooter>
        <p className="w-full text-center text-sm text-muted-foreground">
          Нет аккаунта?{' '}
          <Link href="/register" className="text-primary hover:underline" data-testid="register-link">
            Зарегистрироваться
          </Link>
        </p>
      </CardFooter>
    </Card>
  );
}
