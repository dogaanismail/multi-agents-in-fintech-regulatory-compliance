import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Card, Badge, LoadingSpinner, Input } from '@/components/common';
import { accountService } from '@/api/accountService';
import { AccountResponse } from '@/types';

export const AccountListPage = () => {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [accountIdFilter, setAccountIdFilter] = useState('');
  const [customerIdFilter, setCustomerIdFilter] = useState('');

  useEffect(() => {
    fetchAccounts();
  }, [customerIdFilter]);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      
      if (customerIdFilter) {
        const data = await accountService.getAccountsByCustomerId(customerIdFilter);
        setAccounts(data);
      } else {
        // If no filter, we can't list all accounts without an API endpoint
        // For now, prompt user to filter by customer
        setAccounts([]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch accounts');
    } finally {
      setLoading(false);
    }
  };

  const filteredAccounts = accounts.filter((account) => {
    if (accountIdFilter && !account.id.toLowerCase().includes(accountIdFilter.toLowerCase())) {
      return false;
    }
    return true;
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'SUSPENDED':
        return 'bg-yellow-100 text-yellow-800';
      case 'CLOSED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'CHECKING':
        return 'bg-blue-100 text-blue-800';
      case 'SAVINGS':
        return 'bg-green-100 text-green-800';
      case 'CREDIT':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading && customerIdFilter) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Accounts</h1>
        <Link
          to="/accounts/open"
          className="inline-flex items-center gap-2 px-4 py-2 bg-purple-600 text-white text-sm font-medium rounded-md hover:bg-purple-700 transition-colors shadow-sm"
        >
          <span>＋</span> Open Account
        </Link>
      </div>

      {/* Filters */}
      <Card>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input
            label="Customer ID"
            value={customerIdFilter}
            onChange={(e) => setCustomerIdFilter(e.target.value)}
            placeholder="Enter customer ID to search"
          />
          <Input
            label="Account ID"
            value={accountIdFilter}
            onChange={(e) => setAccountIdFilter(e.target.value)}
            placeholder="Filter by account ID"
            disabled={!customerIdFilter}
          />
        </div>
      </Card>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {!customerIdFilter && !loading && (
        <Card>
          <div className="text-center py-8">
            <p className="text-gray-500 text-lg">Please enter a Customer ID to view their accounts</p>
          </div>
        </Card>
      )}

      {customerIdFilter && !loading && filteredAccounts.length === 0 && (
        <Card>
          <div className="text-center py-8">
            <p className="text-gray-500 text-lg">No accounts found</p>
          </div>
        </Card>
      )}

      {/* Accounts List */}
      {filteredAccounts.length > 0 && (
        <Card>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Account ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Account Number
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Bank Location
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
                {filteredAccounts.map((account) => (
                  <tr key={account.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {account.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {account.accountNumber}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge className={getTypeColor(account.accountType)}>
                        {account.accountType}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge className={getStatusColor(account.accountStatus)}>
                        {account.accountStatus}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {account.bankLocation}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(account.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <Link
                        to={`/accounts/${account.id}`}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        View Details
                      </Link>
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
