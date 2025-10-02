import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Translation resources
import en from './locales/en.json';
import es from './locales/es.json';
import de from './locales/de.json';

const resources = {
  en: {
    translation: en
  },
  es: {
    translation: es
  },
  de: {
    translation: de
  }
};

i18n
  // Detect user language
  .use(LanguageDetector)
  // Pass the i18n instance to react-i18next
  .use(initReactI18next)
  // Initialize i18next
  .init({
    resources,
    fallbackLng: 'en', // Default language
    debug: process.env.NODE_ENV === 'development',
    
    interpolation: {
      escapeValue: false // React already escapes by default
    },
    
    // Language detection options
    detection: {
      order: ['localStorage', 'navigator', 'htmlTag'],
      caches: ['localStorage']
    }
  });

export default i18n;
