import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { serverEventsApi } from '@/lib/api/server';
import { generateEventJsonLd } from '@/lib/utils/seo';
import {
  EventHero,
  EventInfo,
  EventStateMessage,
  ParticipantsPlaceholder,
  RegistrationForm,
} from '@/components/features/public-event';
import type { Event, TicketType } from '@/lib/api/types';

interface EventPageProps {
  params: Promise<{
    slug: string;
  }>;
}

// ISR: revalidate every 60 seconds
export const revalidate = 60;

/**
 * Получает событие по slug на сервере.
 * Использует серверный API клиент для SSR в Docker.
 */
async function getEvent(slug: string): Promise<Event | null> {
  try {
    return await serverEventsApi.getBySlug(slug);
  } catch {
    return null;
  }
}

/**
 * Получает типы билетов по slug на сервере.
 * Использует серверный API клиент для SSR в Docker.
 */
async function getTicketTypes(slug: string): Promise<TicketType[]> {
  try {
    return await serverEventsApi.getPublicTicketTypes(slug);
  } catch {
    return [];
  }
}

/**
 * Проверяет, открыта ли регистрация (учитывает оба ограничения)
 */
function isRegistrationOpen(event: Event): boolean {
  const now = new Date();

  // Регистрация ещё не открыта
  if (event.registrationOpensAt && new Date(event.registrationOpensAt) > now) {
    return false;
  }

  // Регистрация уже закрыта
  if (event.registrationClosesAt && new Date(event.registrationClosesAt) < now) {
    return false;
  }

  return true;
}

/**
 * Проверяет, закрыта ли регистрация (для отображения сообщения)
 */
function isRegistrationClosed(event: Event): boolean {
  if (event.registrationClosesAt) {
    return new Date(event.registrationClosesAt) < new Date();
  }
  return false;
}

/**
 * Проверяет, все ли билеты распроданы
 */
function areAllTicketsSoldOut(ticketTypes: TicketType[]): boolean {
  if (ticketTypes.length === 0) return false;
  return ticketTypes.every((tt) => tt.isSoldOut || !tt.isActive);
}

/**
 * Генерирует метаданные страницы
 */
export async function generateMetadata({ params }: EventPageProps): Promise<Metadata> {
  const { slug } = await params;
  const event = await getEvent(slug);

  if (!event || event.status === 'DRAFT') {
    return {
      title: 'Событие не найдено - AqStream',
      description: 'Запрашиваемое событие не найдено или недоступно.',
    };
  }

  const description = event.description
    ? event.description.replace(/[#*_`\[\]()]/g, '').slice(0, 160)
    : 'Событие на платформе AqStream';

  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://aqstream.ru';
  const ogImage = event.coverImageUrl || `${baseUrl}/og-default.svg`;

  return {
    title: `${event.title} - AqStream`,
    description,
    openGraph: {
      title: event.title,
      description,
      type: 'website',
      url: `${baseUrl}/events/${slug}`,
      images: [{ url: ogImage, width: 1200, height: 630, alt: event.title }],
    },
    twitter: {
      card: 'summary_large_image',
      title: event.title,
      description,
      images: [ogImage],
    },
  };
}

export default async function EventPage({ params }: EventPageProps) {
  const { slug } = await params;

  // Загружаем данные параллельно
  const [event, ticketTypes] = await Promise.all([
    getEvent(slug),
    getTicketTypes(slug),
  ]);

  // Событие не найдено или DRAFT
  if (!event || event.status === 'DRAFT') {
    notFound();
  }

  const registrationOpen = isRegistrationOpen(event);
  const registrationClosed = isRegistrationClosed(event);
  const allSoldOut = areAllTicketsSoldOut(ticketTypes);
  const canRegister =
    event.status === 'PUBLISHED' && registrationOpen && !allSoldOut;

  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://aqstream.ru';
  const jsonLd = generateEventJsonLd(event, baseUrl);

  return (
    <>
      {/* JSON-LD Structured Data */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      <div className="min-h-screen bg-background">
        {/* Hero Section */}
        <div className="container pt-8">
          <EventHero event={event} />
        </div>

        {/* Main Content */}
        <div className="container py-8">
          {/* State Message */}
          <div className="mb-8">
            <EventStateMessage
              event={event}
              isRegistrationClosed={registrationClosed}
              allSoldOut={allSoldOut}
            />
          </div>

          {/* Two Column Layout */}
          <div className="grid gap-8 lg:grid-cols-3">
            {/* Left Column - Event Info (2/3 width) */}
            <div className="lg:col-span-2 space-y-6">
              <EventInfo event={event} />

              {/* Participants Placeholder */}
              {event.participantsVisibility === 'OPEN' && (
                <ParticipantsPlaceholder />
              )}
            </div>

            {/* Right Column - Registration (1/3 width) */}
            <div className="lg:col-span-1">
              <div className="sticky top-8">
                <RegistrationForm
                  event={event}
                  ticketTypes={ticketTypes}
                  disabled={!canRegister}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
