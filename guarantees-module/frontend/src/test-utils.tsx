import React, { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ConfigProvider } from 'antd';
import enUS from 'antd/locale/en_US';

// Create a custom render function that includes providers
const AllTheProviders = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ConfigProvider locale={enUS}>
          {children}
        </ConfigProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

const customRender = (
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) => render(ui, { wrapper: AllTheProviders, ...options });

// Mock API functions
export const mockApi = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

// Mock guarantee data
export const mockGuarantee = {
  id: 1,
  reference: 'GT-20240101-000001',
  guaranteeType: 'PERFORMANCE',
  amount: 100000.00,
  currency: 'USD',
  issueDate: '2024-01-01',
  expiryDate: '2024-12-31',
  status: 'DRAFT',
  beneficiaryName: 'Test Beneficiary',
  applicantName: 'Test Applicant',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
};

export const mockGuarantees = [
  mockGuarantee,
  {
    ...mockGuarantee,
    id: 2,
    reference: 'GT-20240101-000002',
    amount: 50000.00,
    status: 'APPROVED',
  },
];

// Mock FX rate data
export const mockFxRates = [
  {
    id: 1,
    fromCurrency: 'USD',
    toCurrency: 'EUR',
    rate: 0.85,
    effectiveDate: '2024-01-01',
    provider: 'ECB',
  },
  {
    id: 2,
    fromCurrency: 'USD',
    toCurrency: 'GBP',
    rate: 0.78,
    effectiveDate: '2024-01-01',
    provider: 'ECB',
  },
];

// Mock client data
export const mockClients = [
  {
    id: 1,
    name: 'Test Client 1',
    code: 'TC001',
    type: 'CORPORATE',
    status: 'ACTIVE',
  },
  {
    id: 2,
    name: 'Test Client 2',
    code: 'TC002',
    type: 'INDIVIDUAL',
    status: 'ACTIVE',
  },
];

// Helper functions for testing
export const waitForLoadingToFinish = () => {
  return new Promise(resolve => setTimeout(resolve, 0));
};

export const createMockUser = (overrides = {}) => ({
  id: 1,
  username: 'testuser',
  email: 'test@example.com',
  roles: ['USER'],
  ...overrides,
});

// Re-export everything
export * from '@testing-library/react';
export { customRender as render };
