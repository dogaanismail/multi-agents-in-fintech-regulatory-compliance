import { apiClient } from './client';
import { CustomerResponse, Page, CreateCustomerRequest } from '@/types';

export const customerService = {
  // Get all customers
  getAllCustomers: async (): Promise<CustomerResponse[]> => {
    const response = await apiClient.get<Page<CustomerResponse>>('/customers');
    return response.data.content;
  },

  // Get customer by ID
  getCustomerById: async (customerId: string): Promise<CustomerResponse> => {
    const response = await apiClient.get<CustomerResponse>(
      `/customers/${customerId}`
    );
    return response.data;
  },

  // Create a new customer
  createCustomer: async (request: CreateCustomerRequest): Promise<CustomerResponse> => {
    const response = await apiClient.post<CustomerResponse>('/customers', request);
    return response.data;
  },
};
