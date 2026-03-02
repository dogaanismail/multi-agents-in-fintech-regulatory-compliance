import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { HomePage } from './pages/HomePage';
import { PaymentListPage } from './pages/PaymentListPage';
import { PaymentDetailPage } from './pages/PaymentDetailPage';
import { CustomerListPage } from './pages/CustomerListPage';
import { CustomerDetailPage } from './pages/CustomerDetailPage';
import { AccountListPage } from './pages/AccountListPage';
import { AccountDetailPage } from './pages/AccountDetailPage';
import { ConfigurationListPage } from './pages/ConfigurationListPage';
import { ConfigurationDetailPage } from './pages/ConfigurationDetailPage';
import { MarlTrainingPage } from './pages/MarlTrainingPage';
import { ReplayBufferPage } from './pages/ReplayBufferPage';
import { DashboardPage } from './pages/DashboardPage';
import { AuditLogPage } from './pages/AuditLogPage';
import { CreateCustomerPage } from './pages/CreateCustomerPage';
import { CreateAccountPage } from './pages/CreateAccountPage';
import { CreatePaymentPage } from './pages/CreatePaymentPage';
import { ExchangeRatePage } from './pages/ExchangeRatePage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="payments" element={<PaymentListPage />} />
          <Route path="payments/create" element={<CreatePaymentPage />} />
          <Route path="payments/:paymentId" element={<PaymentDetailPage />} />
          <Route path="customers" element={<CustomerListPage />} />
          <Route path="customers/create" element={<CreateCustomerPage />} />
          <Route path="customers/:customerId" element={<CustomerDetailPage />} />
          <Route path="accounts" element={<AccountListPage />} />
          <Route path="accounts/open" element={<CreateAccountPage />} />
          <Route path="accounts/:accountId" element={<AccountDetailPage />} />
          <Route path="configurations" element={<ConfigurationListPage />} />
          <Route path="configurations/:configId" element={<ConfigurationDetailPage />} />
          <Route path="marl-training" element={<MarlTrainingPage />} />
          <Route path="replay-buffer" element={<ReplayBufferPage />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="audit-log" element={<AuditLogPage />} />
          <Route path="exchange-rates" element={<ExchangeRatePage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
