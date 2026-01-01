import { useOrganizationStore } from '@/lib/store/organization-store';

/**
 * Hook для проверки прав доступа пользователя.
 * Определяет, является ли пользователь организатором, владельцем или модератором.
 */
export function usePermissions() {
  const { currentOrganization, currentRole } = useOrganizationStore();

  // Пользователь является организатором, если выбрана организация и известна роль
  const isOrganizer = currentOrganization !== null && currentRole !== null;

  // Проверка конкретных ролей
  const isOwner = currentRole === 'OWNER';
  const isModerator = currentRole === 'MODERATOR';

  return {
    // Базовые проверки
    isOrganizer,
    isOwner,
    isModerator,

    // Готовые проверки для UI (согласно docs/business/role-model.md)
    canDeleteOrganization: isOwner,
    canTransferOwnership: isOwner,
    canAssignRoles: isOwner,
    canDeleteGroups: isOwner,
    canEditOrganization: isOrganizer,
    canManageMembers: isOrganizer,
    canCreateEvent: isOrganizer,
    canEditEvent: isOrganizer,
    canCancelEvent: isOrganizer,
    canCheckIn: isOrganizer,
    canViewRegistrations: isOrganizer,
    canViewAnalytics: isOrganizer,
    canExportData: isOrganizer,
    canCreateGroups: isOrganizer,
    canInviteToGroups: isOrganizer,
  };
}
