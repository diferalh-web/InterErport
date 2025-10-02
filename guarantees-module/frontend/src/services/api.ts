/**
 * API service for communicating with the Guarantees Module backend.
 * Provides typed methods for all API endpoints.
 */

import axios, { AxiosInstance } from 'axios';
import {
  GuaranteeContract,
  Amendment,
  Claim,
  FeeItem,
  Client,
  FxRate,
  FxRateProvider,
  GuaranteeFormData,
  AmendmentFormData,
  ClaimFormData,
  SearchFilters,
  PaginatedResponse
} from '../types/guarantee';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: process.env.REACT_APP_API_BASE_URL || '/api/v1',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for auth tokens, logging, etc.
    this.api.interceptors.request.use(
      (config) => {
        // Add Basic auth credentials for POC (admin:admin123)
        // In production, this should be handled via proper login flow
        const credentials = btoa('admin:admin123');
        config.headers.Authorization = `Basic ${credentials}`;
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          console.error('Authentication failed - check backend credentials');
        }
        return Promise.reject(error);
      }
    );
  }

  // Guarantee CRUD operations
  async createGuarantee(guaranteeData: GuaranteeFormData): Promise<GuaranteeContract> {
    const response = await this.api.post<GuaranteeContract>('/guarantees', guaranteeData);
    return response.data;
  }

  async getGuarantee(id: number): Promise<GuaranteeContract> {
    const response = await this.api.get<GuaranteeContract>(`/guarantees/${id}`);
    return response.data;
  }

  async getGuaranteeByReference(reference: string): Promise<GuaranteeContract> {
    const response = await this.api.get<GuaranteeContract>(`/guarantees/reference/${reference}`);
    return response.data;
  }

  async updateGuarantee(id: number, guaranteeData: Partial<GuaranteeFormData>): Promise<GuaranteeContract> {
    const response = await this.api.put<GuaranteeContract>(`/guarantees/${id}`, guaranteeData);
    return response.data;
  }

  async searchGuarantees(
    filters: SearchFilters = {},
    page: number = 0,
    size: number = 20,
    sort?: string
  ): Promise<PaginatedResponse<GuaranteeContract>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      ...(sort && { sort }),
      ...Object.entries(filters).reduce((acc, [key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          acc[key] = value.toString();
        }
        return acc;
      }, {} as Record<string, string>)
    });

    const response = await this.api.get<PaginatedResponse<GuaranteeContract>>(
      `/guarantees?${params.toString()}`
    );
    return response.data;
  }

  async getGuaranteeDetails(id: number): Promise<GuaranteeContract> {
    const response = await this.api.get<GuaranteeContract>(`/guarantees/${id}`);
    return response.data;
  }

  // Guarantee lifecycle operations
  async submitGuarantee(id: number): Promise<GuaranteeContract> {
    const response = await this.api.post<GuaranteeContract>(`/guarantees/${id}/submit`);
    return response.data;
  }

  async approveGuarantee(id: number, approvedBy: string): Promise<GuaranteeContract> {
    const response = await this.api.post<GuaranteeContract>(
      `/guarantees/${id}/approve?approvedBy=${encodeURIComponent(approvedBy)}`
    );
    return response.data;
  }

  async rejectGuarantee(id: number, rejectedBy: string, reason: string): Promise<GuaranteeContract> {
    const response = await this.api.post<GuaranteeContract>(
      `/guarantees/${id}/reject?rejectedBy=${encodeURIComponent(rejectedBy)}&reason=${encodeURIComponent(reason)}`
    );
    return response.data;
  }

  async cancelGuarantee(id: number, cancelledBy: string, reason: string): Promise<GuaranteeContract> {
    const response = await this.api.post<GuaranteeContract>(
      `/guarantees/${id}/cancel?cancelledBy=${encodeURIComponent(cancelledBy)}&reason=${encodeURIComponent(reason)}`
    );
    return response.data;
  }

  // Expiring guarantees
  async getExpiringGuarantees(daysAhead: number = 30): Promise<GuaranteeContract[]> {
    const response = await this.api.get<GuaranteeContract[]>(
      `/guarantees/expiring?daysAhead=${daysAhead}`
    );
    return response.data;
  }

  // Outstanding amounts summary
  async getOutstandingAmounts(): Promise<{
    byCurrency: Array<[string, number]>;
    totalInBaseCurrency: number;
  }> {
    const response = await this.api.get(`/guarantees/outstanding-amounts`);
    return response.data;
  }

  // Amendment operations
  async createAmendment(guaranteeId: number, amendmentData: AmendmentFormData): Promise<Amendment> {
    const response = await this.api.post<Amendment>(
      `/guarantees/${guaranteeId}/amendments`,
      amendmentData
    );
    return response.data;
  }

  async getAmendments(guaranteeId: number): Promise<Amendment[]> {
    const response = await this.api.get<PaginatedResponse<Amendment>>(`/guarantees/${guaranteeId}/amendments`);
    return response.data.content;
  }

  // Claim operations
  async createClaim(guaranteeId: number, claimData: ClaimFormData): Promise<Claim> {
    const response = await this.api.post<Claim>(`/guarantees/${guaranteeId}/claims`, claimData);
    return response.data;
  }

  async getClaims(guaranteeId: number): Promise<Claim[]> {
    const response = await this.api.get<PaginatedResponse<Claim>>(`/guarantees/${guaranteeId}/claims`);
    return response.data.content;
  }

  async approveClaim(guaranteeId: number, claimId: number, approvedBy: string): Promise<Claim> {
    const response = await this.api.post<Claim>(
      `/claims/${claimId}/approve?approvedBy=${encodeURIComponent(approvedBy)}`
    );
    return response.data;
  }

  async rejectClaim(
    guaranteeId: number,
    claimId: number,
    rejectedBy: string,
    reason: string
  ): Promise<Claim> {
    const response = await this.api.post<Claim>(
      `/claims/${claimId}/reject?rejectedBy=${encodeURIComponent(rejectedBy)}&reason=${encodeURIComponent(reason)}`
    );
    return response.data;
  }

  async settleClaim(guaranteeId: number, claimId: number, paymentReference: string): Promise<Claim> {
    const response = await this.api.post<Claim>(
      `/claims/${claimId}/settle?paymentReference=${encodeURIComponent(paymentReference)}`
    );
    return response.data;
  }

  // Fee operations
  async getFeeItems(guaranteeId: number): Promise<FeeItem[]> {
    const response = await this.api.get<FeeItem[]>(`/guarantees/${guaranteeId}/fees`);
    return response.data;
  }

  // Client operations
  async getClients(page: number = 0, size: number = 20): Promise<PaginatedResponse<Client>> {
    const response = await this.api.get<PaginatedResponse<Client>>(
      `/clients?page=${page}&size=${size}`
    );
    return response.data;
  }

  async getClient(id: number): Promise<Client> {
    const response = await this.api.get<Client>(`/clients/${id}`);
    return response.data;
  }

  async searchClients(searchTerm: string): Promise<Client[]> {
    const response = await this.api.get<PaginatedResponse<Client>>(
      `/clients/search?q=${encodeURIComponent(searchTerm)}&page=0&size=100`
    );
    return response.data.content;
  }

  async createClient(clientData: Partial<Client>): Promise<Client> {
    const response = await this.api.post<Client>('/clients', clientData);
    return response.data;
  }

  async updateClient(id: number, clientData: Partial<Client>): Promise<Client> {
    const response = await this.api.put<Client>(`/clients/${id}`, clientData);
    return response.data;
  }

  async deleteClient(id: number): Promise<void> {
    await this.api.delete(`/clients/${id}`);
  }

  // FX Rate operations
  async getFxRates(): Promise<FxRate[]> {
    const response = await this.api.get<PaginatedResponse<FxRate>>('/fx-rates?size=100');
    return response.data.content;
  }

  async getCurrentRate(fromCurrency: string, toCurrency: string): Promise<number> {
    const response = await this.api.get<{ rate: number }>(
      `/fx-rates/current?from=${fromCurrency}&to=${toCurrency}`
    );
    return response.data.rate;
  }

  async createManualRate(rateData: {
    baseCurrency: string;
    targetCurrency: string;
    rate: number;
    effectiveDate: string;
    buyingRate?: number;
    sellingRate?: number;
    notes?: string;
    provider: FxRateProvider;
    isActive: boolean;
  }): Promise<FxRate> {
    const response = await this.api.post<FxRate>('/fx-rates', rateData);
    return response.data;
  }

  // Health check
  async healthCheck(): Promise<{ status: string; service: string; timestamp: string }> {
    const response = await this.api.get('/guarantees/health');
    return response.data;
  }

  // Utility methods
  async uploadFile(file: File, type: 'guarantee' | 'claim', entityId: number): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    formData.append('entityId', entityId.toString());

    const response = await this.api.post<{ filePath: string }>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.filePath;
  }

  // Convert amount between currencies
  async convertAmount(
    amount: number,
    fromCurrency: string,
    toCurrency: string
  ): Promise<{ convertedAmount: number; rate: number }> {
    const response = await this.api.post<{ convertedAmount: number; rate: number }>(
      '/fx-rates/convert',
      { amount, fromCurrency, toCurrency }
    );
    return response.data;
  }

  // Report operations
  async downloadCommissionReport(startDate: string, endDate: string, format: string = 'PDF', includeProjections: boolean = false): Promise<Blob> {
    const response = await this.api.get(`/reports/commissions`, {
      params: { startDate, endDate, format, includeProjections },
      responseType: 'blob'
    });
    return response.data;
  }

  async downloadActiveTransactionsReport(startDate: string, endDate: string, format: string = 'PDF', includeDrafts: boolean = false, includeExpired: boolean = false): Promise<Blob> {
    const response = await this.api.get(`/reports/active-transactions`, {
      params: { startDate, endDate, format, includeDrafts, includeExpired },
      responseType: 'blob'
    });
    return response.data;
  }

  async downloadAuditReport(startDate: string, endDate: string, format: string = 'PDF'): Promise<Blob> {
    const response = await this.api.get(`/reports/audit`, {
      params: { startDate, endDate, format },
      responseType: 'blob'
    });
    return response.data;
  }

  async downloadExpiryAlertReport(daysAhead: number = 30, format: string = 'PDF'): Promise<Blob> {
    const response = await this.api.get(`/reports/expiry-alerts`, {
      params: { daysAhead, format },
      responseType: 'blob'
    });
    return response.data;
  }

  // Dashboard Analytics operations
  async getDashboardSummary(): Promise<{
    guarantees: { total: number; totalAmount: number; active: number };
    claims: { total: number; totalAmount: number };
    amendments: { total: number };
  }> {
    const response = await this.api.get('/dashboard/summary');
    return response.data;
  }

  async getMonthlyStatistics(monthsBack: number = 12): Promise<{
    monthlyData: Array<{
      month: string;
      monthLabel: string;
      guarantees: { count: number; amount: number };
      claims: { count: number; amount: number };
      amendments: { count: number };
    }>;
    period: { startDate: string; endDate: string; monthsBack: number };
  }> {
    const response = await this.api.get(`/dashboard/monthly-stats?monthsBack=${monthsBack}`);
    return response.data;
  }

  async getMetricsByCurrency(): Promise<{
    guaranteesByCurrency: Array<{ currency: string; amount: number; count: number }>;
    claimsByCurrency: Array<{ currency: string; amount: number; count: number }>;
  }> {
    const response = await this.api.get('/dashboard/metrics-by-currency');
    return response.data;
  }

  async getActivityTrend(daysBack: number = 30): Promise<{
    guarantees: Array<{ date: string; count: number }>;
    claims: Array<{ date: string; count: number }>;
    amendments: Array<{ date: string; count: number }>;
    period: { startDate: string; endDate: string; daysBack: number };
  }> {
    const response = await this.api.get(`/dashboard/activity-trend?daysBack=${daysBack}`);
    return response.data;
  }

  // Template operations
  async getAllActiveTemplates(): Promise<any[]> {
    const response = await this.api.get('/templates');
    return response.data;
  }

  async getTemplatesByType(guaranteeType: string): Promise<any[]> {
    const response = await this.api.get(`/templates/type/${guaranteeType}`);
    return response.data;
  }

  async selectTemplate(guaranteeType: string, language: string = 'EN', field22A?: string, field22D?: string): Promise<any> {
    const params = new URLSearchParams({
      guaranteeType,
      language,
      ...(field22A && { field22A }),
      ...(field22D && { field22D })
    });
    const response = await this.api.get(`/templates/select?${params}`);
    return response.data;
  }

  async renderTemplatePreview(templateId: number, guaranteeId: number): Promise<{
    renderedText: string;
    missingRequired: string[];
    allVariables: string[];
    variableValues: Record<string, string>;
    isValid: boolean;
  }> {
    const response = await this.api.post(`/templates/${templateId}/preview?guaranteeId=${guaranteeId}`);
    return response.data;
  }

  async applyTemplateToGuarantee(templateId: number, guaranteeId: number, customVariables?: Record<string, any>): Promise<any> {
    const response = await this.api.post(`/templates/${templateId}/apply/${guaranteeId}`, customVariables);
    return response.data;
  }

  async getTemplateVariables(templateId: number): Promise<{
    allVariables: string[];
    requiredVariables: string[];
    optionalVariables: string[];
    variableDescriptions: Record<string, string>;
  }> {
    const response = await this.api.get(`/templates/${templateId}/variables`);
    return response.data;
  }

  async validateTemplate(templateId: number, guaranteeId: number): Promise<{
    isValid: boolean;
    missingRequired: string[];
    allVariables: string[];
    errorMessage?: string;
  }> {
    const response = await this.api.post(`/templates/${templateId}/validate?guaranteeId=${guaranteeId}`);
    return response.data;
  }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;
