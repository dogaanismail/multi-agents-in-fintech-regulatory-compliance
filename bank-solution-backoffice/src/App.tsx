import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { HomePage } from './pages/HomePage';
import { PaymentListPage } from './pages/PaymentListPage';
import { PaymentDetailPage } from './pages/PaymentDetailPage';
import { CustomerListPage } from './pages/CustomerListPage';
import { CustomerDetailPage } from './pages/CustomerDetailPage';
import { AccountListPage } from './pages/AccountListPage';
import { AccountDetailPage } from './pages/AccountDetailPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="payments" element={<PaymentListPage />} />
          <Route path="payments/:paymentId" element={<PaymentDetailPage />} />
          <Route path="customers" element={<CustomerListPage />} />
          <Route path="customers/:customerId" element={<CustomerDetailPage />} />
          <Route path="accounts" element={<AccountListPage />} />
          <Route path="accounts/:accountId" element={<AccountDetailPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
