import React from 'react';
import { useTranslation } from 'react-i18next';
import { ArrowRight, Sparkles, Clock, ShieldCheck, Layers } from 'lucide-react';

export const Hero: React.FC = () => {
  const { t } = useTranslation();

  return (
    <section className="relative overflow-hidden pt-12 pb-10 bg-gradient-to-b from-brand-50/60 via-slate-50 to-slate-50 border-b border-slate-200/60">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto">
          {/* Badge */}
          <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-full bg-brand-100 text-brand-800 text-xs font-semibold uppercase tracking-wider mb-6 shadow-sm">
            <Sparkles className="w-3.5 h-3.5 text-brand-600" />
            <span>{t('hero.badge')}</span>
          </div>

          {/* Main Title */}
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-slate-900 tracking-tight leading-tight sm:leading-none mb-6">
            {t('hero.title')}
          </h1>

          {/* Subtitle */}
          <p className="text-lg sm:text-xl text-slate-600 leading-relaxed mb-8">
            {t('hero.subtitle')}
          </p>

          {/* Value Props Pills */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 max-w-2xl mx-auto mb-8 text-left">
            <div className="flex items-center space-x-2.5 p-3 rounded-xl bg-white border border-slate-200/80 shadow-sm">
              <Layers className="w-5 h-5 text-brand-600 flex-shrink-0" />
              <div className="text-xs">
                <span className="font-semibold text-slate-900 block">1 Unified Profile</span>
                <span className="text-slate-500">e-Visa + Arrival + Health</span>
              </div>
            </div>

            <div className="flex items-center space-x-2.5 p-3 rounded-xl bg-white border border-slate-200/80 shadow-sm">
              <ShieldCheck className="w-5 h-5 text-emerald-600 flex-shrink-0" />
              <div className="text-xs">
                <span className="font-semibold text-slate-900 block">Never Lose Progress</span>
                <span className="text-slate-500">Autosave & Offline Sync</span>
              </div>
            </div>

            <div className="flex items-center space-x-2.5 p-3 rounded-xl bg-white border border-slate-200/80 shadow-sm">
              <Clock className="w-5 h-5 text-indigo-600 flex-shrink-0" />
              <div className="text-xs">
                <span className="font-semibold text-slate-900 block">Clear Timelines</span>
                <span className="text-slate-500">Instant VoA to 3-day e-Visa</span>
              </div>
            </div>
          </div>

          {/* Quick CTA anchor */}
          <div className="flex flex-col sm:flex-row items-center justify-center space-y-3 sm:space-y-0 sm:space-x-4">
            <a
              href="#eligibility"
              className="w-full sm:w-auto inline-flex items-center justify-center px-6 py-3.5 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-medium text-sm shadow-md shadow-brand-600/25 transition-all"
            >
              <span>{t('hero.ctaCheck')}</span>
              <ArrowRight className="w-4 h-4 ml-2" />
            </a>
            <a
              href="#apply"
              className="w-full sm:w-auto inline-flex items-center justify-center px-6 py-3.5 rounded-xl bg-white hover:bg-slate-50 text-slate-700 font-medium text-sm border border-slate-300 shadow-sm transition-all"
            >
              <span>{t('hero.ctaApply')}</span>
            </a>
          </div>
        </div>
      </div>
    </section>
  );
};
