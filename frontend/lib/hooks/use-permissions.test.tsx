import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { usePermissions } from './use-permissions';

// Мокаем organization store
const mockOrganizationState = {
  currentOrganization: null as { id: string; name: string } | null,
  currentRole: null as string | null,
};

vi.mock('@/lib/store/organization-store', () => ({
  useOrganizationStore: () => mockOrganizationState,
}));

describe('usePermissions', () => {
  beforeEach(() => {
    mockOrganizationState.currentOrganization = null;
    mockOrganizationState.currentRole = null;
  });

  describe('when no organization selected', () => {
    it('returns isOrganizer as false', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.isOrganizer).toBe(false);
      expect(result.current.isOwner).toBe(false);
      expect(result.current.isModerator).toBe(false);
    });

    it('denies all permissions', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.canCreateEvent).toBe(false);
      expect(result.current.canEditEvent).toBe(false);
      expect(result.current.canDeleteOrganization).toBe(false);
    });
  });

  describe('when user is OWNER', () => {
    beforeEach(() => {
      mockOrganizationState.currentOrganization = { id: '1', name: 'Test Org' };
      mockOrganizationState.currentRole = 'OWNER';
    });

    it('returns correct role flags', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.isOrganizer).toBe(true);
      expect(result.current.isOwner).toBe(true);
      expect(result.current.isModerator).toBe(false);
    });

    it('grants all owner permissions', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.canDeleteOrganization).toBe(true);
      expect(result.current.canTransferOwnership).toBe(true);
      expect(result.current.canAssignRoles).toBe(true);
      expect(result.current.canDeleteGroups).toBe(true);
    });

    it('grants all organizer permissions', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.canCreateEvent).toBe(true);
      expect(result.current.canEditEvent).toBe(true);
      expect(result.current.canCancelEvent).toBe(true);
      expect(result.current.canCheckIn).toBe(true);
      expect(result.current.canViewRegistrations).toBe(true);
      expect(result.current.canViewAnalytics).toBe(true);
      expect(result.current.canExportData).toBe(true);
    });
  });

  describe('when user is MODERATOR', () => {
    beforeEach(() => {
      mockOrganizationState.currentOrganization = { id: '1', name: 'Test Org' };
      mockOrganizationState.currentRole = 'MODERATOR';
    });

    it('returns correct role flags', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.isOrganizer).toBe(true);
      expect(result.current.isOwner).toBe(false);
      expect(result.current.isModerator).toBe(true);
    });

    it('denies owner-only permissions', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.canDeleteOrganization).toBe(false);
      expect(result.current.canTransferOwnership).toBe(false);
      expect(result.current.canAssignRoles).toBe(false);
      expect(result.current.canDeleteGroups).toBe(false);
    });

    it('grants organizer permissions', () => {
      const { result } = renderHook(() => usePermissions());

      expect(result.current.canCreateEvent).toBe(true);
      expect(result.current.canEditEvent).toBe(true);
      expect(result.current.canCancelEvent).toBe(true);
      expect(result.current.canCheckIn).toBe(true);
    });
  });
});
