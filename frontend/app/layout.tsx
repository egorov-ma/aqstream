import type { Metadata, Viewport } from 'next';
import { Inter } from 'next/font/google';
import { ThemeProvider } from '@/components/theme-provider';
import { QueryProvider } from '@/lib/providers/query-provider';
import { Toaster } from '@/components/ui/sonner';
import { WelcomeScript } from '@/components/welcome-script';
import '@/styles/globals.css';

const inter = Inter({
  subsets: ['latin', 'cyrillic'],
  display: 'swap', // Оптимизация render blocking - показываем fallback font сразу
  preload: true,
});

export const metadata: Metadata = {
  title: {
    default: 'AqStream - Платформа для управления мероприятиями',
    template: '%s | AqStream',
  },
  description:
    'Open-source платформа для организации мероприятий любого масштаба. Создавайте события, управляйте регистрациями, отслеживайте аналитику — всё в одном месте.',
  keywords: [
    'управление мероприятиями',
    'организация событий',
    'регистрация на события',
    'билеты на мероприятия',
    'open source',
    'event management',
  ],
  authors: [{ name: 'AqStream Team' }],
  creator: 'AqStream',
  publisher: 'AqStream',
  manifest: '/manifest.json',
  appleWebApp: {
    capable: true,
    statusBarStyle: 'default',
    title: 'AqStream Check-in',
  },
  openGraph: {
    type: 'website',
    locale: 'ru_RU',
    url: 'https://aqstream.ru',
    siteName: 'AqStream',
    title: 'AqStream - Платформа для управления мероприятиями',
    description:
      'Open-source платформа для организации мероприятий любого масштаба. От митапов до конференций.',
    images: [
      {
        url: '/og-image.png',
        width: 1200,
        height: 630,
        alt: 'AqStream - Платформа для управления мероприятиями',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'AqStream - Платформа для управления мероприятиями',
    description:
      'Open-source платформа для организации мероприятий любого масштаба',
    images: ['/og-image.png'],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
};

export const viewport: Viewport = {
  themeColor: '#000000',
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ru" suppressHydrationWarning>
      <body className={inter.className}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <QueryProvider>
            {children}
            <Toaster />
            <WelcomeScript />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
