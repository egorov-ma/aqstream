'use client';

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

// Популярные часовые пояса
const TIMEZONE_OPTIONS = [
  { value: 'Europe/Moscow', label: 'Москва (UTC+3)' },
  { value: 'Europe/Kaliningrad', label: 'Калининград (UTC+2)' },
  { value: 'Europe/Samara', label: 'Самара (UTC+4)' },
  { value: 'Asia/Yekaterinburg', label: 'Екатеринбург (UTC+5)' },
  { value: 'Asia/Omsk', label: 'Омск (UTC+6)' },
  { value: 'Asia/Krasnoyarsk', label: 'Красноярск (UTC+7)' },
  { value: 'Asia/Irkutsk', label: 'Иркутск (UTC+8)' },
  { value: 'Asia/Yakutsk', label: 'Якутск (UTC+9)' },
  { value: 'Asia/Vladivostok', label: 'Владивосток (UTC+10)' },
  { value: 'Asia/Magadan', label: 'Магадан (UTC+11)' },
  { value: 'Asia/Kamchatka', label: 'Камчатка (UTC+12)' },
  { value: 'UTC', label: 'UTC (Всемирное время)' },
  { value: 'Europe/London', label: 'Лондон (UTC+0/+1)' },
  { value: 'Europe/Paris', label: 'Париж (UTC+1/+2)' },
  { value: 'Europe/Berlin', label: 'Берлин (UTC+1/+2)' },
  { value: 'America/New_York', label: 'Нью-Йорк (UTC-5/-4)' },
  { value: 'America/Los_Angeles', label: 'Лос-Анджелес (UTC-8/-7)' },
  { value: 'Asia/Tokyo', label: 'Токио (UTC+9)' },
  { value: 'Asia/Shanghai', label: 'Шанхай (UTC+8)' },
  { value: 'Asia/Dubai', label: 'Дубай (UTC+4)' },
];

interface TimezoneSelectProps {
  value: string;
  onValueChange: (value: string) => void;
  disabled?: boolean;
}

export function TimezoneSelect({
  value,
  onValueChange,
  disabled,
}: TimezoneSelectProps) {
  return (
    <Select value={value} onValueChange={onValueChange} disabled={disabled}>
      <SelectTrigger data-testid="event-timezone-select">
        <SelectValue placeholder="Выберите часовой пояс" />
      </SelectTrigger>
      <SelectContent>
        {TIMEZONE_OPTIONS.map((tz) => (
          <SelectItem key={tz.value} value={tz.value}>
            {tz.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

// Получить опции для использования в других местах
export function getTimezoneOptions() {
  return TIMEZONE_OPTIONS;
}

// Получить лейбл часового пояса
export function getTimezoneLabel(timezone: string): string {
  const option = TIMEZONE_OPTIONS.find((tz) => tz.value === timezone);
  return option?.label ?? timezone;
}
