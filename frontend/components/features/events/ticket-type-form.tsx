'use client';

import * as React from 'react';
import { GripVertical, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Switch } from '@/components/ui/switch';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import type { TicketTypeFormData } from '@/lib/validations/events';

interface TicketTypeFormProps {
  index: number;
  data: TicketTypeFormData;
  onChange: (index: number, data: Partial<TicketTypeFormData>) => void;
  onRemove: (index: number) => void;
  disabled?: boolean;
  hasRegistrations?: boolean;
  dragHandleProps?: React.HTMLAttributes<HTMLDivElement>;
  errors?: {
    name?: string;
    description?: string;
    quantity?: string;
  };
}

export function TicketTypeForm({
  index,
  data,
  onChange,
  onRemove,
  disabled,
  hasRegistrations,
  dragHandleProps,
  errors,
}: TicketTypeFormProps) {
  const [isExpanded, setIsExpanded] = React.useState(!data.id);

  const handleChange = (field: keyof TicketTypeFormData, value: unknown) => {
    onChange(index, { [field]: value });
  };

  return (
    <div
      className={cn(
        'rounded-lg border bg-card p-4',
        disabled && 'opacity-60'
      )}
      data-testid={`ticket-type-item-${index}`}
    >
      {/* Заголовок с drag handle */}
      <div className="flex items-center gap-2 mb-3">
        {/* Drag handle */}
        <div
          {...dragHandleProps}
          className="cursor-grab active:cursor-grabbing touch-none"
        >
          <GripVertical className="h-5 w-5 text-muted-foreground" />
        </div>

        {/* Название (компактный вид) */}
        <div className="flex-1">
          <Input
            value={data.name}
            onChange={(e) => handleChange('name', e.target.value)}
            placeholder="Название билета"
            disabled={disabled}
            className={cn(errors?.name && 'border-destructive')}
            data-testid={`ticket-type-name-${index}`}
          />
          {errors?.name && (
            <p className="text-destructive text-xs mt-1">{errors.name}</p>
          )}
        </div>

        {/* Количество (компактное) */}
        <div className="w-24">
          <Input
            type="number"
            value={data.quantity ?? ''}
            onChange={(e) =>
              handleChange(
                'quantity',
                e.target.value ? parseInt(e.target.value, 10) : null
              )
            }
            placeholder="∞"
            disabled={disabled}
            min={1}
            data-testid={`ticket-type-quantity-${index}`}
          />
        </div>

        {/* Кнопки */}
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setIsExpanded(!isExpanded)}
          disabled={disabled}
        >
          {isExpanded ? 'Свернуть' : 'Подробнее'}
        </Button>

        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={() => onRemove(index)}
                disabled={disabled || hasRegistrations}
                className="text-destructive hover:text-destructive"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            {hasRegistrations && (
              <TooltipContent>
                <p>Невозможно удалить — есть регистрации</p>
              </TooltipContent>
            )}
          </Tooltip>
        </TooltipProvider>
      </div>

      {/* Расширенные поля */}
      {isExpanded && (
        <div className="space-y-4 pl-7 border-t pt-4 mt-2">
          {/* Описание */}
          <div className="space-y-2">
            <Label htmlFor={`ticket-${index}-description`}>Описание</Label>
            <Textarea
              id={`ticket-${index}-description`}
              value={data.description ?? ''}
              onChange={(e) => handleChange('description', e.target.value)}
              placeholder="Описание типа билета (опционально)"
              disabled={disabled}
              rows={2}
            />
          </div>

          {/* Период продаж */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor={`ticket-${index}-salesStart`}>
                Начало продаж
              </Label>
              <Input
                id={`ticket-${index}-salesStart`}
                type="datetime-local"
                value={
                  data.salesStart
                    ? new Date(data.salesStart).toISOString().slice(0, 16)
                    : ''
                }
                onChange={(e) =>
                  handleChange(
                    'salesStart',
                    e.target.value ? new Date(e.target.value).toISOString() : null
                  )
                }
                disabled={disabled}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor={`ticket-${index}-salesEnd`}>
                Окончание продаж
              </Label>
              <Input
                id={`ticket-${index}-salesEnd`}
                type="datetime-local"
                value={
                  data.salesEnd
                    ? new Date(data.salesEnd).toISOString().slice(0, 16)
                    : ''
                }
                onChange={(e) =>
                  handleChange(
                    'salesEnd',
                    e.target.value ? new Date(e.target.value).toISOString() : null
                  )
                }
                disabled={disabled}
              />
            </div>
          </div>

          {/* Показать количество null = безлимит */}
          <div className="flex items-center gap-2">
            <Switch
              id={`ticket-${index}-unlimited`}
              checked={data.quantity === null}
              onCheckedChange={(checked) =>
                handleChange('quantity', checked ? null : 100)
              }
              disabled={disabled}
            />
            <Label htmlFor={`ticket-${index}-unlimited`} className="font-normal">
              Без ограничения количества
            </Label>
          </div>
        </div>
      )}
    </div>
  );
}
