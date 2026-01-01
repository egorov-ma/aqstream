'use client';

import { Building, Check, ChevronDown, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Skeleton } from '@/components/ui/skeleton';
import { useOrganizations, useSwitchOrganization } from '@/lib/hooks/use-organizations';
import { useOrganizationStore } from '@/lib/store/organization-store';
import { useEffect } from 'react';
import { cn } from '@/lib/utils';

export function OrganizationSwitcher() {
  const { data: organizations, isLoading } = useOrganizations();
  const switchOrganization = useSwitchOrganization();
  const { currentOrganization } = useOrganizationStore();

  // Автоматически переключаем на первую организацию при входе
  // Это обновляет JWT токен с правильным tenantId
  useEffect(() => {
    if (
      organizations &&
      organizations.length > 0 &&
      !currentOrganization &&
      !switchOrganization.isPending
    ) {
      switchOrganization.mutate(organizations[0].id);
    }
  }, [organizations, currentOrganization, switchOrganization]);

  // Не показываем если только одна организация или загрузка
  if (isLoading) {
    return <Skeleton className="h-9 w-[180px]" />;
  }

  if (!organizations || organizations.length <= 1) {
    // Показываем только название текущей организации без dropdown
    if (currentOrganization) {
      return (
        <div className="flex items-center gap-2 text-sm font-medium">
          <Building className="h-4 w-4 text-muted-foreground" />
          <span className="truncate max-w-[150px]">{currentOrganization.name}</span>
        </div>
      );
    }
    return null;
  }

  const handleSwitch = (organizationId: string) => {
    if (organizationId !== currentOrganization?.id) {
      switchOrganization.mutate(organizationId);
    }
  };

  const isPending = switchOrganization.isPending;
  const isError = switchOrganization.isError;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            'gap-2 max-w-[200px]',
            isError && 'border-destructive',
            isPending && 'opacity-70'
          )}
          disabled={isPending}
        >
          {isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Building className="h-4 w-4" />
          )}
          <span className="truncate">{currentOrganization?.name || 'Выбрать'}</span>
          <ChevronDown className="h-4 w-4 opacity-50" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-[200px]" align="start">
        <DropdownMenuLabel>Организации</DropdownMenuLabel>
        <DropdownMenuSeparator />
        {organizations.map((org) => (
          <DropdownMenuItem
            key={org.id}
            onClick={() => handleSwitch(org.id)}
            className="cursor-pointer"
            disabled={isPending}
          >
            <span className="truncate flex-1">{org.name}</span>
            {org.id === currentOrganization?.id && <Check className="h-4 w-4 ml-2" />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
