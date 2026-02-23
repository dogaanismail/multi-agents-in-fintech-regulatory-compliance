import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '@/components/common';

export const HomePage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Welcome to Compliance Backoffice</h1>
        <p className="mt-2 text-gray-600">
          Manage payments, customers, and accounts with advanced fraud detection and risk assessment.
        </p>
      </div>

      {/* Quick Actions */}
      <Card title="🚀 Quick Actions">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Link
            to="/customers/create"
            className="flex items-center gap-4 p-4 rounded-lg border-2 border-dashed border-green-300 bg-green-50 hover:bg-green-100 hover:border-green-400 transition-all group"
          >
            <div className="flex items-center justify-center h-11 w-11 rounded-full bg-green-500 text-white text-xl flex-shrink-0">
              👤
            </div>
            <div>
              <p className="font-semibold text-green-900 group-hover:text-green-800">
                Create Customer
              </p>
              <p className="text-xs text-green-700 mt-0.5">Register a new customer profile</p>
            </div>
          </Link>

          <Link
            to="/accounts/open"
            className="flex items-center gap-4 p-4 rounded-lg border-2 border-dashed border-purple-300 bg-purple-50 hover:bg-purple-100 hover:border-purple-400 transition-all group"
          >
            <div className="flex items-center justify-center h-11 w-11 rounded-full bg-purple-500 text-white text-xl flex-shrink-0">
              🏦
            </div>
            <div>
              <p className="font-semibold text-purple-900 group-hover:text-purple-800">
                Open Account
              </p>
              <p className="text-xs text-purple-700 mt-0.5">Open a bank account for a customer</p>
            </div>
          </Link>

          <Link
            to="/payments/create"
            className="flex items-center gap-4 p-4 rounded-lg border-2 border-dashed border-blue-300 bg-blue-50 hover:bg-blue-100 hover:border-blue-400 transition-all group"
          >
            <div className="flex items-center justify-center h-11 w-11 rounded-full bg-blue-500 text-white text-xl flex-shrink-0">
              💳
            </div>
            <div>
              <p className="font-semibold text-blue-900 group-hover:text-blue-800">
                New Payment
              </p>
              <p className="text-xs text-blue-700 mt-0.5">Submit a payment for MARL assessment</p>
            </div>
          </Link>
        </div>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Payments Card */}
        <Link to="/payments">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <div className="flex items-center justify-center h-12 w-12 rounded-md bg-blue-500 text-white text-2xl">
                  💳
                </div>
              </div>
              <div className="ml-4">
                <h3 className="text-lg font-medium text-gray-900">Payments</h3>
                <p className="mt-2 text-sm text-gray-500">
                  View and manage all payments. Review, approve, or reject payments requiring manual
                  review. Filter by status, fraud level, and risk assessment.
                </p>
                <div className="mt-3 text-sm text-blue-600 font-medium">
                  View Payments →
                </div>
              </div>
            </div>
          </Card>
        </Link>

        {/* Customers Card */}
        <Link to="/customers">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <div className="flex items-center justify-center h-12 w-12 rounded-md bg-green-500 text-white text-2xl">
                  👥
                </div>
              </div>
              <div className="ml-4">
                <h3 className="text-lg font-medium text-gray-900">Customers</h3>
                <p className="mt-2 text-sm text-gray-500">
                  Browse customer profiles, view their accounts, and review transaction history.
                  Monitor customer risk profiles and activity patterns.
                </p>
                <div className="mt-3 text-sm text-green-600 font-medium">
                  View Customers →
                </div>
              </div>
            </div>
          </Card>
        </Link>

        {/* Accounts Card */}
        <Link to="/accounts">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <div className="flex items-center justify-center h-12 w-12 rounded-md bg-purple-500 text-white text-2xl">
                  🏦
                </div>
              </div>
              <div className="ml-4">
                <h3 className="text-lg font-medium text-gray-900">Accounts</h3>
                <p className="mt-2 text-sm text-gray-500">
                  View account details and balances across multiple currencies. Monitor account
                  status and transaction history.
                </p>
                <div className="mt-3 text-sm text-purple-600 font-medium">
                  View Accounts →
                </div>
              </div>
            </div>
          </Card>
        </Link>

        {/* Configurations Card */}
        <Link to="/configurations">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <div className="flex items-center justify-center h-12 w-12 rounded-md bg-orange-500 text-white text-2xl">
                  ⚙️
                </div>
              </div>
              <div className="ml-4">
                <h3 className="text-lg font-medium text-gray-900">Configurations</h3>
                <p className="mt-2 text-sm text-gray-500">
                  Manage MARL reward weights, training thresholds, and system parameters
                  dynamically without redeployment.
                </p>
                <div className="mt-3 text-sm text-orange-600 font-medium">
                  View Configurations →
                </div>
              </div>
            </div>
          </Card>
        </Link>

        {/* MARL Training Card */}
        <Link to="/marl-training">
          <Card className="hover:shadow-lg transition-shadow cursor-pointer">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <div className="flex items-center justify-center h-12 w-12 rounded-md bg-indigo-600 text-white text-2xl">
                  🤖
                </div>
              </div>
              <div className="ml-4">
                <h3 className="text-lg font-medium text-gray-900">MARL Training</h3>
                <p className="mt-2 text-sm text-gray-500">
                  Monitor MADDPG offline training, view replay buffer stats, training loss history,
                  and manually trigger training cycles.
                </p>
                <div className="mt-3 text-sm text-indigo-600 font-medium">
                  View Training →
                </div>
              </div>
            </div>
          </Card>
        </Link>
      </div>

      {/* Features Overview */}
      <Card title="Key Features">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <h4 className="font-semibold text-gray-900 mb-2">🔍 Advanced Filtering</h4>
            <p className="text-sm text-gray-600">
              Filter payments by ID, customer, account, status, fraud status, and risk level to
              quickly find what you need.
            </p>
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 mb-2">✅ Manual Review</h4>
            <p className="text-sm text-gray-600">
              Approve or reject payments flagged for manual review with detailed notes and audit
              trail.
            </p>
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 mb-2">📊 MARL Assessment</h4>
            <p className="text-sm text-gray-600">
              View multi-agent reinforcement learning assessments with detailed agent observations
              and risk scoring.
            </p>
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 mb-2">💰 Balance Tracking</h4>
            <p className="text-sm text-gray-600">
              Monitor account balances across multiple currencies with available, pending, and total
              balance breakdowns.
            </p>
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 mb-2">📈 Risk Insights</h4>
            <p className="text-sm text-gray-600">
              Access comprehensive risk assessments, fraud indicators, and confidence scores for
              each transaction.
            </p>
          </div>
          <div>
            <h4 className="font-semibold text-gray-900 mb-2">🕐 Complete Timeline</h4>
            <p className="text-sm text-gray-600">
              Track the full lifecycle of each payment from initiation through completion or
              rejection.
            </p>
          </div>
        </div>
      </Card>

      {/* Status Legend */}
      <Card title="Payment Status Guide">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatusItem status="INITIATED" color="gray" description="Payment started" />
          <StatusItem
            status="FRAUD_CHECK_PENDING"
            color="blue"
            description="Under fraud analysis"
          />
          <StatusItem
            status="MANUAL_REVIEW_REQUIRED"
            color="yellow"
            description="Needs officer review"
          />
          <StatusItem
            status="FRAUD_CHECK_APPROVED"
            color="green"
            description="Fraud check passed"
          />
          <StatusItem status="COMPLETED" color="green" description="Successfully completed" />
          <StatusItem status="BLOCKED" color="red" description="Payment blocked" />
        </div>
      </Card>
    </div>
  );
};

const StatusItem: React.FC<{
  status: string;
  color: string;
  description: string;
}> = ({ status, color, description }) => {
  const colorClasses: Record<string, string> = {
    gray: 'bg-gray-100 text-gray-800',
    blue: 'bg-blue-100 text-blue-800',
    yellow: 'bg-yellow-100 text-yellow-800',
    green: 'bg-green-100 text-green-800',
    red: 'bg-red-100 text-red-800',
  };

  return (
    <div>
      <span
        className={`inline-block px-3 py-1 text-xs font-semibold rounded-full ${colorClasses[color]}`}
      >
        {status}
      </span>
      <p className="text-xs text-gray-600 mt-1">{description}</p>
    </div>
  );
};
