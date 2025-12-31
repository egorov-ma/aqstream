import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader } from '@/components/ui/card';

export default function MyRegistrationsLoading() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <Skeleton className="h-8 w-48" />
      </div>

      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-40" />
          <Skeleton className="h-4 w-60" />
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Table header */}
            <div className="grid grid-cols-6 gap-4 pb-4 border-b">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-4 w-16" />
              <Skeleton className="h-4 w-16" />
              <Skeleton className="h-4 w-16 ml-auto" />
            </div>

            {/* Table rows */}
            {[1, 2, 3].map((i) => (
              <div key={i} className="grid grid-cols-6 gap-4 items-center py-4">
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-5 w-28" />
                <Skeleton className="h-5 w-24" />
                <Skeleton className="h-6 w-20" />
                <Skeleton className="h-6 w-24" />
                <Skeleton className="h-8 w-20 ml-auto" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
