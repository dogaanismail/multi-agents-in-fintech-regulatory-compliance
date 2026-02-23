import { apiClient } from './client';
import {
  BufferStatsResponse,
  TrainingRunResponse,
  TrainingStatusResponse,
  TriggerTrainingResponse,
} from '@/types';

export const marlTrainingService = {
  getStatus: async (): Promise<TrainingStatusResponse> => {
    const response = await apiClient.get<TrainingStatusResponse>('/marl/training/status');
    return response.data;
  },

  triggerTraining: async (): Promise<TriggerTrainingResponse> => {
    const response = await apiClient.post<TriggerTrainingResponse>('/marl/training/trigger');
    return response.data;
  },

  getHistory: async (limit = 20): Promise<TrainingRunResponse[]> => {
    const response = await apiClient.get<TrainingRunResponse[]>(
      `/marl/training/history?limit=${limit}`,
    );
    return response.data;
  },

  getBufferStats: async (): Promise<BufferStatsResponse> => {
    const response = await apiClient.get<BufferStatsResponse>('/marl/training/buffer');
    return response.data;
  },
};
