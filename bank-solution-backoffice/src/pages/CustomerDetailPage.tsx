import React, { useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { customerService, accountService, paymentService } from '@/api';
import { CustomerResponse, AccountResponse, PaymentHistoryResponse, Page } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Button } from '@/components/common';
import { formatDate, formatCurrency, getStatusColor } from '@/utils/formatters';

const PAYMENT_TYPE_META: Record<string, { icon: string; label: string; color: string }> = {
  TRANSFER_OUT: { icon: '⬆️', label: 'Transfer Out', color: 'bg-blue-100 text-blue-800' },
  TRANSFER_IN:  { icon: '⬇️', label: 'Transfer In',  color: 'bg-green-100 text-green-800' },
  DEPOSIT:      { icon: '💰', label: 'Deposit',       color: 'bg-emerald-100 text-emerald-800' },
  WITHDRAWAL:   { icon: '🏧', label: 'Withdrawal',    color: 'bg-orange-100 text-orange-800' },
};

export const CustomerDetailPage: React.FC = () => {
  const { customerId } = useParams<{ customerId: string }>();
  const navigate = useNavigate();
  const { data: customer, loading: customerLoading, error: customerError, execute: loadCustomer } =
    useApi<CustomerResponse>();
  const { data: accounts, loading: accountsLoading, execute: loadAccounts } =
    useApi<AccountResponse[]>();
  const { data: paymentsData, loading: paymentsLoading, execute: loadPayments } =
    useApi<Page<PaymentHistoryResponse>>();

  useEffect(() => {
    if (customerId) {
      loadCustomer(() => customerService.getCustomerById(customerId));
      loadAccounts(() => accountService.getAccountsByCustomerId(customerId));
      loadPayments(() => paymentService.getPaymentsByCustomerId(customerId, 0, 10));
    }
  }, [customerId]);

  if (customerLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (customerError) {
    return (
      <div className="text-red-600 p-4 bg-red-50 rounded">
        Error loading customer: {customerError.message}
      </div>
    );
  }

  if (!customer) {
    return <div className="text-gray-500">Customer not found</div>;
  }

  const payments = paymentsData?.content || [];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Customer Details</h1>
        <div className="flex items-center gap-3">
          <Link
            to={`/accounts/open?customerId=${customerId}`}
            className="inline-flex items-center gap-2 px-4 py-2 bg-purple-600 text-white text-sm font-medium rounded-md hover:bg-purple-700 transition-colors shadow-sm"
          >
            🏦 Open Account
          </Link>
          <Link
            to="/payments/create"
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors shadow-sm"
          >
            💳 New Payment
          </Link>
          <Button variant="secondary" onClick={() => navigate('/customers')}>
            Back to Customers
          </Button>
        </div>
      </div>

      {/* Customer Information */}
      <Card title="Customer Information">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow label="Customer ID" value={customer.id} />
          <InfoRow
            label="Name"
            value={`${customer.firstName} ${customer.middleName || ''} ${customer.lastName}`}
          />
          <InfoRow label="Email" value={customer.email} />
          <InfoRow label="Phone Number" value={customer.phoneNumber} />
          <InfoRow label="Date of Birth" value={customer.dateOfBirth} />
          <InfoRow label="Nationality" value={customer.nationality} />
          <InfoRow
            label="Customer Type"
            value={<Badge className="bg-blue-100 text-blue-800">{customer.customerType}</Badge>}
          />
          <InfoRow
            label="Status"
            value={
              <Badge
                className={
                  customer.customerStatus === 'ACTIVE'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
                }
              >
                {customer.customerStatus}
              </Badge>
            }
          />
          <InfoRow label="Created At" value={formatDate(customer.createdAt)} />
          <InfoRow label="Updated At" value={formatDate(customer.updatedAt)} />
        </div>
      </Card>

      {/* Address Information */}
      <Card title="Address Information">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow label="City" value={customer.address.city} />
          <InfoRow label="Country Code" value={customer.address.countryCode} />
        </div>
      </Card>

      {/* Accounts */}
      <Card title="Customer Accounts">
        {accountsLoading && <LoadingSpinner />}
        {!accountsLoading && (!accounts || accounts.length === 0) && (
          <div className="text-gray-500">No accounts found</div>
        )}
        {!accountsLoading && accounts && accounts.length > 0 && (
          <div className="space-y-3">
            {accounts.map((account) => (
              <div
                key={account.id}
                className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50"
              >
                <div className="flex justify-between items-start">
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold">Account #{account.accountNumber}</span>
                      <Badge className="bg-blue-100 text-blue-800">{account.accountType}</Badge>
                      <Badge
                        className={
                          account.accountStatus === 'ACTIVE'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-gray-100 text-gray-800'
                        }
                      >
                        {account.accountStatus}
                      </Badge>
                    </div>
                    <div className="text-sm text-gray-600">
                      <span>Bank Location: {account.bankLocation}</span>
                      <span className="mx-2">|</span>
                      <span>Opened: {account.openingDate}</span>
                    </div>
                    <div className="flex flex-wrap gap-3 mt-2">
                      {account.balances.map((balance) => (
                        <div key={balance.id} className="text-sm">
                          <span className="font-medium">{balance.currency}:</span>{' '}
                          <span className="text-green-600">
                            {formatCurrency(balance.availableBalance, balance.currency)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <Link
                    to={`/accounts/${account.id}`}
                    className="text-blue-600 hover:underline text-sm"
                  >
                    View Details →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Recent Payments */}
      <Card title="Recent Payments">
        {paymentsLoading && <LoadingSpinner />}
        {!paymentsLoading && payments.length === 0 && (
          <div className="text-gray-500">No payments found</div>
        )}
        {!paymentsLoading && payments.length > 0 && (
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
                      Status
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
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {formatCurrency(payment.amount, payment.currency)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <Badge className={getStatusColor(payment.status)}>{payment.status}</Badge>
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
            {paymentsData && paymentsData.totalElements > 10 && (
              <div className="mt-4 text-center">
                <Link
                  to={`/payments?customerId=${customerId}`}
                  className="text-blue-600 hover:underline"
                >
                  View all {paymentsData.totalElements} payments →
                </Link>
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
};

const InfoRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700">{label}</label>
    <div className="mt-1 text-sm text-gray-900">{value}</div>
  </div>
);
