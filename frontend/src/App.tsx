import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { OciSection } from './components/OciSection';
import { VisaCategories } from './components/VisaCategories';
import { EligibilityChecker } from './components/EligibilityChecker';
import { Footer } from './components/Footer';
import { AiAgentModal } from './components/AiAgentModal';
import {
  fetchNationalities,
  fetchPurposes,
  checkEligibility,
  NationalityOption,
  PurposeOption,
  RouteDto,
} from './api/eligibility';

export const App: React.FC = () => {
  const [nationalities, setNationalities] = useState<NationalityOption[]>([]);
  const [purposes, setPurposes] = useState<PurposeOption[]>([]);
  const [selectedNationality, setSelectedNationality] = useState<string>('JPN'); // default Japan
  const [selectedPurpose, setSelectedPurpose] = useState<string>('TOURISM');
  const [applicableRoutes, setApplicableRoutes] = useState<RouteDto[]>([]);

  const [fontSize, setFontSize] = useState<'normal' | 'large'>('normal');
  const [aiModalOpen, setAiModalOpen] = useState(false);

  // Load initial options
  useEffect(() => {
    async function init() {
      try {
        const [nats, purps] = await Promise.all([
          fetchNationalities(),
          fetchPurposes(),
        ]);
        setNationalities(nats);
        setPurposes(purps);
      } catch {
        // Fallback default dataset
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

  const handleStartApplication = () => {
    const el = document.getElementById('route-comparison');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSelectCategory = (purposeCode: string) => {
    setSelectedPurpose(purposeCode);
    const heroEl = document.querySelector('section');
    if (heroEl) heroEl.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className={`min-h-screen flex flex-col bg-white text-slate-900 selection:bg-accent-500 selection:text-white ${fontSize === 'large' ? 'text-lg' : ''}`}>
      {/* 1. Navbar matching Screenshot 1 & 2 */}
      <Navbar
        onOpenAiAgent={() => setAiModalOpen(true)}
        fontSize={fontSize}
        onToggleFontSize={handleToggleFontSize}
      />

      <main className="flex-1">
        {/* 2. Hero Section matching Screenshot 1 & 2 */}
        <Hero
          nationalities={nationalities}
          purposes={purposes}
          selectedNationality={selectedNationality}
          selectedPurpose={selectedPurpose}
          onNationalityChange={(nat) => setSelectedNationality(nat)}
          onPurposeChange={(purp) => setSelectedPurpose(purp)}
          onStartApplication={handleStartApplication}
          onOpenAiAgent={() => setAiModalOpen(true)}
          applicableRoutes={applicableRoutes}
        />

        {/* 3. OCI Card Section matching Screenshot 3 */}
        <OciSection
          onStartOci={() => {
            const el = document.getElementById('route-comparison');
            if (el) el.scrollIntoView({ behavior: 'smooth' });
          }}
        />

        {/* 4. Select a Visa Category Section matching Screenshot 4 & 5 */}
        <VisaCategories onSelectCategory={handleSelectCategory} />

        {/* 5. Complete Route Comparison & Eligibility Details (Shows VoA for Japan!) */}
        <div id="route-comparison" className="bg-slate-50/70 border-t border-slate-200">
          <EligibilityChecker />
        </div>
      </main>

      {/* 6. Footer with mandatory concept disclaimer */}
      <Footer />

      {/* 7. Connect AI Agent Modal */}
      <AiAgentModal
        isOpen={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
      />
    </div>
  );
};

export default App;
