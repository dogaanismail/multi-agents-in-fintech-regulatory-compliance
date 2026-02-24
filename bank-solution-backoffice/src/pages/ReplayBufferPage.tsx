import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { marlTrainingService } from '@/api/marlTrainingService';
import { ExperienceEntry, ReplayBufferAggStats } from '@/types';
import { Card, Badge, LoadingSpinner } from '@/components/common';
import { formatDate } from '@/utils/formatters';

// ─── Helpers ─────────────────────────────────────────────────────────────────

const actionStyle = (action: string) => {
  switch (action) {
    case 'ALLOW':  return 'bg-green-100 text-green-800';
    case 'BLOCK':  return 'bg-red-100 text-red-800';
    case 'REVIEW': return 'bg-amber-100 text-amber-800';
    default:       return 'bg-gray-100 text-gray-600';
  }
};

const actionIcon = (action: string) => {
  switch (action) {
    case 'ALLOW':  return '✅';
    case 'BLOCK':  return '🚫';
    case 'REVIEW': return '🔍';
    default:       return '•';
  }
};

const rewardStyle = (v: number) => {
  if (v > 0.01)  return 'text-green-700 font-semibold';
  if (v < -0.01) return 'text-red-600 font-semibold';
  return 'text-gray-500';
};

// ─── PaymentIdCell ────────────────────────────────────────────────────────────

const PaymentIdCell: React.FC<{ paymentId: string }> = ({ paymentId }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    navigator.clipboard.writeText(paymentId).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  };

  return (
    <div className="flex items-center gap-1.5 min-w-0">
      {/* Clickable short ID → payment detail */}
      <Link
        to={`/payments/${paymentId}`}
        className="font-mono text-xs text-blue-600 hover:text-blue-800 hover:underline truncate"
        title={`View payment ${paymentId}`}
      >
        {paymentId.slice(0, 8)}…
      </Link>

      {/* Copy button */}
      <button
        onClick={handleCopy}
        title={copied ? 'Copied!' : 'Copy full payment ID'}
        className={`flex-shrink-0 p-0.5 rounded transition-colors ${
          copied
            ? 'text-green-600'
            : 'text-gray-300 hover:text-gray-600'
        }`}
      >
        {copied ? (
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        ) : (
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
            <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1" />
          </svg>
        )}
      </button>
    </div>
  );
};

// ─── StatCard ─────────────────────────────────────────────────────────────────

const StatCard: React.FC<{
  label: string;
  value: React.ReactNode;
  sub?: string;
  accent?: string;
}> = ({ label, value, sub, accent = 'bg-blue-500' }) => (
  <div className="bg-white rounded-lg shadow p-5 flex items-start gap-4">
    <div className={`${accent} w-1 rounded-full self-stretch`} />
    <div>
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className="mt-1 text-2xl font-bold text-gray-900">{value}</p>
      {sub && <p className="mt-1 text-xs text-gray-500">{sub}</p>}
    </div>
  </div>
);

// ─── Page ─────────────────────────────────────────────────────────────────────

export const ReplayBufferPage: React.FC = () => {
  const [experiences, setExperiences] = useState<ExperienceEntry[]>([]);
  const [aggStats, setAggStats] = useState<ReplayBufferAggStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(false);

  // Filters & pagination
  const [actionFilter, setActionFilter] = useState<string>('ALL');
  const [sourceFilter, setSourceFilter] = useState<string>('ALL');
  const [limit, setLimit] = useState(50);
  const [offset, setOffset] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [entries, stats] = await Promise.all([
        marlTrainingService.getExperiences(limit, offset),
        marlTrainingService.getExperienceStats(),
      ]);
      setExperiences(entries);
      setAggStats(stats);
    } catch {
      // non-fatal
    } finally {
      setLoading(false);
    }
  }, [limit, offset]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(load, 10_000);
    return () => clearInterval(id);
  }, [autoRefresh, load]);

  const filtered = experiences
    .filter(e => actionFilter === 'ALL' || e.marl_action === actionFilter)
    .filter(e => sourceFilter === 'ALL' || e.reward_source === sourceFilter);

  return (
    <div className="space-y-6">

      {/* ── Header ── */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">🧠 Replay Buffer</h1>
          <p className="mt-1 text-sm text-gray-500">
            Every payment decision stored as a training experience — reward signal, agent actions,
            and MADDPG learning status.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
            <div
              onClick={() => setAutoRefresh(v => !v)}
              className={`relative w-10 h-5 rounded-full transition-colors ${autoRefresh ? 'bg-blue-500' : 'bg-gray-300'}`}
            >
              <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${autoRefresh ? 'translate-x-5' : ''}`} />
            </div>
            Auto-refresh
          </label>
          <button
            onClick={load}
            className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
          >
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* ── Aggregate stat cards ── */}
      {aggStats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <StatCard
            label="Avg Effective Reward"
            value={aggStats.avg_effective_reward != null ? aggStats.avg_effective_reward.toFixed(4) : '—'}
            sub="Across all buffer entries"
            accent="bg-indigo-500"
          />
          <StatCard
            label="Manual Review"
            value={aggStats.manual_review_count}
            sub={`of ${aggStats.total_experiences} total (${aggStats.total_experiences > 0
              ? Math.round(aggStats.manual_review_count / aggStats.total_experiences * 100) : 0}%)`}
            accent="bg-amber-400"
          />
          <StatCard
            label="Training Coverage"
            value={`${aggStats.total_experiences > 0
              ? Math.round(aggStats.used_in_training_count / aggStats.total_experiences * 100) : 0}%`}
            sub={`${aggStats.used_in_training_count} / ${aggStats.total_experiences} trained`}
            accent="bg-green-500"
          />
          <StatCard
            label="Avg Confidence"
            value={aggStats.avg_confidence != null ? `${(aggStats.avg_confidence * 100).toFixed(1)}%` : '—'}
            sub={`Avg risk score: ${aggStats.avg_risk_score != null
              ? (aggStats.avg_risk_score * 100).toFixed(1) + '%' : '—'}`}
            accent="bg-purple-500"
          />
        </div>
      )}

      {/* ── Action distribution pills ── */}
      {aggStats && Object.keys(aggStats.action_counts).length > 0 && (
        <div className="bg-white rounded-lg shadow p-4 flex flex-wrap items-center gap-3">
          <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide mr-1">
            Action Distribution
          </span>
          {(['ALLOW', 'BLOCK', 'REVIEW'] as const).map(action => {
            const count = aggStats.action_counts[action] ?? 0;
            const pct = aggStats.total_experiences > 0
              ? Math.round(count / aggStats.total_experiences * 100) : 0;
            return (
              <button
                key={action}
                onClick={() => setActionFilter(f => f === action ? 'ALL' : action)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium border-2 transition-all ${
                  actionFilter === action
                    ? `${actionStyle(action)} border-current scale-105 shadow-sm`
                    : `${actionStyle(action)} border-transparent opacity-80 hover:opacity-100`
                }`}
              >
                <span>{actionIcon(action)}</span>
                <span>{action}</span>
                <span className="font-bold">{count}</span>
                <span className="opacity-60 text-xs">({pct}%)</span>
              </button>
            );
          })}
          {actionFilter !== 'ALL' && (
            <button
              onClick={() => setActionFilter('ALL')}
              className="ml-1 text-xs text-gray-400 hover:text-gray-600 underline"
            >
              clear filter
            </button>
          )}
        </div>
      )}

      {/* ── Experience table ── */}
      <Card title={`Experience Entries${aggStats ? ` (${aggStats.total_experiences} total)` : ''}`}>

        {/* toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5">
              <label className="text-xs text-gray-500 uppercase tracking-wide">Action</label>
              <select
                value={actionFilter}
                onChange={e => setActionFilter(e.target.value)}
                className="text-sm border border-gray-300 rounded px-2 py-1"
              >
                {['ALL', 'ALLOW', 'BLOCK', 'REVIEW'].map(a => (
                  <option key={a} value={a}>{a}</option>
                ))}
              </select>
            </div>
            <div className="flex items-center gap-1.5">
              <label className="text-xs text-gray-500 uppercase tracking-wide">Source</label>
              <select
                value={sourceFilter}
                onChange={e => setSourceFilter(e.target.value)}
                className="text-sm border border-gray-300 rounded px-2 py-1"
              >
                {['ALL', 'automated', 'manual_review'].map(s => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
            <span className="text-xs text-gray-400">{filtered.length} shown</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              disabled={offset === 0}
              onClick={() => setOffset(Math.max(0, offset - limit))}
              className="px-2.5 py-1 text-sm border rounded disabled:opacity-40 hover:bg-gray-50 transition-colors"
            >
              ‹ Prev
            </button>
            <span className="text-xs text-gray-500 min-w-[60px] text-center">
              {offset + 1}–{offset + experiences.length}
            </span>
            <button
              disabled={experiences.length < limit}
              onClick={() => setOffset(offset + limit)}
              className="px-2.5 py-1 text-sm border rounded disabled:opacity-40 hover:bg-gray-50 transition-colors"
            >
              Next ›
            </button>
            <select
              value={limit}
              onChange={e => { setLimit(Number(e.target.value)); setOffset(0); }}
              className="text-sm border border-gray-300 rounded px-2 py-1 ml-1"
            >
              {[25, 50, 100].map(n => <option key={n} value={n}>{n} / page</option>)}
            </select>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-12"><LoadingSpinner size="md" /></div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-14 text-gray-400">
            <div className="text-5xl mb-3">📭</div>
            <p className="font-medium">No experiences found</p>
            <p className="text-sm mt-1">Try clearing the filters, or wait for new payments to be processed.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {[
                    'Payment ID', 'Action', 'Confidence', 'Q-Value',
                    'Risk Score', 'Auto Reward', 'Manual Reward', 'Effective Reward',
                    'Source', 'Trained', 'Date',
                  ].map(h => (
                    <th
                      key={h}
                      className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-100">
                {filtered.map(e => (
                  <tr
                    key={e.id}
                    className={`hover:bg-blue-50/30 transition-colors ${
                      e.reward_source === 'manual_review' ? 'bg-amber-50/40' : ''
                    }`}
                  >
                    {/* Payment ID — link + copy */}
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <PaymentIdCell paymentId={e.payment_id} />
                    </td>

                    {/* Action */}
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <Badge className={actionStyle(e.marl_action)}>
                        {actionIcon(e.marl_action)} {e.marl_action}
                      </Badge>
                    </td>

                    {/* Confidence */}
                    <td className="px-3 py-2.5 whitespace-nowrap text-gray-700">
                      {(e.marl_confidence * 100).toFixed(1)}%
                    </td>

                    {/* Q-Value */}
                    <td className={`px-3 py-2.5 whitespace-nowrap font-mono text-xs ${rewardStyle(e.marl_q_value)}`}>
                      {e.marl_q_value.toFixed(4)}
                    </td>

                    {/* Risk Score */}
                    <td className="px-3 py-2.5 whitespace-nowrap text-gray-700">
                      {(e.mean_risk_score * 100).toFixed(1)}%
                    </td>

                    {/* Auto Reward */}
                    <td className={`px-3 py-2.5 whitespace-nowrap font-mono text-xs ${rewardStyle(e.automated_reward)}`}>
                      {e.automated_reward > 0 ? '+' : ''}{e.automated_reward.toFixed(4)}
                    </td>

                    {/* Manual Reward */}
                    <td className={`px-3 py-2.5 whitespace-nowrap font-mono text-xs ${
                      e.manual_reward != null ? rewardStyle(e.manual_reward) : 'text-gray-300'
                    }`}>
                      {e.manual_reward != null
                        ? `${e.manual_reward > 0 ? '+' : ''}${e.manual_reward.toFixed(4)}`
                        : '—'}
                    </td>

                    {/* Effective Reward — bold, colour coded */}
                    <td className={`px-3 py-2.5 whitespace-nowrap font-mono text-sm font-bold ${rewardStyle(e.effective_reward)}`}>
                      {e.effective_reward > 0 ? '+' : ''}{e.effective_reward.toFixed(4)}
                    </td>

                    {/* Source */}
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      {e.reward_source === 'manual_review' ? (
                        <Badge className="bg-amber-100 text-amber-800">👤 manual</Badge>
                      ) : (
                        <Badge className="bg-gray-100 text-gray-600">⚡ auto</Badge>
                      )}
                    </td>

                    {/* Trained */}
                    <td className="px-3 py-2.5 whitespace-nowrap text-center">
                      {e.is_used_in_training ? (
                        <span className="text-green-600 font-bold text-base" title="Used in training">✓</span>
                      ) : (
                        <span className="text-gray-300 text-base" title="Pending training">○</span>
                      )}
                    </td>

                    {/* Date */}
                    <td className="px-3 py-2.5 whitespace-nowrap font-mono text-xs text-gray-500">
                      {formatDate(e.created_at)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
};
