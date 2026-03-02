import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { paymentService } from '@/api';
import { PaymentHistoryResponse } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Button, Input, CopyButton } from '@/components/common';
import { formatDate, formatCurrency, getStatusColor, getRiskLevelColor, getRiskActionColor } from '@/utils/formatters';

export const PaymentDetailPage: React.FC = () => {
  const { paymentId } = useParams<{ paymentId: string }>();
  const navigate = useNavigate();
  const { data: payment, loading, error, execute } = useApi<PaymentHistoryResponse>();
  const [showApproveModal, setShowApproveModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [approvedBy, setApprovedBy] = useState('');
  const [approvalNotes, setApprovalNotes] = useState('');
  const [rejectedBy, setRejectedBy] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const [showOverrideModal, setShowOverrideModal] = useState(false);
  const [overrideApprove, setOverrideApprove] = useState(true);
  const [overriddenBy, setOverriddenBy] = useState('');
  const [overrideReason, setOverrideReason] = useState('');

  useEffect(() => {
    if (paymentId) {
      execute(() => paymentService.getPaymentById(paymentId));
    }
  }, [paymentId]);

  const handleApprove = async () => {
    if (!paymentId || !approvedBy || !approvalNotes) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      setActionLoading(true);
      await paymentService.approveManualReview(paymentId, {
        paymentId,
        approvedBy,
        approvalNotes,
      });
      alert('Payment approved successfully');
      setShowApproveModal(false);
      // Reload payment
      execute(() => paymentService.getPaymentById(paymentId));
    } catch (err) {
      alert('Failed to approve payment: ' + (err as Error).message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!paymentId || !rejectedBy || !rejectionReason) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      setActionLoading(true);
      await paymentService.rejectManualReview(paymentId, {
        paymentId,
        rejectedBy,
        rejectionReason,
      });
      alert('Payment rejected successfully');
      setShowRejectModal(false);
      // Reload payment
      execute(() => paymentService.getPaymentById(paymentId));
    } catch (err) {
      alert('Failed to reject payment: ' + (err as Error).message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleOverride = async () => {
    if (!paymentId || !overriddenBy || !overrideReason) {
      alert('Please fill in all required fields');
      return;
    }

    try {
      setActionLoading(true);
      await paymentService.overrideDecision(paymentId, {
        overriddenBy,
        overrideReason,
        approvePayment: overrideApprove,
      });
      alert(`Decision override applied: payment ${overrideApprove ? 'approved' : 'rejected'} successfully`);
      setShowOverrideModal(false);
      execute(() => paymentService.getPaymentById(paymentId));
    } catch (err) {
      alert('Failed to override decision: ' + (err as Error).message);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-red-600 p-4 bg-red-50 rounded">
        Error loading payment: {error.message}
      </div>
    );
  }

  if (!payment) {
    return <div className="text-gray-500">Payment not found</div>;
  }

  const canApproveOrReject = payment.status === 'MANUAL_REVIEW_REQUIRED';
  const canOverride = payment.status === 'BLOCKED';

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Payment Details</h1>
        <Button variant="secondary" onClick={() => navigate('/payments')}>
          Back to Payments
        </Button>
      </div>

      {/* Manual Review Actions */}
      {canApproveOrReject && (
        <Card>
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-yellow-800 mb-3">
              ⚠️ Manual Review Required
            </h3>
            <p className="text-yellow-700 mb-4">
              This payment requires manual review by a compliance officer.
            </p>
            <div className="flex gap-3">
              <Button variant="success" onClick={() => setShowApproveModal(true)}>
                ✓ Approve Payment
              </Button>
              <Button variant="danger" onClick={() => setShowRejectModal(true)}>
                ✗ Reject Payment
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Decision Override Actions */}
      {canOverride && (
        <Card>
          <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
            <h3 className="text-lg font-semibold text-purple-800 mb-3">
              🔄 Decision Override
            </h3>
            <p className="text-purple-700 mb-1">
              Current status: <strong>{payment.status}</strong>. A compliance officer may override this terminal decision.
            </p>
            <p className="text-purple-600 text-sm mb-4">
              Override actions are recorded as strong training signals for the MARL system.
            </p>
            <Button variant="secondary" onClick={() => setShowOverrideModal(true)}>
              Override Decision
            </Button>
          </div>
        </Card>
      )}

      {/* Payment Information */}
      <Card title="Payment Information">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow label="Payment ID" value={<CopyButton text={payment.paymentId} />} />
          <InfoRow label="Reference Number" value={payment.referenceNumber} />
          <InfoRow
            label="From Currency"
            value={<span className="font-mono font-semibold">{payment.fromCurrency}</span>}
          />
          <InfoRow
            label="From Amount"
            value={formatCurrency(payment.amount, payment.fromCurrency)}
          />
          {payment.toCurrency && payment.toCurrency !== payment.fromCurrency ? (
            <>
              <InfoRow
                label="To Currency"
                value={<span className="font-mono font-semibold">{payment.toCurrency}</span>}
              />
              {payment.convertedAmount != null && (
                <InfoRow
                  label="Converted Amount"
                  value={formatCurrency(payment.convertedAmount, payment.toCurrency)}
                />
              )}
              {payment.appliedExchangeRate != null && (
                <InfoRow
                  label="Exchange Rate"
                  value={`1 ${payment.fromCurrency} = ${payment.appliedExchangeRate} ${payment.toCurrency}`}
                />
              )}
            </>
          ) : (
            <InfoRow
              label="To Currency"
              value={<span className="font-mono font-semibold">{payment.toCurrency || payment.fromCurrency}</span>}
            />
          )}
          <InfoRow label="Payment Type" value={payment.paymentType} />
          <InfoRow label="Description" value={payment.description || 'N/A'} />
          <InfoRow
            label="Status"
            value={<Badge className={getStatusColor(payment.status)}>{payment.status}</Badge>}
          />
          <InfoRow
            label="Fraud Status"
            value={
              <Badge className={getStatusColor(payment.fraudStatus)}>{payment.fraudStatus}</Badge>
            }
          />
          {payment.riskLevel && (
            <InfoRow
              label="Risk Level"
              value={
                <Badge className={getRiskLevelColor(payment.riskLevel)}>
                  {payment.riskLevel}
                </Badge>
              }
            />
          )}
        </div>
      </Card>

      {/* Customer & Account Info */}
      <Card title="Customer & Account Details">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow
            label="Customer ID"
            value={
              <div className="flex items-center gap-2">
                <Link to={`/customers/${payment.customerId}`} className="text-blue-600 hover:underline font-mono text-sm">
                  {payment.customerId}
                </Link>
                <CopyButton text={payment.customerId} hideLabel />
              </div>
            }
          />
          <InfoRow
            label="Source Account"
            value={
              <div className="flex items-center gap-2">
                <Link to={`/accounts/${payment.sourceAccountId}`} className="text-blue-600 hover:underline font-mono text-sm">
                  {payment.sourceAccountId}
                </Link>
                <CopyButton text={payment.sourceAccountId} hideLabel />
              </div>
            }
          />
          <InfoRow
            label="Destination Account"
            value={
              <div className="flex items-center gap-2">
                <Link to={`/accounts/${payment.destinationAccountId}`} className="text-blue-600 hover:underline font-mono text-sm">
                  {payment.destinationAccountId}
                </Link>
                <CopyButton text={payment.destinationAccountId} hideLabel />
              </div>
            }
          />
        </div>
      </Card>

      {/* Risk Assessment */}
      {payment.riskScore !== null && (
        <Card title="Risk Assessment">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <InfoRow label="Risk Score" value={payment.riskScore?.toFixed(4) || 'N/A'} />
            <InfoRow 
              label="Risk Action" 
              value={
                payment.riskAction ? (
                  <Badge className={getRiskActionColor(payment.riskAction)}>
                    {payment.riskAction}
                  </Badge>
                ) : 'N/A'
              } 
            />
            {payment.fraudIndicators && payment.fraudIndicators.length > 0 && (
              <div className="col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Fraud Indicators
                </label>
                <div className="flex flex-wrap gap-2">
                  {payment.fraudIndicators.map((indicator, idx) => (
                    <Badge key={idx} className="bg-red-100 text-red-800">
                      {indicator}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </div>
        </Card>
      )}

      {/* MARL Assessment */}
      {payment.marlAssessment && (
        <Card title="MARL Assessment">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <InfoRow 
              label="Action" 
              value={
                <Badge className={payment.marlAssessment.action === 'ALLOW' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}>
                  {payment.marlAssessment.action}
                </Badge>
              } 
            />
            <InfoRow
              label="Confidence"
              value={
                <Badge className={
                  payment.marlAssessment.confidence >= 0.8 ? 'bg-green-100 text-green-800' :
                  payment.marlAssessment.confidence >= 0.6 ? 'bg-yellow-100 text-yellow-800' :
                  'bg-red-100 text-red-800'
                }>
                  {`${(payment.marlAssessment.confidence * 100).toFixed(2)}%`}
                </Badge>
              }
            />
            <InfoRow
              label="MADDPG Q-Value"
              value={payment.marlAssessment.maddpgQValue.toFixed(4)}
            />
            <InfoRow label="Mode" value={payment.marlAssessment.mode} />
            <InfoRow
              label="Processing Time"
              value={`${payment.marlAssessment.processingTimeMs} ms`}
            />
          </div>

          {/* Agent Observations */}
          <div className="mt-6">
            <h4 className="font-semibold mb-4">Agent Observations</h4>
            <div className="space-y-4">
              {payment.marlAssessment.transactionAgentObservation && (
                <AgentCard agent={payment.marlAssessment.transactionAgentObservation} />
              )}
              {payment.marlAssessment.customerAgentObservation && (
                <AgentCard agent={payment.marlAssessment.customerAgentObservation} />
              )}
              {payment.marlAssessment.networkAgentObservation && (
                <AgentCard agent={payment.marlAssessment.networkAgentObservation} />
              )}
            </div>
          </div>
        </Card>
      )}

      {/* Timeline */}
      <Card title="🗓️ Payment Timeline">
        {(() => {
          // ── Duration formatter ────────────────────────────────────────────────
          const fmtDur = (ms: number): string => {
            if (ms < 0) return '?';
            if (ms < 1_000) return `${ms} ms`;
            if (ms < 60_000) return `${(ms / 1_000).toFixed(1)} s`;
            if (ms < 3_600_000) {
              const m = Math.floor(ms / 60_000);
              const s = Math.floor((ms % 60_000) / 1_000);
              return s > 0 ? `${m}m ${s}s` : `${m} min`;
            }
            const h = Math.floor(ms / 3_600_000);
            const m = Math.floor((ms % 3_600_000) / 60_000);
            return m > 0 ? `${h}h ${m}m` : `${h} h`;
          };

          const ALL_STEPS: {
            label: string;
            timestamp: string | null;
            icon: string;
            variant: 'default' | 'success' | 'warning' | 'error';
          }[] = [
            { label: 'Initiated',               timestamp: payment.initiatedAt,              icon: '🚀', variant: 'default'  },
            { label: 'Risk Check Requested',     timestamp: payment.riskCheckRequestedAt,     icon: '🔍', variant: 'default'  },
            { label: 'Risk Check Completed',     timestamp: payment.riskCheckCompletedAt,     icon: '📊', variant: 'default'  },
            { label: 'Fraud Check Approved',     timestamp: payment.fraudCheckApprovedAt,     icon: '🤖', variant: 'success'  },
            { label: 'Manual Review Requested',  timestamp: payment.manualReviewRequestedAt,  icon: '👁️', variant: 'warning'  },
            { label: 'Manual Review Approved',   timestamp: payment.manualReviewApprovedAt,   icon: '✅', variant: 'success'  },
            { label: 'Manual Review Rejected',   timestamp: payment.manualReviewRejectedAt,   icon: '❌', variant: 'error'    },
            { label: 'Account Charge Initiated', timestamp: payment.accountChargeInitiatedAt, icon: '💳', variant: 'default'  },
            { label: 'Account Charged',          timestamp: payment.accountChargedAt,         icon: '💰', variant: 'success'  },
            { label: 'Account Charge Failed',    timestamp: payment.accountChargeFailedAt,    icon: '⚠️', variant: 'error'    },
            { label: 'Completed',                timestamp: payment.completedAt,              icon: '🎉', variant: 'success'  },
            { label: 'Blocked',                  timestamp: payment.blockedAt,                icon: '🚫', variant: 'error'    },
          ];
          const steps = ALL_STEPS.filter((s) => s.timestamp !== null);
          if (steps.length === 0)
            return <p className="text-sm text-gray-400 italic">No timeline events yet.</p>;
          const dotCls: Record<string, string> = {
            default: 'bg-blue-100 text-blue-700 ring-blue-200',
            success: 'bg-green-100 text-green-700 ring-green-200',
            warning: 'bg-yellow-100 text-yellow-700 ring-yellow-200',
            error:   'bg-red-100   text-red-700   ring-red-200',
          };
          const lineCls: Record<string, string> = {
            default: 'bg-blue-200',
            success: 'bg-green-200',
            warning: 'bg-yellow-200',
            error:   'bg-red-200',
          };
          return (
            <div className="relative">
              {steps.map((step, i) => {
                const nextStep = steps[i + 1];
                const durMs = nextStep
                  ? new Date(nextStep.timestamp!).getTime() - new Date(step.timestamp!).getTime()
                  : null;
                return (
                  <div key={step.label} className="relative flex gap-4 pb-7 last:pb-0">
                    {/* Vertical connector line */}
                    {i < steps.length - 1 && (
                      <div
                        className={`absolute left-[13px] top-8 bottom-0 w-0.5 ${lineCls[step.variant]}`}
                      />
                    )}
                    {/* Dot */}
                    <div
                      className={`relative z-10 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full ring-2 ${dotCls[step.variant]}`}
                    >
                      <span className="text-xs leading-none">{step.icon}</span>
                    </div>
                    {/* Content */}
                    <div className="flex flex-1 flex-col min-w-0 pt-0.5">
                      <div className="flex items-start justify-between">
                        <span className="text-sm font-semibold text-gray-800">{step.label}</span>
                        <span className="text-xs text-gray-400 ml-6 whitespace-nowrap tabular-nums">
                          {formatDate(step.timestamp)}
                        </span>
                      </div>
                      {/* Duration to next step */}
                      {durMs !== null && (
                        <span className="mt-1.5 self-start text-[11px] px-2 py-0.5 rounded-full bg-gray-100 text-gray-500 border border-gray-200 tabular-nums">
                          ⏱ {fmtDur(durMs)} to next step
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          );
        })()}
      </Card>

      {/* Decision Metadata */}
      {(payment.manualReviewedBy || payment.blockReason || payment.failureReason || payment.decisionOverriddenBy) && (
        <Card title="Decision Metadata">
          <div className="space-y-2">
            {payment.manualReviewedBy && (
              <InfoRow label="Reviewed By" value={payment.manualReviewedBy} />
            )}
            {payment.manualReviewNotes && (
              <InfoRow label="Review Notes" value={payment.manualReviewNotes} />
            )}
            {payment.blockReason && <InfoRow label="Block Reason" value={payment.blockReason} />}
            {payment.failureReason && (
              <InfoRow label="Failure Reason" value={payment.failureReason} />
            )}
            {payment.decisionOverriddenBy && (
              <InfoRow label="Override By" value={payment.decisionOverriddenBy} />
            )}
            {payment.decisionOverrideReason && (
              <InfoRow label="Override Reason" value={payment.decisionOverrideReason} />
            )}
            {payment.decisionOverriddenAt && (
              <InfoRow label="Overridden At" value={formatDate(payment.decisionOverriddenAt)} />
            )}
          </div>
        </Card>
      )}

      {/* Approve Modal */}
      {showApproveModal && (
        <Modal
          title="Approve Payment"
          onClose={() => setShowApproveModal(false)}
          onConfirm={handleApprove}
          loading={actionLoading}
          confirmText="Approve"
          confirmVariant="success"
        >
          <Input
            label="Approved By (Your Name)"
            placeholder="Enter your name"
            value={approvedBy}
            onChange={(e) => setApprovedBy(e.target.value)}
            required
          />
          <Input
            label="Approval Notes"
            placeholder="Enter approval notes"
            value={approvalNotes}
            onChange={(e) => setApprovalNotes(e.target.value)}
            required
          />
        </Modal>
      )}

      {/* Reject Modal */}
      {showRejectModal && (
        <Modal
          title="Reject Payment"
          onClose={() => setShowRejectModal(false)}
          onConfirm={handleReject}
          loading={actionLoading}
          confirmText="Reject"
          confirmVariant="danger"
        >
          <Input
            label="Rejected By (Your Name)"
            placeholder="Enter your name"
            value={rejectedBy}
            onChange={(e) => setRejectedBy(e.target.value)}
            required
          />
          <Input
            label="Rejection Reason"
            placeholder="Enter rejection reason"
            value={rejectionReason}
            onChange={(e) => setRejectionReason(e.target.value)}
            required
          />
        </Modal>
      )}

      {/* Override Modal */}
      {showOverrideModal && (
        <Modal
          title="Override Terminal Decision"
          onClose={() => setShowOverrideModal(false)}
          onConfirm={handleOverride}
          loading={actionLoading}
          confirmText={overrideApprove ? 'Override — Approve' : 'Override — Reject'}
          confirmVariant={overrideApprove ? 'success' : 'danger'}
        >
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">Override Action</label>
            <div className="flex gap-3">
              <Button
                variant={overrideApprove ? 'success' : 'secondary'}
                onClick={() => setOverrideApprove(true)}
              >
                ✓ Approve Payment
              </Button>
              <Button
                variant={!overrideApprove ? 'danger' : 'secondary'}
                onClick={() => setOverrideApprove(false)}
              >
                ✗ Reject Payment
              </Button>
            </div>
          </div>
          <Input
            label="Override By (Your Name)"
            placeholder="Enter your name"
            value={overriddenBy}
            onChange={(e) => setOverriddenBy(e.target.value)}
            required
          />
          <Input
            label="Override Reason"
            placeholder="Enter the reason for overriding this decision"
            value={overrideReason}
            onChange={(e) => setOverrideReason(e.target.value)}
            required
          />
        </Modal>
      )}
    </div>
  );
};

// Helper Components
const InfoRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <label className="block text-sm font-bold text-gray-700">{label}</label>
    <div className="mt-1 text-sm text-gray-900">{value}</div>
  </div>
);

// TimelineItem replaced by inline steps renderer above

const AgentCard: React.FC<{ agent: any }> = ({ agent }) => (
  <div className="border border-gray-200 rounded-lg p-4">
    <h5 className="font-semibold mb-2">{agent.agentName}</h5>
    <div className="grid grid-cols-2 gap-2 text-sm">
      <div>
        <span className="font-bold text-gray-600">Suspicious:</span>{' '}
        <Badge className={agent.isSuspicious ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}>
          {agent.isSuspicious ? 'Yes' : 'No'}
        </Badge>
      </div>
      <div>
        <span className="font-bold text-gray-600">Probability:</span> {(agent.probability * 100).toFixed(2)}%
      </div>
      <div>
        <span className="font-bold text-gray-600">Risk Score:</span> {agent.riskScore.toFixed(4)}
      </div>
      <div>
        <span className="font-bold text-gray-600">Confidence:</span>{' '}
        <Badge className={
          agent.confidence === 'HIGH' ? 'bg-green-100 text-green-800' :
          agent.confidence === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
          'bg-red-100 text-red-800'
        }>
          {agent.confidence}
        </Badge>
      </div>
    </div>
  </div>
);

interface ModalProps {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
  onConfirm: () => void;
  loading?: boolean;
  confirmText?: string;
  confirmVariant?: 'primary' | 'secondary' | 'success' | 'danger';
}

const Modal: React.FC<ModalProps> = ({
  title,
  children,
  onClose,
  onConfirm,
  loading = false,
  confirmText = 'Confirm',
  confirmVariant = 'primary',
}) => (
  <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div className="bg-white rounded-lg p-6 max-w-md w-full">
      <h3 className="text-xl font-semibold mb-4">{title}</h3>
      <div className="mb-6">{children}</div>
      <div className="flex justify-end gap-3">
        <Button variant="secondary" onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Button variant={confirmVariant} onClick={onConfirm} disabled={loading}>
          {loading ? 'Processing...' : confirmText}
        </Button>
      </div>
    </div>
  </div>
);
