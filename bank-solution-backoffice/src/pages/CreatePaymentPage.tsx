import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { paymentService } from '@/api/paymentService';
import { customerService } from '@/api/customerService';
import { accountService } from '@/api/accountService';
import { currencyConversionService } from '@/api/currencyConversionService';
import {
  CreatePaymentRequest,
  PaymentType,
  Currency,
  CustomerResponse,
  AccountResponse,
  ExchangeRateResponse,
} from '@/types';
import { Card, Button, LoadingSpinner } from '@/components/common';

// ─── Constants ────────────────────────────────────────────────────────────────

const PAYMENT_TYPES: {
  value: PaymentType;
  label: string;
  icon: string;
  desc: string;
  needsCounterparty: boolean;
  counterpartyIsDestination: boolean;
}[] = [
  {
    value: 'TRANSFER_OUT',
    icon: '⬆️',
    label: 'Transfer Out',
    desc: 'Send to another customer',
    needsCounterparty: true,
    counterpartyIsDestination: true,  // counterparty account → destinationAccountId
  },
  {
    value: 'TRANSFER_IN',
    icon: '⬇️',
    label: 'Transfer In',
    desc: 'Receive from another customer',
    needsCounterparty: true,
    counterpartyIsDestination: false, // counterparty account → sourceAccountId
  },
  {
    value: 'DEPOSIT',
    icon: '💰',
    label: 'Deposit',
    desc: 'Deposit into own account',
    needsCounterparty: false,
    counterpartyIsDestination: false,
  },
  {
    value: 'WITHDRAWAL',
    icon: '🏧',
    label: 'Withdrawal',
    desc: 'Withdraw from own account',
    needsCounterparty: false,
    counterpartyIsDestination: false,
  },
];

const ALL_CURRENCIES: { value: Currency; label: string }[] = [
  { value: 'AED', label: 'AED — UAE Dirham' },
  { value: 'ALL', label: 'ALL — Albanian Lek' },
  { value: 'CHF', label: 'CHF — Swiss Franc' },
  { value: 'EUR', label: 'EUR — Euro' },
  { value: 'GBP', label: 'GBP — British Pound' },
  { value: 'INR', label: 'INR — Indian Rupee' },
  { value: 'JPY', label: 'JPY — Japanese Yen' },
  { value: 'MAD', label: 'MAD — Moroccan Dirham' },
  { value: 'MXN', label: 'MXN — Mexican Peso' },
  { value: 'NGN', label: 'NGN — Nigerian Naira' },
  { value: 'PKR', label: 'PKR — Pakistani Rupee' },
  { value: 'TRY', label: 'TRY — Turkish Lira' },
  { value: 'USD', label: 'USD — US Dollar' },
];

// Sentinel UUID of the main ledger account (AccountType.LEDGER).
// Used as the implicit counterparty for DEPOSIT (source) and WITHDRAWAL (destination).
const LEDGER_ACCOUNT_ID = '00000000-0000-0000-0000-000000000000';

const inputCls =
  'w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm';
const selectCls =
  'w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-white';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const accountLabel = (a: AccountResponse) => {
  const balStr =
    a.balances && a.balances.length > 0
      ? ' — ' +
        a.balances
          .map((b) => `${b.availableBalance.toLocaleString()} ${b.currency}`)
          .join(' | ')
      : '';
  return `${a.accountNumber} (${a.accountType}, ${a.bankLocation})${balStr}`;
};

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

// ─── Account Selector sub-component ──────────────────────────────────────────

const AccountSelector: React.FC<{
  label: string;
  required?: boolean;
  hint?: string;
  accounts: AccountResponse[];
  loading: boolean;
  error: string | null;
  value: string;
  onChange: (id: string) => void;
}> = ({ label, required, hint, accounts, loading, error, value, onChange }) => (
  <Field label={label} required={required} hint={hint}>
    {loading ? (
      <div className="flex items-center gap-2 py-2">
        <LoadingSpinner size="sm" />
        <span className="text-sm text-gray-500">Loading accounts…</span>
      </div>
    ) : error ? (
      <p className="text-sm text-red-600">{error}</p>
    ) : accounts.length === 0 ? (
      <p className="text-sm text-gray-400 italic">No accounts found for selected customer</p>
    ) : (
      <select
        className={selectCls}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required={required}
      >
        <option value="">Select an account…</option>
        {accounts.map((a) => (
          <option key={a.id} value={a.id}>
            {accountLabel(a)}
          </option>
        ))}
      </select>
    )}
  </Field>
);

// ─── Page ─────────────────────────────────────────────────────────────────────

export const CreatePaymentPage: React.FC = () => {
  const navigate = useNavigate();

  // ── All customers list ──
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [customersError, setCustomersError] = useState<string | null>(null);

  // ── Primary customer (customerId sent in the request) ──
  const [primaryCustomerId, setPrimaryCustomerId] = useState('');
  const [primaryAccounts, setPrimaryAccounts] = useState<AccountResponse[]>([]);
  const [primaryAccountsLoading, setPrimaryAccountsLoading] = useState(false);
  const [primaryAccountsError, setPrimaryAccountsError] = useState<string | null>(null);
  const [ownAccountId, setOwnAccountId] = useState('');

  // ── Counterparty customer (TRANSFER_IN / TRANSFER_OUT only) ──
  const [counterpartyCustomerId, setCounterpartyCustomerId] = useState('');
  const [counterpartyAccounts, setCounterpartyAccounts] = useState<AccountResponse[]>([]);
  const [counterpartyAccountsLoading, setCounterpartyAccountsLoading] = useState(false);
  const [counterpartyAccountsError, setCounterpartyAccountsError] = useState<string | null>(null);
  const [counterpartyAccountId, setCounterpartyAccountId] = useState('');

  // ── Currency conversion ──
  const [conversionRate, setConversionRate] = useState<ExchangeRateResponse | null>(null);
  const [conversionRateLoading, setConversionRateLoading] = useState(false);

  // ── Payment fields ──
  const [paymentType, setPaymentType] = useState<PaymentType>('TRANSFER_OUT');
  const [amount, setAmount] = useState('');
  const [fromCurrency, setFromCurrency] = useState<Currency>('USD');
  const [toCurrency, setToCurrency] = useState<Currency>('USD');
  const [description, setDescription] = useState('');

  // ── Submit state ──
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const paymentTypeCfg = PAYMENT_TYPES.find((t) => t.value === paymentType)!;

  // Load all customers on mount
  useEffect(() => {
    customerService
      .getAllCustomers()
      .then(setCustomers)
      .catch(() => setCustomersError('Failed to load customers'))
      .finally(() => setCustomersLoading(false));
  }, []);

  // Load primary customer's accounts
  useEffect(() => {
    if (!primaryCustomerId) {
      setPrimaryAccounts([]);
      setPrimaryAccountsError(null);
      setOwnAccountId('');
      return;
    }
    setPrimaryAccountsLoading(true);
    setPrimaryAccountsError(null);
    setOwnAccountId('');
    accountService
      .getAccountsByCustomerId(primaryCustomerId)
      .then(setPrimaryAccounts)
      .catch(() => setPrimaryAccountsError('Failed to load accounts for selected customer'))
      .finally(() => setPrimaryAccountsLoading(false));
  }, [primaryCustomerId]);

  // Load counterparty's accounts
  useEffect(() => {
    if (!counterpartyCustomerId) {
      setCounterpartyAccounts([]);
      setCounterpartyAccountsError(null);
      setCounterpartyAccountId('');
      return;
    }
    setCounterpartyAccountsLoading(true);
    setCounterpartyAccountsError(null);
    setCounterpartyAccountId('');
    accountService
      .getAccountsByCustomerId(counterpartyCustomerId)
      .then(setCounterpartyAccounts)
      .catch(() => setCounterpartyAccountsError('Failed to load accounts for counterparty customer'))
      .finally(() => setCounterpartyAccountsLoading(false));
  }, [counterpartyCustomerId]);

  // Reset account selections when payment type changes
  useEffect(() => {
    setOwnAccountId('');
    setCounterpartyCustomerId('');
    setCounterpartyAccountId('');
    setConversionRate(null);
  }, [paymentType]);

  // Fetch exchange rate when fromCurrency and toCurrency differ
  useEffect(() => {
    if (!fromCurrency || !toCurrency || fromCurrency === toCurrency) {
      setConversionRate(null);
      return;
    }
    setConversionRateLoading(true);
    currencyConversionService
      .getRate(fromCurrency, toCurrency)
      .then(setConversionRate)
      .catch(() => setConversionRate(null))
      .finally(() => setConversionRateLoading(false));
  }, [fromCurrency, toCurrency]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const amountNum = parseFloat(amount);
    if (isNaN(amountNum) || amountNum < 0.01) {
      setError('Amount must be at least 0.01.');
      return;
    }

    // Map own / counterparty accounts → sourceAccountId / destinationAccountId
    let sourceAccountId: string | undefined;
    let destinationAccountId: string | undefined;
    if (paymentType === 'TRANSFER_OUT') {
      sourceAccountId = ownAccountId || undefined;
      destinationAccountId = counterpartyAccountId || undefined;
    } else if (paymentType === 'TRANSFER_IN') {
      sourceAccountId = counterpartyAccountId || undefined;
      destinationAccountId = ownAccountId || undefined;
    } else if (paymentType === 'DEPOSIT') {
      // Money flows FROM ledger → customer account
      sourceAccountId      = LEDGER_ACCOUNT_ID;
      destinationAccountId = ownAccountId || undefined;
    } else {
      // WITHDRAWAL: money flows FROM customer account → ledger
      sourceAccountId      = ownAccountId || undefined;
      destinationAccountId = LEDGER_ACCOUNT_ID;
    }

    const request: CreatePaymentRequest = {
      customerId: primaryCustomerId,
      paymentType,
      amount: amountNum,
      fromCurrency,
      toCurrency,
      description: description.trim() || undefined,
      sourceAccountId,
      destinationAccountId,
    };

    setLoading(true);
    try {
      await paymentService.createPayment(request);
      navigate('/payments');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to create payment');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 flex items-center gap-2">
        <Link to="/payments" className="hover:text-blue-600 transition-colors">
          Payments
        </Link>
        <span>›</span>
        <span className="text-gray-900 font-medium">Create New Payment</span>
      </nav>

      <div>
        <h1 className="text-2xl font-bold text-gray-900">Create New Payment</h1>
        <p className="mt-1 text-sm text-gray-500">
          Submit a new payment request. The MARL fraud detection system will assess it automatically.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Payment Type */}
        <Card title="Payment Type">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {PAYMENT_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => setPaymentType(t.value)}
                className={`flex flex-col items-center p-3 rounded-lg border-2 transition-all text-center ${
                  paymentType === t.value
                    ? 'border-blue-500 bg-blue-50 text-blue-900'
                    : 'border-gray-200 hover:border-gray-300 text-gray-700'
                }`}
              >
                <span className="text-2xl">{t.icon}</span>
                <span className="mt-1 text-sm font-semibold">{t.label}</span>
                <span className="mt-0.5 text-xs text-gray-500 leading-tight">{t.desc}</span>
              </button>
            ))}
          </div>
        </Card>

        {/* Customer & Accounts */}
        <Card title="Customer & Accounts">
          <div className="space-y-5">

            {/* ── Primary customer ── */}
            <div className="space-y-3">
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                {paymentType === 'TRANSFER_OUT' ? '🧑 Sender'
                  : paymentType === 'TRANSFER_IN' ? '🧑 Receiver'
                  : '🧑 Customer'}
              </p>
              <Field label="Customer" required>
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
                    value={primaryCustomerId}
                    onChange={(e) => setPrimaryCustomerId(e.target.value)}
                    required
                  >
                    <option value="">Select a customer…</option>
                    {customers.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.firstName} {c.lastName}{c.middleName ? ` ${c.middleName}` : ''} — {c.email}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
              <AccountSelector
                label={
                  paymentType === 'TRANSFER_OUT' ? 'Source Account (debited)'
                  : paymentType === 'TRANSFER_IN' ? 'Destination Account (credited)'
                  : paymentType === 'DEPOSIT'     ? 'Account to deposit into'
                  :                                 'Account to withdraw from'
                }
                required
                accounts={primaryAccounts}
                loading={primaryAccountsLoading}
                error={primaryAccountsError}
                value={ownAccountId}
                onChange={setOwnAccountId}
              />
            </div>

            {/* ── Counterparty (transfers only) ── */}
            {paymentTypeCfg.needsCounterparty && (
              <div className="border-t border-gray-100 pt-4 space-y-3">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                  {paymentType === 'TRANSFER_OUT' ? '🎯 Recipient' : '🎯 Sender'}
                </p>
                <Field
                  label="Counterparty Customer"
                  required
                  hint="The other party in this transfer — their account UUID will be used"
                >
                  <select
                    className={selectCls}
                    value={counterpartyCustomerId}
                    onChange={(e) => setCounterpartyCustomerId(e.target.value)}
                    required
                  >
                    <option value="">Select a customer…</option>
                    {customers
                      .filter((c) => c.id !== primaryCustomerId)
                      .map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.firstName} {c.lastName}{c.middleName ? ` ${c.middleName}` : ''} — {c.email}
                        </option>
                      ))}
                  </select>
                </Field>
                <AccountSelector
                  label={
                    paymentType === 'TRANSFER_OUT'
                      ? 'Destination Account (credited)'
                      : 'Source Account (debited)'
                  }
                  required
                  accounts={counterpartyAccounts}
                  loading={counterpartyAccountsLoading}
                  error={counterpartyAccountsError}
                  value={counterpartyAccountId}
                  onChange={setCounterpartyAccountId}
                />

                {conversionRateLoading && (
                  <div className="flex items-center gap-2 py-1">
                    <LoadingSpinner size="sm" />
                    <span className="text-xs text-gray-500">Fetching exchange rate…</span>
                  </div>
                )}

                {conversionRate && (() => {
                  const amtNum = parseFloat(amount);
                  const preview = !isNaN(amtNum) && amtNum > 0
                    ? (amtNum * conversionRate.rate).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 4 })
                    : null;
                  return (
                    <div className="bg-amber-50 border border-amber-200 rounded-md p-3 flex flex-col gap-1">
                      <p className="text-xs font-semibold text-amber-800 uppercase tracking-wide">💱 Currency Conversion</p>
                      <p className="text-sm text-amber-700">
                        1 <strong>{fromCurrency}</strong> = <strong>{conversionRate.rate.toLocaleString(undefined, { minimumFractionDigits: 4, maximumFractionDigits: 6 })}</strong> {toCurrency}
                      </p>
                      {preview && (
                        <p className="text-sm text-amber-600">
                          {amtNum.toLocaleString()} {fromCurrency} ≈ <strong>{preview}</strong> {toCurrency}
                        </p>
                      )}
                      <p className="text-xs text-amber-500 mt-0.5">Rate refreshed every hour via scheduled job</p>
                    </div>
                  );
                })()}

              </div>
            )}

          </div>
        </Card>

        {/* Payment Details */}
        <Card title="Payment Details">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Amount" required hint="Minimum 0.01">
              <input
                className={inputCls}
                type="number"
                step="0.01"
                min="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="e.g. 1500.00"
                required
              />
            </Field>

            <Field label="From Currency" required>
              <select
                className={selectCls}
                value={fromCurrency}
                onChange={(e) => setFromCurrency(e.target.value as Currency)}
                required
              >
                {ALL_CURRENCIES.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="To Currency" required hint="Select destination currency; same as From Currency for no conversion">
              <select
                className={selectCls}
                value={toCurrency}
                onChange={(e) => setToCurrency(e.target.value as Currency)}
                required
              >
                {ALL_CURRENCIES.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
            </Field>

            <div className="sm:col-span-2">
              <Field label="Description" hint="Optional — max 500 characters">
                <textarea
                  className={`${inputCls} resize-none`}
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g. Invoice payment for services rendered"
                  maxLength={500}
                />
              </Field>
            </div>
          </div>
        </Card>

        {/* MARL notice */}
        <div className="bg-indigo-50 border border-indigo-200 rounded-md p-4 flex items-start gap-3">
          <span className="text-2xl">🤖</span>
          <div>
            <p className="text-sm font-semibold text-indigo-800">MARL Fraud Assessment</p>
            <p className="text-sm text-indigo-600 mt-1">
              After submission, the MADDPG multi-agent system will automatically analyse this
              payment. You will be redirected to the payment detail page to watch the assessment
              in real time.
            </p>
          </div>
        </div>

        {/* Error Banner */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4 flex items-start gap-3">
            <span className="text-red-500 text-lg">⚠️</span>
            <div>
              <p className="text-sm font-medium text-red-800">Failed to create payment</p>
              <p className="text-sm text-red-600 mt-1">{error}</p>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-between">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/payments')}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={loading}>
            {loading ? '⏳ Submitting…' : '💳 Submit Payment'}
          </Button>
        </div>
      </form>
    </div>
  );
};
