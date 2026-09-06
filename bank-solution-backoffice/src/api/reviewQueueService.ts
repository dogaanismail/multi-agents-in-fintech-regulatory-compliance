import {apiClient} from './client';
import {ReviewQueueResponse} from '@/types';

export const reviewQueueService = {
    getQueue: async (limit = 200): Promise<ReviewQueueResponse> => {
        const response = await apiClient.get<ReviewQueueResponse>(
            `/marl/review-queue?limit=${limit}`,
        );
        return response.data;
    },
};
