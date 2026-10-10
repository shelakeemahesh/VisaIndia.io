import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { OciSection } from './components/OciSection';
import { VisaCategories } from './components/VisaCategories';
import { EligibilityChecker } from './components/EligibilityChecker';
import { Footer } from './components/Footer';
import { AiAgentModal } from './components/AiAgentModal';
import { HashRedirector } from './components/HashRedirector';

// Pages
import { ApplyPage } from './pages/ApplyPage';
import { VisaCategoriesPage } from './pages/VisaCategoriesPage';
import { CategoryDetailPage } from './pages/CategoryDetailPage';
import { RequirementsPage } from './pages/RequirementsPage';
import { HowItWorksPage } from './pages/HowItWorksPage';
import { ExistingApplicationsPage } from './pages/ExistingApplicationsPage';
import { NoticesPage } from './pages/NoticesPage';
import { NoticeDetailPage } from './pages/NoticeDetailPage';
import { HelpPage } from './pages/HelpPage';
import { ConnectAiAgentPage } from './pages/ConnectAiAgentPage';

import {
  fetchNationalities,
  fetchPurposes,
  checkEligibility,
  NationalityOption,
  PurposeOption,
  RouteDto,
} from './api/eligibility';
import { fetchNotices, NoticeDto } from './api/content';

// Home Page Component with teasers linking to full pages
const HomePage: React.FC<{
  nationalities: NationalityOption[];
  purposes: PurposeOption[];
  selectedNationality: string;
  selectedPurpose: string;
  setSelectedNationality: (nat: string) => void;
  setSelectedPurpose: (purp: string) => void;
  applicableRoutes: RouteDto[];
  onOpenAiAgent: () => void;
  pinnedNotice: NoticeDto | null;
}> = ({
  nationalities,
  purposes,
  selectedNationality,
  selectedPurpose,
  setSelectedNationality,
  setSelectedPurpose,
  applicableRoutes,
  onOpenAiAgent,
  pinnedNotice,
}) => {
  const navigate = useNavigate();

  const handleStartApplication = () => {
    navigate(`/apply?nationality=${selectedNationality}&purpose=${selectedPurpose}`);
  };

  const handleSelectCategory = (purposeCode: string) => {
    navigate(`/apply?purpose=${purposeCode}&nationality=${selectedNationality}`);
  };

  return (
    <main className="flex-1">
      {/* Pinned Official Advisory Banner */}
      {pinnedNotice && (
        <div className="bg-amber-500 text-amber-950 px-4 py-2.5 text-xs font-medium border-b border-amber-600/30 flex items-center justify-between">
          <div className="max-w-[1400px] mx-auto w-full flex items-center justify-between gap-4">
            <div className="flex items-center gap-2 truncate">
              <span className="font-bold px-1.5 py-0.5 rounded bg-amber-600 text-white text-[10px] tracking-wide uppercase">
                Advisory
              </span>
              <span className="truncate font-semibold">{pinnedNotice.title}</span>
            </div>
            <Link
              to={`/notices/${pinnedNotice.noticeId}`}
              className="underline font-bold text-black hover:text-amber-900 whitespace-nowrap ml-2"
            >
              Read Directive →
            </Link>
          </div>
        </div>
      )}

      {/* Hero Section */}
      <Hero
        nationalities={nationalities}
        purposes={purposes}
        selectedNationality={selectedNationality}
        selectedPurpose={selectedPurpose}
        onNationalityChange={setSelectedNationality}
        onPurposeChange={setSelectedPurpose}
        onStartApplication={handleStartApplication}
        onOpenAiAgent={onOpenAiAgent}
        applicableRoutes={applicableRoutes}
      />

      {/* OCI Card Section Teaser */}
      <OciSection
        onStartOci={() => {
          navigate('/apply?purpose=OCI');
        }}
      />

      {/* Visa Categories Grid Teaser with Link to Full Catalog */}
      <div className="relative">
        <VisaCategories onSelectCategory={handleSelectCategory} />
        <div className="text-center pb-8 bg-slate-50">
          <Link
            to="/visa-categories"
            className="inline-flex items-center gap-1.5 text-xs font-bold text-teal-700 hover:text-teal-900 px-4 py-2 rounded-lg bg-teal-50 border border-teal-200 transition-colors"
          >
            Explore all 17 sub-categories & compare parameters →
          </Link>
        </div>
      </div>

      {/* Eligibility Checker & Full Route Comparison */}
      <div id="route-comparison" className="bg-slate-50/70 border-t border-slate-200">
        <EligibilityChecker
          onSelectRoute={(route, nationality, purpose) => {
            navigate(
              `/apply?nationality=${nationality}&purpose=${purpose}&category=${encodeURIComponent(
                route.title
              )}`
            );
          }}
        />
      </div>

      {/* Teaser Banners Linking to Other Deep Pages */}
      <section className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-14">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Link
            to="/requirements"
            className="p-6 bg-white rounded-2xl border border-slate-200 hover:border-teal-500 hover:shadow-md transition-all group"
          >
            <span className="text-3xl mb-3 block">📸</span>
            <h3 className="text-base font-bold text-slate-900 mb-1 group-hover:text-teal-700">
              Biometric Photo Validator
            </h3>
            <p className="text-xs text-slate-600 mb-3 leading-relaxed">
              Upload your passport picture to test 1:1 aspect ratio, 350-1000px resolution, and 10KB-1MB specs before applying.
            </p>
            <span className="text-xs font-semibold text-teal-700 flex items-center gap-1">
              Verify your photo now →
            </span>
          </Link>

          <Link
            to="/how-it-works"
            className="p-6 bg-white rounded-2xl border border-slate-200 hover:border-teal-500 hover:shadow-md transition-all group"
          >
            <span className="text-3xl mb-3 block">🔄</span>
            <h3 className="text-base font-bold text-slate-900 mb-1 group-hover:text-teal-700">
              Unified 3-in-1 Architecture
            </h3>
            <p className="text-xs text-slate-600 mb-3 leading-relaxed">
              See how VisaSetu replaces 3 disjoint portals by unifying e-Visa + e-Arrival Card + e-Suvidha health declaration.
            </p>
            <span className="text-xs font-semibold text-teal-700 flex items-center gap-1">
              Understand the 6-step flow →
            </span>
          </Link>

          <Link
            to="/existing-applications"
            className="p-6 bg-white rounded-2xl border border-slate-200 hover:border-teal-500 hover:shadow-md transition-all group"
          >
            <span className="text-3xl mb-3 block">💳</span>
            <h3 className="text-base font-bold text-slate-900 mb-1 group-hover:text-teal-700">
              Payment Re-sync Engine
            </h3>
            <p className="text-xs text-slate-600 mb-3 leading-relaxed">
              Debited without an instant confirmation? Our gateway reconciliation engine syncs states and repairs pending transactions.
            </p>
            <span className="text-xs font-semibold text-teal-700 flex items-center gap-1">
              Track or re-sync payment →
            </span>
          </Link>
        </div>
      </section>
    </main>
  );
};

export const App: React.FC = () => {
  const [nationalities, setNationalities] = useState<NationalityOption[]>([]);
  const [purposes, setPurposes] = useState<PurposeOption[]>([]);
  const [selectedNationality, setSelectedNationality] = useState<string>('JPN');
  const [selectedPurpose, setSelectedPurpose] = useState<string>('TOURISM');
  const [applicableRoutes, setApplicableRoutes] = useState<RouteDto[]>([]);
  const [pinnedNotice, setPinnedNotice] = useState<NoticeDto | null>(null);

  const [fontSize, setFontSize] = useState<'normal' | 'large'>('normal');
  const [aiModalOpen, setAiModalOpen] = useState(false);

  // Load initial options & pinned notice
  useEffect(() => {
    async function init() {
      try {
        const [nats, purps, nots] = await Promise.all([
          fetchNationalities(),
          fetchPurposes(),
          fetchNotices(undefined, true).catch(() => []),
        ]);
        setNationalities(nats);
        setPurposes(purps);
        if (nots && nots.length > 0) {
          setPinnedNotice(nots[0]);
        }
      } catch {
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
    init();
  }, []);

  // Update routes whenever nationality or purpose changes
  useEffect(() => {
    async function loadRoutes() {
      if (!selectedNationality || !selectedPurpose) return;
      try {
        const res = await checkEligibility(selectedNationality, selectedPurpose);
        setApplicableRoutes(res.routes || []);
      } catch {
        setApplicableRoutes([]);
      }
    }
    loadRoutes();
  }, [selectedNationality, selectedPurpose]);

  const handleToggleFontSize = () => {
    setFontSize(fontSize === 'normal' ? 'large' : 'normal');
  };

  return (
    <BrowserRouter>
      <HashRedirector />
      <div
        className={`min-h-screen flex flex-col bg-white text-slate-900 selection:bg-accent-500 selection:text-white ${
          fontSize === 'large' ? 'text-lg' : ''
        }`}
      >
        {/* Responsive Navbar */}
        <Navbar
          fontSize={fontSize}
          onToggleFontSize={handleToggleFontSize}
        />

        {/* Client-Side Routing */}
        <Routes>
          <Route
            path="/"
            element={
              <HomePage
                nationalities={nationalities}
                purposes={purposes}
                selectedNationality={selectedNationality}
                selectedPurpose={selectedPurpose}
                setSelectedNationality={setSelectedNationality}
                setSelectedPurpose={setSelectedPurpose}
                applicableRoutes={applicableRoutes}
                onOpenAiAgent={() => setAiModalOpen(true)}
                pinnedNotice={pinnedNotice}
              />
            }
          />
          <Route path="/visa-categories" element={<VisaCategoriesPage />} />
          <Route path="/visa-categories/:slug" element={<CategoryDetailPage />} />
          <Route path="/apply" element={<ApplyPage />} />
          <Route path="/requirements" element={<RequirementsPage />} />
          <Route path="/how-it-works" element={<HowItWorksPage />} />
          <Route path="/existing-applications" element={<ExistingApplicationsPage />} />
          <Route path="/notices" element={<NoticesPage />} />
          <Route path="/notices/:id" element={<NoticeDetailPage />} />
          <Route path="/help" element={<HelpPage />} />
          <Route path="/connect-ai-agent" element={<ConnectAiAgentPage />} />
        </Routes>

        {/* Footer with mandatory concept disclaimer */}
        <Footer />

        {/* Quick Connect AI Agent Modal */}
        <AiAgentModal
          isOpen={aiModalOpen}
          onClose={() => setAiModalOpen(false)}
        />
      </div>
    </BrowserRouter>
  );
};

export default App;
