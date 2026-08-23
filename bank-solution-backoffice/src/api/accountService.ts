import { apiClient } from './client';
import {AccountResponse, AccountWalletResponse, OpenAccountRequest} from '@/types';

export const accountService = {
  getAccountById: async (accountId: string): Promise<AccountResponse> => {
    const response = await apiClient.get<AccountResponse>(
      `/accounts/${accountId}`
    );
    return response.data;
  },

  getAccountsByCustomerId: async (
    customerId: string
  ): Promise<AccountResponse[]> => {
    const response = await apiClient.get<AccountResponse[]>(
      `/accounts/customer/${customerId}`
    );
    return response.data;
  },

    getWalletsByAccountId: async (
    accountId: string
    ): Promise<AccountWalletResponse[]> => {
        const response = await apiClient.get<AccountWalletResponse[]>(
            `/accounts/${accountId}/wallets`
    );
    return response.data;
  },

  openAccount: async (request: OpenAccountRequest): Promise<AccountResponse> => {
    const response = await apiClient.post<AccountResponse>('/accounts/open-account', request);
    return response.data;
  },
};
