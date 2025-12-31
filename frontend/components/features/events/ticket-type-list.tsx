'use client';

import * as React from 'react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Plus, Ticket } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TicketTypeForm } from './ticket-type-form';
import type { TicketTypeFormData } from '@/lib/validations/events';

interface TicketTypeListProps {
  ticketTypes: TicketTypeFormData[];
  onChange: (ticketTypes: TicketTypeFormData[]) => void;
  disabled?: boolean;
  errors?: Record<number, { name?: string; description?: string; quantity?: string }>;
}

// Обёртка для sortable элемента
function SortableTicketType({
  id,
  index,
  data,
  onChange,
  onRemove,
  disabled,
  errors,
}: {
  id: string;
  index: number;
  data: TicketTypeFormData;
  onChange: (index: number, data: Partial<TicketTypeFormData>) => void;
  onRemove: (index: number) => void;
  disabled?: boolean;
  errors?: { name?: string; description?: string; quantity?: string };
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes}>
      <TicketTypeForm
        index={index}
        data={data}
        onChange={onChange}
        onRemove={onRemove}
        disabled={disabled}
        dragHandleProps={listeners}
        errors={errors}
      />
    </div>
  );
}

export function TicketTypeList({
  ticketTypes,
  onChange,
  disabled,
  errors,
}: TicketTypeListProps) {
  // Генерируем уникальные ID для DnD
  const ids = React.useMemo(
    () => ticketTypes.map((_, i) => `ticket-${i}`),
    [ticketTypes]
  );

  // Настраиваем сенсоры для DnD
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // Обработчик окончания перетаскивания
  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = ids.indexOf(active.id as string);
      const newIndex = ids.indexOf(over.id as string);

      const newTicketTypes = arrayMove(ticketTypes, oldIndex, newIndex).map(
        (tt, i) => ({ ...tt, sortOrder: i })
      );
      onChange(newTicketTypes);
    }
  };

  // Обработчик изменения типа билета
  const handleTicketTypeChange = (
    index: number,
    data: Partial<TicketTypeFormData>
  ) => {
    const newTicketTypes = [...ticketTypes];
    newTicketTypes[index] = { ...newTicketTypes[index], ...data };
    onChange(newTicketTypes);
  };

  // Обработчик удаления типа билета
  const handleRemove = (index: number) => {
    const newTicketTypes = ticketTypes.filter((_, i) => i !== index);
    // Пересчитываем sortOrder
    onChange(newTicketTypes.map((tt, i) => ({ ...tt, sortOrder: i })));
  };

  // Добавление нового типа билета
  const handleAdd = () => {
    onChange([
      ...ticketTypes,
      {
        name: '',
        description: '',
        quantity: null,
        salesStart: null,
        salesEnd: null,
        sortOrder: ticketTypes.length,
      },
    ]);
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
        <CardTitle className="text-lg font-medium flex items-center gap-2">
          <Ticket className="h-5 w-5" />
          Типы билетов
        </CardTitle>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleAdd}
          disabled={disabled}
          data-testid="ticket-type-add-button"
        >
          <Plus className="h-4 w-4 mr-1" />
          Добавить
        </Button>
      </CardHeader>
      <CardContent>
        {ticketTypes.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <Ticket className="h-10 w-10 mx-auto mb-2 opacity-50" />
            <p>Нет типов билетов</p>
            <p className="text-sm">
              Добавьте хотя бы один тип билета для регистрации участников
            </p>
          </div>
        ) : (
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext items={ids} strategy={verticalListSortingStrategy}>
              <div className="space-y-3">
                {ticketTypes.map((ticketType, index) => (
                  <SortableTicketType
                    key={ids[index]}
                    id={ids[index]}
                    index={index}
                    data={ticketType}
                    onChange={handleTicketTypeChange}
                    onRemove={handleRemove}
                    disabled={disabled}
                    errors={errors?.[index]}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        )}
      </CardContent>
    </Card>
  );
}
