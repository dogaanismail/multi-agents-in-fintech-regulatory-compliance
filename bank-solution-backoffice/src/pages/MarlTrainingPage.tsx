import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { marlTrainingService } from '@/api/marlTrainingService';
import {
  TrainingStatusResponse,
  TrainingRunResponse,
  BufferStatsResponse,
  TriggerTrainingResponse,
} from '@/types';
import { Card, Badge, LoadingSpinner } from '@/components/common';
import { formatDate } from '@/utils/formatters';

// ─── Helpers ─────────────────────────────────────────────────────────────────

const statusColor = (status: string) => {
  switch (status) {
    case 'SUCCESS':
      return 'bg-green-100 text-green-800';
    case 'FAILED':
      return 'bg-red-100 text-red-800';
    case 'SKIPPED':
      return 'bg-gray-100 text-gray-600';
    case 'IN_PROGRESS':
      return 'bg-blue-100 text-blue-800';
    default:
      return 'bg-yellow-100 text-yellow-800';
  }
};

const fmt4 = (v: number | null) => (v == null ? '—' : v.toFixed(4));

const duration = (started: string, completed: string | null) => {
  if (!completed) return '—';
  const ms = new Date(completed).getTime() - new Date(started).getTime();
  return ms < 1000 ? `${ms}ms` : `${(ms / 1000).toFixed(1)}s`;
};

// ─── Sub-components ──────────────────────────────────────────────────────────

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

export const MarlTrainingPage: React.FC = () => {
  const [status, setStatus] = useState<TrainingStatusResponse | null>(null);
  const [buffer, setBuffer] = useState<BufferStatsResponse | null>(null);
  const [history, setHistory] = useState<TrainingRunResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [triggerLoading, setTriggerLoading] = useState(false);
  const [triggerResult, setTriggerResult] = useState<TriggerTrainingResponse | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [historyLimit, setHistoryLimit] = useState(20);
  const [error, setError] = useState<string | null>(null);

  const loadStatus = useCallback(async () => {
    try {
      const [s, b] = await Promise.all([
        marlTrainingService.getStatus(),
        marlTrainingService.getBufferStats(),
      ]);
      setStatus(s);
      setBuffer(b);
      setError(null);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      const runs = await marlTrainingService.getHistory(historyLimit);
      setHistory(runs);
    } catch {
      // history failure is non-fatal
    } finally {
      setHistoryLoading(false);
    }
  }, [historyLimit]);

  useEffect(() => {
    loadStatus();
    loadHistory();
  }, [loadStatus, loadHistory]);

  // Auto-refresh every 10s
  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(() => {
      loadStatus();
      loadHistory();
    }, 10_000);
    return () => clearInterval(id);
  }, [autoRefresh, loadStatus, loadHistory]);

  const handleTrigger = async () => {
    setTriggerLoading(true);
    setTriggerResult(null);
    try {
      const result = await marlTrainingService.triggerTraining();
      setTriggerResult(result);
      // Refresh status after a short delay so new run appears
      setTimeout(() => {
        loadStatus();
        loadHistory();
      }, 2000);
    } catch (e) {
      setTriggerResult({ triggered: false, reason: (e as Error).message, available_experiences: null, batch_size: null });
    } finally {
      setTriggerLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">🤖 MARL Training</h1>
          <p className="mt-1 text-gray-500 text-sm">
            Monitor and control offline MADDPG training for the multi-agent AML detection system.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
            <div
              onClick={() => setAutoRefresh(v => !v)}
              className={`relative w-10 h-5 rounded-full transition-colors ${autoRefresh ? 'bg-blue-500' : 'bg-gray-300'}`}
            >
              <span
                className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${autoRefresh ? 'translate-x-5' : ''}`}
              />
            </div>
            Auto-refresh (10s)
          </label>
          <button
            onClick={() => { loadStatus(); loadHistory(); }}
            className="px-3 py-1.5 text-sm bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
          >
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* View Replay Buffer link card */}
      <Link
        to="/replay-buffer"
        className="group block bg-white rounded-lg shadow hover:shadow-md transition-shadow border border-transparent hover:border-indigo-200"
      >
        <div className="p-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="text-4xl">🧠</div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 group-hover:text-indigo-700 transition-colors">
                Replay Buffer Experience Log
              </h3>
              <p className="text-sm text-gray-500 mt-0.5">
                Browse every payment decision stored as a training tuple — reward signals,
                agent actions, confidence scores, and training status.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-indigo-600 font-medium text-sm group-hover:gap-3 transition-all">
            <span>View Replay Buffer</span>
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
            </svg>
          </div>
        </div>
      </Link>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm">
          ⚠️ Could not reach MARL Orchestrator: {error}
        </div>
      )}

      {/* Buffer Stats */}
      {loading ? (
        <div className="flex justify-center py-8"><LoadingSpinner size="lg" /></div>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard
              label="Total Experiences"
              value={buffer?.total_experiences ?? '—'}
              sub="All entries in replay buffer"
              accent="bg-blue-500"
            />
            <StatCard
              label="Unused Experiences"
              value={buffer?.unused_experiences ?? '—'}
              sub="Available for next training run"
              accent="bg-amber-400"
            />
            <StatCard
              label="Used Experiences"
              value={buffer?.used_experiences ?? '—'}
              sub="Already consumed in training"
              accent="bg-green-500"
            />
          </div>

          {/* Training Status */}
          <Card title="Training Scheduler">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <StatusRow
                label="Scheduler"
                value={
                  status?.scheduler_running ? (
                    <Badge className="bg-green-100 text-green-800">● Running</Badge>
                  ) : (
                    <Badge className="bg-red-100 text-red-800">● Stopped</Badge>
                  )
                }
              />
              <StatusRow
                label="Training Now"
                value={
                  status?.is_training ? (
                    <Badge className="bg-blue-100 text-blue-800 animate-pulse">⚙ In Progress</Badge>
                  ) : (
                    <Badge className="bg-gray-100 text-gray-600">Idle</Badge>
                  )
                }
              />
              <StatusRow
                label="Interval"
                value={`${status?.training_interval_seconds ?? '—'}s`}
              />
              <StatusRow
                label="Min Experiences Required"
                value={status?.min_experiences_required ?? '—'}
              />
              <StatusRow
                label="Unused / Total"
                value={`${status?.unused_experiences ?? '—'} / ${status?.total_experiences ?? '—'}`}
              />
              <StatusRow
                label="Total Training Runs"
                value={status?.total_training_runs ?? '—'}
              />
              <StatusRow
                label="Total Experiences Trained"
                value={status?.total_experiences_trained ?? '—'}
              />
              <StatusRow
                label="Last Training"
                value={status?.last_training_at ? formatDate(status.last_training_at) : '—'}
              />
            </div>
          </Card>

          {/* Manual Trigger */}
          <Card title="Manual Training Trigger">
            <div className="flex flex-col gap-4">
              <p className="text-sm text-gray-600">
                Immediately schedules a training cycle outside the regular interval. The training
                runs asynchronously — this returns instantly and the result appears in the history
                table below.
              </p>
              <div className="flex items-center gap-4">
                <button
                  onClick={handleTrigger}
                  disabled={triggerLoading || status?.is_training}
                  className={`px-5 py-2.5 rounded-lg text-sm font-semibold transition-colors ${
                    triggerLoading || status?.is_training
                      ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                      : 'bg-indigo-600 hover:bg-indigo-700 text-white'
                  }`}
                >
                  {triggerLoading ? '⏳ Triggering…' : '▶ Trigger Training Now'}
                </button>
                {status?.is_training && (
                  <span className="text-sm text-blue-600">
                    Training is already running…
                  </span>
                )}
              </div>

              {triggerResult && (
                <div
                  className={`rounded-lg p-3 text-sm border ${
                    triggerResult.triggered
                      ? 'bg-green-50 border-green-200 text-green-800'
                      : 'bg-yellow-50 border-yellow-200 text-yellow-800'
                  }`}
                >
                  {triggerResult.triggered ? (
                    <>
                      ✅ Training triggered — batch_size={triggerResult.batch_size}, experiences={triggerResult.available_experiences}
                    </>
                  ) : (
                    <>⚠️ Not triggered — {triggerResult.reason}</>
                  )}
                </div>
              )}
            </div>
          </Card>
        </>
      )}

      {/* ── Loss Trend Chart ── */}
      {!historyLoading && history.length >= 2 && (() => {
        // Use SUCCESS runs with non-null losses, reversed to show oldest→newest left-to-right
        const CHART_W = 600;
        const CHART_H = 160;
        const PAD = { top: 12, right: 16, bottom: 28, left: 46 };
        const plotW = CHART_W - PAD.left - PAD.right;
        const plotH = CHART_H - PAD.top - PAD.bottom;

        const runs = [...history]
          .reverse()
          .filter(
            (r) =>
              r.status === 'SUCCESS' &&
              r.critic_loss !== null &&
              (r.actor_transaction_loss !== null ||
                r.actor_customer_loss !== null ||
                r.actor_network_loss !== null),
          );

        if (runs.length < 2) return null;

        const criticVals = runs.map((r) => r.critic_loss ?? 0);
        const actorVals  = runs.map((r) => {
          const vals = [r.actor_transaction_loss, r.actor_customer_loss, r.actor_network_loss].filter(
            (v): v is number => v !== null,
          );
          return vals.length > 0 ? vals.reduce((a, b) => a + b, 0) / vals.length : 0;
        });

        const allVals = [...criticVals, ...actorVals];
        const yMax = Math.max(...allVals) * 1.1 || 1;
        const yMin = 0;

        const toX = (i: number) => PAD.left + (i / (runs.length - 1)) * plotW;
        const toY = (v: number) => PAD.top + plotH - ((v - yMin) / (yMax - yMin)) * plotH;

        const polyline = (vals: number[], stroke: string, dash?: string) => {
          const pts = vals.map((v, i) => `${toX(i)},${toY(v)}`).join(' ');
          return (
            <polyline
              points={pts}
              fill="none"
              stroke={stroke}
              strokeWidth="1.8"
              strokeLinejoin="round"
              strokeLinecap="round"
              strokeDasharray={dash}
            />
          );
        };

        const dots = (vals: number[], fill: string) =>
          vals.map((v, i) => (
            <circle key={i} cx={toX(i)} cy={toY(v)} r="3" fill={fill} />
          ));

        // Y-axis tick labels (4 ticks)
        const yTicks = [0, 0.33, 0.67, 1].map((t) => {
          const val = yMin + t * (yMax - yMin);
          return { y: toY(val), label: val.toFixed(3) };
        });

        // X-axis tick labels (show up to 6 evenly spaced)
        const xStep = Math.max(1, Math.floor(runs.length / 5));
        const xTicks = runs
          .map((r, i) => ({ i, label: new Date(r.started_at).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) }))
          .filter((_, i) => i % xStep === 0 || i === runs.length - 1);

        return (
          <Card title="📈 Training Loss Trend">
            <div className="flex items-center gap-5 mb-3">
              <span className="flex items-center gap-1.5 text-xs font-medium text-gray-600">
                <span className="inline-block w-5 h-0.5 bg-blue-500 rounded" /> Critic Loss
              </span>
              <span className="flex items-center gap-1.5 text-xs font-medium text-gray-600">
                <span className="inline-block w-5 h-0.5 bg-orange-400 rounded" style={{ borderTop: '2px dashed' }} /> Avg Actor Loss
              </span>
              <span className="ml-auto text-xs text-gray-400">{runs.length} successful runs (oldest → newest)</span>
            </div>
            <div className="overflow-x-auto">
              <svg
                viewBox={`0 0 ${CHART_W} ${CHART_H}`}
                className="w-full"
                style={{ minWidth: '320px', maxHeight: '200px' }}
              >
                {/* Grid lines */}
                {yTicks.map(({ y, label }) => (
                  <g key={label}>
                    <line x1={PAD.left} y1={y} x2={CHART_W - PAD.right} y2={y} stroke="#e5e7eb" strokeWidth="1" />
                    <text x={PAD.left - 4} y={y + 4} textAnchor="end" fontSize="9" fill="#9ca3af">{label}</text>
                  </g>
                ))}
                {/* X-axis labels */}
                {xTicks.map(({ i, label }) => (
                  <text key={i} x={toX(i)} y={CHART_H - 4} textAnchor="middle" fontSize="9" fill="#9ca3af">
                    {label}
                  </text>
                ))}
                {/* Lines */}
                {polyline(actorVals, '#fb923c', '4 2')}
                {polyline(criticVals, '#3b82f6')}
                {/* Dots */}
                {dots(actorVals, '#fb923c')}
                {dots(criticVals, '#3b82f6')}
              </svg>
            </div>
            {(() => {
              const lastCritic = criticVals[criticVals.length - 1];
              const prevCritic = criticVals[criticVals.length - 2];
              const improving = lastCritic < prevCritic;
              return (
                <p className={`mt-2 text-xs font-medium ${improving ? 'text-green-700' : 'text-orange-700'}`}>
                  {improving
                    ? `✅ Critic loss decreased ${((1 - lastCritic / prevCritic) * 100).toFixed(1)}% from last run — model is improving`
                    : `⚠️ Critic loss increased ${((lastCritic / prevCritic - 1) * 100).toFixed(1)}% from last run — watch for instability`}
                </p>
              );
            })()}
          </Card>
        );
      })()}

      {/* Training History Table */}
      <Card title="Training Run History">
        <div className="flex justify-between items-center mb-4">
          <p className="text-sm text-gray-500">
            {history.length} run{history.length !== 1 ? 's' : ''} shown
          </p>
          <div className="flex items-center gap-2">
            <label className="text-sm text-gray-600">Show last</label>
            <select
              value={historyLimit}
              onChange={e => setHistoryLimit(Number(e.target.value))}
              className="text-sm border border-gray-300 rounded px-2 py-1"
            >
              {[10, 20, 50, 100].map(n => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
            <label className="text-sm text-gray-600">runs</label>
          </div>
        </div>

        {historyLoading ? (
          <div className="flex justify-center py-8"><LoadingSpinner size="md" /></div>
        ) : history.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <div className="text-4xl mb-3">🤖</div>
            <p className="font-medium">No training runs yet</p>
            <p className="text-sm mt-1">Trigger a training cycle or wait for the scheduler.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  {['Started At', 'Status', 'Experiences', 'Steps', 'Batch', 'Critic Loss', 'Txn Loss', 'Cust Loss', 'Net Loss', 'Model Saved', 'Duration'].map(h => (
                    <th key={h} className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {history.map(run => (
                  <tr key={run.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-3 py-3 whitespace-nowrap text-gray-700 font-mono text-xs">
                      {formatDate(run.started_at)}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap">
                      <Badge className={statusColor(run.status)}>{run.status}</Badge>
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap text-gray-900 font-semibold">
                      {run.experiences_count}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap text-gray-700">
                      {run.train_steps_completed}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap text-gray-700">
                      {run.batch_size}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap font-mono text-gray-700">
                      {fmt4(run.critic_loss)}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap font-mono text-gray-700">
                      {fmt4(run.actor_transaction_loss)}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap font-mono text-gray-700">
                      {fmt4(run.actor_customer_loss)}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap font-mono text-gray-700">
                      {fmt4(run.actor_network_loss)}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap">
                      {run.model_saved ? (
                        <Badge className="bg-green-100 text-green-700">✓ Saved</Badge>
                      ) : (
                        <Badge className="bg-gray-100 text-gray-500">—</Badge>
                      )}
                    </td>
                    <td className="px-3 py-3 whitespace-nowrap text-gray-500 font-mono text-xs">
                      {duration(run.started_at, run.completed_at)}
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

// ─── Small helper component ───────────────────────────────────────────────────
const StatusRow: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
  <div className="flex justify-between items-center py-2 border-b border-gray-100 last:border-0">
    <span className="text-sm font-medium text-gray-600">{label}</span>
    <span className="text-sm text-gray-900">{value}</span>
  </div>
);
