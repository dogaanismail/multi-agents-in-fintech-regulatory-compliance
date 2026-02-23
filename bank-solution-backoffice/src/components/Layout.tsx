import React, { useState, useRef, useEffect } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

export const Layout: React.FC = () => {
  const location = useLocation();
  const [createOpen, setCreateOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const isActive = (path: string) => {
    return location.pathname.startsWith(path);
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setCreateOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Close dropdown on route change
  useEffect(() => {
    setCreateOpen(false);
  }, [location.pathname]);

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
            <nav className="flex items-center space-x-6">
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

              {/* Create Dropdown */}
              <div ref={dropdownRef} className="relative">
                <button
                  onClick={() => setCreateOpen((o) => !o)}
                  className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors border ${
                    createOpen
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100'
                  }`}
                >
                  <span>＋ Create</span>
                  <svg
                    className={`w-3.5 h-3.5 transition-transform ${createOpen ? 'rotate-180' : ''}`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={2.5}
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {createOpen && (
                  <div className="absolute right-0 mt-2 w-52 bg-white rounded-lg shadow-lg border border-gray-100 py-1 z-50">
                    <DropdownItem to="/customers/create" icon="👤" label="New Customer" />
                    <DropdownItem to="/accounts/open" icon="🏦" label="Open Account" />
                    <DropdownItem to="/payments/create" icon="💳" label="New Payment" />
                  </div>
                )}
              </div>
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
            © 2026 Bank Solution. Compliance Backoffice for AML &amp; Fraud Detection.
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

const DropdownItem: React.FC<{ to: string; icon: string; label: string }> = ({
  to,
  icon,
  label,
}) => (
  <Link
    to={to}
    className="flex items-center gap-2.5 px-4 py-2.5 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
  >
    <span className="text-base">{icon}</span>
    {label}
  </Link>
);
