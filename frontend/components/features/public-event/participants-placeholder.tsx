'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Users } from 'lucide-react';

/**
 * Placeholder для списка участников
 * Показывается когда participantsVisibility === 'OPEN'
 */
export function ParticipantsPlaceholder() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Users className="h-5 w-5" />
          Участники
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-muted-foreground">
          Список участников появится после регистрации
        </p>
      </CardContent>
    </Card>
  );
}
