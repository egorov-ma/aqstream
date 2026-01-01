import { GroupList } from '@/components/features/account/group-list';

export default function GroupsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Мои группы</h1>
        <p className="text-muted-foreground">
          Группы, в которых вы состоите
        </p>
      </div>

      <GroupList />
    </div>
  );
}
