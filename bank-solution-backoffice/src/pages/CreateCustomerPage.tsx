import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { customerService } from '@/api/customerService';
import { CreateCustomerRequest } from '@/types';
import { Card, Button } from '@/components/common';

// ─── Constants ────────────────────────────────────────────────────────────────

const COUNTRY_OPTIONS = [
  { value: '', label: 'Select country…' },
  { value: 'AE', label: 'AE — United Arab Emirates' },
  { value: 'AL', label: 'AL — Albania' },
  { value: 'AT', label: 'AT — Austria' },
  { value: 'AU', label: 'AU — Australia' },
  { value: 'BE', label: 'BE — Belgium' },
  { value: 'BR', label: 'BR — Brazil' },
  { value: 'CA', label: 'CA — Canada' },
  { value: 'CH', label: 'CH — Switzerland' },
  { value: 'CN', label: 'CN — China' },
  { value: 'DE', label: 'DE — Germany' },
  { value: 'DZ', label: 'DZ — Algeria' },
  { value: 'EG', label: 'EG — Egypt' },
  { value: 'ES', label: 'ES — Spain' },
  { value: 'ET', label: 'ET — Ethiopia' },
  { value: 'FR', label: 'FR — France' },
  { value: 'GB', label: 'GB — United Kingdom' },
  { value: 'GH', label: 'GH — Ghana' },
  { value: 'ID', label: 'ID — Indonesia' },
  { value: 'IN', label: 'IN — India' },
  { value: 'IT', label: 'IT — Italy' },
  { value: 'JP', label: 'JP — Japan' },
  { value: 'KE', label: 'KE — Kenya' },
  { value: 'KR', label: 'KR — South Korea' },
  { value: 'MA', label: 'MA — Morocco' },
  { value: 'MX', label: 'MX — Mexico' },
  { value: 'NG', label: 'NG — Nigeria' },
  { value: 'NL', label: 'NL — Netherlands' },
  { value: 'PH', label: 'PH — Philippines' },
  { value: 'PK', label: 'PK — Pakistan' },
  { value: 'PL', label: 'PL — Poland' },
  { value: 'PT', label: 'PT — Portugal' },
  { value: 'RU', label: 'RU — Russia' },
  { value: 'SA', label: 'SA — Saudi Arabia' },
  { value: 'SE', label: 'SE — Sweden' },
  { value: 'SG', label: 'SG — Singapore' },
  { value: 'TR', label: 'TR — Turkey' },
  { value: 'TZ', label: 'TZ — Tanzania' },
  { value: 'UA', label: 'UA — Ukraine' },
  { value: 'US', label: 'US — United States' },
  { value: 'ZA', label: 'ZA — South Africa' },
  { value: 'ZW', label: 'ZW — Zimbabwe' },
];

// ─── Field Component ──────────────────────────────────────────────────────────

const Field: React.FC<{
  label: string;
  required?: boolean;
  hint?: string;
  children: React.ReactNode;
}> = ({ label, required, hint, children }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700 mb-1">
      {label}
      {required && <span className="text-red-500 ml-1">*</span>}
    </label>
    {children}
    {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
  </div>
);

const inputCls =
  'w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm';
const selectCls =
  'w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm bg-white';

// ─── Page ─────────────────────────────────────────────────────────────────────

const initialForm: CreateCustomerRequest = {
  firstName: '',
  lastName: '',
  middleName: '',
  email: '',
  phoneNumber: '',
  dateOfBirth: '',
  nationality: '',
  customerType: 'INDIVIDUAL',
  address: { city: '', countryCode: '' },
};

export const CreateCustomerPage: React.FC = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState<CreateCustomerRequest>(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = (field: keyof CreateCustomerRequest, value: string) =>
    setForm((f) => ({ ...f, [field]: value }));

  const setAddress = (field: 'city' | 'countryCode', value: string) =>
    setForm((f) => ({ ...f, address: { ...f.address, [field]: value } }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Trim optional fields
    const payload: CreateCustomerRequest = {
      ...form,
      middleName: form.middleName?.trim() || undefined,
    };

    setLoading(true);
    try {
      const customer = await customerService.createCustomer(payload);
      navigate(`/customers/${customer.id}`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to create customer');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 flex items-center gap-2">
        <Link to="/customers" className="hover:text-blue-600 transition-colors">
          Customers
        </Link>
        <span>›</span>
        <span className="text-gray-900 font-medium">Create New Customer</span>
      </nav>

      <div>
        <h1 className="text-2xl font-bold text-gray-900">Create New Customer</h1>
        <p className="mt-1 text-sm text-gray-500">
          Register a new customer profile in the system. All required fields are marked with{' '}
          <span className="text-red-500">*</span>.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Personal Information */}
        <Card title="Personal Information">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="First Name" required>
              <input
                className={inputCls}
                type="text"
                value={form.firstName}
                onChange={(e) => set('firstName', e.target.value)}
                placeholder="e.g. John"
                minLength={2}
                maxLength={100}
                required
              />
            </Field>

            <Field label="Last Name" required>
              <input
                className={inputCls}
                type="text"
                value={form.lastName}
                onChange={(e) => set('lastName', e.target.value)}
                placeholder="e.g. Doe"
                minLength={2}
                maxLength={100}
                required
              />
            </Field>

            <Field label="Middle Name" hint="Optional">
              <input
                className={inputCls}
                type="text"
                value={form.middleName}
                onChange={(e) => set('middleName', e.target.value)}
                placeholder="e.g. Robert"
                maxLength={100}
              />
            </Field>

            <Field label="Customer Type" required>
              <select
                className={selectCls}
                value={form.customerType}
                onChange={(e) => set('customerType', e.target.value)}
                required
              >
                <option value="INDIVIDUAL">👤 Individual</option>
                <option value="BUSINESS">🏢 Business</option>
              </select>
            </Field>

            <Field label="Date of Birth" required hint="Must be in the past">
              <input
                className={inputCls}
                type="date"
                value={form.dateOfBirth}
                onChange={(e) => set('dateOfBirth', e.target.value)}
                max={new Date().toISOString().split('T')[0]}
                required
              />
            </Field>

            <Field label="Nationality" required hint="2-letter ISO country code">
              <select
                className={selectCls}
                value={form.nationality}
                onChange={(e) => set('nationality', e.target.value)}
                required
              >
                {COUNTRY_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </Card>

        {/* Contact Information */}
        <Card title="Contact Information">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Email Address" required>
              <input
                className={inputCls}
                type="email"
                value={form.email}
                onChange={(e) => set('email', e.target.value)}
                placeholder="john.doe@example.com"
                minLength={7}
                maxLength={255}
                required
              />
            </Field>

            <Field
              label="Phone Number"
              required
              hint="E.164 format: +1234567890"
            >
              <input
                className={inputCls}
                type="tel"
                value={form.phoneNumber}
                onChange={(e) => set('phoneNumber', e.target.value)}
                placeholder="+12025551234"
                required
              />
            </Field>
          </div>
        </Card>

        {/* Address */}
        <Card title="Address">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="City" required>
              <input
                className={inputCls}
                type="text"
                value={form.address.city}
                onChange={(e) => setAddress('city', e.target.value)}
                placeholder="e.g. New York"
                minLength={2}
                maxLength={100}
                required
              />
            </Field>

            <Field label="Country" required hint="2-letter ISO country code">
              <select
                className={selectCls}
                value={form.address.countryCode}
                onChange={(e) => setAddress('countryCode', e.target.value)}
                required
              >
                {COUNTRY_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </Card>

        {/* Error Banner */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4 flex items-start gap-3">
            <span className="text-red-500 text-lg">⚠️</span>
            <div>
              <p className="text-sm font-medium text-red-800">Failed to create customer</p>
              <p className="text-sm text-red-600 mt-1">{error}</p>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-between">
          <Button
            type="button"
            variant="secondary"
            onClick={() => navigate('/customers')}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={loading}>
            {loading ? '⏳ Creating…' : '✅ Create Customer'}
          </Button>
        </div>
      </form>
    </div>
  );
};
