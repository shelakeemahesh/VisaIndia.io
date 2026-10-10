import '@testing-library/jest-dom';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from '../i18n/locales/en.json';

// Initialize i18n with English translations for tests
i18n.use(initReactI18next).init({
  lng: 'en',
  fallbackLng: 'en',
  resources: {
    en: { translation: en },
  },
  interpolation: {
    escapeValue: false,
  },
});

// Mock crypto.randomUUID
if (!globalThis.crypto) {
  globalThis.crypto = {
    randomUUID: () => '00000000-0000-0000-0000-000000000000' as `${string}-${string}-${string}-${string}-${string}`,
  } as any;
} else if (!globalThis.crypto.randomUUID) {
  globalThis.crypto.randomUUID = () => '00000000-0000-0000-0000-000000000000' as `${string}-${string}-${string}-${string}-${string}`;
}

// Mock window.scrollTo
if (typeof window !== 'undefined') {
  window.scrollTo = (() => {}) as any;
}
