import React from 'react';
import { render, screen, waitFor } from '../test-utils';
import { mockGuarantees, mockFxRates } from '../test-utils';
import Dashboard from '../../pages/Dashboard';

// Mock the API service
jest.mock('../../services/api', () => ({
  getGuarantees: jest.fn(() => Promise.resolve({ data: mockGuarantees })),
  getFxRates: jest.fn(() => Promise.resolve({ data: mockFxRates })),
  getDashboardStats: jest.fn(() => Promise.resolve({
    data: {
      totalGuarantees: 2,
      totalAmount: 150000,
      activeGuarantees: 1,
      expiringThisMonth: 0,
    }
  })),
}));

// Mock the useTranslation hook
jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: {
      changeLanguage: jest.fn(),
    },
  }),
}));

describe('Dashboard Page', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders dashboard without crashing', async () => {
    render(<Dashboard />);
    
    // Wait for the component to load
    await waitFor(() => {
      expect(screen.getByText(/dashboard/i)).toBeInTheDocument();
    });
  });

  it('displays dashboard statistics', async () => {
    render(<Dashboard />);
    
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument(); // totalGuarantees
      expect(screen.getByText('150000')).toBeInTheDocument(); // totalAmount
    });
  });

  it('shows loading state initially', () => {
    render(<Dashboard />);
    
    // Should show loading indicator
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('handles error state gracefully', async () => {
    // Mock API to return error
    const { getDashboardStats } = require('../../services/api');
    getDashboardStats.mockRejectedValueOnce(new Error('API Error'));
    
    render(<Dashboard />);
    
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
    });
  });
});
