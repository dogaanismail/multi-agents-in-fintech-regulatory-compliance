import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { paymentService } from '@/api';
import { PaymentHistoryResponse, Page } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Input, Select, Button } from '@/components/common';
import { formatDate, formatCurrency, getStatusColor, getRiskLevelColor } from '@/utils/formatters';

const PAYMENT_TYPE_META: Record<string, { icon: string; label: string; color: string }> = {
  TRANSFER_OUT: { icon: '⬆️', label: 'Transfer Out', color: 'bg-blue-100 text-blue-800' },
  TRANSFER_IN:  { icon: '⬇️', label: 'Transfer In',  color: 'bg-green-100 text-green-800' },
  DEPOSIT:      { icon: '💰', label: 'Deposit',       color: 'bg-emerald-100 text-emerald-800' },
  WITHDRAWAL:   { icon: '🏧', label: 'Withdrawal',    color: 'bg-orange-100 text-orange-800' },
};

// ─── Page ─────────────────────────────────────────────────────────────────────

export const PaymentListPage: React.FC = () => {
  const { data: paymentsData, loading, error, execute } = useApi<Page<PaymentHistoryResponse>>();
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState({
    paymentId: '',
    customerId: '',
    accountId: searchParams.get('accountId') || '',
    status: '',
    fraudStatus: '',
    riskLevel: '',
  });
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;

  useEffect(() => {
    loadPayments();
  }, [currentPage]);

  const loadPayments = async () => {
    // Determine which API to call based on filters
    if (filters.paymentId) {
      // For payment ID, get single payment and wrap in page structure
      try {
        const payment = await paymentService.getPaymentById(filters.paymentId);
        await execute(() => Promise.resolve({
          content: [payment],
          pageable: {
            pageNumber: 0,
            pageSize: 1,
            sort: { empty: true, sorted: false, unsorted: true },
            offset: 0,
            paged: true,
            unpaged: false
          },
          totalPages: 1,
          totalElements: 1,
          last: true,
          size: 1,
          number: 0,
          sort: { empty: true, sorted: false, unsorted: true },
          numberOfElements: 1,
          first: true,
          empty: false,
        } as Page<PaymentHistoryResponse>));
      } catch (err) {
        await execute(() => Promise.resolve({
          content: [],
          pageable: {
            pageNumber: 0,
            pageSize: 0,
            sort: { empty: true, sorted: false, unsorted: true },
            offset: 0,
            paged: true,
            unpaged: false
          },
          totalPages: 0,
          totalElements: 0,
          last: true,
          size: 0,
          number: 0,
          sort: { empty: true, sorted: false, unsorted: true },
          numberOfElements: 0,
          first: true,
          empty: true,
        } as Page<PaymentHistoryResponse>));
      }
    } else if (filters.accountId) {
      // Fetch a broad set and filter client-side by source or destination account
      try {
        const all = await paymentService.getAllPayments(0, 200);
        const id = filters.accountId.trim();
        const filtered = all.content.filter(
          (p) => p.sourceAccountId === id || p.destinationAccountId === id
        );
        await execute(() => Promise.resolve({
          content: filtered,
          pageable: { pageNumber: 0, pageSize: filtered.length, sort: { empty: true, sorted: false, unsorted: true }, offset: 0, paged: true, unpaged: false },
          totalPages: 1, totalElements: filtered.length, last: true, size: filtered.length,
          number: 0, sort: { empty: true, sorted: false, unsorted: true },
          numberOfElements: filtered.length, first: true, empty: filtered.length === 0,
        } as Page<PaymentHistoryResponse>));
      } catch {
        await execute(() => paymentService.getAllPayments(currentPage, pageSize));
      }
    } else if (filters.customerId) {
      await execute(() =>
        paymentService.getPaymentsByCustomerId(filters.customerId, currentPage, pageSize)
      );
    } else if (filters.status) {
      await execute(() =>
        paymentService.getPaymentsByStatus(filters.status, currentPage, pageSize)
      );
    } else if (filters.fraudStatus) {
      await execute(() =>
        paymentService.getPaymentsByFraudStatus(filters.fraudStatus, currentPage, pageSize)
      );
    } else if (filters.riskLevel) {
      await execute(() =>
        paymentService.getPaymentsByRiskLevel(filters.riskLevel, currentPage, pageSize)
      );
    } else {
      await execute(() => paymentService.getAllPayments(currentPage, pageSize));
    }
  };

  const handleFilterChange = (key: string, value: string) => {
    setFilters({ ...filters, [key]: value });
    setCurrentPage(0);
  };

  const handleApplyFilters = () => {
    setCurrentPage(0);
    loadPayments();
  };

  const handleClearFilters = () => {
    setFilters({
      paymentId: '',
      customerId: '',
      accountId: '',
      status: '',
      fraudStatus: '',
      riskLevel: '',
    });
    setCurrentPage(0);
  };

  const payments = paymentsData?.content || [];
  const totalPages = paymentsData?.totalPages || 0;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Payments</h1>
        <Link
          to="/payments/create"
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors shadow-sm"
        >
          <span>＋</span> New Payment
        </Link>
      </div>

      {/* Filters */}
      <Card title="Filters">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Input
            label="Payment ID"
            placeholder="UUID"
            value={filters.paymentId}
            onChange={(e) => handleFilterChange('paymentId', e.target.value)}
          />
          <Input
            label="Customer ID"
            placeholder="UUID"
            value={filters.customerId}
            onChange={(e) => handleFilterChange('customerId', e.target.value)}
          />
          <Input
            label="Account ID"
            placeholder="Source or destination account UUID"
            value={filters.accountId}
            onChange={(e) => handleFilterChange('accountId', e.target.value)}
          />
          <Select
            label="Status"
            value={filters.status}
            onChange={(e) => handleFilterChange('status', e.target.value)}
            options={[
              { value: '', label: 'All Statuses' },
              { value: 'INITIATED', label: 'Initiated' },
              { value: 'FRAUD_CHECK_PENDING', label: 'Fraud Check Pending' },
              { value: 'FRAUD_CHECK_APPROVED', label: 'Fraud Check Approved' },
              { value: 'FRAUD_CHECK_FAILED', label: 'Fraud Check Failed' },
              { value: 'MANUAL_REVIEW_REQUIRED', label: 'Manual Review Required' },
              { value: 'ACCOUNT_CHARGE_PENDING', label: 'Account Charge Pending' },
              { value: 'ACCOUNT_CHARGED', label: 'Account Charged' },
              { value: 'COMPLETED', label: 'Completed' },
              { value: 'BLOCKED', label: 'Blocked' },
              { value: 'FAILED', label: 'Failed' },
            ]}
          />
          <Select
            label="Fraud Status"
            value={filters.fraudStatus}
            onChange={(e) => handleFilterChange('fraudStatus', e.target.value)}
            options={[
              { value: '', label: 'All Fraud Statuses' },
              { value: 'PENDING', label: 'Pending' },
              { value: 'APPROVED', label: 'Approved' },
              { value: 'REVIEW_REQUIRED', label: 'Review Required' },
              { value: 'BLOCKED', label: 'Blocked' },
              { value: 'FAILED', label: 'Failed' },
            ]}
          />
          <Select
            label="Risk Level"
            value={filters.riskLevel}
            onChange={(e) => handleFilterChange('riskLevel', e.target.value)}
            options={[
              { value: '', label: 'All Risk Levels' },
              { value: 'LOW', label: 'Low' },
              { value: 'MEDIUM', label: 'Medium' },
              { value: 'HIGH', label: 'High' },
              { value: 'CRITICAL', label: 'Critical' },
            ]}
          />
        </div>
        <div className="flex gap-2 mt-4">
          <Button onClick={handleApplyFilters}>Apply Filters</Button>
          <Button variant="secondary" onClick={handleClearFilters}>
            Clear Filters
          </Button>
        </div>
      </Card>

      {/* Payments Table */}
      <Card>
        {loading && <LoadingSpinner />}
        {error && (
          <div className="text-red-600 p-4 bg-red-50 rounded">
            Error loading payments: {error.message}
          </div>
        )}
        {!loading && !error && payments.length === 0 && (
          <div className="text-gray-500 text-center py-8">No payments found</div>
        )}
        {!loading && !error && payments.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Payment ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Amount
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      From
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      To
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Fraud Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Risk Level
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {payments.map((payment) => (
                    <tr key={payment.paymentId} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <Link
                            to={`/payments/${payment.paymentId}`}
                            className="text-sm font-mono text-blue-600 hover:text-blue-800 hover:underline"
                            title={payment.paymentId}
                          >
                            {payment.paymentId.substring(0, 8)}…
                          </Link>
                          <button
                            type="button"
                            title="Copy full Payment ID"
                            onClick={() => navigator.clipboard.writeText(payment.paymentId)}
                            className="text-gray-400 hover:text-gray-700 transition-colors text-xs leading-none"
                          >
                            📋
                          </button>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {(() => {
                          const meta = PAYMENT_TYPE_META[payment.paymentType];
                          return meta ? (
                            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${meta.color}`}>
                              {meta.icon} {meta.label}
                            </span>
                          ) : (
                            <span className="text-xs text-gray-500">{payment.paymentType}</span>
                          );
                        })()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {formatCurrency(payment.amount, payment.fromCurrency)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-700 font-mono">
                          {payment.fromCurrency}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {payment.toCurrency && payment.toCurrency !== payment.fromCurrency ? (
                          <div className="flex flex-col gap-0.5">
                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-amber-100 text-amber-700 font-mono w-fit">
                              {payment.toCurrency}
                            </span>
                            {payment.convertedAmount != null && (
                              <span className="text-xs text-amber-600 whitespace-nowrap">
                                {formatCurrency(payment.convertedAmount, payment.toCurrency)}
                              </span>
                            )}
                          </div>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-700 font-mono">
                            {payment.toCurrency || payment.fromCurrency}
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge className={getStatusColor(payment.status)}>
                          {payment.status}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge className={getStatusColor(payment.fraudStatus)}>
                          {payment.fraudStatus}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {payment.riskLevel && (
                          <Badge className={getRiskLevelColor(payment.riskLevel)}>
                            {payment.riskLevel}
                          </Badge>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatDate(payment.createdAt)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-blue-600">
                        <Link to={`/payments/${payment.paymentId}`} className="hover:underline">
                          View Details
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="flex items-center justify-between mt-4">
              <div className="text-sm text-gray-700">
                Showing page {currentPage + 1} of {totalPages}
                {paymentsData && (
                  <span className="ml-2">
                    ({paymentsData.totalElements} total payments)
                  </span>
                )}
              </div>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => setCurrentPage((p) => p + 1)}
                  disabled={currentPage >= totalPages - 1}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  );
};
