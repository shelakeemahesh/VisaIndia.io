import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Compass,
  Calendar,
  Clock,
  MapPin,
  ExternalLink,
  AlertTriangle,
  ArrowRight,
  CheckCircle,
  HelpCircle,
  Sparkles,
} from 'lucide-react';
import {
  checkEligibility,
  fetchNationalities,
  fetchPurposes,
  NationalityOption,
  PurposeOption,
  EligibilityResponse,
  RouteDto,
} from '../api/eligibility';

interface EligibilityCheckerProps {
  onSelectRoute?: (route: RouteDto, nationality: string, purpose: string) => void;
}

export const EligibilityChecker: React.FC<EligibilityCheckerProps> = ({ onSelectRoute }) => {
  const { t } = useTranslation();

  const [nationalities, setNationalities] = useState<NationalityOption[]>([]);
  const [purposes, setPurposes] = useState<PurposeOption[]>([]);
  const [selectedNationality, setSelectedNationality] = useState<string>('JPN'); // default Japan to showcase acceptance criteria
  const [selectedPurpose, setSelectedPurpose] = useState<string>('TOURISM');
  const [arrivalDate, setArrivalDate] = useState<string>('');

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<EligibilityResponse | null>(null);

  // Load initial dropdown options
  useEffect(() => {
    async function loadOptions() {
      try {
        const [natData, purpData] = await Promise.all([
          fetchNationalities(),
          fetchPurposes(),
        ]);
        setNationalities(natData);
        setPurposes(purpData);
      } catch (err) {
        // Fallback static list if backend hasn't booted yet
        setNationalities([
          { code: 'JPN', name: 'Japan', flagEmoji: '🇯🇵' },
          { code: 'USA', name: 'United States', flagEmoji: '🇺🇸' },
          { code: 'GBR', name: 'United Kingdom', flagEmoji: '🇬🇧' },
          { code: 'DEU', name: 'Germany', flagEmoji: '🇩🇪' },
          { code: 'FRA', name: 'France', flagEmoji: '🇫🇷' },
          { code: 'KOR', name: 'South Korea', flagEmoji: '🇰🇷' },
          { code: 'NPL', name: 'Nepal', flagEmoji: '🇳🇵' },
        ]);
        setPurposes([
          { code: 'TOURISM', displayName: 'Tourism & Recreation' },
          { code: 'BUSINESS', displayName: 'Business & Commerce' },
          { code: 'MEDICAL', displayName: 'Medical Treatment' },
          { code: 'CONFERENCE', displayName: 'International Conference' },
          { code: 'TRANSIT', displayName: 'Transit' },
        ]);
      }
    }
    loadOptions();
  }, []);

  // Fetch eligibility
  const handleCheck = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!selectedNationality || !selectedPurpose) return;

    setLoading(true);
    setError(null);
    try {
      const data = await checkEligibility(selectedNationality, selectedPurpose, arrivalDate || undefined);
      setResult(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Unable to check visa rules at this moment. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Run automatically on mount for default Japan + Tourism acceptance showcase
  useEffect(() => {
    handleCheck();
  }, []);

  return (
    <section id="eligibility" className="py-12 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      {/* Card Header */}
      <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200 overflow-hidden mb-12">
        <div className="bg-gradient-to-r from-brand-900 via-brand-800 to-brand-700 px-6 py-8 text-white">
          <div className="flex items-center space-x-3 mb-2">
            <Compass className="w-6 h-6 text-brand-300" />
            <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">
              {t('eligibility.heading')}
            </h2>
          </div>
          <p className="text-brand-100 text-sm sm:text-base max-w-2xl">
            {t('eligibility.subheading')}
          </p>
        </div>

        {/* Search Inputs Bar */}
        <form onSubmit={handleCheck} className="p-6 sm:p-8 bg-slate-50/50 border-b border-slate-200">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            {/* Nationality Combobox */}
            <div>
              <label htmlFor="nationality-select" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                {t('eligibility.nationalityLabel')}
              </label>
              <div className="relative">
                <select
                  id="nationality-select"
                  aria-label={t('eligibility.nationalityLabel')}
                  value={selectedNationality}
                  onChange={(e) => setSelectedNationality(e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-4 py-3 text-sm text-slate-800 font-medium focus:ring-2 focus:ring-brand-500 focus:border-brand-500 shadow-sm"
                >
                  <option value="">{t('eligibility.nationalityPlaceholder')}</option>
                  {nationalities.map((nat) => (
                    <option key={nat.code} value={nat.code}>
                      {nat.flagEmoji} {nat.name} ({nat.code})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Travel Purpose */}
            <div>
              <label htmlFor="purpose-select" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                {t('eligibility.purposeLabel')}
              </label>
              <select
                id="purpose-select"
                aria-label={t('eligibility.purposeLabel')}
                value={selectedPurpose}
                onChange={(e) => setSelectedPurpose(e.target.value)}
                className="w-full bg-white border border-slate-300 rounded-xl px-4 py-3 text-sm text-slate-800 font-medium focus:ring-2 focus:ring-brand-500 focus:border-brand-500 shadow-sm"
              >
                {purposes.map((purp) => (
                  <option key={purp.code} value={purp.code}>
                    {purp.displayName}
                  </option>
                ))}
              </select>
            </div>

            {/* Arrival Date (Optional) */}
            <div>
              <label htmlFor="arrival-date" className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2">
                {t('eligibility.arrivalDateLabel')}
              </label>
              <div className="relative">
                <input
                  id="arrival-date"
                  type="date"
                  value={arrivalDate}
                  min={new Date().toISOString().split('T')[0]}
                  onChange={(e) => setArrivalDate(e.target.value)}
                  className="w-full bg-white border border-slate-300 rounded-xl px-4 py-3 text-sm text-slate-800 font-medium focus:ring-2 focus:ring-brand-500 focus:border-brand-500 shadow-sm"
                />
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-xs text-slate-500 flex items-center space-x-1.5">
              <Sparkles className="w-4 h-4 text-amber-500 flex-shrink-0" />
              <span>{t('eligibility.sampleDisclaimer')}</span>
            </div>

            <button
              type="submit"
              disabled={loading || !selectedNationality || !selectedPurpose}
              className="w-full sm:w-auto px-8 py-3 bg-brand-600 hover:bg-brand-700 disabled:bg-slate-300 text-white font-medium text-sm rounded-xl shadow-md shadow-brand-600/20 transition-all flex items-center justify-center space-x-2"
            >
              <span>{loading ? t('eligibility.searching') : t('eligibility.checkButton')}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </form>

        {/* Results Body */}
        <div className="p-6 sm:p-8">
          {error && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-start space-x-3">
              <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {result && (
            <div>
              {/* Results Overview Bar */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 gap-2">
                <div>
                  <h3 className="text-xl font-bold text-slate-900">
                    {t('eligibility.resultsHeading')} <span className="text-brand-600">{result.nationalityName}</span>
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Purpose: <span className="font-semibold text-slate-700">{result.purpose}</span>
                    {result.arrivalDate && (
                      <span className="ml-3">
                        Arrival: <span className="font-semibold text-slate-700">{result.arrivalDate}</span>
                      </span>
                    )}
                  </p>
                </div>
                <div className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-brand-50 text-brand-700 border border-brand-200 self-start sm:self-auto">
                  {t('eligibility.routesFound', { count: result.routesFound })}
                </div>
              </div>

              {/* If no route found -> Embassy Fallback */}
              {result.routesFound === 0 && (
                <div className="p-8 rounded-2xl bg-amber-50/70 border border-amber-200 text-center max-w-2xl mx-auto my-6">
                  <div className="w-12 h-12 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center mx-auto mb-4">
                    <HelpCircle className="w-6 h-6" />
                  </div>
                  <h4 className="text-lg font-bold text-amber-900 mb-2">
                    {t('eligibility.fallbackTitle')}
                  </h4>
                  <p className="text-sm text-amber-800 leading-relaxed mb-6">
                    {result.fallbackMessage}
                  </p>
                  <a
                    href="https://www.mea.gov.in/indian-missions-abroad-new.htm"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center space-x-2 px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-medium text-xs shadow-sm transition-all"
                  >
                    <span>{t('eligibility.fallbackContact')}</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              )}

              {/* Cards Grid for Applicable Routes */}
              {result.routesFound > 0 && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6" data-testid="routes-grid">
                  {result.routes.map((route) => (
                    <div
                      key={route.id}
                      className={`relative rounded-2xl p-6 transition-all border flex flex-col justify-between ${
                        route.isRecommended
                          ? 'bg-gradient-to-b from-brand-50/40 via-white to-white border-brand-300 shadow-md ring-1 ring-brand-400/30'
                          : 'bg-white border-slate-200 hover:border-slate-300 shadow-sm'
                      }`}
                      data-testid={`route-card-${route.routeType.toLowerCase()}`}
                    >
                      {/* Card Badges */}
                      <div>
                        <div className="flex items-start justify-between gap-2 mb-3">
                          <div className="flex items-center space-x-2">
                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                              route.routeType === 'VISA_ON_ARRIVAL'
                                ? 'bg-purple-100 text-purple-800 border border-purple-200'
                                : route.routeType === 'E_VISA'
                                ? 'bg-blue-100 text-blue-800 border border-blue-200'
                                : route.routeType === 'VISA_FREE'
                                ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                                : 'bg-slate-100 text-slate-800 border border-slate-200'
                            }`}>
                              {route.routeTypeName}
                            </span>
                            {route.isRecommended && (
                              <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-300">
                                <Sparkles className="w-3 h-3 text-amber-600" />
                                <span>{t('eligibility.recommended')}</span>
                              </span>
                            )}
                          </div>

                          {/* Fee Tag */}
                          <div className="text-right">
                            <div className="text-2xl font-black text-slate-900 tracking-tight">
                              {route.fee === 0 ? 'FREE' : `$${route.fee}`}
                            </div>
                            <span className="text-[10px] uppercase font-bold text-slate-400">
                              {route.currency} {t('eligibility.fee')}
                            </span>
                          </div>
                        </div>

                        {/* Route Title & Description */}
                        <h4 className="text-lg font-bold text-slate-900 mb-1">
                          {route.title}
                        </h4>
                        <p className="text-xs text-slate-600 mb-5 leading-relaxed">
                          {route.description}
                        </p>

                        {/* Route Specs Matrix */}
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 p-3.5 rounded-xl bg-slate-50 border border-slate-100 mb-5 text-xs">
                          <div>
                            <span className="text-slate-400 block font-medium">{t('eligibility.processingTime')}</span>
                            <span className="font-semibold text-slate-800 flex items-center space-x-1 mt-0.5">
                              <Clock className="w-3.5 h-3.5 text-brand-600 mr-1" />
                              {route.processingTimeText}
                            </span>
                          </div>
                          <div>
                            <span className="text-slate-400 block font-medium">{t('eligibility.validity')}</span>
                            <span className="font-semibold text-slate-800 mt-0.5 block">
                              {route.validityDays > 0 ? `${route.validityDays} Days` : 'N/A'}
                            </span>
                          </div>
                          <div>
                            <span className="text-slate-400 block font-medium">{t('eligibility.maxStay')}</span>
                            <span className="font-semibold text-slate-800 mt-0.5 block">
                              {route.maxStayDays > 0 ? `${route.maxStayDays} Days` : 'Unlimited'}
                            </span>
                          </div>
                          <div>
                            <span className="text-slate-400 block font-medium">{t('eligibility.entries')}</span>
                            <span className="font-semibold text-slate-800 mt-0.5 block">
                              {route.entries}
                            </span>
                          </div>
                          {route.applyByDate && (
                            <div className="col-span-2">
                              <span className="text-slate-400 block font-medium">{t('eligibility.applyBy')}</span>
                              <span className="font-semibold text-rose-700 flex items-center space-x-1 mt-0.5">
                                <Calendar className="w-3.5 h-3.5 text-rose-600 mr-1" />
                                {route.applyByDate}
                              </span>
                            </div>
                          )}
                        </div>

                        {/* Document Checklist */}
                        {route.documents && route.documents.length > 0 && (
                          <div className="mb-4">
                            <span className="text-xs font-semibold text-slate-700 block mb-2">
                              {t('eligibility.documents')}
                            </span>
                            <ul className="space-y-1.5 text-xs text-slate-600">
                              {route.documents.map((doc, idx) => (
                                <li key={idx} className="flex items-start space-x-2">
                                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0 mt-0.5" />
                                  <span>{doc}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* Allowed Ports Checklist */}
                        {route.allowedPorts && route.allowedPorts.length > 0 && (
                          <div className="mb-5">
                            <span className="text-xs font-semibold text-slate-700 block mb-1.5">
                              {t('eligibility.allowedPorts')}
                            </span>
                            <div className="flex flex-wrap gap-1.5">
                              {route.allowedPorts.map((port, idx) => (
                                <span
                                  key={idx}
                                  className="inline-flex items-center px-2 py-0.5 rounded text-[11px] bg-slate-100 text-slate-700 border border-slate-200"
                                >
                                  <MapPin className="w-3 h-3 text-slate-400 mr-1" />
                                  {port}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      {/* Card Action & Verification Link */}
                      <div className="pt-4 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3">
                        <a
                          href={route.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-xs font-medium text-slate-500 hover:text-brand-600 flex items-center space-x-1"
                        >
                          <span>{t('eligibility.officialSource')}</span>
                          <ExternalLink className="w-3 h-3" />
                        </a>

                        <button
                          type="button"
                          onClick={() => onSelectRoute?.(route, selectedNationality, selectedPurpose)}
                          className={`w-full sm:w-auto px-5 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-center space-x-2 ${
                            route.isRecommended
                              ? 'bg-brand-600 hover:bg-brand-700 text-white shadow-md shadow-brand-600/20'
                              : 'bg-slate-900 hover:bg-slate-800 text-white'
                          }`}
                        >
                          <span>{t('eligibility.startApplication')}</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
};
