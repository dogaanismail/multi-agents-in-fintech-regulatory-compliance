import React, {useCallback, useEffect, useState} from 'react';
import {learningEvidenceService} from '@/api/learningEvidenceService';
import {
    LearningCurvePoint,
    LearningReceipt,
    LearningSummaryResponse,
} from '@/types';
import {Badge, Card, LoadingSpinner} from '@/components/common';
import {formatDate} from '@/utils/formatters';

// ─── Helpers ─────────────────────────────────────────────────────────────────

const pct = (v: number | null | undefined) =>
    v == null ? '—' : `${(v * 100).toFixed(0)}%`;

const fmt2 = (v: number | null | undefined) => (v == null ? '—' : v.toFixed(2));

const actionBadge = (action: string) => {
    switch (action) {
        case 'ALLOW':
            return 'bg-green-100 text-green-800';
        case 'BLOCK':
            return 'bg-red-100 text-red-800';
        case 'REVIEW':
            return 'bg-yellow-100 text-yellow-800';
        default:
            return 'bg-gray-100 text-gray-600';
    }
};

// ─── Sub-components ──────────────────────────────────────────────────────────

const StatCard: React.FC<{
    label: string;
    value: React.ReactNode;
    sub?: string;
    accent?: string;
}> = ({label, value, sub, accent = 'bg-blue-500'}) => (
    <div className="bg-white rounded-lg shadow p-5 flex items-start gap-4">
        <div className={`${accent} w-1 rounded-full self-stretch`}/>
        <div>
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
            <p className="mt-1 text-2xl font-bold text-gray-900">{value}</p>
            {sub && <p className="mt-1 text-xs text-gray-500">{sub}</p>}
        </div>
    </div>
);

const AgreementCurveChart: React.FC<{ points: LearningCurvePoint[] }> = ({points}) => {
    const width = 720;
    const height = 220;
    const padding = {top: 16, right: 16, bottom: 28, left: 44};
    const plotWidth = width - padding.left - padding.right;
    const plotHeight = height - padding.top - padding.bottom;

    const usable = points.filter((p) => p.probe_agreement_rate != null);
    if (usable.length === 0) {
        return (
            <p className="text-sm text-gray-500">
                No probe evaluations recorded yet — the curve appears after the first
                training run that follows officer feedback.
            </p>
        );
    }

    const x = (index: number) =>
        padding.left + (usable.length === 1 ? plotWidth / 2 : (index / (usable.length - 1)) * plotWidth);
    const y = (rate: number) => padding.top + (1 - rate) * plotHeight;

    const path = usable
        .map((p, i) => `${i === 0 ? 'M' : 'L'} ${x(i).toFixed(1)} ${y(p.probe_agreement_rate as number).toFixed(1)}`)
        .join(' ');

    return (
        <svg viewBox={`0 0 ${width} ${height}`} className="w-full" role="img"
             aria-label="Officer agreement rate per training run">
            {[0, 0.25, 0.5, 0.75, 1].map((tick) => (
                <g key={tick}>
                    <line
                        x1={padding.left} x2={width - padding.right}
                        y1={y(tick)} y2={y(tick)}
                        stroke="#e5e7eb" strokeWidth={1}
                    />
                    <text x={padding.left - 8} y={y(tick) + 4} textAnchor="end"
                          className="fill-gray-400" fontSize={11}>
                        {pct(tick)}
                    </text>
                </g>
            ))}
            <path d={path} fill="none" stroke="#2563eb" strokeWidth={2.5}/>
            {usable.map((p, i) => (
                <circle key={p.training_run_id} cx={x(i)} cy={y(p.probe_agreement_rate as number)}
                        r={4} fill="#2563eb">
                    <title>
                        {`run ${i + 1}: ${pct(p.probe_agreement_rate)} agreement over ${p.probe_count} probes`}
                    </title>
                </circle>
            ))}
            <text x={padding.left} y={height - 8} className="fill-gray-400" fontSize={11}>
                oldest run
            </text>
            <text x={width - padding.right} y={height - 8} textAnchor="end"
                  className="fill-gray-400" fontSize={11}>
                latest run
            </text>
        </svg>
    );
};

// ─── Page ─────────────────────────────────────────────────────────────────────

export const LearningEvidencePage: React.FC = () => {
    const [summary, setSummary] = useState<LearningSummaryResponse | null>(null);
    const [curve, setCurve] = useState<LearningCurvePoint[]>([]);
    const [receipts, setReceipts] = useState<LearningReceipt[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadAll = useCallback(async () => {
        try {
            const [summaryResponse, curvePoints, receiptRows] = await Promise.all([
                learningEvidenceService.getSummary(),
                learningEvidenceService.getCurve(),
                learningEvidenceService.getReceipts(),
            ]);
            setSummary(summaryResponse);
            setCurve(curvePoints);
            setReceipts(receiptRows);
            setError(null);
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadAll();
        const interval = setInterval(loadAll, 30000);
        return () => clearInterval(interval);
    }, [loadAll]);

    if (loading) return <LoadingSpinner size="lg"/>;

    const probe = summary?.current_probe_evaluation ?? null;
    const flippedCount = receipts.filter((r) => r.policy_flipped).length;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900">Model Learning</h1>
                <p className="mt-1 text-sm text-gray-500">
                    Evidence that MADDPG learns from compliance-officer feedback: after every
                    training run, each officer-reviewed payment is replayed through the current
                    policy and the agreement is recorded.
                </p>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-4">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    label="Officer Agreement"
                    value={pct(probe?.agreement_rate)}
                    sub={probe ? `over ${probe.probe_count} reviewed payments` : 'no reviews yet'}
                    accent="bg-green-500"
                />
                <StatCard
                    label="Q-Value Gap"
                    value={fmt2(probe?.avg_q_gap)}
                    sub="critic preference for the officer's choice"
                    accent="bg-indigo-500"
                />
                <StatCard
                    label="Decisions Flipped"
                    value={`${flippedCount} / ${receipts.length}`}
                    sub="reviewed payments the policy now decides differently"
                    accent="bg-amber-500"
                />
                <StatCard
                    label="Gradient Steps"
                    value={summary?.total_gradient_steps ?? '—'}
                    sub={`${summary?.completed_training_runs ?? 0} completed training runs`}
                    accent="bg-blue-500"
                />
            </div>

            <Card title="Human-in-the-Loop Learning Curve">
                <p className="text-sm text-gray-500 mb-4">
                    Agreement between the policy and compliance-officer verdicts, evaluated on the
                    full probe set after each training run.
                </p>
                <AgreementCurveChart points={curve}/>
            </Card>

            <Card title="Decision Receipts">
                <p className="text-sm text-gray-500 mb-4">
                    Each row is a payment a compliance officer reviewed, re-evaluated by the
                    current policy. A flipped row means the officer&apos;s feedback changed the
                    model&apos;s mind for that exact payment state.
                </p>
                {receipts.length === 0 ? (
                    <p className="text-sm text-gray-500">No officer-reviewed payments yet.</p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 text-sm">
                            <thead>
                            <tr className="text-left text-xs font-medium text-gray-500 uppercase tracking-wide">
                                <th className="px-3 py-2">Payment</th>
                                <th className="px-3 py-2">Original</th>
                                <th className="px-3 py-2">Officer</th>
                                <th className="px-3 py-2">Policy Now</th>
                                <th className="px-3 py-2">Q(BLOCK)</th>
                                <th className="px-3 py-2">Q(ALLOW)</th>
                                <th className="px-3 py-2">Agrees</th>
                                <th className="px-3 py-2">Reviewed</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                            {receipts.map((receipt) => (
                                <tr key={`${receipt.payment_id}-${receipt.reviewed_at}`}>
                                    <td className="px-3 py-2 font-mono text-xs text-gray-600">
                                        {receipt.payment_id.slice(0, 8)}…
                                    </td>
                                    <td className="px-3 py-2">
                                        <Badge className={actionBadge(receipt.original_action)}>
                                            {receipt.original_action}
                                        </Badge>
                                        <span className="ml-1 text-xs text-gray-400">
                        {pct(receipt.original_confidence)}
                      </span>
                                    </td>
                                    <td className="px-3 py-2">
                                        <Badge className={receipt.officer_decision === 'APPROVE'
                                            ? 'bg-green-100 text-green-800'
                                            : 'bg-red-100 text-red-800'}>
                                            {receipt.officer_decision}
                                        </Badge>
                                    </td>
                                    <td className="px-3 py-2">
                                        <Badge className={actionBadge(receipt.current_action)}>
                                            {receipt.current_action}
                                        </Badge>
                                        <span className="ml-1 text-xs text-gray-400">
                        {pct(receipt.current_confidence)}
                      </span>
                                        {receipt.policy_flipped && (
                                            <span className="ml-1 text-xs text-amber-600 font-medium">flipped</span>
                                        )}
                                    </td>
                                    <td className="px-3 py-2 font-mono text-xs">{fmt2(receipt.q_block)}</td>
                                    <td className="px-3 py-2 font-mono text-xs">{fmt2(receipt.q_allow)}</td>
                                    <td className="px-3 py-2">
                                        {receipt.agrees_with_officer ? '✅' : '❌'}
                                    </td>
                                    <td className="px-3 py-2 text-xs text-gray-500">
                                        {formatDate(receipt.reviewed_at)}
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
