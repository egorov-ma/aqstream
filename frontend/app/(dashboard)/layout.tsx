import { Header } from '@/components/layout/header';
import { Sidebar } from '@/components/layout/sidebar';
import { BreadcrumbNav } from '@/components/layout/breadcrumb-nav';
import { AuthGuard } from '@/components/auth';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <div className="grid min-h-screen w-full lg:grid-cols-[280px_1fr]">
        <Sidebar />
        <div className="flex flex-col">
          <Header />
          <BreadcrumbNav />
          <main className="flex flex-1 flex-col gap-4 p-4 lg:gap-6 lg:p-6">{children}</main>
        </div>
      </div>
    </AuthGuard>
  );
}
