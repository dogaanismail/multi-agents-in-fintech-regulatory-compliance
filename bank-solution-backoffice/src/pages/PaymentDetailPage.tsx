import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { paymentService } from '@/api';
import { PaymentHistoryResponse } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Button, Input } from '@/components/common';
import { formatDate, formatCurrency, getStatusColor, getRiskLevelColor } from '@/utils/formatters';

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

      {/* Payment Information */}
      <Card title="Payment Information">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow label="Payment ID" value={payment.paymentId} />
          <InfoRow label="Reference Number" value={payment.referenceNumber} />
          <InfoRow
            label="Amount"
            value={formatCurrency(payment.amount, payment.currency)}
          />
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
              <Link to={`/customers/${payment.customerId}`} className="text-blue-600 hover:underline">
                {payment.customerId}
              </Link>
            }
          />
          <InfoRow
            label="Source Account"
            value={
              <Link to={`/accounts/${payment.sourceAccountId}`} className="text-blue-600 hover:underline">
                {payment.sourceAccountId}
              </Link>
            }
          />
          <InfoRow
            label="Destination Account"
            value={payment.destinationAccountId}
          />
        </div>
      </Card>

      {/* Risk Assessment */}
      {payment.riskScore !== null && (
        <Card title="Risk Assessment">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <InfoRow label="Risk Score" value={payment.riskScore?.toFixed(4) || 'N/A'} />
            <InfoRow label="Risk Action" value={payment.riskAction || 'N/A'} />
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
            <InfoRow label="Action" value={payment.marlAssessment.action} />
            <InfoRow
              label="Confidence"
              value={`${(payment.marlAssessment.confidence * 100).toFixed(2)}%`}
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
      <Card title="Payment Timeline">
        <div className="space-y-2">
          <TimelineItem label="Initiated" timestamp={payment.initiatedAt} />
          <TimelineItem label="Risk Check Requested" timestamp={payment.riskCheckRequestedAt} />
          <TimelineItem label="Risk Check Completed" timestamp={payment.riskCheckCompletedAt} />
          <TimelineItem label="Fraud Check Approved" timestamp={payment.fraudCheckApprovedAt} />
          <TimelineItem
            label="Manual Review Requested"
            timestamp={payment.manualReviewRequestedAt}
          />
          <TimelineItem
            label="Manual Review Approved"
            timestamp={payment.manualReviewApprovedAt}
          />
          <TimelineItem
            label="Manual Review Rejected"
            timestamp={payment.manualReviewRejectedAt}
          />
          <TimelineItem label="Account Charge Initiated" timestamp={payment.accountChargeInitiatedAt} />
          <TimelineItem label="Account Charged" timestamp={payment.accountChargedAt} />
          <TimelineItem label="Account Charge Failed" timestamp={payment.accountChargeFailedAt} />
          <TimelineItem label="Completed" timestamp={payment.completedAt} />
          <TimelineItem label="Blocked" timestamp={payment.blockedAt} />
        </div>
      </Card>

      {/* Decision Metadata */}
      {(payment.manualReviewedBy || payment.blockReason || payment.failureReason) && (
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
    </div>
  );
};

// Helper Components
const InfoRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700">{label}</label>
    <div className="mt-1 text-sm text-gray-900">{value}</div>
  </div>
);

const TimelineItem: React.FC<{ label: string; timestamp: string | null }> = ({
  label,
  timestamp,
}) => {
  if (!timestamp) return null;
  return (
    <div className="flex justify-between py-2 border-b border-gray-200">
      <span className="text-sm font-medium text-gray-700">{label}</span>
      <span className="text-sm text-gray-500">{formatDate(timestamp)}</span>
    </div>
  );
};

const AgentCard: React.FC<{ agent: any }> = ({ agent }) => (
  <div className="border border-gray-200 rounded-lg p-4">
    <h5 className="font-semibold mb-2">{agent.agentName}</h5>
    <div className="grid grid-cols-2 gap-2 text-sm">
      <div>
        <span className="text-gray-600">Suspicious:</span>{' '}
        <Badge className={agent.isSuspicious ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}>
          {agent.isSuspicious ? 'Yes' : 'No'}
        </Badge>
      </div>
      <div>
        <span className="text-gray-600">Probability:</span> {(agent.probability * 100).toFixed(2)}%
      </div>
      <div>
        <span className="text-gray-600">Risk Score:</span> {agent.riskScore.toFixed(4)}
      </div>
      <div>
        <span className="text-gray-600">Confidence:</span> {agent.confidence}
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
