'use client';

import { useRef, useState } from 'react';
import { Camera, Loader2, User } from 'lucide-react';

import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

import { useUploadAvatar } from '@/lib/hooks/use-media';
import { useAuthStore } from '@/lib/store/auth-store';

const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

/**
 * Компонент для загрузки аватара пользователя.
 */
export function AvatarUpload() {
  const { user } = useAuthStore();
  const uploadAvatar = useUploadAvatar();
  const inputRef = useRef<HTMLInputElement>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Валидация типа
    if (!ACCEPTED_TYPES.includes(file.type)) {
      alert('Недопустимый тип файла. Разрешены: JPEG, PNG, WebP, GIF');
      return;
    }

    // Валидация размера
    if (file.size > MAX_FILE_SIZE) {
      alert('Размер файла превышает 10MB');
      return;
    }

    // Превью
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);

    // Загрузка
    uploadAvatar.mutate(file, {
      onSettled: () => {
        // Очищаем input
        if (inputRef.current) {
          inputRef.current.value = '';
        }
      },
    });
  };

  const displayUrl = previewUrl || user?.avatarUrl;
  const initials = user
    ? `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`
    : '';

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Camera className="h-5 w-5" />
          Фото профиля
        </CardTitle>
        <CardDescription>Загрузите изображение для вашего профиля</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col items-center gap-4">
        <div className="relative">
          <Avatar className="h-24 w-24">
            <AvatarImage src={displayUrl || undefined} alt={user?.firstName || 'Аватар'} />
            <AvatarFallback className="text-2xl">
              {initials || <User className="h-10 w-10" />}
            </AvatarFallback>
          </Avatar>

          {uploadAvatar.isPending && (
            <div className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50">
              <Loader2 className="h-8 w-8 animate-spin text-white" />
            </div>
          )}
        </div>

        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(',')}
          onChange={handleFileChange}
          className="hidden"
        />

        <Button
          variant="outline"
          onClick={handleClick}
          disabled={uploadAvatar.isPending}
        >
          {uploadAvatar.isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Загрузка...
            </>
          ) : (
            <>
              <Camera className="mr-2 h-4 w-4" />
              Выбрать изображение
            </>
          )}
        </Button>

        <p className="text-center text-xs text-muted-foreground">
          JPEG, PNG, WebP или GIF. Максимум 10MB.
        </p>
      </CardContent>
    </Card>
  );
}
