import type { Metadata } from 'next';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

export const metadata: Metadata = {
  title: 'Регистрации - AqStream',
  description: 'Управление регистрациями на мероприятия',
};

export default function RegistrationsPage() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Регистрации</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Все регистрации</CardTitle>
          <CardDescription>Список регистраций на ваши мероприятия</CardDescription>
        </CardHeader>
        <CardContent>
          {/* Placeholder таблица — будет заполнена в Phase 2 */}
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Участник</TableHead>
                <TableHead>Событие</TableHead>
                <TableHead>Дата регистрации</TableHead>
                <TableHead>Статус</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground">
                  Нет регистраций. Данные появятся после реализации в Phase 2.
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
