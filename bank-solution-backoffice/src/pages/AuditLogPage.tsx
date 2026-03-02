import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { paymentService } from '@/api/paymentService';
import { configurationService } from '@/api/configurationService';
import { PaymentHistoryResponse, ConfigAuditLogResponse } from '@/types';
import { Card, LoadingSpinner } from '@/components/common';
import { formatDate, formatCurrency } from '@/utils/formatters';

// ─── Event meta ────────────────────────────────────────────────────────────────

type PaymentAuditEventType = 'MANUAL_APPROVED' | 'MANUAL_REJECTED' | 'DECISION_OVERRIDDEN' | 'BLOCKED';

const PAYMENT_EVENT_META: Record<PaymentAuditEventType, { icon: string; label: string; color: string }> = {
  MANUAL_APPROVED:     { icon: '✅', label: 'Manual Approved',    color: 'bg-green-100 text-green-800 border-green-200'   },
  MANUAL_REJECTED:     { icon: '❌', label: 'Manual Rejected',    color: 'bg-red-100 text-red-800 border-red-200'         },
  DECISION_OVERRIDDEN: { icon: '🔄', label: 'Decision Overridden', color: 'bg-purple-100 text-purple-800 border-purple-200' },
  BLOCKED:             { icon: '🚫', label: 'Auto Blocked',        color: 'bg-red-100 text-red-800 border-red-200'         },
};

const CONFIG_CHANGE_META: Record<string, { icon: string; color: string }> = {
  CREATED: { icon: '➕', color: 'bg-green-100 text-green-800 border-green-200' },
  UPDATED: { icon: '✏️', color: 'bg-blue-100 text-blue-800 border-blue-200'   },
  DELETED: { icon: '🗑️', color: 'bg-red-100 text-red-800 border-red-200'      },
};

// ─── Derived types ─────────────────────────────────────────────────────────────

interface PaymentAuditEvent {
  id:          string;
  eventType:   PaymentAuditEventType;
  timestamp:   string;
  paymentId:   string;
  amount:      number;
  fromCurrency: string;
  actor:       string | null;
  notes:       string | null;
}

// ─── Helper: extract audit events from payments ────────────────────────────────

function extractPaymentEvents(payments: PaymentHistoryResponse[]): PaymentAuditEvent[] {
  const events: PaymentAuditEvent[] = [];

  payments.forEach((p) => {
    if (p.manualReviewApprovedAt) {
      events.push({
        id:        `${p.paymentId}-approved`,
        eventType:    'MANUAL_APPROVED',
        timestamp:    p.manualReviewApprovedAt,
        paymentId:    p.paymentId,
        amount:       p.amount,
        fromCurrency: p.fromCurrency,
        actor:        p.manualReviewedBy ?? null,
        notes:        null,
      });
    }
    if (p.manualReviewRejectedAt) {
      events.push({
        id:        `${p.paymentId}-rejected`,
        eventType:    'MANUAL_REJECTED',
        timestamp:    p.manualReviewRejectedAt,
        paymentId:    p.paymentId,
        amount:       p.amount,
        fromCurrency: p.fromCurrency,
        actor:        p.manualReviewedBy ?? null,
        notes:        null,
      });
    }
    if (p.decisionOverriddenAt) {
      events.push({
        id:        `${p.paymentId}-overridden`,
        eventType:    'DECISION_OVERRIDDEN',
        timestamp:    p.decisionOverriddenAt,
        paymentId:    p.paymentId,
        amount:       p.amount,
        fromCurrency: p.fromCurrency,
        actor:        p.decisionOverriddenBy ?? null,
        notes:        p.decisionOverrideReason ?? null,
      });
    }
    if (p.blockedAt) {
      events.push({
        id:        `${p.paymentId}-blocked`,
        eventType:    'BLOCKED',
        timestamp:    p.blockedAt,
        paymentId:    p.paymentId,
        amount:       p.amount,
        fromCurrency: p.fromCurrency,
        actor:        null,
        notes:        p.blockReason ?? null,
      });
    }
  });

  return events.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
}

// ─── Tab components ────────────────────────────────────────────────────────────

const PaymentDecisionsTab: React.FC = () => {
  const [events, setEvents]   = useState<PaymentAuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getAllPayments(0, 200);
      setEvents(extractPaymentEvents(data.content));
    } catch (e: any) {
      setError(e?.message ?? 'Failed to load payment decisions');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>;
  if (error)   return <div className="text-center py-10 text-red-600">{error}</div>;
  if (events.length === 0)
    return (
      <div className="text-center py-16 text-gray-400">
        <div className="text-5xl mb-3">📭</div>
        <p className="text-lg font-medium">No payment audit events found</p>
        <p className="text-sm mt-1">Manual reviews, overrides and blocks will appear here</p>
      </div>
    );

  return (
    <div>
      <p className="mb-4 text-sm text-gray-500">{events.length} event{events.length !== 1 ? 's' : ''} found</p>
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {['Timestamp', 'Event', 'Payment ID', 'Amount', 'Actor', 'Notes / Reason'].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {events.map((ev) => {
              const meta = PAYMENT_EVENT_META[ev.eventType];
              return (
                <tr key={ev.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-500 tabular-nums">{formatDate(ev.timestamp)}</td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${meta.color}`}>
                      {meta.icon} {meta.label}
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <Link to={`/payments/${ev.paymentId}`} className="font-mono text-xs text-blue-600 hover:underline" title={ev.paymentId}>
                      {ev.paymentId.substring(0, 8)}…
                    </Link>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-gray-900">
                    {formatCurrency(ev.amount, ev.fromCurrency)}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-600">
                    {ev.actor ?? <span className="text-gray-400">—</span>}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500 max-w-xs">
                    {ev.notes
                      ? <span className="truncate block" title={ev.notes}>{ev.notes}</span>
                      : <span className="text-gray-300">—</span>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const ConfigChangesTab: React.FC = () => {
  const [logs, setLogs]       = useState<ConfigAuditLogResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const configs = await configurationService.getAllConfigurations();
      const allLogs = await Promise.all(
        configs.map((c) => configurationService.getAuditLogByKey(c.configKey))
      );
      const merged = allLogs
        .flat()
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      setLogs(merged);
    } catch (e: any) {
      setError(e?.message ?? 'Failed to load config changes');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) return <div className="flex justify-center py-16"><LoadingSpinner /></div>;
  if (error)   return <div className="text-center py-10 text-red-600">{error}</div>;
  if (logs.length === 0)
    return (
      <div className="text-center py-16 text-gray-400">
        <div className="text-5xl mb-3">📋</div>
        <p className="text-lg font-medium">No configuration changes found</p>
        <p className="text-sm mt-1">Creates, updates and deletions will appear here</p>
      </div>
    );

  return (
    <div>
      <p className="mb-4 text-sm text-gray-500">{logs.length} change{logs.length !== 1 ? 's' : ''} found</p>
      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {['Timestamp', 'Change', 'Config Key', 'Old Value', 'New Value', 'Changed By'].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {logs.map((log) => {
              const meta = CONFIG_CHANGE_META[log.changeType] ?? { icon: '?', color: 'bg-gray-100 text-gray-700 border-gray-200' };
              return (
                <tr key={log.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-500 tabular-nums">{formatDate(log.createdAt)}</td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${meta.color}`}>
                      {meta.icon} {log.changeType}
                    </span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <Link to={`/configurations/${log.configId}`} className="font-mono text-xs text-gray-800 hover:text-blue-600 hover:underline">
                      {log.configKey}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-xs max-w-[12rem]">
                    {log.oldValue
                      ? <code className="block truncate bg-red-50 text-red-700 border border-red-200 rounded px-1.5 py-0.5 font-mono" title={log.oldValue}>{log.oldValue}</code>
                      : <span className="text-gray-300">—</span>}
                  </td>
                  <td className="px-4 py-3 text-xs max-w-[12rem]">
                    {log.newValue
                      ? <code className="block truncate bg-green-50 text-green-700 border border-green-200 rounded px-1.5 py-0.5 font-mono" title={log.newValue}>{log.newValue}</code>
                      : <span className="text-gray-300">—</span>}
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-600">
                    {log.changedBy ?? <span className="text-gray-400">—</span>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

// ─── Page ──────────────────────────────────────────────────────────────────────

type TabId = 'payment-decisions' | 'config-changes';

export const AuditLogPage: React.FC = () => {
  const [activeTab, setActiveTab]         = useState<TabId>('payment-decisions');
  const [configTabVisited, setConfigTabVisited] = useState(false);

  const handleTabClick = (tab: TabId) => {
    setActiveTab(tab);
    if (tab === 'config-changes') setConfigTabVisited(true);
  };

  const tabs: { id: TabId; label: string; icon: string }[] = [
    { id: 'payment-decisions', label: 'Payment Decisions', icon: '💳' },
    { id: 'config-changes',    label: 'Config Changes',    icon: '⚙️' },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">📋 Audit Log</h1>
        <p className="mt-1 text-sm text-gray-500">
          Compliance trail of manual reviews, decision overrides, blocks and configuration changes
        </p>
      </div>

      {/* Tab Bar */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => handleTabClick(tab.id)}
              className={`group inline-flex items-center gap-2 py-3 px-1 border-b-2 text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <span>{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <Card>
        {activeTab === 'payment-decisions' && <PaymentDecisionsTab />}
        {activeTab === 'config-changes'    && (activeTab === 'config-changes' || configTabVisited) && (
          <ConfigChangesTab />
        )}
      </Card>
    </div>
  );
};
