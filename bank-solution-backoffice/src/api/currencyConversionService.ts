import { apiClient } from './client';
import { ExchangeRateResponse } from '@/types';

export const currencyConversionService = {
  getAllRates: async (): Promise<ExchangeRateResponse[]> => {
    const response = await apiClient.get<ExchangeRateResponse[]>('/exchange-rates');
    return response.data;
  },

  getRate: async (baseCurrency: string, targetCurrency: string): Promise<ExchangeRateResponse> => {
    const response = await apiClient.get<ExchangeRateResponse>(
      `/exchange-rates/${baseCurrency}/${targetCurrency}`
    );
    return response.data;
  },
};
