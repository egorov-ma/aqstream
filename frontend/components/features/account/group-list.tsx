'use client';

import { useState } from 'react';
import { Users, LogOut, UserPlus } from 'lucide-react';

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';

import { useMyGroups, useLeaveGroup, useJoinGroup } from '@/lib/hooks/use-groups';

/**
 * Компонент списка групп пользователя.
 */
export function GroupList() {
  const { data: groups, isLoading } = useMyGroups();
  const leaveGroup = useLeaveGroup();
  const joinGroup = useJoinGroup();
  const [inviteCode, setInviteCode] = useState('');

  const handleJoin = () => {
    if (inviteCode.trim()) {
      joinGroup.mutate(inviteCode.trim(), {
        onSuccess: () => setInviteCode(''),
      });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2].map((i) => (
          <Card key={i}>
            <CardHeader>
              <Skeleton className="h-5 w-48" />
              <Skeleton className="h-4 w-32" />
            </CardHeader>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Список групп */}
      {groups && groups.length > 0 ? (
        <div className="space-y-4">
          {groups.map((group) => (
            <Card key={group.id}>
              <CardHeader className="flex-row items-start justify-between space-y-0">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <Users className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <CardTitle className="text-lg">{group.name}</CardTitle>
                    <CardDescription>{group.organizationName}</CardDescription>
                  </div>
                </div>
                <Badge variant="secondary">{group.memberCount} участников</Badge>
              </CardHeader>

              {group.description && (
                <CardContent>
                  <p className="text-sm text-muted-foreground">{group.description}</p>
                </CardContent>
              )}

              <CardFooter>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => leaveGroup.mutate(group.id)}
                  disabled={leaveGroup.isPending}
                >
                  <LogOut className="mr-2 h-4 w-4" />
                  Выйти
                </Button>
              </CardFooter>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12 text-center">
            <Users className="mb-4 h-12 w-12 text-muted-foreground" />
            <CardTitle className="mb-2">Нет групп</CardTitle>
            <CardDescription>
              Вы пока не состоите ни в одной группе. Присоединитесь по инвайт-коду ниже.
            </CardDescription>
          </CardContent>
        </Card>
      )}

      {/* Присоединиться по коду */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <UserPlus className="h-5 w-5" />
            Присоединиться к группе
          </CardTitle>
          <CardDescription>Введите инвайт-код, полученный от организатора</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-2">
            <Input
              placeholder="Введите инвайт-код"
              value={inviteCode}
              onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
              maxLength={8}
              className="uppercase"
            />
            <Button onClick={handleJoin} disabled={!inviteCode.trim() || joinGroup.isPending}>
              Присоединиться
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
