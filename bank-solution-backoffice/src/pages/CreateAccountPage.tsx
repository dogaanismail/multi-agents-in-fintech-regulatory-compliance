import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { accountService } from '@/api/accountService';
import { customerService } from '@/api/customerService';
import { OpenAccountRequest, AccountType, BankLocation, Currency, CustomerResponse } from '@/types';
import { Card, Button, LoadingSpinner } from '@/components/common';

// ─── Constants ────────────────────────────────────────────────────────────────

const ACCOUNT_TYPES: { value: AccountType; label: string; icon: string; desc: string }[] = [
  { value: 'CHECKING', icon: '💳', label: 'Checking', desc: 'Day-to-day transactions' },
  { value: 'SAVINGS', icon: '🏦', label: 'Savings', desc: 'Interest-bearing savings' },
  { value: 'BUSINESS', icon: '🏢', label: 'Business', desc: 'Commercial operations' },
];

const BANK_LOCATIONS: { value: BankLocation; label: string; flag: string }[] = [
  { value: 'AE', flag: '🇦🇪', label: 'United Arab Emirates' },
  { value: 'AL', flag: '🇦🇱', label: 'Albania' },
  { value: 'AT', flag: '🇦🇹', label: 'Austria' },
  { value: 'CH', flag: '🇨🇭', label: 'Switzerland' },
  { value: 'DE', flag: '🇩🇪', label: 'Germany' },
  { value: 'ES', flag: '🇪🇸', label: 'Spain' },
  { value: 'FR', flag: '🇫🇷', label: 'France' },
  { value: 'GB', flag: '🇬🇧', label: 'United Kingdom' },
  { value: 'IN', flag: '🇮🇳', label: 'India' },
  { value: 'IT', flag: '🇮🇹', label: 'Italy' },
  { value: 'JP', flag: '🇯🇵', label: 'Japan' },
  { value: 'MA', flag: '🇲🇦', label: 'Morocco' },
  { value: 'MX', flag: '🇲🇽', label: 'Mexico' },
  { value: 'NG', flag: '🇳🇬', label: 'Nigeria' },
  { value: 'NL', flag: '🇳🇱', label: 'Netherlands' },
  { value: 'PK', flag: '🇵🇰', label: 'Pakistan' },
  { value: 'TR', flag: '🇹🇷', label: 'Turkey' },
  { value: 'US', flag: '🇺🇸', label: 'United States' },
];

// Map bank location → default currencies (most common for that country)
const LOCATION_CURRENCY_MAP: Record<BankLocation, Currency[]> = {
  AE: ['AED', 'USD'],
  AL: ['ALL', 'EUR'],
  AT: ['EUR'],
  CH: ['CHF', 'EUR'],
  DE: ['EUR'],
  ES: ['EUR'],
  FR: ['EUR'],
  GB: ['GBP', 'EUR'],
  IN: ['INR', 'USD'],
  IT: ['EUR'],
  JP: ['JPY', 'USD'],
  MA: ['MAD', 'EUR'],
  MX: ['MXN', 'USD'],
  NG: ['NGN', 'USD'],
  NL: ['EUR'],
  PK: ['PKR', 'USD'],
  TR: ['TRY', 'EUR'],
  US: ['USD'],
};

const ALL_CURRENCIES: { value: Currency; label: string; symbol: string }[] = [
  { value: 'AED', label: 'AED — UAE Dirham', symbol: 'د.إ' },
  { value: 'ALL', label: 'ALL — Albanian Lek', symbol: 'L' },
  { value: 'CHF', label: 'CHF — Swiss Franc', symbol: 'Fr' },
  { value: 'EUR', label: 'EUR — Euro', symbol: '€' },
  { value: 'GBP', label: 'GBP — British Pound', symbol: '£' },
  { value: 'INR', label: 'INR — Indian Rupee', symbol: '₹' },
  { value: 'JPY', label: 'JPY — Japanese Yen', symbol: '¥' },
  { value: 'MAD', label: 'MAD — Moroccan Dirham', symbol: 'د.م.' },
  { value: 'MXN', label: 'MXN — Mexican Peso', symbol: '$' },
  { value: 'NGN', label: 'NGN — Nigerian Naira', symbol: '₦' },
  { value: 'PKR', label: 'PKR — Pakistani Rupee', symbol: '₨' },
  { value: 'TRY', label: 'TRY — Turkish Lira', symbol: '₺' },
  { value: 'USD', label: 'USD — US Dollar', symbol: '$' },
];

const selectCls =
  'w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-white';

// ─── Field Component ──────────────────────────────────────────────────────────

const Field: React.FC<{
  label: string;
  required?: boolean;
  hint?: string;
  children: React.ReactNode;
}> = ({ label, required, hint, children }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700 mb-1">
      {label}
      {required && <span className="text-red-500 ml-1">*</span>}
    </label>
    {children}
    {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
  </div>
);

// ─── Page ─────────────────────────────────────────────────────────────────────

export const CreateAccountPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Pre-fill customerId from query param (e.g. navigating from customer detail)
  const prefilledCustomerId = searchParams.get('customerId') ?? '';

  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState<string | null>(null);

  const [customerId, setCustomerId] = useState(prefilledCustomerId);
  const [accountType, setAccountType] = useState<AccountType>('CHECKING');
  const [bankLocation, setBankLocation] = useState<BankLocation | ''>('');
  const [selectedCurrencies, setSelectedCurrencies] = useState<Currency[]>([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load all customers for the selector
  useEffect(() => {
    customerService
      .getAllCustomers()
      .then(setCustomers)
      .catch(() => setCustomersError('Failed to load customers'))
      .finally(() => setCustomersLoading(false));
  }, []);

  // Auto-select default currencies when bank location changes
  const handleBankLocationChange = (loc: BankLocation) => {
    setBankLocation(loc);
    setSelectedCurrencies(LOCATION_CURRENCY_MAP[loc] ?? []);
  };

  const toggleCurrency = (c: Currency) => {
    setSelectedCurrencies((prev) =>
      prev.includes(c) ? prev.filter((x) => x !== c) : [...prev, c]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!bankLocation) {
      setError('Please select a bank location.');
      return;
    }
    if (selectedCurrencies.length === 0) {
      setError('Please select at least one currency.');
      return;
    }

    const request: OpenAccountRequest = {
      customerId,
      accountType,
      bankLocation,
      currencies: selectedCurrencies,
    };

    setLoading(true);
    try {
      const account = await accountService.openAccount(request);
      navigate(`/accounts/${account.id}`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to open account');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 flex items-center gap-2">
        <Link to="/accounts" className="hover:text-blue-600 transition-colors">
          Accounts
        </Link>
        <span>›</span>
        <span className="text-gray-900 font-medium">Open New Account</span>
      </nav>

      <div>
        <h1 className="text-2xl font-bold text-gray-900">Open New Account</h1>
        <p className="mt-1 text-sm text-gray-500">
          Open a new bank account for an existing customer. All required fields are marked with{' '}
          <span className="text-red-500">*</span>.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Customer Selection */}
        <Card title="Customer">
          <Field label="Customer" required hint="Select the customer for this account">
            {customersLoading ? (
              <div className="flex items-center gap-2 py-2">
                <LoadingSpinner size="sm" />
                <span className="text-sm text-gray-500">Loading customers…</span>
              </div>
            ) : customersError ? (
              <p className="text-sm text-red-600">{customersError}</p>
            ) : (
              <select
                className={selectCls}
                value={customerId}
                onChange={(e) => setCustomerId(e.target.value)}
                required
              >
                <option value="">Select a customer…</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.firstName} {c.lastName}
                    {c.middleName ? ` ${c.middleName}` : ''} — {c.email}
                  </option>
                ))}
              </select>
            )}
          </Field>

          {customerId && (
            <p className="mt-2 text-xs text-gray-400 font-mono break-all">ID: {customerId}</p>
          )}
        </Card>

        {/* Account Configuration */}
        <Card title="Account Configuration">
          <div className="space-y-5">
            {/* Account Type */}
            <Field label="Account Type" required>
              <div className="grid grid-cols-3 gap-3 mt-1">
                {ACCOUNT_TYPES.map((t) => (
                  <button
                    key={t.value}
                    type="button"
                    onClick={() => setAccountType(t.value)}
                    className={`flex flex-col items-center p-3 rounded-lg border-2 transition-all text-center ${
                      accountType === t.value
                        ? 'border-blue-500 bg-blue-50 text-blue-900'
                        : 'border-gray-200 hover:border-gray-300 text-gray-700'
                    }`}
                  >
                    <span className="text-2xl">{t.icon}</span>
                    <span className="mt-1 text-sm font-medium">{t.label}</span>
                    <span className="mt-0.5 text-xs text-gray-500">{t.desc}</span>
                  </button>
                ))}
              </div>
            </Field>

            {/* Bank Location */}
            <Field label="Bank Location" required hint="Country where the account will be held">
              <select
                className={selectCls}
                value={bankLocation}
                onChange={(e) => handleBankLocationChange(e.target.value as BankLocation)}
                required
              >
                <option value="">Select a country…</option>
                {BANK_LOCATIONS.map((l) => (
                  <option key={l.value} value={l.value}>
                    {l.flag} {l.value} — {l.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </Card>

        {/* Currency Selection */}
        <Card title="Currencies">
          <p className="text-sm text-gray-500 mb-3">
            Select at least one currency for this account. Default currencies for the selected
            location are pre-checked.
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {ALL_CURRENCIES.map((c) => {
              const checked = selectedCurrencies.includes(c.value);
              return (
                <label
                  key={c.value}
                  className={`flex items-center gap-2 p-2 rounded-lg border cursor-pointer transition-all ${
                    checked
                      ? 'border-blue-400 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => toggleCurrency(c.value)}
                    className="accent-blue-600"
                  />
                  <span className="text-xs font-medium text-gray-700">{c.value}</span>
                  <span className="text-xs text-gray-400 truncate">{c.symbol}</span>
                </label>
              );
            })}
          </div>
          {selectedCurrencies.length > 0 && (
            <p className="mt-3 text-xs text-gray-500">
              Selected:{' '}
              <span className="font-medium text-gray-700">
                {selectedCurrencies.join(', ')}
              </span>
            </p>
          )}
        </Card>

        {/* Error Banner */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4 flex items-start gap-3">
            <span className="text-red-500 text-lg">⚠️</span>
            <div>
              <p className="text-sm font-medium text-red-800">Failed to open account</p>
              <p className="text-sm text-red-600 mt-1">{error}</p>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-between">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/accounts')}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={loading}>
            {loading ? '⏳ Opening…' : '🏦 Open Account'}
          </Button>
        </div>
      </form>
    </div>
  );
};
