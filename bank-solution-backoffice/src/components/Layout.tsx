import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

export const Layout: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) => {
    return location.pathname.startsWith(path);
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <Link to="/" className="flex items-center">
                <h1 className="text-xl font-bold text-gray-900">
                  🏦 Bank Solution
                </h1>
                <span className="ml-3 text-sm text-gray-500 border-l pl-3">
                  Compliance Backoffice
                </span>
              </Link>
            </div>
            <nav className="flex space-x-8">
              <NavLink to="/payments" active={isActive('/payments')}>
                Payments
              </NavLink>
              <NavLink to="/customers" active={isActive('/customers')}>
                Customers
              </NavLink>
              <NavLink to="/accounts" active={isActive('/accounts')}>
                Accounts
              </NavLink>
              <NavLink to="/configurations" active={isActive('/configurations')}>
                Configurations
              </NavLink>
              <NavLink to="/marl-training" active={isActive('/marl-training')}>
                MARL Training
              </NavLink>
            </nav>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="text-center text-sm text-gray-500">
            © 2026 Bank Solution. Compliance Backoffice for AML & Fraud Detection.
          </div>
        </div>
      </footer>
    </div>
  );
};

interface NavLinkProps {
  to: string;
  active: boolean;
  children: React.ReactNode;
}

const NavLink: React.FC<NavLinkProps> = ({ to, active, children }) => {
  return (
    <Link
      to={to}
      className={`inline-flex items-center px-1 pt-1 text-sm font-medium border-b-2 transition-colors ${
        active
          ? 'border-blue-500 text-gray-900'
          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
      }`}
    >
      {children}
    </Link>
  );
};
