/**
 * TypeScript type definitions for the Guarantees Module frontend.
 * Maps to the backend Java entities and DTOs.
 */

export enum GuaranteeStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
  SETTLED = 'SETTLED'
}

export enum GuaranteeType {
  PERFORMANCE = 'PERFORMANCE',
  ADVANCE_PAYMENT = 'ADVANCE_PAYMENT',
  BID_BOND = 'BID_BOND',
  WARRANTY = 'WARRANTY',
  CUSTOMS = 'CUSTOMS',
  PAYMENT = 'PAYMENT',
  OTHER = 'OTHER'
}

export enum AmendmentType {
  AMOUNT_INCREASE = 'AMOUNT_INCREASE',
  AMOUNT_DECREASE = 'AMOUNT_DECREASE',
  EXTEND_VALIDITY = 'EXTEND_VALIDITY',
  REDUCE_VALIDITY = 'REDUCE_VALIDITY',
  CHANGE_BENEFICIARY = 'CHANGE_BENEFICIARY',
  CHANGE_CURRENCY = 'CHANGE_CURRENCY',
  OTHER = 'OTHER'
}

export enum ClaimStatus {
  SUBMITTED = 'SUBMITTED',
  REQUESTED = 'REQUESTED', 
  UNDER_REVIEW = 'UNDER_REVIEW',
  PENDING_DOCUMENTS = 'PENDING_DOCUMENTS',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SETTLED = 'SETTLED'
}

export enum FxRateProvider {
  MANUAL = 'MANUAL',
  ECB = 'ECB',
  BLOOMBERG = 'BLOOMBERG'
}

export interface BaseEntity {
  id?: number;
  createdDate?: string;
  lastModifiedDate?: string;
  createdBy?: string;
  lastModifiedBy?: string;
  version?: number;
}

export interface GuaranteeContract extends BaseEntity {
  reference: string;
  guaranteeType: GuaranteeType;
  status: GuaranteeStatus;
  amount: number;
  currency: string;
  baseAmount?: number;
  exchangeRate?: number;
  issueDate: string;
  expiryDate: string;
  guaranteeText?: string;
  applicantId: number;
  beneficiaryName: string;
  beneficiaryAddress?: string;
  advisingBankBic?: string;
  isDomestic: boolean;
  underlyingContractRef?: string;
  specialConditions?: string;
  language: string;
  purpose?: string;
  undertakingText?: string;
  conditions?: string;
  amendments?: Amendment[];
  claims?: Claim[];
  feeItems?: FeeItem[];
}

export interface Amendment extends BaseEntity {
  amendmentReference: string;
  amendmentType: AmendmentType;
  status: GuaranteeStatus;
  changesJson: string;
  description?: string;
  reason?: string;
  submittedDate?: string;
  processedDate?: string;
  processedBy?: string;
  processingComments?: string;
  requiresConsent: boolean;
  consentReceivedDate?: string;
  swiftMessageReference?: string;
}

export interface Claim extends BaseEntity {
  claimReference: string;
  status: ClaimStatus;
  amount: number;
  currency: string;
  claimDate: string;
  processingDeadline?: string;
  claimReason?: string;
  documentsSubmitted?: string;
  missingDocuments?: string;
  beneficiaryContact?: string;
  approvedDate?: string;
  approvedBy?: string;
  paymentDate?: string;
  paymentReference?: string;
  rejectedDate?: string;
  rejectedBy?: string;
  rejectionReason?: string;
  processingNotes?: string;
  swiftMessageReference?: string;
  requiresSpecialApproval: boolean;
  documentPaths?: string[];
}

export interface FeeItem extends BaseEntity {
  feeType: string;
  description?: string;
  amount: number;
  currency: string;
  baseAmount?: number;
  exchangeRate?: number;
  dueDate?: string;
  rate?: number;
  minimumAmount?: number;
  installmentNumber?: number;
  totalInstallments?: number;
  isPaid: boolean;
  paymentDate?: string;
  paymentReference?: string;
  accountingEntryRef?: string;
  isCalculated: boolean;
  notes?: string;
}

export interface Client extends BaseEntity {
  clientCode: string;
  name: string;
  address?: string;
  city?: string;
  countryCode?: string;
  postalCode?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  entityType?: string;
  industryCode?: string;
  riskRating?: string;
  creditLimit?: number;
  creditCurrency?: string;
  isActive: boolean;
  kycDate?: string;
  kycReviewDate?: string;
  notes?: string;
}

export interface FxRate extends BaseEntity {
  baseCurrency: string;
  targetCurrency: string;
  rate: number;
  buyingRate?: number;
  sellingRate?: number;
  averageRate?: number;
  effectiveDate: string;
  expiryDate?: string;
  provider: FxRateProvider;
  providerReference?: string;
  isActive: boolean;
  retrievedAt?: string;
  notes?: string;
}

export interface GuaranteeTemplate extends BaseEntity {
  templateName: string;
  guaranteeType: GuaranteeType;
  language: string;
  version: number;
  templateText: string;
  templateContent?: string; // Legacy field name for compatibility
  description?: string;
  isActive: boolean;
  isDefault: boolean;
  priority: number;
  isDomestic?: boolean;
  requiredVariables?: string; // JSON array of required variable names
  optionalVariables?: string; // JSON array of optional variable names
  usageNotes?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
}

// API Response types
export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Form types
export interface GuaranteeFormData {
  reference?: string;  // Optional for creation, will be auto-generated if not provided
  guaranteeType: GuaranteeType;
  amount: number;
  currency: string;
  issueDate: any; // Can be string or Dayjs object from DatePicker
  expiryDate: any; // Can be string or Dayjs object from DatePicker
  applicantId: number;
  beneficiaryName: string;
  beneficiaryAddress?: string;
  advisingBankBic?: string;
  isDomestic: boolean;
  underlyingContractRef?: string;
  specialConditions?: string;
  guaranteeText?: string;  // Added missing field
  language: string;
}

export interface AmendmentFormData {
  amendmentType: AmendmentType;
  description?: string;
  reason?: string;
  changesJson: string;
  requiresConsent?: boolean;
}

export interface ClaimFormData {
  claimReference?: string;
  amount: number;
  currency: string;
  claimDate?: string;
  processingDeadline?: string;
  claimReason?: string;
  documentsSubmitted?: boolean;
  beneficiaryContact?: string;
  requiresSpecialApproval?: boolean;
  processingNotes?: string;
}

export interface SearchFilters {
  reference?: string;
  status?: GuaranteeStatus;
  guaranteeType?: GuaranteeType;
  applicantId?: number;
  currency?: string;
  fromDate?: string;
  toDate?: string;
}

// Utility types
export interface SelectOption {
  value: string;
  label: string;
}

export interface TableColumn {
  key: string;
  title: string;
  dataIndex: string;
  width?: number;
  sorter?: boolean;
  render?: (value: any, record: any) => React.ReactNode;
}

// Status display helpers
export const GuaranteeStatusLabels: Record<GuaranteeStatus, string> = {
  [GuaranteeStatus.DRAFT]: 'Draft',
  [GuaranteeStatus.SUBMITTED]: 'Submitted',
  [GuaranteeStatus.APPROVED]: 'Approved',
  [GuaranteeStatus.REJECTED]: 'Rejected',
  [GuaranteeStatus.CANCELLED]: 'Cancelled',
  [GuaranteeStatus.EXPIRED]: 'Expired',
  [GuaranteeStatus.SETTLED]: 'Settled'
};

export const GuaranteeTypeLabels: Record<GuaranteeType, string> = {
  [GuaranteeType.PERFORMANCE]: 'Performance Guarantee',
  [GuaranteeType.ADVANCE_PAYMENT]: 'Advance Payment Guarantee',
  [GuaranteeType.BID_BOND]: 'Bid Bond',
  [GuaranteeType.WARRANTY]: 'Warranty Guarantee',
  [GuaranteeType.CUSTOMS]: 'Customs Guarantee',
  [GuaranteeType.PAYMENT]: 'Payment Guarantee',
  [GuaranteeType.OTHER]: 'Other'
};

export const ClaimStatusLabels: Record<ClaimStatus, string> = {
  [ClaimStatus.SUBMITTED]: 'Submitted',
  [ClaimStatus.REQUESTED]: 'Requested',
  [ClaimStatus.UNDER_REVIEW]: 'Under Review',
  [ClaimStatus.PENDING_DOCUMENTS]: 'Pending Documents',
  [ClaimStatus.APPROVED]: 'Approved',
  [ClaimStatus.REJECTED]: 'Rejected',
  [ClaimStatus.SETTLED]: 'Settled'
};
