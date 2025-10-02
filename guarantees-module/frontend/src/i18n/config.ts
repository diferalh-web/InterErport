// Internationalization Configuration
export interface Language {
  code: string;
  name: string;
  nativeName: string;
  flag: string;
  rtl: boolean;
  dateFormat: string;
  timeFormat: string;
  numberFormat: {
    decimal: string;
    thousands: string;
    currency: {
      symbol: string;
      position: 'before' | 'after';
    };
  };
  antdLocale: string; // Maps to antd locale import
  dayjsLocale: string; // Maps to dayjs locale import
}

export const SUPPORTED_LANGUAGES: Language[] = [
  {
    code: 'en',
    name: 'English',
    nativeName: 'English',
    flag: '🇺🇸',
    rtl: false,
    dateFormat: 'MM/DD/YYYY',
    timeFormat: 'hh:mm A',
    numberFormat: {
      decimal: '.',
      thousands: ',',
      currency: {
        symbol: '$',
        position: 'before'
      }
    },
    antdLocale: 'enUS',
    dayjsLocale: 'en'
  },
  {
    code: 'es',
    name: 'Spanish',
    nativeName: 'Español',
    flag: '🇪🇸',
    rtl: false,
    dateFormat: 'DD/MM/YYYY',
    timeFormat: 'HH:mm',
    numberFormat: {
      decimal: ',',
      thousands: '.',
      currency: {
        symbol: '€',
        position: 'after'
      }
    },
    antdLocale: 'esES',
    dayjsLocale: 'es'
  },
  {
    code: 'de',
    name: 'German',
    nativeName: 'Deutsch',
    flag: '🇩🇪',
    rtl: false,
    dateFormat: 'DD.MM.YYYY',
    timeFormat: 'HH:mm',
    numberFormat: {
      decimal: ',',
      thousands: '.',
      currency: {
        symbol: '€',
        position: 'after'
      }
    },
    antdLocale: 'deDE',
    dayjsLocale: 'de'
  },
  {
    code: 'fr',
    name: 'French',
    nativeName: 'Français',
    flag: '🇫🇷',
    rtl: false,
    dateFormat: 'DD/MM/YYYY',
    timeFormat: 'HH:mm',
    numberFormat: {
      decimal: ',',
      thousands: ' ',
      currency: {
        symbol: '€',
        position: 'after'
      }
    },
    antdLocale: 'frFR',
    dayjsLocale: 'fr'
  },
  {
    code: 'pt',
    name: 'Portuguese',
    nativeName: 'Português',
    flag: '🇧🇷',
    rtl: false,
    dateFormat: 'DD/MM/YYYY',
    timeFormat: 'HH:mm',
    numberFormat: {
      decimal: ',',
      thousands: '.',
      currency: {
        symbol: 'R$',
        position: 'before'
      }
    },
    antdLocale: 'ptBR',
    dayjsLocale: 'pt-br'
  },
  {
    code: 'ar',
    name: 'Arabic',
    nativeName: 'العربية',
    flag: '🇸🇦',
    rtl: true,
    dateFormat: 'DD/MM/YYYY',
    timeFormat: 'HH:mm',
    numberFormat: {
      decimal: '.',
      thousands: ',',
      currency: {
        symbol: 'ر.س',
        position: 'before'
      }
    },
    antdLocale: 'arEG',
    dayjsLocale: 'ar'
  }
];

export const DEFAULT_LANGUAGE = 'en';
export const FALLBACK_LANGUAGE = 'en';

// Helper functions
export const getLanguageByCode = (code: string): Language | undefined => {
  return SUPPORTED_LANGUAGES.find(lang => lang.code === code);
};

export const isLanguageSupported = (code: string): boolean => {
  return SUPPORTED_LANGUAGES.some(lang => lang.code === code);
};

export const getLanguageOptions = () => {
  return SUPPORTED_LANGUAGES.map(lang => ({
    value: lang.code,
    label: lang.name,
    nativeLabel: lang.nativeName,
    flag: lang.flag
  }));
};

// Translation namespaces - helps organize translations
export const TRANSLATION_NAMESPACES = {
  COMMON: 'common',
  NAVIGATION: 'navigation',
  DASHBOARD: 'dashboard',
  GUARANTEES: 'guarantees',
  CLAIMS: 'claims',
  AMENDMENTS: 'amendments',
  CLIENTS: 'clients',
  REPORTS: 'reports',
  SETTINGS: 'settings',
  VALIDATION: 'validation',
  MESSAGES: 'messages',
  AUTH: 'auth',
  TEMPLATES: 'templates',
  FX_RATES: 'fxRates',
  TABLES: 'tables',
  BUTTONS: 'buttons',
  FORMS: 'forms',
  ERRORS: 'errors',
  STATUS: 'status'
} as const;

// Translation keys that must exist in all languages
export const REQUIRED_TRANSLATION_KEYS = [
  'common.loading',
  'common.error',
  'common.success',
  'common.cancel',
  'common.save',
  'common.edit',
  'common.delete',
  'navigation.dashboard',
  'navigation.guarantees',
  'validation.required',
  'buttons.submit',
  'buttons.cancel'
];

// Feature flags for translations
export const TRANSLATION_FEATURES = {
  PLURALIZATION: true,
  INTERPOLATION: true,
  CONTEXT: true,
  NAMESPACES: true,
  FALLBACK: true,
  VALIDATION: process.env.NODE_ENV === 'development'
};

// Cultural settings
export const CULTURAL_SETTINGS = {
  en: {
    firstDayOfWeek: 0, // Sunday
    workingDays: [1, 2, 3, 4, 5], // Monday to Friday
    holidays: []
  },
  es: {
    firstDayOfWeek: 1, // Monday
    workingDays: [1, 2, 3, 4, 5],
    holidays: []
  },
  de: {
    firstDayOfWeek: 1, // Monday
    workingDays: [1, 2, 3, 4, 5],
    holidays: []
  },
  fr: {
    firstDayOfWeek: 1, // Monday
    workingDays: [1, 2, 3, 4, 5],
    holidays: []
  },
  pt: {
    firstDayOfWeek: 0, // Sunday
    workingDays: [1, 2, 3, 4, 5],
    holidays: []
  },
  ar: {
    firstDayOfWeek: 6, // Saturday
    workingDays: [0, 1, 2, 3, 4], // Sunday to Thursday
    holidays: []
  }
};