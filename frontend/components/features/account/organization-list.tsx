'use client';

import Link from 'next/link';
import { Building2, ChevronRight, LogIn } from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

import { useOrganizations, useSwitchOrganization } from '@/lib/hooks/use-organizations';
import { useOrganizationStore } from '@/lib/store/organization-store';

/**
 * Компонент списка организаций пользователя.
 */
export function OrganizationList() {
  const { data: organizations, isLoading } = useOrganizations();
  const switchOrg = useSwitchOrganization();
  const { currentOrganization } = useOrganizationStore();

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

  if (!organizations || organizations.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12 text-center">
          <Building2 className="mb-4 h-12 w-12 text-muted-foreground" />
          <CardTitle className="mb-2">Нет организаций</CardTitle>
          <CardDescription className="mb-4">
            Вы пока не состоите ни в одной организации
          </CardDescription>
          <Button asChild>
            <Link href="/dashboard/organizations/new">Создать организацию</Link>
          </Button>
        </CardContent>
      </Card>
    );
  }

  const handleSwitch = (orgId: string) => {
    if (currentOrganization?.id !== orgId) {
      switchOrg.mutate(orgId);
    }
  };

  return (
    <div className="space-y-4">
      {organizations.map((org) => {
        const isCurrent = currentOrganization?.id === org.id;

        return (
          <Card key={org.id} className={isCurrent ? 'border-primary' : ''}>
            <CardHeader className="flex-row items-center justify-between space-y-0">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                  <Building2 className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <CardTitle className="flex items-center gap-2 text-lg">
                    {org.name}
                    {isCurrent && (
                      <Badge variant="secondary" className="font-normal">
                        Текущая
                      </Badge>
                    )}
                  </CardTitle>
                  <CardDescription>/{org.slug}</CardDescription>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {!isCurrent && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSwitch(org.id)}
                    disabled={switchOrg.isPending}
                  >
                    <LogIn className="mr-2 h-4 w-4" />
                    Переключиться
                  </Button>
                )}
                <Button variant="ghost" size="icon" asChild>
                  <Link href={`/dashboard/organizations/${org.id}`}>
                    <ChevronRight className="h-4 w-4" />
                  </Link>
                </Button>
              </div>
            </CardHeader>
          </Card>
        );
      })}

      <Button variant="outline" className="w-full" asChild>
        <Link href="/dashboard/organizations/new">
          <Building2 className="mr-2 h-4 w-4" />
          Создать организацию
        </Link>
      </Button>
    </div>
  );
}
