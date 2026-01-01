'use client';

import { useState } from 'react';
import { Search } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface ManualSearchProps {
  onSearch: (code: string) => void;
  isLoading?: boolean;
}

/**
 * Компонент для ручного поиска регистрации по коду.
 */
export function ManualSearch({ onSearch, isLoading = false }: ManualSearchProps) {
  const [code, setCode] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (code.trim()) {
      onSearch(code.trim().toUpperCase());
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-lg">
          <Search className="h-5 w-5" />
          Поиск по коду
        </CardTitle>
        <CardDescription>
          Введите код подтверждения регистрации
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="flex gap-2">
          <Input
            placeholder="Введите код (напр. ABC123)"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            className="flex-1 uppercase"
            maxLength={10}
          />
          <Button type="submit" disabled={!code.trim() || isLoading}>
            {isLoading ? 'Поиск...' : 'Найти'}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
