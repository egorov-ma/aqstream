import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader } from '@/components/ui/card';

export default function SuccessPageLoading() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container py-12">
        <Card className="max-w-md mx-auto">
          <CardHeader className="text-center space-y-4">
            <Skeleton className="h-16 w-16 rounded-full mx-auto" />
            <Skeleton className="h-8 w-48 mx-auto" />
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Confirmation code */}
            <div className="text-center space-y-2">
              <Skeleton className="h-4 w-32 mx-auto" />
              <Skeleton className="h-10 w-40 mx-auto" />
            </div>

            <Skeleton className="h-px w-full" />

            {/* Event details */}
            <div className="space-y-2">
              <Skeleton className="h-6 w-3/4" />
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-4 w-2/3" />
            </div>

            {/* Telegram message */}
            <Skeleton className="h-16 w-full rounded-lg" />

            {/* Buttons */}
            <div className="space-y-3">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
