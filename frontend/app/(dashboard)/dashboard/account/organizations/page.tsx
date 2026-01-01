import { OrganizationList } from '@/components/features/account/organization-list';

export default function OrganizationsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Мои организации</h1>
        <p className="text-muted-foreground">
          Организации, в которых вы являетесь участником
        </p>
      </div>

      <OrganizationList />
    </div>
  );
}
