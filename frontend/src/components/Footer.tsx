import React from 'react';
import { useTranslation } from 'react-i18next';
import { Shield, Sparkles, AlertCircle } from 'lucide-react';

export const Footer: React.FC = () => {
  const { t } = useTranslation();

  return (
    <footer className="bg-slate-900 text-slate-400 mt-20 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 pb-10 border-b border-slate-800">
          <div>
            <div className="flex items-center space-x-2 text-white font-bold text-lg mb-3">
              <Sparkles className="w-5 h-5 text-brand-400" />
              <span>{t('nav.brand')}</span>
            </div>
            <p className="text-sm text-slate-400 leading-relaxed mb-4">
              {t('footer.purpose')}
            </p>
            <div className="inline-flex items-center space-x-2 px-3 py-1.5 rounded-md bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span className="font-semibold">{t('footer.disclaimer')}</span>
            </div>
          </div>

          <div>
            <h4 className="text-white font-semibold text-sm uppercase tracking-wider mb-4">
              Key Innovations
            </h4>
            <ul className="space-y-2 text-sm">
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>Zero Form Data Loss (Autosave + IndexedDB)</span>
              </li>
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>1 Profile auto-fills e-Visa, Arrival & Suvidha</span>
              </li>
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>Idempotent Razorpay Webhooks & Auto-Reconcile</span>
              </li>
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                <span>Multilingual & WCAG 2.2 AA Accessible</span>
              </li>
            </ul>
          </div>

          <div>
            <h4 className="text-white font-semibold text-sm uppercase tracking-wider mb-4">
              Technical Architecture
            </h4>
            <div className="space-y-2 text-xs text-slate-400">
              <p>• Backend: Spring Boot 3.3 (Java 21), Flyway, PostgreSQL, MongoDB, Redis, RabbitMQ</p>
              <p>• Frontend: React + Vite + TypeScript, Tailwind CSS, TanStack Query, i18next</p>
              <p>• Security: Column-level encryption, TLS, HMAC Webhooks, zero PII logging</p>
            </div>
          </div>
        </div>

        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 space-y-4 sm:space-y-0">
          <div className="flex items-center space-x-2">
            <Shield className="w-4 h-4 text-emerald-500" />
            <span>&copy; {new Date().getFullYear()} VisaSetu Engineering. {t('footer.disclaimer')}</span>
          </div>
          <div className="flex space-x-6">
            <a href="#privacy" className="hover:text-slate-300 transition-colors">{t('footer.privacy')}</a>
            <a href="#terms" className="hover:text-slate-300 transition-colors">{t('footer.terms')}</a>
            <a href="#security" className="hover:text-slate-300 transition-colors">{t('footer.security')}</a>
          </div>
        </div>
      </div>
    </footer>
  );
};
