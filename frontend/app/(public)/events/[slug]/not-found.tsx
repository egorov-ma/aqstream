import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Calendar } from 'lucide-react';

export default function EventNotFound() {
  return (
    <div className="container py-12">
      <div className="mx-auto max-w-md text-center">
        <Calendar className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
        <h2 className="mb-4 text-2xl font-semibold">Событие не найдено</h2>
        <p className="mb-8 text-muted-foreground">
          Запрошенное мероприятие не существует или было удалено.
        </p>
        <Button asChild>
          <Link href="/">На главную</Link>
        </Button>
      </div>
    </div>
  );
}
