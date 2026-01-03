import Link from 'next/link';
import { Check } from 'lucide-react';
import { Button } from '@/components/ui/button';

const organizerBenefits = [
  'Настройте типы билетов и лимиты',
  'Отслеживайте регистрации в реальном времени',
  'Получайте аналитику и экспорт данных',
];

const participantBenefits = [
  'Регистрируйтесь в пару кликов',
  'Получайте билеты в Telegram',
  'Храните историю посещений',
];

export function RoleBasedSections() {
  return (
    <>
      {/* Для организаторов */}
      <section className="py-20 md:py-32 bg-background" data-testid="organizer-section">
        <div className="container">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            {/* Контент */}
            <div>
              <p className="text-sm uppercase tracking-wide text-primary mb-4">
                Для организаторов
              </p>
              <h2 className="text-3xl md:text-5xl font-bold mb-6">
                Создавайте события за минуты
              </h2>
              <div className="space-y-4 mb-8">
                {organizerBenefits.map((benefit, index) => (
                  <div key={index} className="flex items-start gap-3">
                    <div className="p-1 rounded-full bg-primary/10 mt-1">
                      <Check className="h-4 w-4 text-primary" />
                    </div>
                    <p className="text-muted-foreground">{benefit}</p>
                  </div>
                ))}
              </div>
              <Button size="lg" asChild data-testid="organizer-cta">
                <Link href="/register">Начать как организатор</Link>
              </Button>
            </div>

            {/* Визуал */}
            <div className="aspect-video bg-muted rounded-lg flex items-center justify-center">
              <p className="text-muted-foreground">Event Creation Flow</p>
            </div>
          </div>
        </div>
      </section>

      {/* Для участников */}
      <section className="py-20 md:py-32 bg-muted/20" data-testid="participant-section">
        <div className="container">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
            {/* Визуал (на desktop слева) */}
            <div className="aspect-video bg-muted rounded-lg flex items-center justify-center order-2 md:order-1">
              <p className="text-muted-foreground">Event Discovery</p>
            </div>

            {/* Контент */}
            <div className="order-1 md:order-2">
              <p className="text-sm uppercase tracking-wide text-primary mb-4">
                Для участников
              </p>
              <h2 className="text-3xl md:text-5xl font-bold mb-6">
                Находите интересные события
              </h2>
              <div className="space-y-4 mb-8">
                {participantBenefits.map((benefit, index) => (
                  <div key={index} className="flex items-start gap-3">
                    <div className="p-1 rounded-full bg-primary/10 mt-1">
                      <Check className="h-4 w-4 text-primary" />
                    </div>
                    <p className="text-muted-foreground">{benefit}</p>
                  </div>
                ))}
              </div>
              <Button size="lg" variant="outline" asChild data-testid="participant-cta">
                <Link href="/events">Все события</Link>
              </Button>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
