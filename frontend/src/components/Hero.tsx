import React, { useState, useEffect } from 'react';
import { ChevronDown, ChevronRight, CheckCircle2, MapPin } from 'lucide-react';
import { NationalityOption, PurposeOption, RouteDto } from '../api/eligibility';
import { SearchableCountrySelect } from './SearchableCountrySelect';
import { ALL_COUNTRIES } from '../data/countries';

export const INDIA_TOURIST_DESTINATIONS = [
  {
    id: 1,
    name: 'Taj Mahal',
    location: 'Agra, Uttar Pradesh',
    badge: '#01 Wonder of the World',
    image: 'https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 2,
    name: 'Varanasi Ghats',
    location: 'Varanasi, Uttar Pradesh',
    badge: '#02 Spiritual Capital',
    image: 'https://images.unsplash.com/photo-1561361513-2d000a50f0dc?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 3,
    name: 'Hawa Mahal & Amber Fort',
    location: 'Jaipur, Rajasthan',
    badge: '#03 Pink City Heritage',
    image: 'https://images.unsplash.com/photo-1603288967910-639a04a572a1?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 4,
    name: 'Kerala Backwaters',
    location: 'Alleppey, Kerala',
    badge: '#04 God’s Own Country',
    image: 'https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 5,
    name: 'Pangong Tso Lake',
    location: 'Ladakh, Himalayas',
    badge: '#05 High-Altitude Serenity',
    image: 'https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 6,
    name: 'Golden Temple (Harmandir Sahib)',
    location: 'Amritsar, Punjab',
    badge: '#06 Golden Sacred Shrine',
    image: 'https://images.unsplash.com/photo-1588096344356-9a2d80766436?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 7,
    name: 'Gateway of India',
    location: 'Mumbai, Maharashtra',
    badge: '#07 City of Dreams Icon',
    image: 'https://images.unsplash.com/photo-1570168007204-dfb528c6958f?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 8,
    name: 'Hampi Stone Chariot',
    location: 'Vijayanagara, Karnataka',
    badge: '#08 UNESCO World Heritage',
    image: 'https://images.unsplash.com/photo-1600100397608-f010f443b7e4?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 9,
    name: 'Goa Golden Coast',
    location: 'Palolem, South Goa',
    badge: '#09 Tropical Paradise',
    image: 'https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=2000&q=85',
  },
  {
    id: 10,
    name: 'Qutub Minar & Historic Delhi',
    location: 'New Delhi, NCR',
    badge: '#10 Ancient Monument',
    image: 'https://images.unsplash.com/photo-1585139097464-6df599c2b95b?auto=format&fit=crop&w=2000&q=85',
  },
];

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
  const [selectedValidity, setSelectedValidity] = useState('');
  const [activeDestIndex, setActiveDestIndex] = useState(0);

  // Auto-advance through the 10 Indian destinations (1 -> 2 -> 3 ... -> 10)
  useEffect(() => {
    const timer = setInterval(() => {
      setActiveDestIndex((prev) => (prev + 1) % INDIA_TOURIST_DESTINATIONS.length);
    }, 5500);
    return () => clearInterval(timer);
  }, []);

  const currentDest = INDIA_TOURIST_DESTINATIONS[activeDestIndex];
  const nextDest = INDIA_TOURIST_DESTINATIONS[(activeDestIndex + 1) % INDIA_TOURIST_DESTINATIONS.length];

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

  const isFormComplete = Boolean(selectedNationality && selectedPurpose);
  const countryOptions = nationalities && nationalities.length > 0 ? nationalities : ALL_COUNTRIES;

  return (
    <section className="relative min-h-[660px] lg:min-h-[740px] flex items-center justify-center overflow-hidden">
      {/* 10 INDIA TOURIST POINTS BACKGROUND CROSSFADE */}
      <div className="absolute inset-0 z-0">
        {INDIA_TOURIST_DESTINATIONS.map((dest, idx) => (
          <div
            key={dest.id}
            className={`absolute inset-0 transition-opacity duration-1000 ease-in-out ${
              idx === activeDestIndex ? 'opacity-100 z-10' : 'opacity-0 z-0'
            }`}
          >
            <img
              src={dest.image}
              alt={`${dest.name} - ${dest.location}`}
              className="w-full h-full object-cover object-center filter brightness-[0.72] contrast-[1.08] transform scale-105 transition-transform duration-[6000ms] ease-out"
            />
          </div>
        ))}

        {/* Ambient Darkened Gradient Overlays for Readability */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950/85 via-slate-950/50 to-slate-950/25 z-20 pointer-events-none" />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-transparent to-slate-950/40 z-20 pointer-events-none" />
      </div>

      {/* FLOATING DESTINATION CARDS & INDICATORS */}
      <div className="absolute bottom-6 left-4 sm:left-8 z-30 hidden sm:flex items-center gap-3">
        {/* Main Floating Active Destination Badge */}
        <div className="bg-slate-900/80 backdrop-blur-md border border-white/20 text-white px-4 py-2.5 rounded-2xl shadow-xl flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-amber-500/20 border border-amber-400/40 text-amber-400 flex items-center justify-center font-bold text-xs">
            {currentDest.id}
          </div>
          <div>
            <div className="flex items-center gap-1.5 text-[11px] font-bold text-amber-300">
              <MapPin className="w-3 h-3 text-amber-400" />
              <span>{currentDest.name}</span>
            </div>
            <span className="text-[10px] text-slate-300 block font-medium">
              {currentDest.location} · {currentDest.badge}
            </span>
          </div>
        </div>

        {/* 1 to 10 Quick Dots / Jump Navigation */}
        <div className="flex items-center gap-1 bg-slate-950/60 backdrop-blur-md border border-white/10 px-2 py-1.5 rounded-xl">
          {INDIA_TOURIST_DESTINATIONS.map((dest, i) => (
            <button
              key={dest.id}
              type="button"
              onClick={() => setActiveDestIndex(i)}
              title={`${dest.id}. ${dest.name}`}
              className={`w-6 h-6 rounded-lg text-[10px] font-bold transition-all ${
                i === activeDestIndex
                  ? 'bg-amber-400 text-slate-950 shadow-sm scale-110'
                  : 'text-slate-400 hover:text-white hover:bg-white/10'
              }`}
            >
              {dest.id}
            </button>
          ))}
        </div>

        {/* Next destination peek */}
        <button
          type="button"
          onClick={() => setActiveDestIndex((activeDestIndex + 1) % INDIA_TOURIST_DESTINATIONS.length)}
          className="bg-white/10 hover:bg-white/20 backdrop-blur-md border border-white/10 text-white/80 hover:text-white text-[11px] px-3 py-2 rounded-xl transition-all hidden md:flex items-center gap-1.5 cursor-pointer"
        >
          <span>Next: #{nextDest.id} {nextDest.name}</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Main Hero Container */}
      <div className="relative z-30 max-w-[1400px] w-full mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-24">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: Heading and Subtitle */}
          <div className="lg:col-span-7 text-white">
            <div className="flex items-center space-x-2 text-[11px] sm:text-xs font-bold tracking-[0.22em] text-amber-300 uppercase mb-5">
              <span>VISAINDIA.IO · POWERED BY VISASETU</span>
              <span className="px-1.5 py-0.5 rounded text-[9px] bg-amber-400/20 text-amber-200 border border-amber-400/30">
                195+ COUNTRIES
              </span>
            </div>

            <h1 className="font-serif text-5xl sm:text-6xl xl:text-7xl font-normal tracking-tight text-white leading-[1.08] mb-6 drop-shadow-md">
              Apply for an<br />
              <span className="italic font-medium text-amber-100">Indian e-Visa</span> online.
            </h1>

            <p className="text-slate-200 text-base sm:text-lg max-w-xl font-normal leading-relaxed mb-8 drop-shadow">
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

          {/* Right Column: Floating White Form Card */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-100/90 w-full max-w-[460px] text-slate-800">
              <form onSubmit={(e) => { e.preventDefault(); onStartApplication(); }}>
                {/* NATIONALITY SEARCH BAR WITH ALL 195+ COUNTRIES */}
                <div className="mb-5">
                  <label
                    htmlFor="nationality-input"
                    className="block text-[11px] font-bold text-slate-500 uppercase tracking-widest mb-2"
                  >
                    PASSPORT NATIONALITY (195+ COUNTRIES)
                  </label>

                  <SearchableCountrySelect
                    id="nationality-input"
                    value={selectedNationality}
                    onChange={onNationalityChange}
                    options={countryOptions}
                    placeholder="Search by country name or code..."
                  />
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
                        className="w-full appearance-none bg-white border border-slate-200 rounded-xl px-3.5 py-3 pr-8 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 shadow-sm"
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
                        className="w-full appearance-none bg-white border border-slate-200 rounded-xl px-3.5 py-3 pr-8 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 shadow-sm"
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
                    <div className="w-full p-3 rounded-xl bg-teal-50/70 border border-teal-200/80 text-xs">
                      <div className="flex items-center justify-between text-slate-900 font-bold mb-1">
                        <span className="flex items-center space-x-1">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          <span>{applicableRoutes[0].title}</span>
                        </span>
                        <span className="text-teal-700 font-mono font-bold text-sm">
                          {applicableRoutes[0].fee === 0 ? 'FREE' : `$${applicableRoutes[0].fee} USD`}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-600">
                        Processing: <strong>{applicableRoutes[0].processingTimeText}</strong> ·{' '}
                        {applicableRoutes[0].entries} entry
                        {applicableRoutes.some((r) => r.routeType === 'VISA_ON_ARRIVAL') && (
                          <span className="text-purple-700 font-semibold block mt-0.5">
                            ★ Visa on Arrival (VoA) also available at 6 designated airports!
                          </span>
                        )}
                      </p>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 leading-relaxed">
                      Select your nationality and purpose of visit to view the official visa route and applicable fee.
                    </p>
                  )}
                </div>

                {/* START APPLICATION BUTTON */}
                <button
                  type="submit"
                  disabled={!isFormComplete}
                  className={`w-full py-3.5 px-6 rounded-xl text-xs font-bold tracking-widest uppercase transition-all flex items-center justify-center space-x-1.5 cursor-pointer ${
                    isFormComplete
                      ? 'border border-teal-700 bg-teal-700 text-white shadow-lg shadow-teal-700/25 hover:bg-teal-800'
                      : 'border border-slate-300 text-slate-400 bg-slate-50 cursor-not-allowed opacity-80'
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
