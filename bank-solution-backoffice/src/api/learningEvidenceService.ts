import {apiClient} from './client';
import {
    LearningCurvePoint,
    LearningReceipt,
    LearningSummaryResponse,
} from '@/types';

export const learningEvidenceService = {
    getSummary: async (): Promise<LearningSummaryResponse> => {
        const response = await apiClient.get<LearningSummaryResponse>('/marl/learning/summary');
        return response.data;
    },

    getCurve: async (limit = 100): Promise<LearningCurvePoint[]> => {
        const response = await apiClient.get<{ points: LearningCurvePoint[] }>(
            `/marl/learning/curve?limit=${limit}`,
        );
        return response.data.points;
    },

    getReceipts: async (limit = 50): Promise<LearningReceipt[]> => {
        const response = await apiClient.get<{ receipts: LearningReceipt[] }>(
            `/marl/learning/receipts?limit=${limit}`,
        );
        return response.data.receipts;
    },
};
