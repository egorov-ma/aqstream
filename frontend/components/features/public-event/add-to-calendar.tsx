'use client';

import { Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { generateIcsContent, downloadFile } from '@/lib/utils/calendar';
import { getEventLocationForCalendar } from '@/lib/utils/event';
import type { Event } from '@/lib/api/types';

interface AddToCalendarButtonProps {
  event: Event;
  className?: string;
}

/**
 * Кнопка "Добавить в календарь"
 * Генерирует и скачивает ICS файл для события
 */
export function AddToCalendarButton({ event, className }: AddToCalendarButtonProps) {
  const handleClick = () => {
    // Генерируем ICS (используем утилиту для определения location)
    const icsContent = generateIcsContent({
      id: event.id,
      title: event.title,
      startsAt: event.startsAt,
      endsAt: event.endsAt,
      description: event.description,
      location: getEventLocationForCalendar(event),
    });

    // Скачиваем файл
    downloadFile(icsContent, `${event.slug}.ics`, 'text/calendar');
  };

  return (
    <Button
      variant="outline"
      className={className ?? 'w-full'}
      onClick={handleClick}
      data-testid="add-to-calendar-button"
    >
      <Calendar className="mr-2 h-4 w-4" />
      Добавить в календарь
    </Button>
  );
}
