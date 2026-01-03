'use client';

import { usePermissions } from '@/lib/hooks/use-permissions';
import { useAuthStore } from '@/lib/store/auth-store';
import { ParticipantDashboard } from './components/participant-dashboard';
import { OrganizerDashboard } from './components/organizer-dashboard';
import { AdminDashboard } from './components/admin-dashboard';

/**
 * Главный компонент дашборда.
 * Выбирает соответствующий дашборд на основе роли пользователя.
 *
 * Приоритет:
 * 1. Организатор (isOrganizer) -> OrganizerDashboard
 * 2. Админ без выбранной организации (isAdmin && !isOrganizer) -> AdminDashboard
 * 3. Обычный пользователь -> ParticipantDashboard
 */
export function DashboardContent() {
  const { isOrganizer } = usePermissions();
  const user = useAuthStore((state) => state.user);
  const isAdmin = user?.isAdmin ?? false;

  // Если пользователь — организатор (выбрана организация), показываем OrganizerDashboard
  // Это работает и для админов, которые выбрали организацию
  if (isOrganizer) {
    return <OrganizerDashboard />;
  }

  // Если пользователь — админ, но без выбранной организации
  if (isAdmin) {
    return <AdminDashboard />;
  }

  // Обычный пользователь (участник)
  return <ParticipantDashboard />;
}
