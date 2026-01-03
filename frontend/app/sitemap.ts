import { MetadataRoute } from 'next';

/**
 * Генерация sitemap.xml для SEO.
 * Включает статические страницы + динамические события.
 */
export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = 'https://aqstream.ru';

  // Статические страницы
  const routes = [
    '',
    '/events',
    '/login',
    '/register',
    '/dashboard',
    '/forgot-password',
  ].map((route) => ({
    url: `${baseUrl}${route}`,
    lastModified: new Date(),
    changeFrequency: 'daily' as const,
    priority: route === '' ? 1 : 0.8,
  }));

  return routes;
}
