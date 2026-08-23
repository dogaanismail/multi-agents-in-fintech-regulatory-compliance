import {useState, useEffect, useCallback} from 'react';
import {Card, Badge, Button, LoadingSpinner} from '@/components/common';
import {ledgerService} from '@/api/ledgerService';
import {
    Currency,
    LedgerInternalAccountResponse,
    LedgerInternalAccountType,
    TrialBalanceResponse,
} from '@/types';
import {formatCurrency, formatDate} from '@/utils/formatters';

const CURRENCIES: Currency[] = ['GBP', 'EUR', 'USD', 'AED', 'ALL', 'CHF', 'INR', 'JPY', 'MAD', 'MXN', 'NGN', 'PKR', 'TRY'];

const INTERNAL_ACCOUNT_TYPES: { value: LedgerInternalAccountType; label: string; desc: string }[] = [
    {value: 'INBOUND_CLEARING', label: 'Inbound Clearing', desc: 'Counterparty for money entering the bank'},
    {value: 'OUTBOUND_CLEARING', label: 'Outbound Clearing', desc: 'Counterparty for money leaving the bank'},
    {value: 'FEES_INCOME', label: 'Fees Income', desc: 'Collected fees'},
    {value: 'SUSPENSE', label: 'Suspense', desc: 'Holding account for unresolved postings'},
    {value: 'FX_POSITION', label: 'FX Position', desc: 'Bank currency exposure from cross-currency transfers'},
];

const typeBadgeCls: Record<LedgerInternalAccountType, string> = {
    INBOUND_CLEARING: 'bg-green-100 text-green-800',
    OUTBOUND_CLEARING: 'bg-blue-100 text-blue-800',
    FEES_INCOME: 'bg-purple-100 text-purple-800',
    SUSPENSE: 'bg-yellow-100 text-yellow-800',
    FX_POSITION: 'bg-orange-100 text-orange-800',
};

const selectCls =
    'px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm bg-white';

export const LedgerPage = () => {
    const [internalAccounts, setInternalAccounts] = useState<LedgerInternalAccountResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [currencyFilter, setCurrencyFilter] = useState<Currency | ''>('');

    const [trialBalanceCurrency, setTrialBalanceCurrency] = useState<Currency>('GBP');
    const [trialBalance, setTrialBalance] = useState<TrialBalanceResponse | null>(null);
    const [trialBalanceLoading, setTrialBalanceLoading] = useState(false);

    const [showCreateForm, setShowCreateForm] = useState(false);
    const [newAccountType, setNewAccountType] = useState<LedgerInternalAccountType>('SUSPENSE');
    const [newAccountCurrency, setNewAccountCurrency] = useState<Currency>('GBP');
    const [createLoading, setCreateLoading] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);

    const fetchInternalAccounts = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await ledgerService.getInternalAccounts(currencyFilter || undefined);
            setInternalAccounts(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load ledger internal accounts');
        } finally {
            setLoading(false);
        }
    }, [currencyFilter]);

    useEffect(() => {
        fetchInternalAccounts();
    }, [fetchInternalAccounts]);

    useEffect(() => {
        setTrialBalanceLoading(true);
        ledgerService
            .getTrialBalance(trialBalanceCurrency)
            .then(setTrialBalance)
            .catch(() => setTrialBalance(null))
            .finally(() => setTrialBalanceLoading(false));
    }, [trialBalanceCurrency]);

    const handleCreate = async () => {
        setCreateLoading(true);
        setCreateError(null);
        try {
            await ledgerService.createInternalAccount({
                accountType: newAccountType,
                currency: newAccountCurrency,
            });
            setShowCreateForm(false);
            await fetchInternalAccounts();
        } catch (err: unknown) {
            const msg =
                (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
                (err instanceof Error ? err.message : 'Failed to create internal account');
            setCreateError(msg);
        } finally {
            setCreateLoading(false);
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">🏛️ Ledger</h1>
                    <p className="mt-1 text-sm text-gray-500">
                        Internal accounts owned by the bank. TigerBeetle is the source of truth for every balance shown
                        here.
                    </p>
                </div>
                <Button variant="primary" onClick={() => setShowCreateForm((v) => !v)}>
                    {showCreateForm ? 'Close' : '＋ Create Internal Account'}
                </Button>
            </div>

            {showCreateForm && (
                <Card title="Create Internal Account">
                    <div className="space-y-4">
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Account Type</label>
                                <select
                                    className={`${selectCls} w-full`}
                                    value={newAccountType}
                                    onChange={(e) => setNewAccountType(e.target.value as LedgerInternalAccountType)}
                                >
                                    {INTERNAL_ACCOUNT_TYPES.map((t) => (
                                        <option key={t.value} value={t.value}>
                                            {t.label} — {t.desc}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Currency</label>
                                <select
                                    className={`${selectCls} w-full`}
                                    value={newAccountCurrency}
                                    onChange={(e) => setNewAccountCurrency(e.target.value as Currency)}
                                >
                                    {CURRENCIES.map((c) => (
                                        <option key={c} value={c}>{c}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                        <p className="text-xs text-gray-500">
                            Internal account ids are derived from type and currency, so creating an existing combination
                            returns the
                            existing account instead of a duplicate.
                        </p>
                        {createError && (
                            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
                                {createError}
                            </div>
                        )}
                        <div className="flex justify-end">
                            <Button variant="success" onClick={handleCreate} disabled={createLoading}>
                                {createLoading ? 'Creating…' : 'Create'}
                            </Button>
                        </div>
                    </div>
                </Card>
            )}

            <Card title="Internal Accounts">
                <div className="flex items-center gap-3 mb-4">
                    <label className="text-sm font-medium text-gray-700">Currency</label>
                    <select
                        className={selectCls}
                        value={currencyFilter}
                        onChange={(e) => setCurrencyFilter(e.target.value as Currency | '')}
                    >
                        <option value="">All currencies</option>
                        {CURRENCIES.map((c) => (
                            <option key={c} value={c}>{c}</option>
                        ))}
                    </select>
                </div>

                {error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
                        {error}
                    </div>
                )}

                {loading ? (
                    <div className="flex justify-center py-8">
                        <LoadingSpinner size="lg"/>
                    </div>
                ) : internalAccounts.length === 0 ? (
                    <p className="text-gray-500 text-center py-8">No internal accounts found</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                            <tr>
                                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Currency</th>
                                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Credits
                                    Posted
                                </th>
                                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Debits
                                    Posted
                                </th>
                                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Pending
                                    (Cr / Dr)
                                </th>
                                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Net
                                    Balance
                                </th>
                                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created</th>
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {internalAccounts.map((account) => (
                                <tr key={account.ledgerAccountId} className="hover:bg-gray-50">
                                    <td className="px-4 py-3 whitespace-nowrap">
                                        <Badge
                                            className={typeBadgeCls[account.accountType]}>{account.accountType}</Badge>
                                    </td>
                                    <td className="px-4 py-3 whitespace-nowrap text-sm font-mono font-semibold text-gray-900">
                                        {account.currency}
                                    </td>
                                    <td className="px-4 py-3 whitespace-nowrap text-sm text-right text-green-600 tabular-nums">
                                        {formatCurrency(account.creditsPosted, account.currency)}
                                    </td>
                                    <td className="px-4 py-3 whitespace-nowrap text-sm text-right text-red-600 tabular-nums">
                                        {formatCurrency(account.debitsPosted, account.currency)}
                                    </td>
                                    <td className="px-4 py-3 whitespace-nowrap text-sm text-right text-orange-500 tabular-nums">
                                        {formatCurrency(account.creditsPending, account.currency)} / {formatCurrency(account.debitsPending, account.currency)}
                                    </td>
                                    <td className={`px-4 py-3 whitespace-nowrap text-sm text-right font-semibold tabular-nums ${account.netBalance < 0 ? 'text-red-700' : 'text-gray-900'}`}>
                                        {formatCurrency(account.netBalance, account.currency)}
                                    </td>
                                    <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                                        {formatDate(account.createdAt)}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </Card>

            <Card title="Trial Balance">
                <div className="flex items-center gap-3 mb-4">
                    <label className="text-sm font-medium text-gray-700">Currency</label>
                    <select
                        className={selectCls}
                        value={trialBalanceCurrency}
                        onChange={(e) => setTrialBalanceCurrency(e.target.value as Currency)}
                    >
                        {CURRENCIES.map((c) => (
                            <option key={c} value={c}>{c}</option>
                        ))}
                    </select>
                    {trialBalanceLoading && <LoadingSpinner size="sm"/>}
                </div>

                {trialBalance && (
                    <div className="flex items-center gap-8 flex-wrap">
                        <div>
                            <div className="text-xs text-gray-600">Customer wallets hold</div>
                            <div className="text-xl font-semibold text-green-700 tabular-nums">
                                {formatCurrency(trialBalance.customerWalletsNet, trialBalance.currency)}
                            </div>
                        </div>
                        <div>
                            <div className="text-xs text-gray-600">Internal accounts net</div>
                            <div className="text-xl font-semibold text-gray-800 tabular-nums">
                                {formatCurrency(trialBalance.internalAccountsNet, trialBalance.currency)}
                            </div>
                        </div>
                        <div>
                            <div className="text-xs text-gray-600">Sum (zero when balanced)</div>
                            <div className="text-2xl font-bold tabular-nums">
                                {formatCurrency(trialBalance.net, trialBalance.currency)}
                            </div>
                        </div>
                        <Badge
                            className={trialBalance.balanced ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}
                        >
                            {trialBalance.balanced ? '✓ Books balance' : '✗ Out of balance'}
                        </Badge>
                    </div>
                )}
                {!trialBalance && !trialBalanceLoading && (
                    <p className="text-gray-500">No trial balance available for {trialBalanceCurrency}</p>
                )}
            </Card>
        </div>
    );
};
