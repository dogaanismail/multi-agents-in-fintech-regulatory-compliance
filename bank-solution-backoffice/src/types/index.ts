// TypeScript types for all backend DTOs

export interface PaymentHistoryResponse {
  paymentId: string;
  referenceNumber: string;
  // Payment Details
  customerId: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: string;
  paymentType: string;
  description: string;
  // Status Tracking
  status: string;
  fraudStatus: string;
  // Risk Assessment
  riskScore: number | null;
  riskLevel: string | null;
  riskAction: string | null;
  fraudIndicators: string[] | null;
  // MARL Assessment
  marlAssessment: MarlAssessmentDto | null;
  // Complete Lifecycle Timestamps
  initiatedAt: string;
  riskCheckRequestedAt: string | null;
  riskCheckCompletedAt: string | null;
  fraudCheckApprovedAt: string | null;
  manualReviewRequestedAt: string | null;
  manualReviewApprovedAt: string | null;
  manualReviewRejectedAt: string | null;
  accountChargeInitiatedAt: string | null;
  accountChargedAt: string | null;
  accountChargeFailedAt: string | null;
  completedAt: string | null;
  blockedAt: string | null;
  // Decision Metadata
  manualReviewedBy: string | null;
  manualReviewNotes: string | null;
  blockReason: string | null;
  failureReason: string | null;
  // Processing Metadata
  riskProcessingTimeMs: number | null;
  marlProcessingTimeMs: number | null;
  mlModelVersion: string | null;
  aggregateVersion: number;
  createdAt: string;
  updatedAt: string;
}

export interface MarlAssessmentDto {
  requestId: string;
  action: string;
  confidence: number;
  maddpgQValue: number;
  transactionAgentObservation: AgentObservationDto;
  customerAgentObservation: AgentObservationDto;
  networkAgentObservation: AgentObservationDto;
  agentContributions: Record<string, number>;
  processingTimeMs: number;
  mode: string;
}

export interface AgentObservationDto {
  agentName: string;
  isSuspicious: boolean;
  probability: number;
  riskScore: number;
  confidence: string;
  responseTimeMs: number;
}

export interface CustomerResponse {
  id: string;
  firstName: string;
  lastName: string;
  middleName: string | null;
  email: string;
  phoneNumber: string;
  dateOfBirth: string;
  nationality: string;
  customerType: 'INDIVIDUAL' | 'BUSINESS';
  customerStatus: string;
  address: AddressResponse;
  createdAt: string;
  updatedAt: string;
}

export interface AddressResponse {
  id: string;
  city: string;
  countryCode: string;
}

export interface AccountResponse {
  id: string;
  customerId: string;
  accountNumber: string;
  accountType: string;
  bankLocation: string;
  accountStatus: string;
  openingDate: string;
  closingDate: string | null;
  balances: BalanceResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface BalanceResponse {
  id: string;
  currency: string;
  availableBalance: number;
  pendingBalance: number;
  totalBalance: number;
}

export interface Page<T> {
  content: T[];
  pageable: Pageable;
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: Sort;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface Pageable {
  pageNumber: number;
  pageSize: number;
  sort: Sort;
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

export interface Sort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface ApproveManualReviewRequest {
  paymentId: string;
  approvedBy: string;
  approvalNotes: string;
}

export interface RejectManualReviewRequest {
  paymentId: string;
  rejectedBy: string;
  rejectionReason: string;
}

export interface ManualReviewResponse {
  paymentId: string;
  message: string;
  reviewedBy: string;
}
