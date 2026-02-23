import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { configurationService } from '@/api';
import { ConfigurationResponse, UpdateConfigRequest, ConfigType, ConfigAuditLogResponse } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Button, Input } from '@/components/common';
import { formatDate } from '@/utils/formatters';

const TYPE_OPTIONS: { value: ConfigType; label: string }[] = [
  { value: 'FLOAT', label: 'Float' },
  { value: 'INTEGER', label: 'Integer' },
  { value: 'BOOLEAN', label: 'Boolean' },
  { value: 'STRING', label: 'String' },
];

const CATEGORY_LABELS: Record<string, string> = {
  OFFLINE_RETRAINING: 'Offline Retraining',
  AUTO_REWARD: 'Auto Reward',
  MANUAL_REWARD: 'Manual Reward',
  ESCALATION: 'Escalation',
  AGENT_BEHAVIOR: 'Agent Behavior',
};

const CATEGORY_COLORS: Record<string, string> = {
  OFFLINE_RETRAINING: 'bg-blue-100 text-blue-800',
  AUTO_REWARD: 'bg-green-100 text-green-800',
  MANUAL_REWARD: 'bg-purple-100 text-purple-800',
  ESCALATION: 'bg-orange-100 text-orange-800',
  AGENT_BEHAVIOR: 'bg-teal-100 text-teal-800',
};

const CHANGE_TYPE_COLORS: Record<string, string> = {
  CREATED: 'bg-green-100 text-green-800',
  UPDATED: 'bg-yellow-100 text-yellow-800',
  DELETED: 'bg-red-100 text-red-800',
};

export const ConfigurationDetailPage: React.FC = () => {
  const { configId } = useParams<{ configId: string }>();
  const navigate = useNavigate();
  const { data: config, loading, error, execute } = useApi<ConfigurationResponse>();
  const [editMode, setEditMode] = useState(false);
  const [form, setForm] = useState<UpdateConfigRequest>({ configValue: '', configType: 'FLOAT', description: '' });
  const [actionLoading, setActionLoading] = useState(false);
  const [auditLog, setAuditLog] = useState<ConfigAuditLogResponse[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);

  useEffect(() => {
    if (configId) {
      execute(() => configurationService.getConfigurationById(configId));
    }
  }, [configId]);

  useEffect(() => {
    if (config) {
      setForm({
        configValue: config.configValue,
        configType: config.configType,
        description: config.description || '',
      });
      // Fetch audit log for this key
      setAuditLoading(true);
      configurationService
        .getAuditLogByKey(config.configKey)
        .then(setAuditLog)
        .catch(() => setAuditLog([]))
        .finally(() => setAuditLoading(false));
    }
  }, [config]);

  const handleUpdate = async () => {
    if (!configId || !form.configValue) {
      alert('Please fill in all required fields');
      return;
    }
    try {
      setActionLoading(true);
      await configurationService.updateConfiguration(configId, form);
      setEditMode(false);
      execute(() => configurationService.getConfigurationById(configId));
    } catch (err) {
      alert('Failed to update configuration: ' + (err as Error).message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!configId || !config) return;
    if (!window.confirm(`Are you sure you want to delete "${config.configKey}"?`)) return;
    try {
      await configurationService.deleteConfiguration(configId);
      navigate('/configurations');
    } catch (err) {
      alert('Failed to delete configuration: ' + (err as Error).message);
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
        Error loading configuration: {error.message}
      </div>
    );
  }

  if (!config) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <button
            onClick={() => navigate('/configurations')}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium mb-2 flex items-center"
          >
            ← Back to Configurations
          </button>
          <h1 className="text-3xl font-bold text-gray-900 font-mono">{config.configKey}</h1>
        </div>
        <div className="flex space-x-3">
          {!editMode && (
            <Button onClick={() => setEditMode(true)}>Edit</Button>
          )}
          <Button variant="danger" onClick={handleDelete}>Delete</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="md:col-span-2">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">
            {editMode ? 'Edit Configuration' : 'Configuration Details'}
          </h2>

          {editMode ? (
            <div className="space-y-4">
              <Input
                label="Config Value *"
                value={form.configValue}
                onChange={(e) => setForm({ ...form, configValue: e.target.value })}
              />
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">Type *</label>
                <select
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={form.configType}
                  onChange={(e) => setForm({ ...form, configType: e.target.value as ConfigType })}
                >
                  {TYPE_OPTIONS.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>
              <Input
                label="Description"
                value={form.description || ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
              <div className="flex space-x-3 pt-2">
                <Button onClick={handleUpdate} disabled={actionLoading}>
                  {actionLoading ? 'Saving…' : 'Save Changes'}
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => {
                    setEditMode(false);
                    setForm({ configValue: config.configValue, configType: config.configType, description: config.description || '' });
                  }}
                  disabled={actionLoading}
                >
                  Cancel
                </Button>
              </div>
            </div>
          ) : (
            <dl className="grid grid-cols-1 gap-4">
              <div className="flex justify-between py-3 border-b border-gray-100">
                <dt className="text-sm font-medium text-gray-500">Config Key</dt>
                <dd className="text-sm font-mono text-gray-900">{config.configKey}</dd>
              </div>
              <div className="flex justify-between py-3 border-b border-gray-100">
                <dt className="text-sm font-medium text-gray-500">Current Value</dt>
                <dd className="text-sm font-semibold text-indigo-700">{config.configValue}</dd>
              </div>
              <div className="flex justify-between py-3 border-b border-gray-100">
                <dt className="text-sm font-medium text-gray-500">Default Value</dt>
                <dd className="text-sm text-gray-700">{config.defaultValue}</dd>
              </div>
              <div className="flex justify-between py-3 border-b border-gray-100">
                <dt className="text-sm font-medium text-gray-500">Type</dt>
                <dd>
                  <Badge className="bg-gray-100 text-gray-700">{config.configType}</Badge>
                </dd>
              </div>
              <div className="flex justify-between py-3 border-b border-gray-100">
                <dt className="text-sm font-medium text-gray-500">Category</dt>
                <dd>
                  <Badge className={CATEGORY_COLORS[config.category] || 'bg-gray-100 text-gray-700'}>
                    {CATEGORY_LABELS[config.category] || config.category}
                  </Badge>
                </dd>
              </div>
              {config.description && (
                <div className="flex justify-between py-3 border-b border-gray-100">
                  <dt className="text-sm font-medium text-gray-500">Description</dt>
                  <dd className="text-sm text-gray-700 max-w-xs text-right">{config.description}</dd>
                </div>
              )}
            </dl>
          )}
        </Card>

        <Card>
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Metadata</h2>
          <dl className="space-y-4">
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">ID</dt>
              <dd className="mt-1 text-sm font-mono text-gray-700 break-all">{config.id}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Created At</dt>
              <dd className="mt-1 text-sm text-gray-700">{formatDate(config.createdAt)}</dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Last Updated</dt>
              <dd className="mt-1 text-sm text-gray-700">{formatDate(config.updatedAt)}</dd>
            </div>
          </dl>
        </Card>
      </div>

      {/* Audit Log */}
      <Card>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Audit Log</h2>
        {auditLoading && <LoadingSpinner />}
        {!auditLoading && auditLog.length === 0 && (
          <p className="text-sm text-gray-500">No audit entries found.</p>
        )}
        {!auditLoading && auditLog.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">When</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Change</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Old Value</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">New Value</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">By</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-100">
                {[...auditLog].reverse().map((entry) => (
                  <tr key={entry.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 whitespace-nowrap text-gray-500">{formatDate(entry.createdAt)}</td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge className={CHANGE_TYPE_COLORS[entry.changeType] || 'bg-gray-100 text-gray-700'}>
                        {entry.changeType}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap font-mono text-gray-500">
                      {entry.oldValue ?? <span className="italic text-gray-300">—</span>}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap font-mono font-semibold text-indigo-700">
                      {entry.newValue ?? <span className="italic text-gray-300">—</span>}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-gray-500">{entry.changedBy}</td>
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
