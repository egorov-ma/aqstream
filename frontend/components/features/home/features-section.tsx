import { Layers, Users, Github } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';

const features = [
  {
    icon: Layers,
    title: 'Всё в одном месте',
    description:
      'Регистрация, билеты, уведомления, аналитика — все инструменты организатора в едином интерфейсе',
  },
  {
    icon: Users,
    title: 'Работайте вместе',
    description:
      'Создавайте организации, приглашайте модераторов, управляйте событиями всей командой',
  },
  {
    icon: Github,
    title: 'Открыто и прозрачно',
    description:
      'Open-source платформа без vendor lock-in. Полный контроль над вашими данными',
  },
];

export function FeaturesSection() {
  return (
    <section className="py-20 md:py-32 bg-muted/30" data-testid="features-section" id="features">
      <div className="container">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">
            Всё что нужно для успешного события
          </h2>
          <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
            Мощные инструменты для организаторов любого масштаба
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            return (
              <Card
                key={index}
                className="border-none bg-card/50 backdrop-blur-sm"
                data-testid={`feature-card-${index}`}
              >
                <CardContent className="p-8">
                  <div className="p-3 rounded-lg bg-primary/10 w-fit mb-4">
                    <Icon className="h-12 w-12 text-primary" />
                  </div>
                  <h3 className="text-xl font-semibold mb-3">{feature.title}</h3>
                  <p className="text-muted-foreground">{feature.description}</p>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>
    </section>
  );
}
