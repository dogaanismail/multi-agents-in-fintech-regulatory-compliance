import React from 'react';
import {AgentObservationDto, FeatureContributionDto} from '@/types';

export const AGENT_COLORS: Record<string, string> = {
    transaction: '#2196F3',
    customer: '#FF9800',
    network: '#4CAF50',
};

export const agentColor = (agentName: string): string =>
    AGENT_COLORS[agentName] ?? '#9E9E9E';

export const ShapContributionChart: React.FC<{
    contributions: FeatureContributionDto[] | null;
    maxRows?: number;
}> = ({contributions, maxRows}) => {
    if (!contributions || contributions.length === 0) {
        return null;
    }
    const rows = maxRows ? contributions.slice(0, maxRows) : contributions;
    const truncated = maxRows ? contributions.length - rows.length : 0;
    const maxMagnitude = Math.max(...rows.map((c) => Math.abs(c.shapValue)));
    return (
        <div className="mt-4 pt-3 border-t border-gray-100">
            <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
          Why this score — SHAP feature contributions
        </span>
                <span className="text-[10px] text-gray-400">
          <span className="text-red-500">■</span> pushes toward suspicious&nbsp;&nbsp;
                    <span className="text-green-600">■</span> pushes away
        </span>
            </div>
            <div className="space-y-1.5">
                {rows.map((c) => {
                    const widthPct = maxMagnitude > 0 ? (Math.abs(c.shapValue) / maxMagnitude) * 100 : 0;
                    const increases = c.direction === 'INCREASES_RISK';
                    const noImpact = c.direction === 'NO_IMPACT';
                    return (
                        <div key={c.feature} className="grid grid-cols-2 items-center gap-2 text-xs">
                            <div className={`truncate ${noImpact ? 'text-gray-400' : 'text-gray-700'}`}
                                 title={`${c.feature} = ${c.value}`}>
                                <span className="font-medium">{c.feature}</span>
                                {c.value && <span className="text-gray-400"> = {c.value}</span>}
                            </div>
                            <div className="flex items-center">
                                <div className="w-1/2 flex justify-end">
                                    {!increases && !noImpact && (
                                        <div
                                            className="h-3 bg-green-500 rounded-l"
                                            style={{width: `${widthPct / 2}%`, minWidth: '2px'}}
                                        />
                                    )}
                                </div>
                                <div className="w-px h-4 bg-gray-300"/>
                                <div className="w-1/2 flex items-center">
                                    {increases && (
                                        <div
                                            className="h-3 bg-red-500 rounded-r"
                                            style={{width: `${widthPct / 2}%`, minWidth: '2px'}}
                                        />
                                    )}
                                    <span
                                        className={`ml-1.5 tabular-nums ${noImpact ? 'text-gray-400' : increases ? 'text-red-600' : 'text-green-700'}`}>
                    {noImpact ? 'no impact' : `${c.shapValue > 0 ? '+' : ''}${c.shapValue.toFixed(3)}`}
                  </span>
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>
            {truncated > 0 && (
                <p className="mt-2 text-[11px] text-gray-400">
                    Showing the {rows.length} strongest of {contributions.length} features — the full set is on the
                    decision
                    explanation page.
                </p>
            )}
        </div>
    );
};

export const additivityCheck = (observation: AgentObservationDto) => {
    const contributions = observation.featureContributions;
    if (!contributions || contributions.length === 0 || observation.shapBaseValue == null) {
        return null;
    }
    const margin =
        observation.shapBaseValue + contributions.reduce((sum, c) => sum + c.shapValue, 0);
    const reconstructed = 1 / (1 + Math.exp(-margin));
    const matches = Math.abs(reconstructed - observation.probability) < 0.01;
    return {margin, reconstructed, matches};
};
