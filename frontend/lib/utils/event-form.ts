import type { Event, EventStatus, CreateEventRequest, UpdateEventRequest } from '@/lib/api/types';
import type { EventFormData, TicketTypeFormData } from '@/lib/validations/events';

// Проверить, можно ли редактировать событие
export function isEventEditable(status: EventStatus): boolean {
  return status === 'DRAFT' || status === 'PUBLISHED';
}

// Проверить, можно ли публиковать событие
export function isEventPublishable(status: EventStatus): boolean {
  return status === 'DRAFT';
}

// Проверить, можно ли отменить событие
export function isEventCancellable(status: EventStatus): boolean {
  return status === 'DRAFT' || status === 'PUBLISHED';
}

// Проверить, можно ли завершить событие
export function isEventCompletable(status: EventStatus): boolean {
  return status === 'PUBLISHED';
}

// Маппинг Event в данные формы
export function mapEventToFormData(event: Event): Partial<EventFormData> {
  return {
    title: event.title,
    description: event.description ?? '',
    startsAt: event.startsAt,
    endsAt: event.endsAt ?? '',
    timezone: event.timezone,
    locationType: event.locationType,
    locationAddress: event.locationAddress ?? '',
    onlineUrl: event.onlineUrl ?? '',
    maxCapacity: event.maxCapacity ?? null,
    registrationOpensAt: event.registrationOpensAt ?? '',
    registrationClosesAt: event.registrationClosesAt ?? '',
    isPublic: event.isPublic,
    participantsVisibility: event.participantsVisibility,
    groupId: event.groupId ?? '',
    coverImageUrl: event.coverImageUrl ?? '',
    ticketTypes: [],
  };
}

// Маппинг формы в CreateEventRequest
export function mapFormDataToCreateRequest(data: EventFormData): CreateEventRequest {
  return {
    title: data.title,
    description: data.description || undefined,
    startsAt: data.startsAt,
    endsAt: data.endsAt || undefined,
    timezone: data.timezone,
    locationType: data.locationType,
    locationAddress: data.locationAddress || undefined,
    onlineUrl: data.onlineUrl || undefined,
    maxCapacity: data.maxCapacity ?? undefined,
    registrationOpensAt: data.registrationOpensAt || undefined,
    registrationClosesAt: data.registrationClosesAt || undefined,
    isPublic: data.isPublic,
    participantsVisibility: data.participantsVisibility,
    groupId: data.groupId || undefined,
    coverImageUrl: data.coverImageUrl || undefined,
  };
}

// Маппинг формы в UpdateEventRequest
export function mapFormDataToUpdateRequest(data: EventFormData): UpdateEventRequest {
  return {
    title: data.title,
    description: data.description || undefined,
    startsAt: data.startsAt,
    endsAt: data.endsAt || undefined,
    timezone: data.timezone,
    locationType: data.locationType,
    locationAddress: data.locationAddress || undefined,
    onlineUrl: data.onlineUrl || undefined,
    maxCapacity: data.maxCapacity ?? undefined,
    registrationOpensAt: data.registrationOpensAt || undefined,
    registrationClosesAt: data.registrationClosesAt || undefined,
    isPublic: data.isPublic,
    participantsVisibility: data.participantsVisibility,
    groupId: data.groupId || undefined,
    coverImageUrl: data.coverImageUrl || undefined,
  };
}

// Метка статуса события
export function getStatusLabel(status: EventStatus): string {
  const labels: Record<EventStatus, string> = {
    DRAFT: 'Черновик',
    PUBLISHED: 'Опубликовано',
    CANCELLED: 'Отменено',
    COMPLETED: 'Завершено',
  };
  return labels[status] ?? status;
}

// Цвет статуса (для badge)
export function getStatusColor(status: EventStatus): 'default' | 'secondary' | 'destructive' | 'outline' {
  const colors: Record<EventStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    DRAFT: 'secondary',
    PUBLISHED: 'default',
    CANCELLED: 'destructive',
    COMPLETED: 'outline',
  };
  return colors[status] ?? 'secondary';
}

// Метка типа локации
export function getLocationTypeLabel(locationType: EventFormData['locationType']): string {
  const labels: Record<EventFormData['locationType'], string> = {
    ONLINE: 'Онлайн',
    OFFLINE: 'Офлайн',
    HYBRID: 'Гибрид',
  };
  return labels[locationType] ?? locationType;
}

// Проверить, требуется ли адрес для типа локации
export function requiresAddress(locationType: EventFormData['locationType']): boolean {
  return locationType === 'OFFLINE' || locationType === 'HYBRID';
}

// Проверить, требуется ли URL для типа локации
export function requiresOnlineUrl(locationType: EventFormData['locationType']): boolean {
  return locationType === 'ONLINE' || locationType === 'HYBRID';
}

// Создать пустой тип билета
export function createEmptyTicketType(sortOrder: number = 0): TicketTypeFormData {
  return {
    name: '',
    description: '',
    quantity: null,
    salesStart: null,
    salesEnd: null,
    sortOrder,
  };
}

// Сортировка типов билетов по sortOrder
export function sortTicketTypes(ticketTypes: TicketTypeFormData[]): TicketTypeFormData[] {
  return [...ticketTypes].sort((a, b) => a.sortOrder - b.sortOrder);
}
