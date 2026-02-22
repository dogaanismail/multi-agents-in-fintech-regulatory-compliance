import { apiClient } from './client';
import { ConfigurationResponse, CreateConfigRequest, UpdateConfigRequest } from '@/types';

export const configurationService = {
  getAllConfigurations: async (): Promise<ConfigurationResponse[]> => {
    const response = await apiClient.get<ConfigurationResponse[]>('/configurations');
    return response.data;
  },

  getConfigurationById: async (id: string): Promise<ConfigurationResponse> => {
    const response = await apiClient.get<ConfigurationResponse>(`/configurations/${id}`);
    return response.data;
  },

  getConfigurationsByCategory: async (category: string): Promise<ConfigurationResponse[]> => {
    const response = await apiClient.get<ConfigurationResponse[]>(`/configurations/category/${category}`);
    return response.data;
  },

  createConfiguration: async (request: CreateConfigRequest): Promise<ConfigurationResponse> => {
    const response = await apiClient.post<ConfigurationResponse>('/configurations', request);
    return response.data;
  },

  updateConfiguration: async (id: string, request: UpdateConfigRequest): Promise<ConfigurationResponse> => {
    const response = await apiClient.put<ConfigurationResponse>(`/configurations/${id}`, request);
    return response.data;
  },

  deleteConfiguration: async (id: string): Promise<void> => {
    await apiClient.delete(`/configurations/${id}`);
  },
};
