'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';

import { useUpdateProfile } from '@/lib/hooks/use-profile';
import { profileSchema, type ProfileFormData } from '@/lib/validations/profile';
import type { User } from '@/lib/api/types';

interface ProfileFormProps {
  user: User;
}

/**
 * Форма редактирования профиля пользователя.
 */
export function ProfileForm({ user }: ProfileFormProps) {
  const updateProfile = useUpdateProfile();

  const form = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user.firstName,
      lastName: user.lastName ?? '',
    },
  });

  const onSubmit = (data: ProfileFormData) => {
    updateProfile.mutate({
      firstName: data.firstName,
      lastName: data.lastName || undefined,
    });
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="firstName"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Имя</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Иван"
                  data-testid="firstName-input"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="lastName"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Фамилия</FormLabel>
              <FormControl>
                <Input
                  {...field}
                  placeholder="Иванов"
                  data-testid="lastName-input"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div>
          <FormLabel>Email</FormLabel>
          <Input
            value={user.email}
            disabled
            className="mt-2 bg-muted"
            data-testid="email-input"
          />
          <p className="text-sm text-muted-foreground mt-1">
            Email изменить нельзя
          </p>
        </div>

        <Button
          type="submit"
          disabled={updateProfile.isPending || !form.formState.isDirty}
          data-testid="profile-submit"
        >
          {updateProfile.isPending ? 'Сохранение...' : 'Сохранить'}
        </Button>
      </form>
    </Form>
  );
}
