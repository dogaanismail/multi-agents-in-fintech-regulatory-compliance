// TypeScript types for all backend DTOs

export interface PaymentHistoryResponse {
  paymentId: string;
  referenceNumber: string;
  // Payment Details
  customerId: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  fromCurrency: string;
  toCurrency: string;
  convertedAmount: number | null;
  appliedExchangeRate: number | null;
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
    ledgerAuthorisationInitiatedAt: string | null;
    ledgerAuthorisedAt: string | null;
    ledgerSettlementInitiatedAt: string | null;
    ledgerSettledAt: string | null;
    ledgerReleaseInitiatedAt: string | null;
    ledgerReleasedAt: string | null;
  completedAt: string | null;
  blockedAt: string | null;
  // Decision Metadata
  manualReviewedBy: string | null;
  manualReviewNotes: string | null;
  blockReason: string | null;
  failureReason: string | null;
  // Decision Override Metadata
  decisionOverriddenBy: string | null;
  decisionOverrideReason: string | null;
  decisionOverriddenAt: string | null;
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
    featureContributions: FeatureContributionDto[] | null;
    shapBaseValue: number | null;
}

export interface FeatureContributionDto {
    feature: string;
    value: string;
    shapValue: number;
    direction: 'INCREASES_RISK' | 'DECREASES_RISK' | 'NO_IMPACT';
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
    wallets: AccountWalletResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface AccountWalletResponse {
  id: string;
    ledgerAccountId: string;
  currency: string;
    walletStatus: string;
    balance: number;
  availableBalance: number;
    primary: boolean;
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

export interface OverrideDecisionRequest {
  overriddenBy: string;
  overrideReason: string;
  approvePayment: boolean;
}

export interface OverrideDecisionResponse {
  paymentId: string;
  message: string;
  overriddenBy: string;
  newStatus: string;
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

// ─── MARL Replay Buffer ──────────────────────────────────────────────────────

export interface ExperienceEntry {
  id: string;
  payment_id: string;
  marl_action: 'ALLOW' | 'BLOCK' | 'REVIEW' | string;
  marl_confidence: number;
  marl_q_value: number;
  mean_risk_score: number;
  automated_reward: number;
  manual_reward: number | null;
  effective_reward: number;
  reward_source: 'automated' | 'manual_review' | string;
  is_used_in_training: boolean;
  training_run_id: string | null;
  created_at: string;
  updated_at: string;
}

export interface ReplayBufferAggStats {
  total_experiences: number;
  manual_review_count: number;
  automated_count: number;
  used_in_training_count: number;
  avg_effective_reward: number | null;
  avg_confidence: number | null;
  avg_risk_score: number | null;
  action_counts: Record<string, number>;
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
export type FixedSide = 'SELL' | 'BUY';

export interface CreatePaymentRequest {
  customerId: string;
  sourceAccountId?: string;
  destinationAccountId?: string;
  amount: number;
  fromCurrency: Currency;
  toCurrency: Currency;
  paymentType: PaymentType;
    fixedSide?: FixedSide;
  description?: string;
}

export interface CreatePaymentResponse {
  id: string;                         
  customerId: string;
  sourceAccountId: string | null;
  destinationAccountId: string | null;
  amount: number;
  fromCurrency: string;
  paymentType: string;
  description: string | null;
  convertedAmount: number | null;
  toCurrency: string;
  appliedExchangeRate: number | null;
  createdAt: string;
  message: string;
}

// ─── Ledger ───────────────────────────────────────────────────────────────────

export type LedgerInternalAccountType =
    | 'INBOUND_CLEARING'
    | 'OUTBOUND_CLEARING'
    | 'FEES_INCOME'
    | 'SUSPENSE'
    | 'FX_POSITION';

export interface LedgerInternalAccountResponse {
    ledgerAccountId: string;
    accountType: LedgerInternalAccountType;
    currency: Currency;
    creditsPosted: number;
    creditsPending: number;
    debitsPosted: number;
    debitsPending: number;
    netBalance: number;
    createdAt: string;
}

export interface TrialBalanceResponse {
    currency: Currency;
    internalAccountsNet: number;
    customerWalletsNet: number;
    net: number;
    balanced: boolean;
    internalAccounts: LedgerInternalAccountResponse[];
}

export interface CreateLedgerInternalAccountRequest {
    accountType: LedgerInternalAccountType;
    currency: Currency;
}

export interface LedgerWalletResponse {
    ledgerAccountId: string;
    accountId: string;
    accountType: string;
    currency: Currency;
    creditsPosted: number;
    creditsPending: number;
    debitsPosted: number;
    debitsPending: number;
    availableBalance: number;
    createdAt: string;
}

export interface LedgerPostingResponse {
    transferId: string;
    clientTransactionId: string;
    postingInstructionType: string;
    debitAccountId: string | null;
    creditAccountId: string | null;
    amount: number | null;
    currency: string | null;
    pendingTransferId: string | null;
    createdAt: string | null;
}

export interface FundWalletRequest {
    clientTransactionId: string;
    inboundHardSettlement: {
        amount: number;
        currency: Currency;
        customerAccountId: string;
    };
}

// ─── Exchange Rates ───────────────────────────────────────────────────────────

export interface ExchangeRateResponse {
  currencyPair: string;
  rate: number;
  fetchedAt: string;
}
