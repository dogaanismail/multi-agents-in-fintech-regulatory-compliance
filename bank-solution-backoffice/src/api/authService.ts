import {apiClient} from './client';
import {CurrentUser} from '@/types';

export const authService = {
    getCurrentUser: async (): Promise<CurrentUser> => {
        const response = await apiClient.get<CurrentUser>('/me');
        return response.data;
    },
};
