import { apiClient } from './client';
import { AccountResponse, BalanceResponse, OpenAccountRequest } from '@/types';

export const accountService = {
  // Get account by ID
  getAccountById: async (accountId: string): Promise<AccountResponse> => {
    const response = await apiClient.get<AccountResponse>(
      `/accounts/${accountId}`
    );
    return response.data;
  },

  // Get accounts by customer ID
  getAccountsByCustomerId: async (
    customerId: string
  ): Promise<AccountResponse[]> => {
    const response = await apiClient.get<AccountResponse[]>(
      `/accounts/customer/${customerId}`
    );
    return response.data;
  },

  // Get balances for an account
  getBalancesByAccountId: async (
    accountId: string
  ): Promise<BalanceResponse[]> => {
    const response = await apiClient.get<BalanceResponse[]>(
      `/accounts/${accountId}/balances`
    );
    return response.data;
  },

  // Get balance by currency
  getBalanceByCurrency: async (
    accountId: string,
    currency: string
  ): Promise<BalanceResponse> => {
    const response = await apiClient.get<BalanceResponse>(
      `/accounts/${accountId}/balances/${currency}`
    );
    return response.data;
  },

  // Open a new account
  openAccount: async (request: OpenAccountRequest): Promise<AccountResponse> => {
    const response = await apiClient.post<AccountResponse>('/accounts/open-account', request);
    return response.data;
  },
};
