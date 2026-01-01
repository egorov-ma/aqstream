import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Organization, OrganizationRole } from '@/lib/api/types';

interface OrganizationState {
  currentOrganization: Organization | null;
  currentRole: OrganizationRole | null;
  setCurrentOrganization: (organization: Organization | null) => void;
  setCurrentRole: (role: OrganizationRole | null) => void;
  setOrganizationWithRole: (organization: Organization, role: OrganizationRole) => void;
  clear: () => void;
}

export const useOrganizationStore = create<OrganizationState>()(
  persist(
    (set) => ({
      currentOrganization: null,
      currentRole: null,

      setCurrentOrganization: (organization) =>
        set({ currentOrganization: organization, currentRole: null }),

      setCurrentRole: (role) => set({ currentRole: role }),

      setOrganizationWithRole: (organization, role) =>
        set({ currentOrganization: organization, currentRole: role }),

      clear: () => set({ currentOrganization: null, currentRole: null }),
    }),
    {
      name: 'organization-storage',
      partialize: (state) => ({
        currentOrganization: state.currentOrganization,
        currentRole: state.currentRole,
      }),
    }
  )
);
