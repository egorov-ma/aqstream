import { Skeleton } from '@/components/ui/skeleton';

export default function PublicLoading() {
  return (
    <div className="container py-12">
      <div className="mx-auto max-w-3xl space-y-8">
        <Skeleton className="h-48 rounded-lg" />
        <Skeleton className="h-32 rounded-lg" />
      </div>
    </div>
  );
}
