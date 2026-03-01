import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { paymentService } from '@/api/paymentService';
import { PaymentHistoryResponse } from '@/types';
import { Card, LoadingSpinner, Badge } from '@/components/common';
import { formatCurrency, formatDate, getRiskLevelColor } from '@/utils/formatters';

// ─── Sub-components ────────────────────────────────────────────────────────────

const StatCard: React.FC<{
  label: string;
  value: string | number;
  icon: string;
  valueColor?: string;
  sub?: string;
  to?: string;
}> = ({ label, value, icon, valueColor = 'text-gray-900', sub, to }) => {
  const inner = (
    <div className="rounded-xl border border-gray-100 bg-white p-5 shadow-sm hover:shadow-md transition-shadow h-full">
      <div className="flex items-start justify-between">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">{label}</p>
          <p className={`mt-1.5 text-3xl font-bold ${valueColor}`}>{value}</p>
          {sub && <p className="mt-1 text-xs text-gray-400">{sub}</p>}
        </div>
        <span className="text-3xl ml-3 flex-shrink-0">{icon}</span>
      </div>
    </div>
  );
  return to ? <Link to={to} className="block">{inner}</Link> : <div>{inner}</div>;
};

const BarRow: React.FC<{
  label: string;
  count: number;
  total: number;
  barColor: string;
  icon?: string;
}> = ({ label, count, total, barColor, icon }) => {
  const pct = total > 0 ? (count / total) * 100 : 0;
  return (
    <div className="flex items-center gap-3">
      <span className="w-40 text-sm text-gray-600 text-right truncate shrink-0">
        {icon && <span className="mr-1">{icon}</span>}
        {label}
      </span>
      <div className="flex-1 bg-gray-100 rounded-full h-2.5 overflow-hidden">
        <div
          className={`h-full rounded-full ${barColor} transition-all duration-700`}
          style={{ width: `${pct > 0 ? Math.max(pct, 2) : 0}%` }}
        />
      </div>
      <span className="w-8 text-sm font-semibold text-gray-700 text-right tabular-nums">{count}</span>
      <span className="w-10 text-xs text-gray-400 text-right tabular-nums">{pct.toFixed(0)}%</span>
    </div>
  );
};

// ─── Constants ─────────────────────────────────────────────────────────────────

const PT_META: Record<string, { icon: string; label: string; bar: string }> = {
  TRANSFER_OUT: { icon: '⬆️', label: 'Transfer Out', bar: 'bg-blue-400' },
  TRANSFER_IN:  { icon: '⬇️', label: 'Transfer In',  bar: 'bg-green-400' },
  DEPOSIT:      { icon: '💰', label: 'Deposit',       bar: 'bg-emerald-400' },
  WITHDRAWAL:   { icon: '🏧', label: 'Withdrawal',    bar: 'bg-orange-400' },
};

const MARL_META: { action: string; icon: string; bar: string }[] = [
  { action: 'ALLOW',  icon: '✅', bar: 'bg-green-400'  },
  { action: 'BLOCK',  icon: '🚫', bar: 'bg-red-400'    },
  { action: 'REVIEW', icon: '👁️', bar: 'bg-yellow-400' },
  { action: 'NO MARL', icon: '—', bar: 'bg-gray-300'  },
];

const FRAUD_META: { status: string; bar: string }[] = [
  { status: 'APPROVED',        bar: 'bg-green-400'  },
  { status: 'PENDING',         bar: 'bg-blue-400'   },
  { status: 'REVIEW_REQUIRED', bar: 'bg-yellow-400' },
  { status: 'BLOCKED',         bar: 'bg-red-500'    },
  { status: 'FAILED',          bar: 'bg-orange-400' },
];

// ─── Page ──────────────────────────────────────────────────────────────────────

export const DashboardPage: React.FC = () => {
  const [payments, setPayments] = useState<PaymentHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastRefresh, setLastRefresh] = useState<Date>(new Date());

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await paymentService.getAllPayments(0, 200);
      setPayments(data.content);
      setLastRefresh(new Date());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const interval = setInterval(load, 30_000);
    return () => clearInterval(interval);
  }, [load]);

  // ── Derived stats ─────────────────────────────────────────────────────────────
  const total      = payments.length;
  const completed  = payments.filter((p) => p.status === 'COMPLETED').length;
  const blocked    = payments.filter((p) => p.status === 'BLOCKED').length;
  const pending    = payments.filter((p) => p.status === 'MANUAL_REVIEW_REQUIRED').length;
  const failed     = payments.filter((p) => p.status === 'FAILED').length;
  const fraudBlocked = payments.filter((p) => p.fraudStatus === 'BLOCKED').length;

  const riskScored = payments.filter((p) => p.riskScore !== null);
  const avgRisk = riskScored.length > 0
    ? riskScored.reduce((s, p) => s + p.riskScore!, 0) / riskScored.length
    : null;

  // Breakdown counts
  const ptCounts: Record<string, number> = {};
  const marlCounts: Record<string, number> = {};
  const fraudStatusCounts: Record<string, number> = {};
  const riskCounts: Record<string, number> = { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0, UNKNOWN: 0 };

  payments.forEach((p) => {
    ptCounts[p.paymentType] = (ptCounts[p.paymentType] || 0) + 1;

    const action = p.marlAssessment?.action ?? 'NO MARL';
    marlCounts[action] = (marlCounts[action] || 0) + 1;

    fraudStatusCounts[p.fraudStatus] = (fraudStatusCounts[p.fraudStatus] || 0) + 1;

    const rl = (p.riskLevel ?? 'UNKNOWN') as keyof typeof riskCounts;
    riskCounts[rl] = (riskCounts[rl] || 0) + 1;
  });

  // Pending reviews and recent blocked (up to 5 each)
  const pendingReviews = payments.filter((p) => p.status === 'MANUAL_REVIEW_REQUIRED').slice(0, 5);
  const recentBlocked  = payments.filter((p) => p.status === 'BLOCKED').slice(0, 5);

  // ── Avg MARL confidence ───────────────────────────────────────────────────────
  const assessed = payments.filter((p) => p.marlAssessment !== null);
  const avgConf  = assessed.length > 0
    ? assessed.reduce((s, p) => s + p.marlAssessment!.confidence, 0) / assessed.length
    : null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">📊 Dashboard</h1>
          <p className="mt-1 text-sm text-gray-500">
            Live snapshot · last <span className="font-medium">{total}</span> payments · auto-refreshes every 30 s
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-xs text-gray-400 tabular-nums">
            Updated: {lastRefresh.toLocaleTimeString()}
          </span>
          <button
            onClick={load}
            disabled={loading}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-md bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 disabled:opacity-50 transition-colors"
          >
            {loading ? <LoadingSpinner size="sm" /> : '🔄'} Refresh
          </button>
        </div>
      </div>

      {loading && payments.length === 0 ? (
        <div className="flex justify-center py-20"><LoadingSpinner /></div>
      ) : (
        <>
          {/* ── Stat Cards ── */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
            <StatCard label="Total"         value={total}       icon="💳" />
            <StatCard label="Completed"     value={completed}   icon="🎉" valueColor="text-green-600" />
            <StatCard label="Blocked"       value={blocked}     icon="🚫" valueColor="text-red-600"    to="/payments" />
            <StatCard label="Pending Review" value={pending}    icon="👁️" valueColor="text-yellow-600" to="/payments" />
            <StatCard label="Failed"        value={failed}      icon="⚠️" valueColor="text-orange-600" />
            <StatCard label="Fraud Blocked" value={fraudBlocked} icon="🕵️" valueColor="text-red-700" />
          </div>

          {/* ── Summary Metrics ── */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Card>
              <div className="flex items-center gap-4">
                <span className="text-4xl">🎯</span>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">Avg Risk Score</p>
                  <p className={`text-2xl font-bold ${
                    avgRisk === null      ? 'text-gray-400'
                    : avgRisk > 0.7      ? 'text-red-600'
                    : avgRisk > 0.4      ? 'text-yellow-600'
                    :                      'text-green-600'
                  }`}>
                    {avgRisk !== null ? (avgRisk * 100).toFixed(1) + '%' : 'N/A'}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    Across {riskScored.length} assessed payments
                  </p>
                </div>
              </div>
            </Card>
            <Card>
              <div className="flex items-center gap-4">
                <span className="text-4xl">🤖</span>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">Avg MARL Confidence</p>
                  <p className={`text-2xl font-bold ${
                    avgConf === null   ? 'text-gray-400'
                    : avgConf > 0.8   ? 'text-green-600'
                    : avgConf > 0.5   ? 'text-yellow-600'
                    :                   'text-orange-600'
                  }`}>
                    {avgConf !== null ? (avgConf * 100).toFixed(1) + '%' : 'N/A'}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    Across {assessed.length} MARL-assessed payments
                  </p>
                </div>
              </div>
            </Card>
          </div>

          {/* ── Distribution Charts ── */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Payment Types */}
            <Card title="Payment Type Breakdown">
              <div className="space-y-3 pt-1">
                {Object.entries(PT_META).map(([k, m]) => (
                  <BarRow key={k} label={m.label} icon={m.icon} count={ptCounts[k] || 0} total={total} barColor={m.bar} />
                ))}
              </div>
            </Card>

            {/* Risk Levels */}
            <Card title="Risk Level Distribution">
              <div className="space-y-3 pt-1">
                <BarRow label="Low"      count={riskCounts.LOW}      total={total} barColor="bg-green-400"  />
                <BarRow label="Medium"   count={riskCounts.MEDIUM}   total={total} barColor="bg-yellow-400" />
                <BarRow label="High"     count={riskCounts.HIGH}     total={total} barColor="bg-orange-400" />
                <BarRow label="Critical" count={riskCounts.CRITICAL} total={total} barColor="bg-red-500"    />
                <BarRow label="Unknown"  count={riskCounts.UNKNOWN}  total={total} barColor="bg-gray-300"   />
              </div>
            </Card>

            {/* MARL Actions */}
            <Card title="MARL Action Distribution">
              <div className="space-y-3 pt-1">
                {MARL_META.map((m) => (
                  <BarRow key={m.action} label={m.action} icon={m.icon} count={marlCounts[m.action] || 0} total={total} barColor={m.bar} />
                ))}
              </div>
            </Card>

            {/* Fraud Status */}
            <Card title="Fraud Status Distribution">
              <div className="space-y-3 pt-1">
                {FRAUD_META.map((m) => (
                  <BarRow
                    key={m.status}
                    label={m.status.replace(/_/g, ' ')}
                    count={fraudStatusCounts[m.status] || 0}
                    total={total}
                    barColor={m.bar}
                  />
                ))}
              </div>
            </Card>
          </div>

          {/* ── Pending Reviews ── */}
          {pending > 0 && (
            <Card title={`🚨 Payments Pending Manual Review (${pending} total)`}>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-yellow-50">
                    <tr>
                      {['Payment ID', 'Type', 'Amount', 'Risk Score', 'Risk Level', 'Initiated', ''].map((h) => (
                        <th key={h} className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {pendingReviews.map((p) => (
                      <tr key={p.paymentId} className="hover:bg-yellow-50">
                        <td className="px-4 py-3 whitespace-nowrap">
                          <Link to={`/payments/${p.paymentId}`} className="font-mono text-xs text-blue-600 hover:underline" title={p.paymentId}>
                            {p.paymentId.substring(0, 8)}…
                          </Link>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-600">{p.paymentType}</td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
                          <div className="flex flex-col gap-0.5">
                            <span>{formatCurrency(p.amount, p.fromCurrency)}</span>
                            {p.toCurrency && p.toCurrency !== p.fromCurrency && (
                              <span className="text-xs text-amber-600">
                                → {p.toCurrency}{p.convertedAmount != null ? ` · ${formatCurrency(p.convertedAmount, p.toCurrency)}` : ''}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm tabular-nums">
                          {p.riskScore !== null ? (p.riskScore * 100).toFixed(1) + '%' : '—'}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          {p.riskLevel && <Badge className={getRiskLevelColor(p.riskLevel)}>{p.riskLevel}</Badge>}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-500 tabular-nums">{formatDate(p.initiatedAt)}</td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          <Link to={`/payments/${p.paymentId}`} className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-yellow-100 text-yellow-800 text-xs font-semibold hover:bg-yellow-200 transition-colors">
                            Review →
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {pending > 5 && (
                <div className="mt-3 text-center">
                  <Link to="/payments" className="text-sm text-blue-600 hover:underline">
                    View all {pending} pending payments →
                  </Link>
                </div>
              )}
            </Card>
          )}

          {/* ── Recent Blocked ── */}
          {blocked > 0 && (
            <Card title={`🚫 Recent Blocked Payments (${blocked} total)`}>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-red-50">
                    <tr>
                      {['Payment ID', 'Type', 'Amount', 'Risk Score', 'Block Reason', 'Blocked At'].map((h) => (
                        <th key={h} className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {recentBlocked.map((p) => (
                      <tr key={p.paymentId} className="hover:bg-red-50">
                        <td className="px-4 py-3 whitespace-nowrap">
                          <Link to={`/payments/${p.paymentId}`} className="font-mono text-xs text-blue-600 hover:underline" title={p.paymentId}>
                            {p.paymentId.substring(0, 8)}…
                          </Link>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-600">{p.paymentType}</td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-900">
                          <div className="flex flex-col gap-0.5">
                            <span>{formatCurrency(p.amount, p.fromCurrency)}</span>
                            {p.toCurrency && p.toCurrency !== p.fromCurrency && (
                              <span className="text-xs text-amber-600">
                                → {p.toCurrency}{p.convertedAmount != null ? ` · ${formatCurrency(p.convertedAmount, p.toCurrency)}` : ''}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-sm tabular-nums">
                          {p.riskScore !== null ? (p.riskScore * 100).toFixed(1) + '%' : '—'}
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-500 max-w-xs truncate">{p.blockReason || '—'}</td>
                        <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-500 tabular-nums">
                          {p.blockedAt ? formatDate(p.blockedAt) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {blocked > 5 && (
                <div className="mt-3 text-center">
                  <Link to="/payments" className="text-sm text-blue-600 hover:underline">
                    View all {blocked} blocked payments →
                  </Link>
                </div>
              )}
            </Card>
          )}
        </>
      )}
    </div>
  );
};
