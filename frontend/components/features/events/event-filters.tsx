'use client';

import * as React from 'react';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import { Search, X, CalendarIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Calendar } from '@/components/ui/calendar';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';
import type { EventStatus } from '@/lib/api/types';

export interface EventFiltersState {
  search: string;
  status: EventStatus | 'ALL';
  dateFrom?: string;
  dateTo?: string;
}

interface EventFiltersProps {
  filters: EventFiltersState;
  onFiltersChange: (filters: EventFiltersState) => void;
}

export function EventFilters({ filters, onFiltersChange }: EventFiltersProps) {
  const [searchValue, setSearchValue] = React.useState(filters.search);
  const [dateFrom, setDateFrom] = React.useState<Date | undefined>(
    filters.dateFrom ? new Date(filters.dateFrom) : undefined
  );
  const [dateTo, setDateTo] = React.useState<Date | undefined>(
    filters.dateTo ? new Date(filters.dateTo) : undefined
  );

  // Debounce поиска
  React.useEffect(() => {
    const timer = setTimeout(() => {
      if (searchValue !== filters.search) {
        onFiltersChange({ ...filters, search: searchValue });
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [searchValue, filters, onFiltersChange]);

  const handleStatusChange = (status: EventStatus | 'ALL') => {
    onFiltersChange({ ...filters, status });
  };

  const handleDateFromChange = (date: Date | undefined) => {
    setDateFrom(date);
    onFiltersChange({
      ...filters,
      dateFrom: date ? date.toISOString() : undefined,
    });
  };

  const handleDateToChange = (date: Date | undefined) => {
    setDateTo(date);
    onFiltersChange({
      ...filters,
      dateTo: date ? date.toISOString() : undefined,
    });
  };

  const handleClearSearch = () => {
    setSearchValue('');
    onFiltersChange({ ...filters, search: '' });
  };

  const handleClearDates = () => {
    setDateFrom(undefined);
    setDateTo(undefined);
    onFiltersChange({ ...filters, dateFrom: undefined, dateTo: undefined });
  };

  const hasActiveFilters =
    filters.search || filters.status !== 'ALL' || filters.dateFrom || filters.dateTo;

  const hasDateFilter = filters.dateFrom || filters.dateTo;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col sm:flex-row gap-4">
        {/* Поиск */}
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            placeholder="Поиск по названию..."
            className="pl-9 pr-9"
            data-testid="events-search-input"
          />
          {searchValue && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
              onClick={handleClearSearch}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>

        {/* Фильтр по статусу */}
        <Select value={filters.status} onValueChange={handleStatusChange}>
          <SelectTrigger className="w-[180px]" data-testid="events-status-filter">
            <SelectValue placeholder="Все статусы" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Все статусы</SelectItem>
            <SelectItem value="DRAFT">Черновики</SelectItem>
            <SelectItem value="PUBLISHED">Опубликованные</SelectItem>
            <SelectItem value="CANCELLED">Отменённые</SelectItem>
            <SelectItem value="COMPLETED">Завершённые</SelectItem>
          </SelectContent>
        </Select>

        {/* Фильтр по дате начала */}
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              className={cn(
                'w-[180px] justify-start text-left font-normal',
                !dateFrom && 'text-muted-foreground'
              )}
              data-testid="events-date-from-filter"
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {dateFrom ? format(dateFrom, 'd MMM yyyy', { locale: ru }) : 'Дата от'}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={dateFrom}
              onSelect={handleDateFromChange}
              locale={ru}
              initialFocus
            />
          </PopoverContent>
        </Popover>

        {/* Фильтр по дате окончания */}
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              className={cn(
                'w-[180px] justify-start text-left font-normal',
                !dateTo && 'text-muted-foreground'
              )}
              data-testid="events-date-to-filter"
            >
              <CalendarIcon className="mr-2 h-4 w-4" />
              {dateTo ? format(dateTo, 'd MMM yyyy', { locale: ru }) : 'Дата до'}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0" align="start">
            <Calendar
              mode="single"
              selected={dateTo}
              onSelect={handleDateToChange}
              locale={ru}
              disabled={(date) => (dateFrom ? date < dateFrom : false)}
              initialFocus
            />
          </PopoverContent>
        </Popover>

        {/* Очистить даты */}
        {hasDateFilter && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={handleClearDates}
            title="Очистить даты"
          >
            <X className="h-4 w-4" />
          </Button>
        )}

        {/* Кнопка сброса всех фильтров */}
        {hasActiveFilters && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => {
              setSearchValue('');
              setDateFrom(undefined);
              setDateTo(undefined);
              onFiltersChange({ search: '', status: 'ALL', dateFrom: undefined, dateTo: undefined });
            }}
          >
            Сбросить всё
          </Button>
        )}
      </div>
    </div>
  );
}
