import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Organization } from '@/lib/api/types';

interface OrganizationState {
  currentOrganization: Organization | null;
  setCurrentOrganization: (organization: Organization | null) => void;
  clear: () => void;
}

export const useOrganizationStore = create<OrganizationState>()(
  persist(
    (set) => ({
      currentOrganization: null,

      setCurrentOrganization: (organization) => set({ currentOrganization: organization }),

      clear: () => set({ currentOrganization: null }),
    }),
    {
      name: 'organization-storage',
      partialize: (state) => ({
        currentOrganization: state.currentOrganization,
      }),
    }
  )
);
