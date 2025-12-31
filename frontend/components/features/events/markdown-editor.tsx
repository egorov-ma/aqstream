'use client';

import * as React from 'react';
import ReactMarkdown from 'react-markdown';
import { Textarea } from '@/components/ui/textarea';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Bold, Italic, Strikethrough, Link, List, ListOrdered } from 'lucide-react';
import { cn } from '@/lib/utils';

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  height?: number;
  className?: string;
  'data-testid'?: string;
}

// Вставка форматирования в текст
function insertFormat(
  textarea: HTMLTextAreaElement,
  before: string,
  after: string,
  placeholder: string
): string {
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = textarea.value;
  const selectedText = text.substring(start, end) || placeholder;

  return text.substring(0, start) + before + selectedText + after + text.substring(end);
}

export function MarkdownEditor({
  value,
  onChange,
  placeholder = 'Введите описание (поддерживается Markdown)',
  disabled,
  height = 200,
  className,
  'data-testid': dataTestId,
}: MarkdownEditorProps) {
  const textareaRef = React.useRef<HTMLTextAreaElement>(null);

  const handleFormat = (before: string, after: string, placeholder: string) => {
    if (!textareaRef.current) return;
    const newValue = insertFormat(textareaRef.current, before, after, placeholder);
    onChange(newValue);

    // Восстановить фокус
    setTimeout(() => {
      textareaRef.current?.focus();
    }, 0);
  };

  const handleBold = () => handleFormat('**', '**', 'жирный текст');
  const handleItalic = () => handleFormat('*', '*', 'курсив');
  const handleStrikethrough = () => handleFormat('~~', '~~', 'зачёркнутый');
  const handleLink = () => handleFormat('[', '](url)', 'ссылка');
  const handleUnorderedList = () => handleFormat('\n- ', '', 'элемент списка');
  const handleOrderedList = () => handleFormat('\n1. ', '', 'элемент списка');

  return (
    <div className={cn('markdown-editor', className)} data-testid={dataTestId}>
      <Tabs defaultValue="edit" className="w-full">
        <div className="flex items-center justify-between border-b pb-2">
          {/* Тулбар */}
          <div className="flex gap-1">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={handleBold}
              disabled={disabled}
              title="Жирный (Ctrl+B)"
            >
              <Bold className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={handleItalic}
              disabled={disabled}
              title="Курсив (Ctrl+I)"
            >
              <Italic className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={handleStrikethrough}
              disabled={disabled}
              title="Зачёркнутый"
            >
              <Strikethrough className="h-4 w-4" />
            </Button>
            <div className="mx-1 w-px bg-border" />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={handleLink}
              disabled={disabled}
              title="Ссылка"
            >
              <Link className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={handleUnorderedList}
              disabled={disabled}
              title="Маркированный список"
            >
              <List className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={handleOrderedList}
              disabled={disabled}
              title="Нумерованный список"
            >
              <ListOrdered className="h-4 w-4" />
            </Button>
          </div>

          {/* Переключатель табов */}
          <TabsList className="h-8">
            <TabsTrigger value="edit" className="text-xs px-3 py-1">
              Редактор
            </TabsTrigger>
            <TabsTrigger value="preview" className="text-xs px-3 py-1">
              Предпросмотр
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="edit" className="mt-2">
          <Textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled}
            className="min-h-[200px] resize-y font-mono text-sm"
            style={{ height }}
          />
        </TabsContent>

        <TabsContent value="preview" className="mt-2">
          <div
            className="min-h-[200px] rounded-md border p-4 prose prose-sm max-w-none"
            style={{ minHeight: height }}
          >
            {value ? (
              <ReactMarkdown>{value}</ReactMarkdown>
            ) : (
              <p className="text-muted-foreground italic">Нет содержимого для предпросмотра</p>
            )}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// Компонент для просмотра Markdown (read-only)
interface MarkdownPreviewProps {
  source: string;
  className?: string;
}

export function MarkdownPreview({ source, className }: MarkdownPreviewProps) {
  if (!source) {
    return (
      <p className="text-muted-foreground text-sm italic">Описание отсутствует</p>
    );
  }

  return (
    <div className={cn('prose prose-sm max-w-none', className)}>
      <ReactMarkdown>{source}</ReactMarkdown>
    </div>
  );
}
