import Link from 'next/link';
import dynamic from 'next/dynamic';
import { Github } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { PublicHeader } from '@/components/layout/public-header';
import {
  UpcomingEventsSection,
  FeaturesSection,
  RoleBasedSections,
} from '@/components/features/home';
import { serverEventsApi } from '@/lib/api/server';
import type { PublicEventSummary } from '@/lib/api/types';

// Dynamic imports для клиентских компонентов (оптимизация INP)
const HeroActions = dynamic(
  () => import('@/components/features/home/hero-actions').then((mod) => ({ default: mod.HeroActions })),
  {
    ssr: false,
    loading: () => (
      <>
        <Skeleton className="h-11 w-40" />
        <Skeleton className="h-11 w-40" />
      </>
    ),
  }
);

const CtaSection = dynamic(
  () => import('@/components/features/home/cta-section').then((mod) => ({ default: mod.CtaSection })),
  {
    ssr: false,
    loading: () => (
      <section className="py-20 md:py-32 bg-gradient-to-t from-muted/30 to-background">
        <div className="container">
          <div className="text-center max-w-3xl mx-auto">
            <Skeleton className="h-12 w-64 mx-auto mb-6" />
            <Skeleton className="h-6 w-96 mx-auto mb-8" />
            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <Skeleton className="h-11 w-40" />
              <Skeleton className="h-11 w-40" />
            </div>
          </div>
        </div>
      </section>
    ),
  }
);

/**
 * Загружает предстоящие публичные события на сервере.
 * При ошибке возвращает пустой массив (секция не отображается).
 */
async function getUpcomingEvents(): Promise<{
  events: PublicEventSummary[];
  hasMore: boolean;
}> {
  try {
    const data = await serverEventsApi.listPublic({ size: 6 });
    return {
      events: data.data,
      hasMore: data.hasNext ?? false,
    };
  } catch {
    // При ошибке API просто не показываем секцию
    return { events: [], hasMore: false };
  }
}

export default async function HomePage() {
  const { events, hasMore } = await getUpcomingEvents();

  // Structured data для SEO (JSON-LD)
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: 'AqStream',
    description:
      'Open-source платформа для управления мероприятиями любого масштаба. От митапов до конференций, от турниров до фестивалей.',
    url: 'https://aqstream.ru',
    publisher: {
      '@type': 'Organization',
      name: 'AqStream',
      description: 'Open-source платформа для управления мероприятиями',
      url: 'https://aqstream.ru',
      logo: {
        '@type': 'ImageObject',
        url: 'https://aqstream.ru/icon.svg',
      },
      sameAs: ['https://github.com/aqstream/aqstream'],
    },
    potentialAction: {
      '@type': 'SearchAction',
      target: {
        '@type': 'EntryPoint',
        urlTemplate: 'https://aqstream.ru/events?search={search_term_string}',
      },
      'query-input': 'required name=search_term_string',
    },
  };

  return (
    <div className="flex min-h-screen flex-col">
      {/* Structured data для SEO */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
      />

      {/* Публичный хедер с условной навигацией */}
      <PublicHeader />

      <main className="flex-1">
        {/* Hero Section */}
        <section
          className="py-16 md:py-32 bg-gradient-to-b from-background to-muted/20"
          data-testid="hero-section"
        >
          <div className="container">
            <div className="text-center max-w-4xl mx-auto">
              <h1 className="mb-6 text-5xl md:text-7xl font-bold tracking-tight">
                Создавайте события.
                <br />
                Объединяйте людей.
              </h1>
              <p className="mb-8 text-lg md:text-xl text-muted-foreground max-w-3xl mx-auto leading-relaxed">
                AqStream — открытая платформа для организации мероприятий любого
                масштаба. От митапов до конференций, от турниров до фестивалей. Всё в
                одном месте: регистрация, уведомления, аналитика.
              </p>

              <div className="flex flex-col sm:flex-row justify-center gap-4">
                <HeroActions />
              </div>
            </div>
          </div>
        </section>

        {/* Предстоящие события */}
        <UpcomingEventsSection events={events} hasMore={hasMore} />

        {/* Ключевые преимущества */}
        <FeaturesSection />

        {/* Для разных ролей */}
        <RoleBasedSections />

        {/* Финальный CTA */}
        <CtaSection />
      </main>

      {/* Футер */}
      <footer className="border-t py-12">
        <div className="container">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
            {/* О платформе */}
            <div>
              <h3 className="font-semibold mb-3">О платформе</h3>
              <p className="text-sm text-muted-foreground mb-4">
                AqStream — Open-source платформа для управления мероприятиями
              </p>
              <a
                href="https://github.com/aqstream"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-2"
              >
                <Github className="h-4 w-4" />
                GitHub
              </a>
            </div>

            {/* Ресурсы */}
            <div>
              <h3 className="font-semibold mb-3">Ресурсы</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link href="/events" className="hover:text-foreground">
                    Все события
                  </Link>
                </li>
                <li>
                  <a
                    href="https://github.com/aqstream/aqstream"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-foreground"
                  >
                    Документация
                  </a>
                </li>
              </ul>
            </div>

            {/* Сообщество */}
            <div>
              <h3 className="font-semibold mb-3">Сообщество</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a
                    href="https://github.com/aqstream/aqstream/issues"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-foreground"
                  >
                    GitHub Issues
                  </a>
                </li>
                <li>
                  <a
                    href="https://github.com/aqstream/aqstream/blob/main/CONTRIBUTING.md"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="hover:text-foreground"
                  >
                    Внести вклад
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <div className="border-t pt-8 text-center text-xs text-muted-foreground">
            © {new Date().getFullYear()} AqStream. Open-source платформа для управления
            мероприятиями
          </div>
        </div>
      </footer>
    </div>
  );
}
