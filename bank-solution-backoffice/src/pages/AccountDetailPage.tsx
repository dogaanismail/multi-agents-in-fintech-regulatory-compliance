import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { accountService } from '@/api';
import { AccountResponse } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Button } from '@/components/common';
import { formatDate, formatCurrency } from '@/utils/formatters';

export const AccountDetailPage: React.FC = () => {
  const { accountId } = useParams<{ accountId: string }>();
  const navigate = useNavigate();
  const { data: account, loading, error, execute } = useApi<AccountResponse>();

  useEffect(() => {
    if (accountId) {
      execute(() => accountService.getAccountById(accountId));
    }
  }, [accountId]);

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
        Error loading account: {error.message}
      </div>
    );
  }

  if (!account) {
    return <div className="text-gray-500">Account not found</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Account Details</h1>
        <Button variant="secondary" onClick={() => navigate(-1)}>
          Back
        </Button>
      </div>

      {/* Account Information */}
      <Card title="Account Information">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow label="Account ID" value={account.id} />
          <InfoRow label="Account Number" value={account.accountNumber} />
          <InfoRow label="Customer ID" value={account.customerId} />
          <InfoRow
            label="Account Type"
            value={<Badge className="bg-blue-100 text-blue-800">{account.accountType}</Badge>}
          />
          <InfoRow label="Bank Location" value={account.bankLocation} />
          <InfoRow
            label="Account Status"
            value={
              <Badge
                className={
                  account.accountStatus === 'ACTIVE'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
                }
              >
                {account.accountStatus}
              </Badge>
            }
          />
          <InfoRow label="Opening Date" value={account.openingDate} />
          {account.closingDate && <InfoRow label="Closing Date" value={account.closingDate} />}
          <InfoRow label="Created At" value={formatDate(account.createdAt)} />
          <InfoRow label="Updated At" value={formatDate(account.updatedAt)} />
        </div>
      </Card>

      {/* Account Balances */}
      <Card title="Account Balances">
        {account.balances && account.balances.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {account.balances.map((balance) => (
              <div
                key={balance.id}
                className="border border-gray-200 rounded-lg p-4 bg-gradient-to-br from-blue-50 to-white"
              >
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-lg font-semibold text-gray-900">{balance.currency}</h3>
                  <Badge className="bg-blue-100 text-blue-800">{balance.currency}</Badge>
                </div>
                <div className="space-y-2">
                  <div>
                    <div className="text-xs text-gray-600">Available Balance</div>
                    <div className="text-xl font-bold text-green-600">
                      {formatCurrency(balance.availableBalance, balance.currency)}
                    </div>
                  </div>
                  <div>
                    <div className="text-xs text-gray-600">Pending Balance</div>
                    <div className="text-sm font-medium text-orange-600">
                      {formatCurrency(balance.pendingBalance, balance.currency)}
                    </div>
                  </div>
                  <div className="pt-2 border-t border-gray-200">
                    <div className="text-xs text-gray-600">Total Balance</div>
                    <div className="text-lg font-semibold text-gray-900">
                      {formatCurrency(balance.totalBalance, balance.currency)}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-gray-500">No balances available</div>
        )}
      </Card>

      {/* Balance Summary */}
      {account.balances && account.balances.length > 0 && (
        <Card title="Balance Summary">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Currency
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Available Balance
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Pending Balance
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Total Balance
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {account.balances.map((balance) => (
                  <tr key={balance.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {balance.currency}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-green-600">
                      {formatCurrency(balance.availableBalance, balance.currency)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-orange-600">
                      {formatCurrency(balance.pendingBalance, balance.currency)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-gray-900">
                      {formatCurrency(balance.totalBalance, balance.currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
};

const InfoRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700">{label}</label>
    <div className="mt-1 text-sm text-gray-900">{value}</div>
  </div>
);
