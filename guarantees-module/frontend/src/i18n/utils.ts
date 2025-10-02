// Translation Utilities and Hooks
import { useTranslation } from 'react-i18next';
import { useCallback } from 'react';
import { getLanguageByCode } from './config';
import dayjs from 'dayjs';

// Hook for enhanced translation with utilities
export const useAppTranslation = () => {
  const { t, i18n } = useTranslation();
  const currentLanguage = getLanguageByCode(i18n.language) || getLanguageByCode('en')!;

  // Format numbers according to locale - memoized to prevent re-renders
  const formatNumber = useCallback((value: number, options?: Intl.NumberFormatOptions): string => {
    return new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
      ...options
    }).format(value);
  }, [i18n.language]);

  // Format currency according to locale - memoized to prevent re-renders
  const formatCurrency = useCallback((
    value: number, 
    currency: string = 'USD',
    options?: Intl.NumberFormatOptions
  ): string => {
    return new Intl.NumberFormat(i18n.language, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
      ...options
    }).format(value);
  }, [i18n.language]);

  // Format dates according to locale - memoized to prevent re-renders
  const formatDate = useCallback((
    date: string | Date | dayjs.Dayjs,
    format?: string
  ): string => {
    const dateObj = dayjs(date);
    if (format) {
      return dateObj.format(format);
    }
    
    // Use locale-specific format
    return dateObj.format(currentLanguage.dateFormat);
  }, [currentLanguage.dateFormat]);

  // Format relative time - memoized to prevent re-renders
  const formatRelativeTime = useCallback((date: string | Date | dayjs.Dayjs): string => {
    return dayjs(date).fromNow();
  }, []);

  // Translate with fallback and pluralization - memoized to prevent re-renders
  const translate = useCallback((
    key: string, 
    options?: {
      defaultValue?: string;
      count?: number;
      context?: string;
      [key: string]: any;
    }
  ): string => {
    try {
      return t(key, options);
    } catch (error) {
      console.warn(`Translation missing for key: ${key}`);
      return options?.defaultValue || key;
    }
  }, [t]);

  // Get enum translations
  const translateEnum = useCallback((
    namespace: string,
    enumValue: string,
    fallback?: string
  ): string => {
    const key = `${namespace}.${enumValue}`;
    const translated = t(key);
    
    if (translated === key) {
      // Translation not found, return fallback or formatted enum value
      return fallback || enumValue.replace(/_/g, ' ').toLowerCase()
        .replace(/\b\w/g, l => l.toUpperCase());
    }
    
    return translated;
  }, [t]);

  // Validate translation key exists
  const hasTranslation = useCallback((key: string): boolean => {
    const translated = t(key);
    return translated !== key;
  }, [t]);

  // Get all translations for a namespace
  const getNamespaceTranslations = useCallback((namespace: string): Record<string, any> => {
    const store = i18n.getResourceBundle(i18n.language, 'translation');
    return store?.[namespace] || {};
  }, [i18n]);

  return {
    t: translate,
    i18n,
    currentLanguage,
    formatNumber,
    formatCurrency,
    formatDate,
    formatRelativeTime,
    translateEnum,
    hasTranslation,
    getNamespaceTranslations,
    // Convenience methods for common patterns
    tField: (entity: string, field: string) => translate(`${entity}.fields.${field}`, { defaultValue: field }),
    tStatus: (entity: string, status: string) => translate(`${entity}.status.${status}`, { defaultValue: status }),
    tButton: (action: string) => translate(`buttons.${action}`, { defaultValue: action }),
    tValidation: (type: string) => translate(`validation.${type}`),
    tTable: (column: string) => translate(`tables.columns.${column}`, { defaultValue: column }),
  };
};

// Translation validation utilities (for development)
export const validateTranslations = (
  languages: string[],
  requiredKeys: string[]
): Record<string, string[]> => {
  const missing: Record<string, string[]> = {};
  
  // This would typically run in development to ensure all translations exist
  // Implementation would check actual translation files
  
  return missing;
};

// Pluralization rules for different languages
export const getPluralRule = (language: string, count: number): string => {
  const rules: Record<string, (n: number) => string> = {
    en: (n: number) => n === 1 ? 'one' : 'other',
    es: (n: number) => n === 1 ? 'one' : 'other', 
    de: (n: number) => n === 1 ? 'one' : 'other',
    fr: (n: number) => n <= 1 ? 'one' : 'other',
    pt: (n: number) => n <= 1 ? 'one' : 'other',
    ar: (n: number) => {
      if (n === 0) return 'zero';
      if (n === 1) return 'one';
      if (n === 2) return 'two';
      if (n % 100 >= 3 && n % 100 <= 10) return 'few';
      if (n % 100 >= 11) return 'many';
      return 'other';
    }
  };
  
  return rules[language]?.(count) || 'other';
};

// Context-aware translation helper
export const getContextualTranslation = (
  key: string,
  context: 'form' | 'table' | 'modal' | 'button' | 'status' | 'error',
  fallback?: string
): string => {
  // Would use actual translation function here
  return fallback || key;
};

// RTL language detection
export const isRTLLanguage = (languageCode: string): boolean => {
  const rtlLanguages = ['ar', 'he', 'fa', 'ur'];
  return rtlLanguages.includes(languageCode);
};
