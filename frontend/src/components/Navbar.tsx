import React from 'react';
import { useTranslation } from 'react-i18next';
import { Globe, PlaneTakeoff, ShieldCheck } from 'lucide-react';
import { supportedLanguages } from '../i18n/i18n';

export const Navbar: React.FC = () => {
  const { t, i18n } = useTranslation();

  const handleLanguageChange = (lng: string) => {
    i18n.changeLanguage(lng);
  };

  return (
    <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo & Title */}
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-700 to-brand-500 flex items-center justify-center text-white shadow-md shadow-brand-500/20">
              <PlaneTakeoff className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-xl font-bold tracking-tight text-slate-900">
                  {t('nav.brand')}
                </span>
                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-800 border border-amber-200">
                  {t('nav.conceptDisclaimer')}
                </span>
              </div>
              <p className="text-xs text-slate-500 hidden sm:block">
                {t('nav.tagline')}
              </p>
            </div>
          </div>

          {/* Navigation Links & Language Switcher */}
          <div className="flex items-center space-x-4 sm:space-x-6">
            <nav className="hidden md:flex items-center space-x-6 text-sm font-medium text-slate-600">
              <a href="#eligibility" className="hover:text-brand-600 transition-colors">
                {t('nav.eligibility')}
              </a>
              <a href="#apply" className="hover:text-brand-600 transition-colors">
                {t('nav.apply')}
              </a>
              <a href="#track" className="hover:text-brand-600 transition-colors">
                {t('nav.track')}
              </a>
            </nav>

            {/* Language Switcher */}
            <div className="flex items-center space-x-1.5 bg-slate-100 hover:bg-slate-200/80 rounded-lg p-1 transition-colors">
              <Globe className="w-4 h-4 text-slate-500 ml-1.5" />
              <select
                aria-label="Select language"
                value={i18n.language.split('-')[0]}
                onChange={(e) => handleLanguageChange(e.target.value)}
                className="bg-transparent text-xs font-medium text-slate-700 py-1 pl-1 pr-2 rounded focus:outline-none cursor-pointer"
              >
                {supportedLanguages.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.nativeName} ({lang.name})
                  </option>
                ))}
              </select>
            </div>

            <div className="hidden lg:flex items-center text-xs text-slate-500 space-x-1 border-l pl-4 border-slate-200">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>TLS 1.3 · End-to-End Encrypted</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
