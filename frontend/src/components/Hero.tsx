import React, { useState, useRef, useEffect } from 'react';
import { Globe, Search, ChevronDown, ChevronRight, Check, CheckCircle2 } from 'lucide-react';
import { NationalityOption, PurposeOption, RouteDto } from '../api/eligibility';

interface HeroProps {
  nationalities: NationalityOption[];
  purposes: PurposeOption[];
  selectedNationality: string;
  selectedPurpose: string;
  onNationalityChange: (nat: string) => void;
  onPurposeChange: (purp: string) => void;
  onValidityChange?: (val: string) => void;
  onStartApplication: () => void;
  onOpenAiAgent: () => void;
  applicableRoutes: RouteDto[];
}

export const Hero: React.FC<HeroProps> = ({
  nationalities,
  purposes,
  selectedNationality,
  selectedPurpose,
  onNationalityChange,
  onPurposeChange,
  onStartApplication,
  onOpenAiAgent,
  applicableRoutes,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [selectedValidity, setSelectedValidity] = useState('');
  const searchContainerRef = useRef<HTMLDivElement>(null);

  // Close nationality dropdown on click outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (searchContainerRef.current && !searchContainerRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Filter nationalities by search query
  const filteredNationalities = nationalities.filter(
    (n) =>
      n.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      n.code.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const selectedCountryObj = nationalities.find((n) => n.code === selectedNationality);

  // Derive available validities from applicable routes or purpose
  const validityOptions = applicableRoutes.length > 0
    ? applicableRoutes.map((r) => {
        if (r.routeType === 'VISA_ON_ARRIVAL') return '60 Days (Visa on Arrival)';
        if (r.validityDays === 30) return '30 Days (Double Entry)';
        if (r.validityDays === 365) return '1 Year (Multiple Entry)';
        if (r.validityDays >= 1800) return '5 Years (Multiple Entry)';
        return `${r.validityDays} Days`;
      })
    : ['30 Days', '1 Year', '5 Years'];

  const handleSelectCountry = (code: string) => {
    onNationalityChange(code);
    const country = nationalities.find((n) => n.code === code);
    if (country) {
      setSearchQuery(country.name);
    }
    setDropdownOpen(false);
  };

  const isFormComplete = Boolean(selectedNationality && selectedPurpose);

  return (
    <section className="relative min-h-[640px] lg:min-h-[720px] flex items-center justify-center overflow-hidden">
      {/* Exact Atmospheric Sunset Background */}
      <div className="absolute inset-0 z-0">
        <img
          src="https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=2200&q=85"
          alt="Historic Indian Mughal architecture at sunset"
          className="w-full h-full object-cover object-center filter brightness-[0.78] contrast-[1.05]"
        />
        {/* Warm twilight gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950/75 via-slate-950/40 to-slate-950/20 pointer-events-none" />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/60 via-transparent to-slate-950/30 pointer-events-none" />
      </div>

      {/* Main Hero Container */}
      <div className="relative z-10 max-w-[1400px] w-full mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-24">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: Heading and Subtitle */}
          <div className="lg:col-span-7 text-white">
            <div className="flex items-center space-x-2 text-[11px] sm:text-xs font-bold tracking-[0.22em] text-amber-300 uppercase mb-5">
              <span>BUREAU OF IMMIGRATION, MINISTRY OF HOME AFFAIRS</span>
              <span className="px-1.5 py-0.5 rounded text-[9px] bg-amber-400/20 text-amber-200 border border-amber-400/30">
                CONCEPT DEMO
              </span>
            </div>

            <h1 className="font-serif text-5xl sm:text-6xl xl:text-7xl font-normal tracking-tight text-white leading-[1.08] mb-6">
              Apply for an<br />
              <span className="italic">Indian e-Visa</span> online.
            </h1>

            <p className="text-slate-200 text-base sm:text-lg max-w-xl font-normal leading-relaxed mb-8 text-shadow">
              Select your nationality and purpose of visit to check eligibility and the applicable fee before you begin the application.
            </p>

            {/* AI Agent Connection Link */}
            <button
              type="button"
              onClick={onOpenAiAgent}
              className="inline-flex items-center space-x-2 text-white/90 hover:text-amber-300 transition-colors text-sm font-medium group"
            >
              <span>Prefer to apply with AI? Connect your agent</span>
              <ChevronRight className="w-4 h-4 text-white/70 group-hover:translate-x-1 group-hover:text-amber-300 transition-all" />
            </button>
          </div>

          {/* Right Column: Floating White Form Card (Matches Screenshot 1 & 2) */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-100/90 w-full max-w-[460px] text-slate-800">
              <form onSubmit={(e) => { e.preventDefault(); onStartApplication(); }}>
                {/* NATIONALITY SEARCH INPUT */}
                <div className="mb-5 relative" ref={searchContainerRef}>
                  <label
                    htmlFor="nationality-input"
                    className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest mb-2"
                  >
                    NATIONALITY
                  </label>

                  <div className="relative">
                    <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
                      <Globe className="w-4 h-4" />
                    </div>

                    <input
                      id="nationality-input"
                      type="text"
                      placeholder="Type your nationality"
                      value={searchQuery || (selectedCountryObj ? selectedCountryObj.name : '')}
                      onFocus={() => {
                        setDropdownOpen(true);
                        setSearchQuery('');
                      }}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        setDropdownOpen(true);
                      }}
                      className="w-full pl-10 pr-10 py-3 bg-white border border-slate-200 rounded-xl text-sm font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-accent-500/20 focus:border-accent-500 shadow-sm"
                    />

                    <div className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-400">
                      <Search className="w-4 h-4" />
                    </div>
                  </div>

                  {/* Nationality Autocomplete Dropdown */}
                  {dropdownOpen && (
                    <div className="absolute left-0 right-0 mt-1 bg-white border border-slate-200 rounded-2xl shadow-xl z-50 max-h-60 overflow-y-auto py-1">
                      {filteredNationalities.length > 0 ? (
                        filteredNationalities.map((nat) => (
                          <button
                            key={nat.code}
                            type="button"
                            onClick={() => handleSelectCountry(nat.code)}
                            className="w-full text-left px-4 py-2.5 hover:bg-slate-50 text-xs font-medium text-slate-700 flex items-center justify-between transition-colors"
                          >
                            <span className="flex items-center space-x-2">
                              <span>{nat.flagEmoji}</span>
                              <span className="font-semibold">{nat.name}</span>
                              <span className="text-slate-400">({nat.code})</span>
                            </span>
                            {selectedNationality === nat.code && (
                              <Check className="w-4 h-4 text-accent-500" />
                            )}
                          </button>
                        ))
                      ) : (
                        <div className="px-4 py-3 text-xs text-slate-400">
                          No country found. Press escape to close.
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* ROW: PURPOSE OF VISIT & VISA VALIDITY */}
                <div className="grid grid-cols-2 gap-3.5 mb-5">
                  {/* Purpose of Visit */}
                  <div>
                    <label
                      htmlFor="purpose-select"
                      className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest mb-2"
                    >
                      PURPOSE OF VISIT
                    </label>
                    <div className="relative">
                      <select
                        id="purpose-select"
                        value={selectedPurpose}
                        onChange={(e) => onPurposeChange(e.target.value)}
                        className="w-full appearance-none bg-white border border-slate-200 rounded-xl px-3.5 py-3 pr-8 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-accent-500/20 focus:border-accent-500 shadow-sm"
                      >
                        <option value="">Select</option>
                        {purposes.map((p) => (
                          <option key={p.code} value={p.code}>
                            {p.displayName.split('&')[0].trim()}
                          </option>
                        ))}
                      </select>
                      <div className="absolute inset-y-0 right-0 pr-2.5 flex items-center pointer-events-none text-slate-400">
                        <ChevronDown className="w-3.5 h-3.5" />
                      </div>
                    </div>
                  </div>

                  {/* Visa Validity */}
                  <div>
                    <label
                      htmlFor="validity-select"
                      className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest mb-2"
                    >
                      VISA VALIDITY
                    </label>
                    <div className="relative">
                      <select
                        id="validity-select"
                        value={selectedValidity}
                        onChange={(e) => setSelectedValidity(e.target.value)}
                        className="w-full appearance-none bg-white border border-slate-200 rounded-xl px-3.5 py-3 pr-8 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-accent-500/20 focus:border-accent-500 shadow-sm"
                      >
                        <option value="">Select</option>
                        {validityOptions.map((v, i) => (
                          <option key={i} value={v}>
                            {v}
                          </option>
                        ))}
                      </select>
                      <div className="absolute inset-y-0 right-0 pr-2.5 flex items-center pointer-events-none text-slate-400">
                        <ChevronDown className="w-3.5 h-3.5" />
                      </div>
                    </div>
                  </div>
                </div>

                {/* DYNAMIC RESULT OR SUBTEXT */}
                <div className="min-h-[56px] flex items-center mb-6">
                  {isFormComplete && applicableRoutes.length > 0 ? (
                    <div className="w-full p-3 rounded-xl bg-amber-50/70 border border-amber-200/80 text-xs">
                      <div className="flex items-center justify-between text-slate-900 font-bold mb-1">
                        <span className="flex items-center space-x-1">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          <span>{applicableRoutes[0].title}</span>
                        </span>
                        <span className="text-accent-600 text-sm">
                          {applicableRoutes[0].fee === 0 ? 'FREE' : `$${applicableRoutes[0].fee} USD`}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-600">
                        Processing: <strong>{applicableRoutes[0].processingTimeText}</strong> ·{' '}
                        {applicableRoutes[0].entries} entry
                        {applicableRoutes.some((r) => r.routeType === 'VISA_ON_ARRIVAL') && (
                          <span className="text-purple-700 font-semibold block mt-0.5">
                            ★ Visa on Arrival (VoA) also available at 6 airports!
                          </span>
                        )}
                      </p>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 leading-relaxed">
                      Select a nationality and purpose of visit to view the visa route and applicable fee.
                    </p>
                  )}
                </div>

                {/* START APPLICATION BUTTON */}
                <button
                  type="submit"
                  disabled={!isFormComplete}
                  className={`w-full py-3.5 px-6 rounded-xl text-xs font-bold tracking-widest uppercase transition-all flex items-center justify-center space-x-1.5 ${
                    isFormComplete
                      ? 'border border-accent-500 bg-accent-500 text-white shadow-lg shadow-accent-500/25 hover:bg-accent-600'
                      : 'border border-accent-300 text-accent-400/80 bg-white hover:border-accent-400 cursor-not-allowed opacity-80'
                  }`}
                >
                  <span>START APPLICATION</span>
                  <span className="text-[10px]">&gt;</span>
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
