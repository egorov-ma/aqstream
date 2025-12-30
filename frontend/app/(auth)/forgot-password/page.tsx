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
import { ForgotPasswordForm } from '@/components/features/auth';

export const metadata: Metadata = {
  title: 'Восстановление пароля - AqStream',
  description: 'Восстановите доступ к аккаунту AqStream',
};

export default function ForgotPasswordPage() {
  return (
    <Card>
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl">Восстановление пароля</CardTitle>
        <CardDescription>
          Введите email, и мы отправим инструкции по восстановлению
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ForgotPasswordForm />
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
