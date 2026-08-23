import {apiClient} from './client';
import {
    CreateLedgerInternalAccountRequest,
    Currency,
    FundWalletRequest,
    LedgerInternalAccountResponse,
    LedgerPostingResponse,
    LedgerWalletResponse,
    TrialBalanceResponse,
} from '@/types';

export const ledgerService = {
    getInternalAccounts: async (currency?: Currency): Promise<LedgerInternalAccountResponse[]> => {
        const response = await apiClient.get<LedgerInternalAccountResponse[]>(
            '/ledger/internal-accounts',
            {params: currency ? {currency} : {}}
        );
        return response.data;
    },

    createInternalAccount: async (
        request: CreateLedgerInternalAccountRequest
    ): Promise<LedgerInternalAccountResponse> => {
        const response = await apiClient.post<LedgerInternalAccountResponse>(
            '/ledger/internal-accounts',
            request
        );
        return response.data;
    },

    getTrialBalance: async (currency: Currency): Promise<TrialBalanceResponse> => {
        const response = await apiClient.get<TrialBalanceResponse>(
            `/ledger/internal-accounts/trial-balance/${currency}`
        );
        return response.data;
    },

    getWalletsByAccountId: async (accountId: string): Promise<LedgerWalletResponse[]> => {
        const response = await apiClient.get<LedgerWalletResponse[]>(
            `/ledger/accounts/bank-account/${accountId}`
        );
        return response.data;
    },

    fundWallet: async (request: FundWalletRequest): Promise<LedgerPostingResponse[]> => {
        const response = await apiClient.post<LedgerPostingResponse[]>('/ledger/postings', request);
        return response.data;
    },

    getPostingsByClientTransactionId: async (
        clientTransactionId: string
    ): Promise<LedgerPostingResponse[]> => {
        const response = await apiClient.get<LedgerPostingResponse[]>(
            `/ledger/postings/${clientTransactionId}`
        );
        return response.data;
    },
};
