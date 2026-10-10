import React, { useState, useRef, useEffect } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Globe, ChevronDown, Sparkles, Menu, X, ShieldCheck } from 'lucide-react';
import { LotusLogo } from './LotusLogo';

interface NavbarProps {
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
  fontSize,
  onToggleFontSize,
}) => {
  const { i18n } = useTranslation();
  const location = useLocation();

  const [langDropdownOpen, setLangDropdownOpen] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  const dropdownRef = useRef<HTMLDivElement>(null);
  const drawerRef = useRef<HTMLDivElement>(null);

  // Close mobile drawer on route transition
  useEffect(() => {
    setMobileDrawerOpen(false);
  }, [location.pathname]);

  // Handle ESC key and scroll lock for mobile drawer
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMobileDrawerOpen(false);
        setLangDropdownOpen(false);
      }
    }

    if (mobileDrawerOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    } else {
      document.body.style.overflow = '';
    }

    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [mobileDrawerOpen]);

  // Close language dropdown on outside click
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

  const navLinks = [
    { label: 'Visa categories', to: '/visa-categories' },
    { label: 'Requirements', to: '/requirements' },
    { label: 'How it works', to: '/how-it-works' },
    { label: 'Existing applications', to: '/existing-applications' },
    { label: 'Notices', to: '/notices' },
    { label: 'Help', to: '/help' },
  ];

  return (
    <>
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200">
        <div className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-20">
            {/* Left: Brand Logo linking to Home */}
            <div className="flex items-center space-x-3">
              <Link to="/" className="flex items-center focus:outline-none focus:ring-2 focus:ring-brand-500 rounded-lg">
                <LotusLogo className="h-8 w-auto" />
              </Link>
            </div>

            {/* Center: Desktop Navigation Links (>= 1024px) */}
            <nav className="hidden lg:flex items-center space-x-6 xl:space-x-7 text-[13px] font-medium text-slate-700">
              {navLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) =>
                    `transition-colors py-1 ${
                      isActive
                        ? 'text-brand-600 font-semibold border-b-2 border-brand-600'
                        : 'hover:text-slate-900 text-slate-700'
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              ))}

              {/* Connect AI agent link */}
              <NavLink
                to="/connect-ai-agent"
                className={({ isActive }) =>
                  `inline-flex items-center space-x-1.5 transition-colors font-medium pl-1 ${
                    isActive ? 'text-brand-600 font-bold' : 'text-slate-800 hover:text-brand-600'
                  }`
                }
              >
                <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                <span>Connect AI agent</span>
              </NavLink>
            </nav>

            {/* Right: Actions, Language Switcher, Aa and Hamburger */}
            <div className="flex items-center space-x-2 sm:space-x-3.5">
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

              {/* Language Switcher Dropdown */}
              <div className="relative" ref={dropdownRef}>
                <button
                  type="button"
                  onClick={() => setLangDropdownOpen(!langDropdownOpen)}
                  className="flex items-center space-x-1 text-xs font-medium text-slate-700 hover:text-slate-900 py-1.5 px-2 rounded-lg hover:bg-slate-100 transition-colors"
                  aria-expanded={langDropdownOpen}
                  aria-label="Select language"
                >
                  <Globe className="w-4 h-4 text-slate-500" />
                  <span className="hidden sm:inline">{currentLang.native}</span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                </button>

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

              {/* Quick Action: Apply Now */}
              <Link
                to="/apply"
                className="hidden md:inline-flex items-center px-4 py-2 rounded-xl bg-teal-700 hover:bg-teal-800 text-white text-xs font-bold shadow-md shadow-teal-700/20 transition-all"
              >
                <span>Apply Now</span>
                <span className="ml-1 text-[10px]">&gt;</span>
              </Link>

              {/* Quick Action: E-VISA > */}
              <Link
                to="/visa-categories"
                className="hidden sm:inline-flex items-center px-3.5 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-800 hover:bg-slate-50 transition-all shadow-sm"
              >
                <span>E-VISA</span>
                <span className="ml-1.5 text-slate-400 text-[10px]">&gt;</span>
              </Link>

              {/* Quick Action: OCI > */}
              <Link
                to="/visa-categories#oci"
                className="hidden sm:inline-flex items-center px-3.5 py-2 rounded-xl border border-slate-300 text-xs font-semibold text-slate-800 hover:bg-slate-50 transition-all shadow-sm"
              >
                <span>OCI</span>
                <span className="ml-1.5 text-slate-400 text-[10px]">&gt;</span>
              </Link>

              {/* Mobile Hamburger Button (< 1024px) */}
              <button
                type="button"
                onClick={() => setMobileDrawerOpen(true)}
                className="lg:hidden p-2 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100 transition-colors focus:outline-none focus:ring-2 focus:ring-brand-500"
                aria-label="Open mobile navigation menu"
                aria-expanded={mobileDrawerOpen}
              >
                <Menu className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Accessible Mobile Slide-Over Drawer (< 1024px) */}
      {mobileDrawerOpen && (
        <div
          className="fixed inset-0 z-50 lg:hidden overflow-hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Mobile Navigation"
        >
          {/* Backdrop overlay */}
          <div
            className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity"
            onClick={() => setMobileDrawerOpen(false)}
            aria-hidden="true"
          />

          {/* Slide-over panel */}
          <div
            ref={drawerRef}
            className="fixed inset-y-0 right-0 max-w-xs w-full bg-white shadow-2xl flex flex-col z-50 animate-in slide-in-from-right duration-250"
          >
            {/* Drawer Header */}
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <LotusLogo className="h-7 w-auto" />
              <button
                type="button"
                onClick={() => setMobileDrawerOpen(false)}
                className="p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 focus:outline-none"
                aria-label="Close navigation"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Drawer Navigation Links */}
            <div className="flex-1 overflow-y-auto px-5 py-6 space-y-2">
              {navLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  onClick={() => setMobileDrawerOpen(false)}
                  className={({ isActive }) =>
                    `block px-4 py-3 rounded-xl text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-blue-50 text-brand-700 font-semibold'
                        : 'text-slate-700 hover:bg-slate-50'
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              ))}

              <NavLink
                to="/connect-ai-agent"
                onClick={() => setMobileDrawerOpen(false)}
                className={({ isActive }) =>
                  `flex items-center space-x-2 px-4 py-3 rounded-xl text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-blue-50 text-brand-700 font-semibold'
                      : 'text-slate-700 hover:bg-slate-50'
                  }`
                }
              >
                <Sparkles className="w-4 h-4 text-blue-600" />
                <span>Connect AI agent</span>
              </NavLink>

              <div className="pt-4 mt-4 border-t border-slate-100 space-y-3">
                <Link
                  to="/apply"
                  onClick={() => setMobileDrawerOpen(false)}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-teal-700 hover:bg-teal-800 text-white text-xs font-bold shadow-md"
                >
                  <span>Start New Application</span>
                  <span>&gt;</span>
                </Link>

                <Link
                  to="/visa-categories"
                  onClick={() => setMobileDrawerOpen(false)}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-slate-200 text-xs font-bold text-slate-800"
                >
                  <span>Explore e-Visa Categories</span>
                  <span>&gt;</span>
                </Link>

                <Link
                  to="/visa-categories#oci"
                  onClick={() => setMobileDrawerOpen(false)}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-slate-200 text-xs font-bold text-slate-800"
                >
                  <span>OCI Card Services</span>
                  <span>&gt;</span>
                </Link>
              </div>

              {/* Language Selector inside Drawer */}
              <div className="pt-4 border-t border-slate-100">
                <span className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                  Select Language
                </span>
                <div className="grid grid-cols-2 gap-1.5">
                  {supportedLanguagesList.map((lang) => (
                    <button
                      key={lang.code}
                      type="button"
                      onClick={() => handleSelectLang(lang.code)}
                      className={`text-left px-2.5 py-1.5 rounded-lg text-xs ${
                        lang.code === currentLang.code
                          ? 'bg-brand-600 text-white font-semibold'
                          : 'text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      {lang.native}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Drawer Footer */}
            <div className="p-4 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
              <span className="flex items-center space-x-1">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                <span>256-bit TLS Protected</span>
              </span>
              <span>VisaSetu Concept</span>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
