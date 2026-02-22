import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { configurationService } from '@/api';
import { ConfigurationResponse, ConfigCategory, CreateConfigRequest, ConfigType } from '@/types';
import { useApi } from '@/hooks/useApi';
import { Card, LoadingSpinner, Badge, Button, Input } from '@/components/common';
import { formatDate } from '@/utils/formatters';

const CATEGORY_OPTIONS: { value: ConfigCategory; label: string }[] = [
  { value: 'OFFLINE_RETRAINING', label: 'Offline Retraining' },
  { value: 'AUTO_REWARD', label: 'Auto Reward' },
  { value: 'MANUAL_REWARD', label: 'Manual Reward' },
  { value: 'ESCALATION', label: 'Escalation' },
];

const TYPE_OPTIONS: { value: ConfigType; label: string }[] = [
  { value: 'FLOAT', label: 'Float' },
  { value: 'INTEGER', label: 'Integer' },
  { value: 'BOOLEAN', label: 'Boolean' },
  { value: 'STRING', label: 'String' },
];

const getCategoryColor = (category: ConfigCategory): string => {
  const colors: Record<ConfigCategory, string> = {
    OFFLINE_RETRAINING: 'bg-blue-100 text-blue-800',
    AUTO_REWARD: 'bg-green-100 text-green-800',
    MANUAL_REWARD: 'bg-purple-100 text-purple-800',
    ESCALATION: 'bg-orange-100 text-orange-800',
  };
  return colors[category] || 'bg-gray-100 text-gray-800';
};

const getCategoryLabel = (category: ConfigCategory): string => {
  const labels: Record<ConfigCategory, string> = {
    OFFLINE_RETRAINING: 'Offline Retraining',
    AUTO_REWARD: 'Auto Reward',
    MANUAL_REWARD: 'Manual Reward',
    ESCALATION: 'Escalation',
  };
  return labels[category] || category;
};

const EMPTY_FORM: CreateConfigRequest = {
  configKey: '',
  configValue: '',
  configType: 'FLOAT',
  category: 'AUTO_REWARD',
  description: '',
  defaultValue: '',
};

export const ConfigurationListPage: React.FC = () => {
  const { data: configurations, loading, error, execute } = useApi<ConfigurationResponse[]>();
  const [selectedCategory, setSelectedCategory] = useState<ConfigCategory | 'ALL'>('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [form, setForm] = useState<CreateConfigRequest>(EMPTY_FORM);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    execute(() => configurationService.getAllConfigurations());
  }, []);

  const filtered = configurations
    ? selectedCategory === 'ALL'
      ? configurations
      : configurations.filter((c) => c.category === selectedCategory)
    : [];

  const handleCreate = async () => {
    if (!form.configKey || !form.configValue || !form.defaultValue) {
      alert('Please fill in all required fields');
      return;
    }
    try {
      setActionLoading(true);
      await configurationService.createConfiguration(form);
      setShowCreateModal(false);
      setForm(EMPTY_FORM);
      execute(() => configurationService.getAllConfigurations());
    } catch (err) {
      alert('Failed to create configuration: ' + (err as Error).message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async (id: string, key: string) => {
    if (!window.confirm(`Are you sure you want to delete configuration "${key}"?`)) return;
    try {
      await configurationService.deleteConfiguration(id);
      execute(() => configurationService.getAllConfigurations());
    } catch (err) {
      alert('Failed to delete configuration: ' + (err as Error).message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Configurations</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage MARL reward and offline retraining parameters
          </p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>+ New Configuration</Button>
      </div>

      <div className="flex space-x-2">
        <button
          onClick={() => setSelectedCategory('ALL')}
          className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
            selectedCategory === 'ALL'
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          All
        </button>
        {CATEGORY_OPTIONS.map((cat) => (
          <button
            key={cat.value}
            onClick={() => setSelectedCategory(cat.value)}
            className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
              selectedCategory === cat.value
                ? 'bg-gray-900 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      <Card>
        {loading && <LoadingSpinner />}
        {error && (
          <div className="text-red-600 p-4 bg-red-50 rounded">
            Error loading configurations: {error.message}
          </div>
        )}
        {!loading && !error && filtered.length === 0 && (
          <div className="text-gray-500 text-center py-8">No configurations found</div>
        )}
        {!loading && !error && filtered.length > 0 && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Key
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Value
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Default
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Category
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Updated
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filtered.map((config) => (
                  <tr key={config.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono font-medium text-gray-900">
                      {config.configKey}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-indigo-700">
                      {config.configValue}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {config.defaultValue}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge className="bg-gray-100 text-gray-700">{config.configType}</Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge className={getCategoryColor(config.category)}>
                        {getCategoryLabel(config.category)}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(config.updatedAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                      <Link
                        to={`/configurations/${config.id}`}
                        className="text-blue-600 hover:text-blue-800 font-medium"
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => handleDelete(config.id, config.configKey)}
                        className="text-red-600 hover:text-red-800 font-medium"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-lg space-y-4">
            <h2 className="text-xl font-semibold text-gray-900">New Configuration</h2>
            <Input
              label="Config Key *"
              value={form.configKey}
              onChange={(e) => setForm({ ...form, configKey: e.target.value.toUpperCase() })}
              placeholder="e.g. REWARD_AUTO_HIGH_RISK_BLOCK"
            />
            <Input
              label="Config Value *"
              value={form.configValue}
              onChange={(e) => setForm({ ...form, configValue: e.target.value })}
              placeholder="e.g. 0.3"
            />
            <Input
              label="Default Value *"
              value={form.defaultValue}
              onChange={(e) => setForm({ ...form, defaultValue: e.target.value })}
              placeholder="e.g. 0.3"
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
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Category *</label>
              <select
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value as ConfigCategory })}
              >
                {CATEGORY_OPTIONS.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
            <Input
              label="Description"
              value={form.description || ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Optional description"
            />
            <div className="flex justify-end space-x-3 pt-2">
              <Button
                variant="secondary"
                onClick={() => { setShowCreateModal(false); setForm(EMPTY_FORM); }}
                disabled={actionLoading}
              >
                Cancel
              </Button>
              <Button onClick={handleCreate} disabled={actionLoading}>
                {actionLoading ? 'Creating…' : 'Create'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
