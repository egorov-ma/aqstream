import { apiClient } from './client';
import type { RegistrationStatus } from './types';

export interface CheckInInfo {
  registrationId: string;
  confirmationCode: string;
  eventId: string;
  eventTitle: string;
  eventStartsAt: string;
  ticketTypeName: string;
  firstName: string;
  lastName: string | null;
  email: string;
  status: RegistrationStatus;
  isCheckedIn: boolean;
  checkedInAt: string | null;
}

export interface CheckInResult {
  registrationId: string;
  confirmationCode: string;
  eventTitle: string;
  ticketTypeName: string;
  firstName: string;
  lastName: string | null;
  checkedInAt: string;
  message: string;
}

export const checkInApi = {
  /**
   * Получить информацию о регистрации по confirmation code.
   */
  getInfo: async (confirmationCode: string): Promise<CheckInInfo> => {
    const response = await apiClient.get<CheckInInfo>(
      `/api/v1/public/check-in/${confirmationCode}`
    );
    return response.data;
  },

  /**
   * Выполнить check-in.
   */
  confirm: async (confirmationCode: string): Promise<CheckInResult> => {
    const response = await apiClient.post<CheckInResult>(
      `/api/v1/public/check-in/${confirmationCode}`
    );
    return response.data;
  },
};
