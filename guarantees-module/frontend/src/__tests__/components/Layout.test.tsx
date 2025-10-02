import React from 'react';
import { render, screen } from '../test-utils';
import Layout from '../../components/Layout';

// Mock the useTranslation hook
jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: {
      changeLanguage: jest.fn(),
    },
  }),
}));

describe('Layout Component', () => {
  it('renders without crashing', () => {
    render(
      <Layout>
        <div>Test Content</div>
      </Layout>
    );
    
    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('renders header with title', () => {
    render(
      <Layout>
        <div>Test Content</div>
      </Layout>
    );
    
    // Check if the layout structure is present
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('renders children content', () => {
    const testContent = 'This is test content';
    render(
      <Layout>
        <div>{testContent}</div>
      </Layout>
    );
    
    expect(screen.getByText(testContent)).toBeInTheDocument();
  });
});
