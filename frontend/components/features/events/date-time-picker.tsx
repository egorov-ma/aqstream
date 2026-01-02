'use client';

import * as React from 'react';
import { format, parseISO, isValid } from 'date-fns';
import { ru } from 'date-fns/locale';
import { CalendarIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { Input } from '@/components/ui/input';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { cn } from '@/lib/utils';

interface DateTimePickerProps {
  value: string; // ISO 8601 string
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  'data-testid'?: string;
}

export function DateTimePicker({
  value,
  onChange,
  placeholder = 'Выберите дату и время',
  disabled,
  'data-testid': dataTestId,
}: DateTimePickerProps) {
  // Парсим значение
  const date = value ? parseISO(value) : undefined;
  const isValidDate = date && isValid(date);

  // Извлекаем время из ISO строки
  const timeValue = isValidDate ? format(date, 'HH:mm') : '';

  // Обработчик изменения даты
  const handleDateChange = (newDate: Date | undefined) => {
    if (!newDate) {
      onChange('');
      return;
    }

    // Если есть время, сохраняем его
    const [hours, minutes] = timeValue ? timeValue.split(':').map(Number) : [12, 0];
    newDate.setHours(hours, minutes, 0, 0);

    onChange(newDate.toISOString());
  };

  // Обработчик изменения времени
  const handleTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTime = e.target.value;
    if (!newTime) return;

    const [hours, minutes] = newTime.split(':').map(Number);

    if (isValidDate) {
      const newDate = new Date(date);
      newDate.setHours(hours, minutes, 0, 0);
      onChange(newDate.toISOString());
    } else {
      // Если дата не выбрана, создаём сегодняшнюю дату с указанным временем
      const today = new Date();
      today.setHours(hours, minutes, 0, 0);
      onChange(today.toISOString());
    }
  };

  return (
    <div className="flex gap-2" data-testid={dataTestId}>
      {/* Выбор даты */}
      <Popover>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            disabled={disabled}
            className={cn(
              'w-[200px] justify-start text-left font-normal',
              !isValidDate && 'text-muted-foreground'
            )}
            data-testid={dataTestId ? `${dataTestId}-button` : undefined}
          >
            <CalendarIcon className="mr-2 h-4 w-4" />
            {isValidDate ? format(date, 'd MMMM yyyy', { locale: ru }) : placeholder}
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            selected={isValidDate ? date : undefined}
            onSelect={handleDateChange}
            locale={ru}
            initialFocus
          />
        </PopoverContent>
      </Popover>

      {/* Выбор времени */}
      <Input
        type="time"
        value={timeValue}
        onChange={handleTimeChange}
        disabled={disabled}
        className="w-[120px]"
      />
    </div>
  );
}

// Хелпер для форматирования даты
export function formatEventDateTime(
  isoString: string | undefined,
  includeTime = true
): string {
  if (!isoString) return '';

  const date = parseISO(isoString);
  if (!isValid(date)) return '';

  if (includeTime) {
    return format(date, 'd MMMM yyyy, HH:mm', { locale: ru });
  }
  return format(date, 'd MMMM yyyy', { locale: ru });
}
