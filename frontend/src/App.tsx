import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { EligibilityChecker } from './components/EligibilityChecker';
import { Footer } from './components/Footer';
import { RouteDto } from './api/eligibility';
import { Layers, ShieldCheck, ArrowRight } from 'lucide-react';

export const App: React.FC = () => {
  const [selectedRoute, setSelectedRoute] = useState<{
    route: RouteDto;
    nationality: string;
    purpose: string;
  } | null>(null);

  const handleSelectRoute = (route: RouteDto, nationality: string, purpose: string) => {
    setSelectedRoute({ route, nationality, purpose });
    const el = document.getElementById('apply');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 selection:bg-brand-500 selection:text-white">
      <Navbar />

      <main className="flex-1">
        {/* Hero Section */}
        <Hero />

        {/* Smart Eligibility Checker (Phase 1 Acceptance Feature) */}
        <EligibilityChecker onSelectRoute={handleSelectRoute} />

        {/* Phase 2 Preview: Unified Application / Trip Wizard anchor */}
        <section id="apply" className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
          <div className="rounded-3xl bg-gradient-to-br from-brand-900 via-slate-900 to-brand-950 text-white p-8 sm:p-12 shadow-2xl relative overflow-hidden">
            <div className="absolute top-0 right-0 -mt-10 -mr-10 w-80 h-80 bg-brand-500/10 rounded-full blur-3xl pointer-events-none"></div>

            <div className="max-w-3xl">
              <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-500/20 text-brand-300 text-xs font-semibold uppercase tracking-wider mb-4 border border-brand-500/30">
                <Layers className="w-3.5 h-3.5" />
                <span>Unified Trip Profile</span>
              </div>

              <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight mb-4">
                {selectedRoute
                  ? `Selected Route: ${selectedRoute.route.title}`
                  : 'One Single Form for Every Travel Clearance'}
              </h2>

              <p className="text-slate-300 text-sm sm:text-base leading-relaxed mb-6">
                No more entering your passport and trip details three times across separate portals.
                VisaSetu binds your <strong>e-Visa</strong>, <strong>e-Arrival Card</strong>, and <strong>e-Suvidha Health Clearance</strong> into a single coordinated application with autosave and offline resilience.
              </p>

              {selectedRoute && (
                <div className="p-4 rounded-xl bg-white/10 backdrop-blur-sm border border-white/15 mb-6 text-xs text-slate-200 grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div>
                    <span className="text-slate-400 block">Nationality</span>
                    <span className="font-bold text-white">{selectedRoute.nationality}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Purpose</span>
                    <span className="font-bold text-white">{selectedRoute.purpose}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Official Fee</span>
                    <span className="font-bold text-emerald-400">${selectedRoute.route.fee} {selectedRoute.route.currency}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Processing Time</span>
                    <span className="font-bold text-brand-300">{selectedRoute.route.processingTimeText}</span>
                  </div>
                </div>
              )}

              <div className="flex flex-wrap items-center gap-4">
                <a
                  href="#eligibility"
                  className="px-6 py-3 rounded-xl bg-brand-500 hover:bg-brand-400 text-white font-semibold text-xs tracking-wide shadow-lg shadow-brand-500/30 transition-all flex items-center space-x-2"
                >
                  <span>{selectedRoute ? 'Ready to Start Wizard' : 'Select a Route to Begin'}</span>
                  <ArrowRight className="w-4 h-4" />
                </a>
                <div className="flex items-center space-x-2 text-xs text-slate-400">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span>Autosave to IndexedDB + MongoDB Enabled</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Phase 3 & 4 Status Tracking Preview */}
        <section id="track" className="pb-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
          <div className="p-8 rounded-2xl bg-white border border-slate-200 text-slate-800 shadow-sm flex flex-col md:flex-row items-center justify-between gap-6">
            <div>
              <h3 className="text-xl font-bold text-slate-900 mb-1">
                Already applied? Track your Unified Trip
              </h3>
              <p className="text-xs text-slate-500">
                Enter your reference ID (e.g., <code className="text-brand-600 bg-brand-50 px-1.5 py-0.5 rounded">VS-2026-XXXX</code>) to check live government review and module QR passes.
              </p>
            </div>
            <div className="flex w-full md:w-auto items-center space-x-2">
              <input
                type="text"
                placeholder="Reference ID (e.g. VS-2026-9481)"
                className="w-full sm:w-64 border border-slate-300 rounded-xl px-4 py-2.5 text-xs text-slate-800 focus:ring-2 focus:ring-brand-500"
              />
              <button
                type="button"
                className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs rounded-xl whitespace-nowrap shadow-sm"
              >
                Track
              </button>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};

export default App;
