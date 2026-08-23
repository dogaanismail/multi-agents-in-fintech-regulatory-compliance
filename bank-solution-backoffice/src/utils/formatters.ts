export function formatDate(date: string | null | undefined): string {
  if (!date) return 'N/A';
  return new Date(date).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatCurrency(amount: number, currency: string): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency || 'USD',
  }).format(amount);
}

export function getStatusColor(status: string): string {
  const statusColors: Record<string, string> = {
    INITIATED: 'bg-gray-100 text-gray-800',
      AUTHORISATION_PENDING: 'bg-blue-100 text-blue-800',
      AUTHORISED: 'bg-indigo-100 text-indigo-800',
      AUTHORISATION_DECLINED: 'bg-red-100 text-red-800',
    FRAUD_CHECK_PENDING: 'bg-blue-100 text-blue-800',
    FRAUD_CHECK_APPROVED: 'bg-green-100 text-green-800',
    FRAUD_CHECK_FAILED: 'bg-red-100 text-red-800',
    MANUAL_REVIEW_REQUIRED: 'bg-yellow-100 text-yellow-800',
      SETTLEMENT_PENDING: 'bg-blue-100 text-blue-800',
      SETTLED: 'bg-green-100 text-green-800',
      RELEASE_PENDING: 'bg-orange-100 text-orange-800',
      RELEASED: 'bg-orange-100 text-orange-800',
    COMPLETED: 'bg-green-100 text-green-800',
    BLOCKED: 'bg-red-100 text-red-800',
    FAILED: 'bg-red-100 text-red-800',
      OVERRIDE_APPROVED: 'bg-purple-100 text-purple-800',
      OVERRIDE_REJECTED: 'bg-purple-100 text-purple-800',
    PENDING: 'bg-blue-100 text-blue-800',
    APPROVED: 'bg-green-100 text-green-800',
    REVIEW_REQUIRED: 'bg-yellow-100 text-yellow-800',
  };
  return statusColors[status] || 'bg-gray-100 text-gray-800';
}

export function getRiskLevelColor(riskLevel: string | null): string {
  if (!riskLevel) return 'bg-gray-100 text-gray-800';
  const colors: Record<string, string> = {
    LOW: 'bg-green-100 text-green-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-orange-100 text-orange-800',
    CRITICAL: 'bg-red-100 text-red-800',
  };
  return colors[riskLevel] || 'bg-gray-100 text-gray-800';
}

export function getRiskActionColor(riskAction: string | null): string {
  if (!riskAction) return 'bg-gray-100 text-gray-800';
  const colors: Record<string, string> = {
    PROCEED: 'bg-green-100 text-green-800',
    ESCALATE: 'bg-yellow-100 text-yellow-800',
    BLOCK: 'bg-red-100 text-red-800',
  };
  return colors[riskAction] || 'bg-gray-100 text-gray-800';
}
