import Link from 'next/link';
import type { Metadata } from 'next';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export const metadata: Metadata = {
  title: 'Подтвердите email - AqStream',
  description: 'Проверьте почту для подтверждения регистрации',
};

export default function VerifyEmailSentPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-2xl text-center">Проверьте почту</CardTitle>
      </CardHeader>
      <CardContent className="text-center space-y-4">
        <p className="text-muted-foreground">
          Мы отправили письмо с ссылкой для подтверждения на указанный email.
        </p>
        <p className="text-muted-foreground text-sm">
          Если письмо не пришло, проверьте папку «Спам».
        </p>
      </CardContent>
      <CardFooter className="flex flex-col gap-4">
        <Button variant="outline" className="w-full" asChild>
          <Link href="/login" data-testid="login-link">
            Перейти к входу
          </Link>
        </Button>
      </CardFooter>
    </Card>
  );
}
