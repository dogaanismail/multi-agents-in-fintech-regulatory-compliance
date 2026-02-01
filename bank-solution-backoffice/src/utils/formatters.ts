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
    COMPLETED: 'bg-green-100 text-green-800',
    BLOCKED: 'bg-red-100 text-red-800',
    MANUAL_REVIEW_REQUIRED: 'bg-yellow-100 text-yellow-800',
    FRAUD_CHECK_PENDING: 'bg-blue-100 text-blue-800',
    FRAUD_CHECK_APPROVED: 'bg-green-100 text-green-800',
    INITIATED: 'bg-gray-100 text-gray-800',
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
