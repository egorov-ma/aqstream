import { Skeleton } from '@/components/ui/skeleton';

export default function Loading() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <Skeleton className="h-12 w-12 rounded-full" />
        <Skeleton className="h-4 w-24" />
      </div>
    </main>
  );
}
