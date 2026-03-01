import React, { useState, useEffect, useCallback } from 'react';
import { currencyConversionService } from '@/api/currencyConversionService';
import { ExchangeRateResponse } from '@/types';
import { Card, LoadingSpinner } from '@/components/common';
import { formatDate } from '@/utils/formatters';

export const ExchangeRatePage: React.FC = () => {
  const [rates, setRates] = useState<ExchangeRateResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState('');

  const loadRates = useCallback(async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);
    try {
      const data = await currencyConversionService.getAllRates();
      setRates(data);
    } catch {
      setError('Failed to load exchange rates. Please try again.');
    } finally {
      if (isRefresh) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    loadRates();
  }, [loadRates]);

  const filteredRates = rates
    .filter((r) =>
      filter.trim() === '' || r.currencyPair.toUpperCase().includes(filter.trim().toUpperCase()),
    )
    .sort((a, b) => a.currencyPair.localeCompare(b.currencyPair));

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Exchange Rates</h1>
          <p className="mt-1 text-sm text-gray-500">
            {rates.length} currency pairs — GBP base, refreshed every 60 seconds.
          </p>
        </div>
        <button
          onClick={() => loadRates(true)}
          disabled={refreshing}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-60 transition-colors"
        >
          {refreshing ? <LoadingSpinner size="sm" /> : '🔄'}
          {refreshing ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {!error && rates.length === 0 && (
        <div className="rounded-md bg-yellow-50 border border-yellow-200 p-4 text-sm text-yellow-700">
          No exchange rates available. The scheduled job may not have run yet.
        </div>
      )}

      {rates.length > 0 && (
        <Card title="Currency Pairs">
          <div className="mb-4">
            <input
              type="text"
              placeholder="Filter by currency (e.g. GBP, TRY, EUR)"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="w-full sm:w-64 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-400"
            />
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">Pair</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">From</th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wider">To</th>
                  <th className="px-4 py-3 text-right font-medium text-gray-500 uppercase tracking-wider">Rate</th>
                  <th className="px-4 py-3 text-right font-medium text-gray-500 uppercase tracking-wider">Last Updated</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-100">
                {filteredRates.map((r) => (
                  <tr key={r.currencyPair} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono font-semibold text-gray-900">{r.currencyPair}</td>
                    <td className="px-4 py-3 text-gray-700">{r.currencyPair.slice(0, 3)}</td>
                    <td className="px-4 py-3 text-gray-700">{r.currencyPair.slice(3, 6)}</td>
                    <td className="px-4 py-3 text-right font-mono text-gray-900">
                      {r.rate.toLocaleString(undefined, { minimumFractionDigits: 4, maximumFractionDigits: 8 })}
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500">{formatDate(r.fetchedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredRates.length === 0 && (
              <p className="text-center text-sm text-gray-500 py-6">No pairs match your filter.</p>
            )}
          </div>
        </Card>
      )}
    </div>
  );
};
