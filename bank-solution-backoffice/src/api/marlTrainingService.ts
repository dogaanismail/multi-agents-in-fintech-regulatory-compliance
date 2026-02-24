import { apiClient } from './client';
import {
  BufferStatsResponse,
  ExperienceEntry,
  ReplayBufferAggStats,
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

  getExperiences: async (limit = 50, offset = 0): Promise<ExperienceEntry[]> => {
    const response = await apiClient.get<ExperienceEntry[]>(
      `/marl/training/experiences?limit=${limit}&offset=${offset}`,
    );
    return response.data;
  },

  getExperienceStats: async (): Promise<ReplayBufferAggStats> => {
    const response = await apiClient.get<ReplayBufferAggStats>('/marl/training/experiences/stats');
    return response.data;
  },
};
