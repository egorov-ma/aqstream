'use client';

import * as React from 'react';
import { ImageIcon, Upload, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import { mediaApi } from '@/lib/api/media';
import { toast } from 'sonner';

interface ImageUploadProps {
  value: string; // URL изображения
  onChange: (url: string) => void;
  disabled?: boolean;
  className?: string;
  'data-testid'?: string;
}

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

export function ImageUpload({
  value,
  onChange,
  disabled,
  className,
  'data-testid': dataTestId,
}: ImageUploadProps) {
  const [isUploading, setIsUploading] = React.useState(false);
  const inputRef = React.useRef<HTMLInputElement>(null);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Валидация типа файла
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      toast.error('Поддерживаются только изображения (JPEG, PNG, WebP, GIF)');
      return;
    }

    // Валидация размера
    if (file.size > MAX_FILE_SIZE) {
      toast.error('Размер файла не должен превышать 10 МБ');
      return;
    }

    setIsUploading(true);
    try {
      const response = await mediaApi.upload(file);
      onChange(response.url);
      toast.success('Изображение загружено');
    } catch {
      toast.error('Ошибка при загрузке изображения');
    } finally {
      setIsUploading(false);
      // Сбрасываем input для возможности повторной загрузки того же файла
      if (inputRef.current) {
        inputRef.current.value = '';
      }
    }
  };

  const handleRemove = () => {
    onChange('');
  };

  const handleClick = () => {
    inputRef.current?.click();
  };

  return (
    <div className={cn('space-y-2', className)} data-testid={dataTestId}>
      <Input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_IMAGE_TYPES.join(',')}
        onChange={handleFileChange}
        disabled={disabled || isUploading}
        className="hidden"
      />

      {value ? (
        // Превью загруженного изображения
        <div className="relative group">
          <div className="relative aspect-video w-full max-w-md overflow-hidden rounded-lg border">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={value}
              alt="Обложка события"
              className="h-full w-full object-cover"
            />
          </div>
          {!disabled && (
            <Button
              type="button"
              variant="destructive"
              size="icon"
              className="absolute top-2 right-2 h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
              onClick={handleRemove}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      ) : (
        // Область для загрузки
        <button
          type="button"
          onClick={handleClick}
          disabled={disabled || isUploading}
          className={cn(
            'flex flex-col items-center justify-center',
            'w-full max-w-md aspect-video',
            'border-2 border-dashed rounded-lg',
            'text-muted-foreground hover:text-foreground',
            'hover:border-primary hover:bg-muted/50',
            'transition-colors cursor-pointer',
            'disabled:cursor-not-allowed disabled:opacity-50'
          )}
        >
          {isUploading ? (
            <>
              <Upload className="h-10 w-10 mb-2 animate-pulse" />
              <span className="text-sm">Загрузка...</span>
            </>
          ) : (
            <>
              <ImageIcon className="h-10 w-10 mb-2" />
              <span className="text-sm">Нажмите для загрузки обложки</span>
              <span className="text-xs text-muted-foreground mt-1">
                JPEG, PNG, WebP или GIF до 10 МБ
              </span>
            </>
          )}
        </button>
      )}
    </div>
  );
}
