import React from 'react';
import { ChevronRight } from 'lucide-react';

interface VisaCategoriesProps {
  onSelectCategory?: (purposeCode: string) => void;
}

export const visaCategoryCards = [
  {
    num: '01',
    title: 'e-Tourist Visa',
    description: 'For recreation, sightseeing, or visits to family and friends.',
    footer: '30 DAYS · 1 YEAR · 5 YEARS, MULTIPLE ENTRY',
    purposeCode: 'TOURISM',
    imageUrl: 'https://images.unsplash.com/photo-1561361513-2d000a50f0dc?auto=format&fit=crop&w=900&q=80',
  },
  {
    num: '02',
    title: 'e-Business Visa',
    description: 'For business meetings, trade, recruitment, or exhibitions.',
    footer: '365 DAYS, MULTIPLE ENTRY · EACH VISIT UP TO 180 DAYS',
    purposeCode: 'BUSINESS',
    imageUrl: 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=900&q=80',
  },
  {
    num: '03',
    title: 'e-Medical Visa',
    description: 'For treatment at a recognised hospital in India.',
    footer: 'AS PRINTED ON THE ETA',
    purposeCode: 'MEDICAL',
    imageUrl: 'https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&w=900&q=80',
  },
  {
    num: '04',
    title: 'e-Medical Attendant Visa',
    description: 'For up to two attendants accompanying a patient.',
    footer: "TIED TO THE PATIENT'S VISA",
    purposeCode: 'MEDICAL',
    imageUrl: 'https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?auto=format&fit=crop&w=900&q=80',
  },
  {
    num: '05',
    title: 'e-Conference Visa',
    description: 'For conferences and seminars with Government clearance.',
    footer: '30 DAYS',
    purposeCode: 'CONFERENCE',
    imageUrl: 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=900&q=80',
  },
];

export const VisaCategories: React.FC<VisaCategoriesProps> = ({ onSelectCategory }) => {
  return (
    <section id="categories" className="py-20 px-4 sm:px-6 lg:px-8 max-w-[1400px] mx-auto bg-white">
      {/* Section Header */}
      <div className="mb-12">
        <h2 className="font-serif text-4xl sm:text-5xl font-normal text-slate-900 tracking-tight mb-3">
          Select a visa category.
        </h2>
        <p className="text-slate-500 text-sm sm:text-base font-normal max-w-2xl">
          Select the category that matches the purpose of visit. The application opens preconfigured for it.
        </p>
      </div>

      {/* Cards Grid (Matches Screenshot 4 & 5) */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {visaCategoryCards.map((card) => (
          <div
            key={card.num}
            onClick={() => onSelectCategory?.(card.purposeCode)}
            className="group relative h-[440px] rounded-3xl overflow-hidden cursor-pointer shadow-md hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-1"
          >
            {/* Background Image */}
            <img
              src={card.imageUrl}
              alt={card.title}
              className="absolute inset-0 w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-700 ease-out"
            />

            {/* Gradient Overlays */}
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/40 to-slate-950/20" />

            {/* Card Content */}
            <div className="relative z-10 p-7 h-full flex flex-col justify-between text-white">
              {/* Top Bar: Number & Arrow pill */}
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono font-semibold tracking-wider text-white/80">
                  {card.num}
                </span>
                <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center text-white group-hover:bg-accent-500 group-hover:text-white transition-colors">
                  <ChevronRight className="w-4 h-4" />
                </div>
              </div>

              {/* Bottom Content: Title, Description & Footer */}
              <div>
                <h3 className="text-2xl font-bold tracking-tight text-white mb-2">
                  {card.title}
                </h3>
                <p className="text-xs text-slate-200 leading-relaxed mb-4 font-normal">
                  {card.description}
                </p>
                <div className="text-[10px] tracking-[0.14em] font-semibold text-slate-400 uppercase pt-2 border-t border-white/10">
                  {card.footer}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
};
