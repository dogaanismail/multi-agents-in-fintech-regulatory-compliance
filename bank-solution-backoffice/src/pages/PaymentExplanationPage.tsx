import React, {useEffect} from 'react';
import {useParams, useNavigate, Link} from 'react-router-dom';
import {paymentService} from '@/api';
import {AgentObservationDto, PaymentHistoryResponse} from '@/types';
import {useApi} from '@/hooks/useApi';
import {Card, LoadingSpinner, Badge, Button} from '@/components/common';
import {formatCurrency, getStatusColor} from '@/utils/formatters';
import {ShapContributionChart, additivityCheck, agentColor} from '@/components/explainability';

const AGENT_KEYS = [
    {key: 'transactionAgentObservation', label: 'Transaction Pattern Agent', short: 'transaction'},
    {key: 'customerAgentObservation', label: 'Customer Risk Agent', short: 'customer'},
    {key: 'networkAgentObservation', label: 'Network Analysis Agent', short: 'network'},
] as const;

export const PaymentExplanationPage: React.FC = () => {
    const {paymentId} = useParams<{ paymentId: string }>();
    const navigate = useNavigate();
    const {data: payment, loading, error, execute} = useApi<PaymentHistoryResponse>();

    useEffect(() => {
        if (paymentId) {
            execute(() => paymentService.getPaymentById(paymentId));
        }
    }, [paymentId]);

    if (loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <LoadingSpinner size="lg"/>
            </div>
        );
    }
    if (error) {
        return <div className="text-red-600 p-4 bg-red-50 rounded">Error loading payment: {error.message}</div>;
    }
    if (!payment) {
        return <div className="text-gray-500">Payment not found</div>;
    }

    const marl = payment.marlAssessment;
    if (!marl) {
        return (
            <div className="space-y-6">
                <PageHeader payment={payment} navigate={navigate}/>
                <Card>
                    <p className="text-gray-500 py-6 text-center">
                        No MARL assessment exists for this payment yet — there is nothing to explain.
                    </p>
                </Card>
            </div>
        );
    }

    const observations = AGENT_KEYS
        .map((a) => ({...a, obs: (marl as any)[a.key] as AgentObservationDto | null}))
        .filter((a) => a.obs);

    const weights = marl.agentContributions ?? {};
    const weightTotal = Object.values(weights).reduce((s, w) => s + Math.abs(w), 0);

    const combined = observations
        .flatMap((a) =>
            (a.obs!.featureContributions ?? []).map((c) => ({...c, agent: a.short}))
        )
        .sort((x, y) => Math.abs(y.shapValue) - Math.abs(x.shapValue))
        .slice(0, 10);
    const combinedMax = Math.max(1e-9, ...combined.map((c) => Math.abs(c.shapValue)));

    return (
        <div className="space-y-6">
            <PageHeader payment={payment} navigate={navigate}/>

            <Card title="🤖 Coordinator Decision (MADDPG)">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                    <Stat label="Decision" value={
                        <Badge
                            className={marl.action === 'ALLOW' ? 'bg-green-100 text-green-800' : marl.action === 'BLOCK' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800'}>
                            {marl.action}
                        </Badge>
                    }/>
                    <Stat label="Confidence" value={`${(marl.confidence * 100).toFixed(1)}%`}/>
                    <Stat label="MADDPG Q-Value" value={marl.maddpgQValue?.toFixed(4)}/>
                    <Stat label="Mode" value={marl.mode}/>
                </div>

                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
          Agent weights in the coordinated decision
        </span>
                <div className="mt-2 space-y-2">
                    {observations.map((a) => {
                        const weight = Math.abs(weights[a.short] ?? 0);
                        const pct = weightTotal > 0 ? (weight / weightTotal) * 100 : 0;
                        return (
                            <div key={a.short} className="flex items-center gap-3 text-sm">
                                <span className="w-28 text-gray-700">{a.short}</span>
                                <div className="flex-1 bg-gray-100 rounded h-4 overflow-hidden">
                                    <div className="h-4"
                                         style={{width: `${pct}%`, backgroundColor: agentColor(a.short)}}/>
                                </div>
                                <span className="w-14 text-right tabular-nums text-gray-600">{pct.toFixed(1)}%</span>
                            </div>
                        );
                    })}
                </div>
            </Card>

            <Card title="🎯 Strongest Drivers Across All Agents">
                <div className="space-y-1.5">
                    {combined.map((c) => {
                        const widthPct = (Math.abs(c.shapValue) / combinedMax) * 100;
                        const increases = c.direction === 'INCREASES_RISK';
                        return (
                            <div key={`${c.agent}-${c.feature}`}
                                 className="grid grid-cols-2 items-center gap-2 text-xs">
                                <div className="flex items-center gap-2 truncate">
                  <span
                      className="inline-block w-2 h-2 rounded-full flex-shrink-0"
                      style={{backgroundColor: agentColor(c.agent)}}
                      title={c.agent}
                  />
                                    <span className="font-medium text-gray-700 truncate"
                                          title={`${c.feature} = ${c.value}`}>
                    {c.feature}
                  </span>
                                    <span className="text-gray-400 truncate">= {c.value}</span>
                                </div>
                                <div className="flex items-center">
                                    <div className="w-1/2 flex justify-end">
                                        {!increases && (
                                            <div className="h-3 bg-green-500 rounded-l"
                                                 style={{width: `${widthPct / 2}%`, minWidth: '2px'}}/>
                                        )}
                                    </div>
                                    <div className="w-px h-4 bg-gray-300"/>
                                    <div className="w-1/2 flex items-center">
                                        {increases && (
                                            <div className="h-3 bg-red-500 rounded-r"
                                                 style={{width: `${widthPct / 2}%`, minWidth: '2px'}}/>
                                        )}
                                        <span
                                            className={`ml-1.5 tabular-nums ${increases ? 'text-red-600' : 'text-green-700'}`}>
                      {c.shapValue > 0 ? '+' : ''}{c.shapValue.toFixed(3)}
                    </span>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
                <p className="mt-3 text-[11px] text-gray-400">
                    SHAP values are expressed in each model's own margin (log-odds) space; magnitudes are comparable
                    within an
                    agent, and indicative across agents.
                </p>
            </Card>

            <div className="grid grid-cols-1 gap-6">
                {observations.map((a) => (
                    <AgentDeepPanel key={a.short} label={a.label} short={a.short} observation={a.obs!}/>
                ))}
            </div>

            <CaseFileSummary payment={payment} observations={observations} weights={weights}/>
        </div>
    );
};

const PageHeader: React.FC<{ payment: PaymentHistoryResponse; navigate: (n: number) => void }> = ({
                                                                                                      payment,
                                                                                                      navigate
                                                                                                  }) => (
    <div className="flex justify-between items-center">
        <div>
            <nav className="text-sm text-gray-500 flex items-center gap-2 mb-1">
                <Link to="/payments" className="hover:text-blue-600 transition-colors">Payments</Link>
                <span>›</span>
                <Link to={`/payments/${payment.paymentId}`} className="hover:text-blue-600 transition-colors">
                    {payment.referenceNumber}
                </Link>
                <span>›</span>
                <span className="text-gray-900 font-medium">Decision Explanation</span>
            </nav>
            <h1 className="text-2xl font-bold text-gray-900">
                Why was this
                payment {payment.status === 'BLOCKED' ? 'blocked' : payment.status === 'MANUAL_REVIEW_REQUIRED' ? 'escalated' : 'decided'}?
            </h1>
            <p className="mt-1 text-sm text-gray-500">
                {formatCurrency(payment.amount, payment.fromCurrency)} · {payment.paymentType} ·{' '}
                <Badge className={getStatusColor(payment.status)}>{payment.status}</Badge>
            </p>
        </div>
        <Button variant="secondary" onClick={() => navigate(-1)}>Back</Button>
    </div>
);

const AgentDeepPanel: React.FC<{
    label: string;
    short: string;
    observation: AgentObservationDto;
}> = ({label, short, observation}) => {
    const check = additivityCheck(observation);
    return (
        <Card>
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                    <span className="inline-block w-3 h-3 rounded-full" style={{backgroundColor: agentColor(short)}}/>
                    <h3 className="text-lg font-semibold text-gray-900">{label}</h3>
                </div>
                <Badge className={observation.isSuspicious ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}>
                    {observation.isSuspicious ? 'SUSPICIOUS' : 'CLEAR'}
                </Badge>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <Stat label="Fraud Probability" value={`${(observation.probability * 100).toFixed(2)}%`}/>
                <Stat label="Risk Score" value={observation.riskScore?.toFixed(2)}/>
                <Stat label="Confidence" value={observation.confidence}/>
                <Stat label="Response Time" value={`${observation.responseTimeMs?.toFixed(0)} ms`}/>
            </div>
            <ShapContributionChart contributions={observation.featureContributions}/>
            {check && (
                <p className="mt-3 text-[11px] text-gray-400 tabular-nums">
                    Additivity check: σ(base {observation.shapBaseValue!.toFixed(3)} + Σ contributions) ={' '}
                    {(check.reconstructed * 100).toFixed(2)}%
                    {check.matches
                        ? ' — matches the reported probability, so the listed factors fully account for this score.'
                        : ' — differs from the reported probability; treat the factor list as partial.'}
                </p>
            )}
        </Card>
    );
};

const Stat: React.FC<{ label: string; value: React.ReactNode }> = ({label, value}) => (
    <div>
        <div className="text-xs text-gray-500">{label}</div>
        <div className="text-base font-semibold text-gray-900">{value}</div>
    </div>
);

const humanizeFeature = (feature: string): string => {
    const spaced = feature.replace(/_/g, ' ').trim();
    return spaced.charAt(0).toUpperCase() + spaced.slice(1);
};

const CaseFileSummary: React.FC<{
    payment: PaymentHistoryResponse;
    observations: { short: string; obs: AgentObservationDto | null }[];
    weights: Record<string, number>;
}> = ({payment, observations, weights}) => {
    const marl = payment.marlAssessment!;
    const sorted = [...observations].sort(
        (a, b) => (b.obs?.probability ?? 0) - (a.obs?.probability ?? 0)
    );
    const lead = sorted[0];
    const leadDrivers = (lead.obs?.featureContributions ?? [])
        .filter((c) => c.direction === 'INCREASES_RISK')
        .slice(0, 3);
    const weightTotal = Object.values(weights).reduce((s, w) => s + Math.abs(w), 0);
    const leadWeight =
        weightTotal > 0 ? ((Math.abs(weights[lead.short] ?? 0) / weightTotal) * 100).toFixed(0) : null;

    const plainText = [
        `Decision: ${marl.action} (${(marl.confidence * 100).toFixed(1)}% confidence, MADDPG coordinator).`,
        `Leading signal: ${lead.short} agent, fraud probability ${((lead.obs?.probability ?? 0) * 100).toFixed(1)}%` +
        (leadWeight ? `, ${leadWeight}% of the decision weight.` : '.'),
        ...leadDrivers.map(
            (c) => `Risk driver: ${humanizeFeature(c.feature)} = ${c.value} (SHAP ${c.shapValue > 0 ? '+' : ''}${c.shapValue.toFixed(2)})`
        ),
        ...(payment.manualReviewedBy ? [`Reviewed by: ${payment.manualReviewedBy}.`] : []),
    ].join('\n');

    return (
        <Card title="📝 Summary for the Case File">
            <div className="space-y-4">
                <div className="flex items-center gap-3 text-sm">
                    <span className="w-36 text-gray-500">Decision</span>
                    <Badge
                        className={marl.action === 'ALLOW' ? 'bg-green-100 text-green-800' : marl.action === 'BLOCK' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800'}>
                        {marl.action}
                    </Badge>
                    <span className="text-gray-700">
            {(marl.confidence * 100).toFixed(1)}% confidence, decided by the MADDPG coordinator
          </span>
                </div>

                <div className="flex items-center gap-3 text-sm">
                    <span className="w-36 text-gray-500 flex-shrink-0">Leading signal</span>
                    <span className="inline-block w-2.5 h-2.5 rounded-full flex-shrink-0"
                          style={{backgroundColor: agentColor(lead.short)}}/>
                    <span className="text-gray-700">
            The <span className="font-semibold">{lead.short}</span> agent reported{' '}
                        <span className="font-semibold">{((lead.obs?.probability ?? 0) * 100).toFixed(1)}%</span> fraud probability
                        {leadWeight && <> and carried <span className="font-semibold">{leadWeight}%</span> of the
                            decision weight</>}
          </span>
                </div>

                {leadDrivers.length > 0 && (
                    <div className="flex items-start gap-3 text-sm">
                        <span className="w-36 text-gray-500 flex-shrink-0 pt-1.5">Main risk drivers</span>
                        <div className="flex-1 divide-y divide-gray-100 rounded-md border border-gray-100">
                            {leadDrivers.map((c) => (
                                <div key={c.feature} className="flex items-center justify-between px-3 py-1.5">
                  <span className="text-gray-700">
                    {humanizeFeature(c.feature)}
                      <span className="text-gray-400"> = {c.value}</span>
                  </span>
                                    <span className="font-mono text-red-600 tabular-nums">
                    +{c.shapValue.toFixed(2)}
                  </span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {payment.manualReviewedBy && (
                    <div className="flex items-center gap-3 text-sm">
                        <span className="w-36 text-gray-500">Officer review</span>
                        <span className="text-gray-700">
              Reviewed by <span className="font-semibold">{payment.manualReviewedBy}</span>
                            {payment.manualReviewNotes &&
                                <span className="text-gray-500"> — "{payment.manualReviewNotes}"</span>}
            </span>
                    </div>
                )}

                <div className="pt-2 border-t border-gray-100 flex justify-end">
                    <button
                        className="px-3 py-1.5 text-xs font-medium rounded-md bg-gray-50 text-gray-600 border border-gray-200 hover:bg-gray-100 transition-colors"
                        onClick={() => navigator.clipboard.writeText(plainText)}
                    >
                        📋 Copy as plain text
                    </button>
                </div>
            </div>
        </Card>
    );
};
