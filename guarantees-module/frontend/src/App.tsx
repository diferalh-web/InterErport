import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from 'react-query';
import { ReactQueryDevtools } from 'react-query/devtools';
import { ConfigProvider } from 'antd';
import enUS from 'antd/locale/en_US';
import esES from 'antd/locale/es_ES';
import deDE from 'antd/locale/de_DE';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import 'dayjs/locale/es';
import 'dayjs/locale/en';
import 'dayjs/locale/de';

// Import i18n configuration
import './i18n';

import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import GuaranteeList from './pages/GuaranteeList';
import GuaranteeDetail from './pages/GuaranteeDetail';
import CreateGuarantee from './pages/CreateGuarantee';
import EditGuarantee from './pages/EditGuarantee';
import FxRateManagement from './pages/FxRateManagement';
import ClientManagement from './pages/ClientManagement';
import AmendmentManagement from './pages/AmendmentManagement';
import ClaimManagement from './pages/ClaimManagement';
import ReportsModule from './pages/ReportsModule';

import './App.css';

// Configure dayjs
dayjs.extend(customParseFormat);

// Create a client for React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      staleTime: 5 * 60 * 1000, // 5 minutes
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 1,
    },
  },
});

// Component to handle locale switching
const AppContent: React.FC = () => {
  const { i18n } = useTranslation();
  const [locale, setLocale] = useState(enUS);

  useEffect(() => {
    // Update antd locale and dayjs locale based on i18n language
    if (i18n.language === 'es') {
      setLocale(esES);
      dayjs.locale('es');
    } else if (i18n.language === 'de') {
      setLocale(deDE);
      dayjs.locale('de');
    } else {
      setLocale(enUS);
      dayjs.locale('en');
    }
  }, [i18n.language]);

  return (
    <ConfigProvider locale={locale}>
      <Router>
        <div className="App">
          <Layout>
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/guarantees" element={<GuaranteeList />} />
          <Route path="/guarantees/new" element={<CreateGuarantee />} />
          <Route path="/guarantees/:id" element={<GuaranteeDetail />} />
          <Route path="/guarantees/:id/edit" element={<EditGuarantee />} />
          <Route path="/guarantees/:id/amendments" element={<AmendmentManagement />} />
          <Route path="/guarantees/:id/claims" element={<ClaimManagement />} />
          <Route path="/fx-rates" element={<FxRateManagement />} />
          <Route path="/clients" element={<ClientManagement />} />
          <Route path="/reports" element={<ReportsModule />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Layout>
        </div>
      </Router>
    </ConfigProvider>
  );
};

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
};

export default App;
