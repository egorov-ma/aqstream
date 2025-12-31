'use client';

import * as React from 'react';
import { Repeat, Calendar, X } from 'lucide-react';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Calendar as CalendarComponent } from '@/components/ui/calendar';
import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';
import { cn } from '@/lib/utils';
import type { RecurrenceRule, RecurrenceFrequency } from '@/lib/api/types';

interface RecurrenceConfigProps {
  value: RecurrenceRule | null;
  onChange: (value: RecurrenceRule | null) => void;
  disabled?: boolean;
}

// Дни недели
const DAYS_OF_WEEK = [
  { value: 'MO', label: 'Пн' },
  { value: 'TU', label: 'Вт' },
  { value: 'WE', label: 'Ср' },
  { value: 'TH', label: 'Чт' },
  { value: 'FR', label: 'Пт' },
  { value: 'SA', label: 'Сб' },
  { value: 'SU', label: 'Вс' },
];

// Частоты повторения
const FREQUENCIES: { value: RecurrenceFrequency; label: string; intervalLabel: string }[] = [
  { value: 'DAILY', label: 'Ежедневно', intervalLabel: 'дней' },
  { value: 'WEEKLY', label: 'Еженедельно', intervalLabel: 'недель' },
  { value: 'MONTHLY', label: 'Ежемесячно', intervalLabel: 'месяцев' },
  { value: 'YEARLY', label: 'Ежегодно', intervalLabel: 'лет' },
];

// Типы окончания
type EndType = 'never' | 'date' | 'count';

export function RecurrenceConfig({ value, onChange, disabled }: RecurrenceConfigProps) {
  const isEnabled = value !== null;

  // Включение/выключение повторения
  const handleToggle = (enabled: boolean) => {
    if (enabled) {
      onChange({
        frequency: 'WEEKLY',
        interval: 1,
      });
    } else {
      onChange(null);
    }
  };

  // Изменение частоты
  const handleFrequencyChange = (frequency: RecurrenceFrequency) => {
    if (!value) return;
    onChange({
      ...value,
      frequency,
      byDay: frequency === 'WEEKLY' ? value.byDay : undefined,
      byMonthDay: frequency === 'MONTHLY' ? value.byMonthDay : undefined,
    });
  };

  // Изменение интервала
  const handleIntervalChange = (interval: number) => {
    if (!value) return;
    onChange({ ...value, interval: Math.max(1, interval) });
  };

  // Переключение дня недели
  const handleDayToggle = (day: string) => {
    if (!value) return;
    const currentDays = value.byDay ? value.byDay.split(',') : [];
    let newDays: string[];

    if (currentDays.includes(day)) {
      newDays = currentDays.filter((d) => d !== day);
    } else {
      newDays = [...currentDays, day];
    }

    onChange({
      ...value,
      byDay: newDays.length > 0 ? newDays.join(',') : undefined,
    });
  };

  // Изменение дня месяца
  const handleMonthDayChange = (day: number) => {
    if (!value) return;
    onChange({ ...value, byMonthDay: Math.min(31, Math.max(1, day)) });
  };

  // Получение типа окончания
  const getEndType = (): EndType => {
    if (!value) return 'never';
    if (value.endsAt) return 'date';
    if (value.occurrenceCount) return 'count';
    return 'never';
  };

  // Изменение типа окончания
  const handleEndTypeChange = (endType: EndType) => {
    if (!value) return;

    const newValue = { ...value };
    delete newValue.endsAt;
    delete newValue.occurrenceCount;

    if (endType === 'date') {
      // По умолчанию через 3 месяца
      const endDate = new Date();
      endDate.setMonth(endDate.getMonth() + 3);
      newValue.endsAt = endDate.toISOString();
    } else if (endType === 'count') {
      newValue.occurrenceCount = 10;
    }

    onChange(newValue);
  };

  // Изменение даты окончания
  const handleEndDateChange = (date: Date | undefined) => {
    if (!value || !date) return;
    onChange({ ...value, endsAt: date.toISOString() });
  };

  // Изменение количества повторений
  const handleOccurrenceCountChange = (count: number) => {
    if (!value) return;
    onChange({ ...value, occurrenceCount: Math.max(1, count) });
  };

  const selectedDays = value?.byDay?.split(',') || [];
  const currentFrequency = FREQUENCIES.find((f) => f.value === value?.frequency);

  return (
    <div className="space-y-4">
      {/* Переключатель */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Repeat className="h-4 w-4 text-muted-foreground" />
          <Label htmlFor="recurrence-toggle" className="font-medium">
            Повторяющееся событие
          </Label>
        </div>
        <Switch
          id="recurrence-toggle"
          checked={isEnabled}
          onCheckedChange={handleToggle}
          disabled={disabled}
        />
      </div>

      {isEnabled && value && (
        <div className="space-y-4 rounded-lg border p-4 bg-muted/30">
          {/* Частота и интервал */}
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label>Частота</Label>
              <Select
                value={value.frequency}
                onValueChange={(v) => handleFrequencyChange(v as RecurrenceFrequency)}
                disabled={disabled}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {FREQUENCIES.map((freq) => (
                    <SelectItem key={freq.value} value={freq.value}>
                      {freq.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Каждые</Label>
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  min={1}
                  max={99}
                  value={value.interval}
                  onChange={(e) => handleIntervalChange(parseInt(e.target.value) || 1)}
                  className="w-20"
                  disabled={disabled}
                />
                <span className="text-sm text-muted-foreground">
                  {currentFrequency?.intervalLabel}
                </span>
              </div>
            </div>
          </div>

          {/* Дни недели (для WEEKLY) */}
          {value.frequency === 'WEEKLY' && (
            <div className="space-y-2">
              <Label>Дни недели</Label>
              <div className="flex flex-wrap gap-2">
                {DAYS_OF_WEEK.map((day) => (
                  <Badge
                    key={day.value}
                    variant={selectedDays.includes(day.value) ? 'default' : 'outline'}
                    className={cn(
                      'cursor-pointer transition-colors',
                      selectedDays.includes(day.value)
                        ? 'hover:bg-primary/80'
                        : 'hover:bg-muted'
                    )}
                    onClick={() => !disabled && handleDayToggle(day.value)}
                  >
                    {day.label}
                  </Badge>
                ))}
              </div>
              {selectedDays.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  Выберите хотя бы один день
                </p>
              )}
            </div>
          )}

          {/* День месяца (для MONTHLY) */}
          {value.frequency === 'MONTHLY' && (
            <div className="space-y-2">
              <Label>День месяца</Label>
              <Input
                type="number"
                min={1}
                max={31}
                value={value.byMonthDay || 1}
                onChange={(e) => handleMonthDayChange(parseInt(e.target.value) || 1)}
                className="w-20"
                disabled={disabled}
              />
            </div>
          )}

          {/* Окончание серии */}
          <div className="space-y-2">
            <Label>Окончание</Label>
            <Select
              value={getEndType()}
              onValueChange={(v) => handleEndTypeChange(v as EndType)}
              disabled={disabled}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="never">Никогда</SelectItem>
                <SelectItem value="date">В определённую дату</SelectItem>
                <SelectItem value="count">После N повторений</SelectItem>
              </SelectContent>
            </Select>

            {/* Дата окончания */}
            {getEndType() === 'date' && value.endsAt && (
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant="outline"
                    className={cn(
                      'w-full justify-start text-left font-normal',
                      !value.endsAt && 'text-muted-foreground'
                    )}
                    disabled={disabled}
                  >
                    <Calendar className="mr-2 h-4 w-4" />
                    {format(parseISO(value.endsAt), 'd MMMM yyyy', { locale: ru })}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <CalendarComponent
                    mode="single"
                    selected={parseISO(value.endsAt)}
                    onSelect={handleEndDateChange}
                    disabled={(date) => date < new Date()}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            )}

            {/* Количество повторений */}
            {getEndType() === 'count' && (
              <div className="flex items-center gap-2">
                <Input
                  type="number"
                  min={1}
                  max={365}
                  value={value.occurrenceCount || 10}
                  onChange={(e) => handleOccurrenceCountChange(parseInt(e.target.value) || 1)}
                  className="w-20"
                  disabled={disabled}
                />
                <span className="text-sm text-muted-foreground">повторений</span>
              </div>
            )}
          </div>

          {/* Превью */}
          <div className="text-sm text-muted-foreground bg-background p-2 rounded border">
            {formatRecurrenceDescription(value)}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Форматирует человекочитаемое описание правила повторения.
 */
function formatRecurrenceDescription(rule: RecurrenceRule): string {
  const parts: string[] = [];

  // Частота и интервал
  const frequency = FREQUENCIES.find((f) => f.value === rule.frequency);
  if (rule.interval === 1) {
    parts.push(frequency?.label.toLowerCase() || '');
  } else {
    parts.push(`каждые ${rule.interval} ${frequency?.intervalLabel}`);
  }

  // Дни недели
  if (rule.frequency === 'WEEKLY' && rule.byDay) {
    const days = rule.byDay.split(',');
    const dayLabels = days.map((d) => DAYS_OF_WEEK.find((dw) => dw.value === d)?.label || d);
    parts.push(`по ${dayLabels.join(', ')}`);
  }

  // День месяца
  if (rule.frequency === 'MONTHLY' && rule.byMonthDay) {
    parts.push(`${rule.byMonthDay}-го числа`);
  }

  // Окончание
  if (rule.endsAt) {
    parts.push(`до ${format(parseISO(rule.endsAt), 'd MMM yyyy', { locale: ru })}`);
  } else if (rule.occurrenceCount) {
    parts.push(`${rule.occurrenceCount} раз`);
  }

  return parts.join(', ').replace(/^./, (c) => c.toUpperCase());
}

// Экспорт для использования в других местах
export { formatRecurrenceDescription };
