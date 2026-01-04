'use client';

import { useState } from 'react';
import { Calendar, ExternalLink, Download, ChevronDown, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  generateIcsContent,
  downloadFile,
  generateGoogleCalendarUrl,
} from '@/lib/utils/calendar';
import { getEventLocationForCalendar } from '@/lib/utils/event';
import type { Event } from '@/lib/api/types';

interface AddToCalendarButtonProps {
  event: Event;
  confirmationCode?: string;
  className?: string;
}

/**
 * Кнопка "Добавить в календарь" с dropdown выбором календаря.
 * Поддерживает Google Calendar (redirect), Apple Calendar и Outlook (.ics файл).
 */
export function AddToCalendarButton({
  event,
  confirmationCode,
  className,
}: AddToCalendarButtonProps) {
  const [isLoading, setIsLoading] = useState(false);

  // Формируем описание с confirmation code
  const getDescription = (): string => {
    let description = event.description ?? '';
    if (confirmationCode) {
      description = `Код билета: ${confirmationCode}\n\n${description}`;
    }
    return description;
  };

  // Получаем location и description
  const location = getEventLocationForCalendar(event);
  const description = getDescription();

  // Обработчик Google Calendar
  const handleGoogleCalendar = () => {
    setIsLoading(true);
    try {
      const url = generateGoogleCalendarUrl({
        title: event.title,
        startsAt: event.startsAt,
        endsAt: event.endsAt,
        description,
        location,
      });
      window.open(url, '_blank', 'noopener,noreferrer');
      toast.success('Открыта страница Google Calendar');
    } catch (error) {
      toast.error('Ошибка при открытии Google Calendar');
      console.error('Google Calendar error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // Обработчик скачивания ICS (Apple/Outlook)
  const handleDownloadIcs = (calendarType: 'apple' | 'outlook') => {
    setIsLoading(true);
    try {
      const icsContent = generateIcsContent({
        id: event.id,
        title: event.title,
        startsAt: event.startsAt,
        endsAt: event.endsAt,
        description,
        location,
      });
      downloadFile(icsContent, `${event.slug}.ics`, 'text/calendar');
      toast.success(
        calendarType === 'apple'
          ? 'Файл календаря скачан для Apple Calendar'
          : 'Файл календаря скачан для Outlook'
      );
    } catch (error) {
      toast.error('Ошибка при создании файла календаря');
      console.error('ICS generation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          className={className ?? 'w-full'}
          disabled={isLoading}
          data-testid="add-to-calendar-button"
        >
          {isLoading ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <Calendar className="mr-2 h-4 w-4" />
          )}
          Добавить в календарь
          <ChevronDown className="ml-2 h-4 w-4 opacity-50" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuItem
          onClick={handleGoogleCalendar}
          className="cursor-pointer"
          data-testid="add-to-calendar-google"
        >
          <ExternalLink className="mr-2 h-4 w-4" />
          Google Calendar
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() => handleDownloadIcs('apple')}
          className="cursor-pointer"
          data-testid="add-to-calendar-apple"
        >
          <Download className="mr-2 h-4 w-4" />
          Apple Calendar
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() => handleDownloadIcs('outlook')}
          className="cursor-pointer"
          data-testid="add-to-calendar-outlook"
        >
          <Download className="mr-2 h-4 w-4" />
          Outlook
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
