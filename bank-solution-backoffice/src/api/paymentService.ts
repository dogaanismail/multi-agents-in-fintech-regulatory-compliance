import { apiClient } from './client';
import {
  PaymentHistoryResponse,
  Page,
  ApproveManualReviewRequest,
  RejectManualReviewRequest,
  ManualReviewResponse,
  CreatePaymentRequest,
  CreatePaymentResponse,
} from '@/types';

export const paymentService = {
  // Get payment by ID
  getPaymentById: async (paymentId: string): Promise<PaymentHistoryResponse> => {
    const response = await apiClient.get<PaymentHistoryResponse>(
      `/payment-history/${paymentId}`
    );
    return response.data;
  },

  // Get all payments with pagination
  getAllPayments: async (
    page: number = 0,
    size: number = 20
  ): Promise<Page<PaymentHistoryResponse>> => {
    const response = await apiClient.get<Page<PaymentHistoryResponse>>(
      '/payment-history',
      {
        params: { page, size, sort: 'createdAt,desc' },
      }
    );
    return response.data;
  },

  // Get payments by customer ID
  getPaymentsByCustomerId: async (
    customerId: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<PaymentHistoryResponse>> => {
    const response = await apiClient.get<Page<PaymentHistoryResponse>>(
      `/payment-history/customer/${customerId}`,
      {
        params: { page, size, sort: 'createdAt,desc' },
      }
    );
    return response.data;
  },

  // Get payments by status
  getPaymentsByStatus: async (
    status: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<PaymentHistoryResponse>> => {
    const response = await apiClient.get<Page<PaymentHistoryResponse>>(
      `/payment-history/status/${status}`,
      {
        params: { page, size, sort: 'createdAt,desc' },
      }
    );
    return response.data;
  },

  // Get payments by fraud status
  getPaymentsByFraudStatus: async (
    fraudStatus: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<PaymentHistoryResponse>> => {
    const response = await apiClient.get<Page<PaymentHistoryResponse>>(
      `/payment-history/fraud-status/${fraudStatus}`,
      {
        params: { page, size, sort: 'createdAt,desc' },
      }
    );
    return response.data;
  },

  // Get payments by risk level
  getPaymentsByRiskLevel: async (
    riskLevel: string,
    page: number = 0,
    size: number = 20
  ): Promise<Page<PaymentHistoryResponse>> => {
    const response = await apiClient.get<Page<PaymentHistoryResponse>>(
      `/payment-history/risk-level/${riskLevel}`,
      {
        params: { page, size, sort: 'createdAt,desc' },
      }
    );
    return response.data;
  },

  // Approve manual review
  approveManualReview: async (
    paymentId: string,
    request: ApproveManualReviewRequest
  ): Promise<ManualReviewResponse> => {
    const response = await apiClient.post<ManualReviewResponse>(
      `/payments/${paymentId}/manual-review/approve`,
      request
    );
    return response.data;
  },

  // Reject manual review
  rejectManualReview: async (
    paymentId: string,
    request: RejectManualReviewRequest
  ): Promise<ManualReviewResponse> => {
    const response = await apiClient.post<ManualReviewResponse>(
      `/payments/${paymentId}/manual-review/reject`,
      request
    );
    return response.data;
  },

  // Create / request a new payment
  createPayment: async (request: CreatePaymentRequest): Promise<CreatePaymentResponse> => {
    const response = await apiClient.post<CreatePaymentResponse>('/payments/request', request);
    return response.data;
  },
};
