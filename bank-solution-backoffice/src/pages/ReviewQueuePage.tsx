import React, {useCallback, useEffect, useState} from 'react';
import {Link} from 'react-router-dom';
import {reviewQueueService} from '@/api/reviewQueueService';
import {ReviewQueueResponse} from '@/types';
import {Badge, Card, LoadingSpinner} from '@/components/common';

// ─── Helpers ─────────────────────────────────────────────────────────────────

const formatAge = (seconds: number) => {
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ${Math.floor((seconds % 3600) / 60)}m`;
    return `${Math.floor(seconds / 86400)}d ${Math.floor((seconds % 86400) / 3600)}h`;
};

const formatAmount = (amount: number | null, currency: string | null) =>
    amount == null
        ? '—'
        : `${amount.toLocaleString(undefined, {minimumFractionDigits: 2})} ${currency ?? ''}`.trim();

const reasonBadge = (code: string) => {
    switch (code) {
        case 'LOW_CONFIDENCE':
            return 'bg-yellow-100 text-yellow-800';
        case 'AGENT_SUSPICIOUS_VOTES':
            return 'bg-red-100 text-red-800';
        case 'EXPLORATION':
            return 'bg-purple-100 text-purple-800';
        default:
            return 'bg-gray-100 text-gray-600';
    }
};

const riskColor = (risk: number) => {
    if (risk >= 0.6) return 'text-red-600';
    if (risk >= 0.3) return 'text-amber-600';
    return 'text-green-600';
};

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

// ─── Page ─────────────────────────────────────────────────────────────────────

export const ReviewQueuePage: React.FC = () => {
    const [queue, setQueue] = useState<ReviewQueueResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const loadQueue = useCallback(async () => {
        try {
            setQueue(await reviewQueueService.getQueue());
            setError(null);
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadQueue();
        const interval = setInterval(loadQueue, 15000);
        return () => clearInterval(interval);
    }, [loadQueue]);

    if (loading) return <LoadingSpinner size="lg"/>;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900">Review Queue</h1>
                <p className="mt-1 text-sm text-gray-500">
                    Payments escalated to human review, ordered by exposure (risk × amount).
                    Open a payment to approve or reject it — your verdict trains the model.
                </p>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-4">
                    {error}
                </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <StatCard
                    label="Pending Reviews"
                    value={queue?.pending_count ?? '—'}
                    accent="bg-blue-500"
                />
                <StatCard
                    label="SLA Breached"
                    value={queue?.sla_breached_count ?? '—'}
                    sub={`SLA: ${queue?.sla_minutes ?? '—'} minutes`}
                    accent={queue && queue.sla_breached_count > 0 ? 'bg-red-500' : 'bg-green-500'}
                />
                <StatCard
                    label="Within SLA"
                    value={queue ? queue.pending_count - queue.sla_breached_count : '—'}
                    accent="bg-green-500"
                />
            </div>

            <Card>
                {!queue || queue.items.length === 0 ? (
                    <p className="text-sm text-gray-500">
                        🎉 The queue is empty — no payments are waiting for review.
                    </p>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200 text-sm">
                            <thead>
                            <tr className="text-left text-xs font-medium text-gray-500 uppercase tracking-wide">
                                <th className="px-3 py-2">Payment</th>
                                <th className="px-3 py-2">Waiting</th>
                                <th className="px-3 py-2">Amount</th>
                                <th className="px-3 py-2">Risk</th>
                                <th className="px-3 py-2">Confidence</th>
                                <th className="px-3 py-2">Why it is here</th>
                                <th className="px-3 py-2"></th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                            {queue.items.map((item) => (
                                <tr key={item.payment_id} className={item.sla_breached ? 'bg-red-50' : ''}>
                                    <td className="px-3 py-2 font-mono text-xs text-gray-600">
                                        {item.payment_id.slice(0, 8)}…
                                    </td>
                                    <td className="px-3 py-2">
                      <span className={item.sla_breached ? 'text-red-600 font-semibold' : 'text-gray-700'}>
                        {formatAge(item.age_seconds)}
                      </span>
                                        {item.sla_breached && (
                                            <span className="ml-1 text-xs text-red-500">SLA</span>
                                        )}
                                    </td>
                                    <td className="px-3 py-2 font-medium">
                                        {formatAmount(item.amount, item.currency)}
                                    </td>
                                    <td className={`px-3 py-2 font-semibold ${riskColor(item.mean_risk_score)}`}>
                                        {(item.mean_risk_score * 100).toFixed(0)}%
                                    </td>
                                    <td className="px-3 py-2 text-gray-600">
                                        {(item.marl_confidence * 100).toFixed(0)}%
                                    </td>
                                    <td className="px-3 py-2">
                                        <div className="flex flex-wrap gap-1">
                                            {item.decision_reasons.length === 0 ? (
                                                <span className="text-xs text-gray-400">no reasons recorded</span>
                                            ) : (
                                                item.decision_reasons.map((reason) => (
                                                    <span key={reason.code} title={reason.detail}>
                              <Badge className={reasonBadge(reason.code)}>
                                {reason.code.replace(/_/g, ' ')}
                              </Badge>
                            </span>
                                                ))
                                            )}
                                        </div>
                                    </td>
                                    <td className="px-3 py-2">
                                        <Link
                                            to={`/payments/${item.payment_id}`}
                                            className="text-blue-600 hover:text-blue-800 font-medium"
                                        >
                                            Review →
                                        </Link>
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
