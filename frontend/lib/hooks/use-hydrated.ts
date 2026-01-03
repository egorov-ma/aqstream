import { useEffect, useState } from 'react';

/**
 * Hook для определения завершения клиентской гидратации.
 * Возвращает false на сервере и true после mount на клиенте.
 *
 * Используется для предотвращения hydration mismatch при работе
 * с localStorage, window и другими browser-only API.
 */
export function useHydrated(): boolean {
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  return isHydrated;
}
