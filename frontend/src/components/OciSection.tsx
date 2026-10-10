import React from 'react';
import { FileText, Upload, UserPlus } from 'lucide-react';

interface OciSectionProps {
  onStartOci?: () => void;
}

export const OciSection: React.FC<OciSectionProps> = ({ onStartOci }) => {
  return (
    <section id="oci" className="bg-[#070D1E] py-20 px-4 sm:px-6 lg:px-8 border-t border-slate-800 text-white overflow-hidden">
      <div className="max-w-[1400px] mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: Heading and Description */}
          <div className="lg:col-span-7">
            <span className="block text-[11px] font-bold tracking-[0.24em] text-amber-300 uppercase mb-4">
              OVERSEAS CITIZEN OF INDIA
            </span>

            <h2 className="font-serif text-4xl sm:text-5xl lg:text-6xl font-normal tracking-tight text-white leading-[1.12] mb-6">
              Hold an OCI card?<br />
              You do not need a visa.
            </h2>

            <p className="text-slate-300 text-sm sm:text-base leading-relaxed max-w-xl font-normal">
              OCI cardholders do not apply for an e-Visa. Since 1 October 2025, every OCI cardholder must file the e-Arrival Card before landing. VisaSetu (VisaIndia.io) helps you prepare that, and everything else an OCI card needs, in one place.
            </p>
          </div>

          {/* Right Column: White Card with 3 bullets and action button */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-2xl w-full max-w-[460px] text-slate-800">
              <ul className="space-y-4 mb-8">
                <li className="flex items-center space-x-3 text-xs sm:text-sm text-slate-700 font-medium">
                  <FileText className="w-4 h-4 text-accent-500 flex-shrink-0" />
                  <span>File your e-Arrival Card before you land</span>
                </li>
                <li className="flex items-center space-x-3 text-xs sm:text-sm text-slate-700 font-medium">
                  <Upload className="w-4 h-4 text-accent-500 flex-shrink-0" />
                  <span>Renew your OCI card after a new passport</span>
                </li>
                <li className="flex items-center space-x-3 text-xs sm:text-sm text-slate-700 font-medium">
                  <UserPlus className="w-4 h-4 text-accent-500 flex-shrink-0" />
                  <span>Add a family member born abroad</span>
                </li>
              </ul>

              <button
                type="button"
                onClick={onStartOci}
                className="w-full py-3.5 px-6 rounded-xl text-xs font-bold tracking-widest uppercase border border-accent-400 text-accent-600 bg-white hover:bg-accent-50 transition-all flex items-center justify-center space-x-1.5 shadow-sm"
              >
                <span>START FOR OCI CARDHOLDERS</span>
                <span className="text-[10px]">&gt;</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
