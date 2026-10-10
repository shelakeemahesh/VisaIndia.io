import React, { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Globe, ChevronDown, Sparkles } from 'lucide-react';
import { LotusLogo } from './LotusLogo';

interface NavbarProps {
  onOpenAiAgent?: () => void;
  onSelectCategory?: (category: string) => void;
  fontSize: 'normal' | 'large';
  onToggleFontSize: () => void;
}

export const supportedLanguagesList = [
  { code: 'en', native: 'English' },
  { code: 'hi', native: 'हिन्दी' },
  { code: 'bn', native: 'বাংলা' },
  { code: 'es', native: 'Español' },
  { code: 'fr', native: 'Français' },
  { code: 'de', native: 'Deutsch' },
  { code: 'ja', native: '日本語' },
  { code: 'ko', native: '한국어' },
  { code: 'ar', native: 'العربية' },
  { code: 'pt', native: 'Português' },
  { code: 'it', native: 'Italiano' },
  { code: 'ru', native: 'Русский' },
];

export const Navbar: React.FC<NavbarProps> = ({
  onOpenAiAgent,
  fontSize,
  onToggleFontSize,
}) => {
  const { i18n } = useTranslation();
  const [langDropdownOpen, setLangDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setLangDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentLang = supportedLanguagesList.find((l) => l.code === i18n.language.split('-')[0]) || supportedLanguagesList[0];

  const handleSelectLang = (code: string) => {
    i18n.changeLanguage(code);
    setLangDropdownOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          {/* Left: Atithi Brand Logo */}
          <div className="flex items-center">
            <LotusLogo className="h-8 w-auto" />
          </div>

          {/* Center: Navigation Links */}
          <nav className="hidden xl:flex items-center space-x-7 text-[13px] font-medium text-slate-700">
            <a href="#categories" className="hover:text-slate-900 transition-colors">
              Visa categories
            </a>
            <a href="#requirements" className="hover:text-slate-900 transition-colors">
              Requirements
            </a>
            <a href="#how-it-works" className="hover:text-slate-900 transition-colors">
              How it works
            </a>
            <a href="#existing" className="hover:text-slate-900 transition-colors">
              Existing applications
            </a>
            <a href="#notices" className="hover:text-slate-900 transition-colors">
              Notices
            </a>
            <a href="#help" className="hover:text-slate-900 transition-colors">
              Help
            </a>

            {/* Connect AI agent button */}
            <button
              type="button"
              onClick={onOpenAiAgent}
              className="inline-flex items-center space-x-1.5 text-slate-800 hover:text-brand-600 transition-colors font-medium pl-1"
            >
              <Sparkles className="w-3.5 h-3.5 text-blue-600" />
              <span>Connect AI agent</span>
            </button>
          </nav>

          {/* Right: Actions */}
          <div className="flex items-center space-x-3 sm:space-x-4">
            {/* Font Size Accessibility Toggle */}
            <button
              type="button"
              onClick={onToggleFontSize}
              title="Toggle font size accessibility"
              className={`w-9 h-9 rounded-full border border-slate-200 flex items-center justify-center text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors ${
                fontSize === 'large' ? 'bg-slate-100 ring-2 ring-brand-500' : ''
              }`}
            >
              Aa
            </button>

            {/* Language Switcher Dropdown (Matches Screenshot 2) */}
            <div className="relative" ref={dropdownRef}>
              <button
                type="button"
                onClick={() => setLangDropdownOpen(!langDropdownOpen)}
                className="flex items-center space-x-1.5 text-xs font-medium text-slate-700 hover:text-slate-900 py-1.5 px-2 rounded-lg hover:bg-slate-100 transition-colors"
                aria-expanded={langDropdownOpen}
              >
                <Globe className="w-4 h-4 text-slate-500" />
                <span>{currentLang.native}</span>
                <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
              </button>

              {/* Exact dropdown panel from Screenshot 2 */}
              {langDropdownOpen && (
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-2xl shadow-2xl border border-slate-200/90 py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-150 max-h-72 overflow-y-auto">
                  <div className="px-1 space-y-0.5">
                    {supportedLanguagesList.map((lang) => {
                      const isSelected = lang.code === currentLang.code;
                      return (
                        <button
                          key={lang.code}
                          type="button"
                          onClick={() => handleSelectLang(lang.code)}
                          className={`w-full text-left px-3.5 py-2 text-xs rounded-xl font-medium transition-colors ${
                            isSelected
                              ? 'bg-blue-50/80 text-brand-700 font-semibold'
                              : 'text-slate-700 hover:bg-slate-100/80 hover:text-slate-900'
                          }`}
                        >
                          {lang.native}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            {/* Pill button: E-VISA > */}
            <a
              href="#apply"
              className="inline-flex items-center px-4 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-800 hover:bg-slate-50 transition-all shadow-sm"
            >
              <span>E-VISA</span>
              <span className="ml-2 text-slate-400 text-[10px]">&gt;</span>
            </a>

            {/* Pill button: OCI > */}
            <a
              href="#oci"
              className="inline-flex items-center px-4 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-800 hover:bg-slate-50 transition-all shadow-sm"
            >
              <span>OCI</span>
              <span className="ml-2 text-slate-400 text-[10px]">&gt;</span>
            </a>
          </div>
        </div>
      </div>
    </header>
  );
};
