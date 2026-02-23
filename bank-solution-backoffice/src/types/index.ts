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

export type ConfigCategory = 'OFFLINE_RETRAINING' | 'AUTO_REWARD' | 'MANUAL_REWARD' | 'ESCALATION' | 'AGENT_BEHAVIOR';
export type ConfigType = 'STRING' | 'FLOAT' | 'INTEGER' | 'BOOLEAN';

export interface ConfigAuditLogResponse {
  id: string;
  configId: string;
  configKey: string;
  oldValue: string | null;
  newValue: string | null;
  changeType: 'CREATED' | 'UPDATED' | 'DELETED';
  changedBy: string;
  createdAt: string;
}

export interface ConfigurationResponse {
  id: string;
  configKey: string;
  configValue: string;
  configType: ConfigType;
  category: ConfigCategory;
  description: string | null;
  defaultValue: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateConfigRequest {
  configKey: string;
  configValue: string;
  configType: ConfigType;
  category: ConfigCategory;
  description?: string;
  defaultValue: string;
}

export interface UpdateConfigRequest {
  configValue: string;
  configType: ConfigType;
  description?: string;
}

// ─── MARL Training ────────────────────────────────────────────────────────────

export interface TrainingStatusResponse {
  scheduler_running: boolean;
  is_training: boolean;
  training_interval_seconds: number;
  min_experiences_required: number;
  unused_experiences: number;
  total_experiences: number;
  last_training_run_id: string | null;
  last_training_at: string | null;
  total_training_runs: number;
  total_experiences_trained: number;
}

export interface TriggerTrainingResponse {
  triggered: boolean;
  reason: string | null;
  available_experiences: number | null;
  batch_size: number | null;
}

export interface TrainingRunResponse {
  id: string;
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED' | string;
  experiences_count: number;
  train_steps_completed: number;
  batch_size: number;
  critic_loss: number | null;
  actor_transaction_loss: number | null;
  actor_customer_loss: number | null;
  actor_network_loss: number | null;
  model_saved: boolean;
  error_message: string | null;
  started_at: string;
  completed_at: string | null;
}

export interface BufferStatsResponse {
  total_experiences: number;
  unused_experiences: number;
  used_experiences: number;
}

// ─── Customer Creation ────────────────────────────────────────────────────────

export interface AddressRequest {
  city: string;
  countryCode: string;
}

export interface CreateCustomerRequest {
  firstName: string;
  lastName: string;
  middleName?: string;
  email: string;
  phoneNumber: string;
  dateOfBirth: string; // ISO date: YYYY-MM-DD
  nationality: string; // 2-char country code
  customerType: 'INDIVIDUAL' | 'BUSINESS';
  address: AddressRequest;
}

// ─── Account Creation ─────────────────────────────────────────────────────────

export type AccountType = 'CHECKING' | 'SAVINGS' | 'BUSINESS';
export type BankLocation = 'AE' | 'AL' | 'AT' | 'CH' | 'DE' | 'ES' | 'FR' | 'GB' | 'IN' | 'IT' | 'JP' | 'MA' | 'MX' | 'NG' | 'NL' | 'PK' | 'TR' | 'US';
export type Currency = 'AED' | 'ALL' | 'CHF' | 'EUR' | 'GBP' | 'INR' | 'JPY' | 'MAD' | 'MXN' | 'NGN' | 'PKR' | 'TRY' | 'USD';

export interface OpenAccountRequest {
  customerId: string;
  accountType: AccountType;
  bankLocation: BankLocation;
  currencies: Currency[];
}

// ─── Payment Creation ─────────────────────────────────────────────────────────

export type PaymentType = 'TRANSFER_IN' | 'TRANSFER_OUT' | 'DEPOSIT' | 'WITHDRAWAL';

export interface CreatePaymentRequest {
  customerId: string;
  sourceAccountId?: string;
  destinationAccountId?: string;
  amount: number;
  currency: Currency;
  paymentType: PaymentType;
  description?: string;
}

export interface CreatePaymentResponse {
  id: string;                         
  customerId: string;
  sourceAccountId: string | null;
  destinationAccountId: string | null;
  amount: number;
  currency: string;
  paymentType: string;
  description: string | null;
  createdAt: string;
  message: string;
}
