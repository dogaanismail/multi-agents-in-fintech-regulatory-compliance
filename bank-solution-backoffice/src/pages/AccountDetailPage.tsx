import React, {useEffect, useState} from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {accountService, ledgerService} from '@/api';
import {AccountResponse, AccountWalletResponse, Currency, LedgerWalletResponse} from '@/types';
import { useApi } from '@/hooks/useApi';
import {Card, LoadingSpinner, Badge, Button, Input} from '@/components/common';
import { formatDate, formatCurrency } from '@/utils/formatters';

export const AccountDetailPage: React.FC = () => {
  const { accountId } = useParams<{ accountId: string }>();
  const navigate = useNavigate();
  const { data: account, loading, error, execute } = useApi<AccountResponse>();
    const [ledgerWallets, setLedgerWallets] = useState<LedgerWalletResponse[]>([]);
    const [fundingWallet, setFundingWallet] = useState<AccountWalletResponse | null>(null);
    const [fundAmount, setFundAmount] = useState('');
    const [fundLoading, setFundLoading] = useState(false);
    const [fundError, setFundError] = useState<string | null>(null);
    const [fundSuccess, setFundSuccess] = useState<string | null>(null);

    const loadAccount = () => {
        if (!accountId) return;
        execute(() => accountService.getAccountById(accountId));
        ledgerService
            .getWalletsByAccountId(accountId)
            .then(setLedgerWallets)
            .catch(() => setLedgerWallets([]));
    };

    useEffect(() => {
        loadAccount();
  }, [accountId]);

    const handleFund = async () => {
        if (!accountId || !fundingWallet) return;
        const amountNum = parseFloat(fundAmount);
        if (isNaN(amountNum) || amountNum <= 0) {
            setFundError('Amount must be greater than 0');
            return;
        }

        setFundLoading(true);
        setFundError(null);
        try {
            await ledgerService.fundWallet({
                clientTransactionId: crypto.randomUUID(),
                inboundHardSettlement: {
                    amount: amountNum,
                    currency: fundingWallet.currency as Currency,
                    customerAccountId: accountId,
                },
            });
            setFundSuccess(
                `Funded ${formatCurrency(amountNum, fundingWallet.currency)} into the ${fundingWallet.currency} wallet`
            );
            setFundingWallet(null);
            setFundAmount('');
            setTimeout(loadAccount, 1200);
        } catch (err: unknown) {
            const msg =
                (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
                (err instanceof Error ? err.message : 'Failed to fund wallet');
            setFundError(msg);
        } finally {
            setFundLoading(false);
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
        Error loading account: {error.message}
      </div>
    );
  }

  if (!account) {
    return <div className="text-gray-500">Account not found</div>;
  }

    const ledgerWalletFor = (wallet: AccountWalletResponse) =>
        ledgerWallets.find((lw) => lw.ledgerAccountId === wallet.ledgerAccountId);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold text-gray-900">Account Details</h1>
        <div className="flex items-center gap-2">
          <Link
            to={`/payments?accountId=${account.id}`}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-md bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors"
          >
            💳 View Payments
          </Link>
          <Button variant="secondary" onClick={() => navigate(-1)}>
            Back
          </Button>
        </div>
      </div>

        {fundSuccess && (
            <div
                className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded flex justify-between items-center">
                <span>✓ {fundSuccess}. The projected balance refreshes as soon as the ledger event lands.</span>
                <button className="text-green-500 hover:text-green-700" onClick={() => setFundSuccess(null)}>✕</button>
        </div>
      )}

      <Card title="Account Information">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <InfoRow label="Account ID" value={account.id} />
          <InfoRow label="Account Number" value={account.accountNumber} />
            <InfoRow
                label="Customer ID"
                value={
                    <Link to={`/customers/${account.customerId}`}
                          className="text-blue-600 hover:underline font-mono text-sm">
                        {account.customerId}
                    </Link>
                }
            />
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

        <Card title="Wallets">
            {account.wallets && account.wallets.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {account.wallets.map((wallet) => {
                  const live = ledgerWalletFor(wallet);
                  return (
                      <div
                          key={wallet.id}
                          className="border border-gray-200 rounded-lg p-4 bg-gradient-to-br from-blue-50 to-white"
                      >
                          <div className="flex items-center justify-between mb-3">
                              <div className="flex items-center gap-2">
                                  <h3 className="text-lg font-semibold text-gray-900">{wallet.currency}</h3>
                                  {wallet.primary && (
                                      <Badge className="bg-indigo-100 text-indigo-800">PRIMARY</Badge>
                                  )}
                              </div>
                              <Badge
                                  className={
                                      wallet.walletStatus === 'ACTIVE'
                                          ? 'bg-green-100 text-green-800'
                                          : 'bg-gray-100 text-gray-800'
                                  }
                              >
                                  {wallet.walletStatus}
                              </Badge>
                          </div>
                          <div className="space-y-2">
                              <div>
                                  <div className="text-xs text-gray-600">Available Balance</div>
                                  <div className="text-xl font-bold text-green-600">
                                      {formatCurrency(wallet.availableBalance, wallet.currency)}
                                  </div>
                              </div>
                              <div>
                                  <div className="text-xs text-gray-600">Posted Balance</div>
                                  <div className="text-sm font-medium text-gray-900">
                                      {formatCurrency(wallet.balance, wallet.currency)}
                                  </div>
                              </div>
                              {live && (
                                  <div className="pt-2 border-t border-gray-200 text-xs text-gray-600 space-y-1">
                                      <div className="flex justify-between">
                                          <span>Held (pending debits)</span>
                                          <span className="text-orange-600 font-medium tabular-nums">
                            {formatCurrency(live.debitsPending, wallet.currency)}
                          </span>
                                      </div>
                                      <div className="flex justify-between">
                                          <span>Incoming (pending credits)</span>
                                          <span className="text-blue-600 font-medium tabular-nums">
                            {formatCurrency(live.creditsPending, wallet.currency)}
                          </span>
                                      </div>
                                      <div className="text-[10px] text-gray-400 mt-1">Live from TigerBeetle</div>
                                  </div>
                              )}
                              <div className="pt-2">
                                  <Button
                                      variant="success"
                                      className="w-full text-sm"
                                      onClick={() => {
                                          setFundingWallet(wallet);
                                          setFundError(null);
                                          setFundAmount('');
                                      }}
                                  >
                                      💰 Fund Wallet
                                  </Button>
                    </div>
                              <div className="text-[10px] text-gray-400 font-mono truncate"
                                   title={wallet.ledgerAccountId}>
                                  ledger: {wallet.ledgerAccountId}
                              </div>
                          </div>
                      </div>
                  );
              })}
          </div>
        ) : (
                <div className="text-gray-500">No wallets for this account</div>
        )}
      </Card>

        {fundingWallet && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                <div className="bg-white rounded-lg p-6 max-w-md w-full">
                    <h3 className="text-xl font-semibold mb-1">Fund {fundingWallet.currency} Wallet</h3>
                    <p className="text-sm text-gray-500 mb-4">
                        Applies an inbound hard settlement on the ledger: inbound clearing is debited and this wallet is
                        credited immediately, with no authorisation hold.
                    </p>
                    <Input
                        label={`Amount (${fundingWallet.currency})`}
                        type="number"
                        step="0.01"
                        min="0.01"
                        placeholder="e.g. 1000.00"
                        value={fundAmount}
                        onChange={(e) => setFundAmount(e.target.value)}
                        required
                    />
                    {fundError && (
                        <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded text-sm mb-4">
                            {fundError}
                        </div>
                    )}
                    <div className="flex justify-end gap-3">
                        <Button variant="secondary" onClick={() => setFundingWallet(null)} disabled={fundLoading}>
                            Cancel
                        </Button>
                        <Button variant="success" onClick={handleFund} disabled={fundLoading}>
                            {fundLoading ? 'Funding…' : 'Fund'}
                        </Button>
                    </div>
                </div>
            </div>
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
