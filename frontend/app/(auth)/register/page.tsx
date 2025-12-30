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
import { RegisterForm, TelegramLogin } from '@/components/features/auth';

export const metadata: Metadata = {
  title: 'Регистрация - AqStream',
  description: 'Создайте аккаунт AqStream',
};

export default function RegisterPage() {
  return (
    <Card>
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl">Регистрация</CardTitle>
        <CardDescription>Создайте новый аккаунт для доступа к платформе</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <RegisterForm />

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-background px-2 text-muted-foreground">
              или зарегистрируйтесь через
            </span>
          </div>
        </div>

        <div className="flex justify-center">
          <TelegramLogin />
        </div>
      </CardContent>
      <CardFooter>
        <p className="w-full text-center text-sm text-muted-foreground">
          Уже есть аккаунт?{' '}
          <Link href="/login" className="text-primary hover:underline" data-testid="login-link">
            Войти
          </Link>
        </p>
      </CardFooter>
    </Card>
  );
}
