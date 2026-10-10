import React, { useState, useEffect, useRef } from 'react';
import { PageTemplate } from '../components/PageTemplate';
import { fetchEntryPoints, EntryPointDto } from '../api/content';

interface PhotoValidationResult {
  file: File;
  previewUrl: string;
  width: number;
  height: number;
  sizeKb: number;
  isFormatValid: boolean;
  isRatioValid: boolean;
  isResolutionValid: boolean;
  isSizeValid: boolean;
  overallValid: boolean;
}

export const RequirementsPage: React.FC = () => {
  const [entryPoints, setEntryPoints] = useState<EntryPointDto[]>([]);
  const [loadingPoints, setLoadingPoints] = useState(true);

  // Filters for entry points
  const [pointTypeFilter, setPointTypeFilter] = useState<string>('ALL');
  const [pointSearch, setPointSearch] = useState<string>('');

  // Checklist generator
  const [chkNationality, setChkNationality] = useState<string>('JPN');
  const [chkCategory, setChkCategory] = useState<string>('TOURIST');

  // Photo validator state
  const [photoResult, setPhotoResult] = useState<PhotoValidationResult | null>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchEntryPoints()
      .then((data) => {
        setEntryPoints(data);
        setLoadingPoints(false);
      })
      .catch((err) => {
        console.error('Error fetching entry points:', err);
        setLoadingPoints(false);
      });
  }, []);

  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPhotoError(null);
    const file = e.target.files?.[0];
    if (!file) return;

    const isFormatValid = file.type === 'image/jpeg' || file.name.toLowerCase().endsWith('.jpg') || file.name.toLowerCase().endsWith('.jpeg');
    const sizeKb = Math.round(file.size / 1024);
    const isSizeValid = sizeKb >= 10 && sizeKb <= 1024; // 10KB to 1MB

    const img = new Image();
    const objectUrl = URL.createObjectURL(file);

    img.onload = () => {
      const width = img.naturalWidth;
      const height = img.naturalHeight;
      const ratio = width / height;
      const isRatioValid = Math.abs(ratio - 1.0) <= 0.05; // 1:1 square
      const isResolutionValid = width >= 350 && width <= 1000 && height >= 350 && height <= 1000;
      const overallValid = isFormatValid && isSizeValid && isRatioValid && isResolutionValid;

      setPhotoResult({
        file,
        previewUrl: objectUrl,
        width,
        height,
        sizeKb,
        isFormatValid,
        isRatioValid,
        isResolutionValid,
        isSizeValid,
        overallValid,
      });
    };

    img.onerror = () => {
      setPhotoError('Unable to decode image file. Please provide a standard JPEG file.');
    };

    img.src = objectUrl;
  };

  const filteredEntryPoints = entryPoints.filter((pt) => {
    if (pointTypeFilter !== 'ALL' && pt.type !== pointTypeFilter) return false;
    if (pointSearch.trim()) {
      const q = pointSearch.toLowerCase();
      return (
        pt.name.toLowerCase().includes(q) ||
        (pt.code && pt.code.toLowerCase().includes(q)) ||
        pt.state.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <PageTemplate
      title="Statutory Visa Requirements & Technical Specifications"
      intro="Official prerequisites, technical file formats, passport mandates, and certified port schedules mandated by the Bureau of Immigration and Ministry of Home Affairs."
      breadcrumbs={[{ label: 'Requirements' }]}
      sourceUrl="https://indianvisaonline.gov.in/evisa/tvoa.html"
      lastVerifiedDate="October 2026"
    >
      {/* SCAM WARNING CALLOUT */}
      <div className="bg-amber-50 border-l-4 border-amber-500 p-5 rounded-r-xl mb-10 shadow-sm">
        <div className="flex items-start gap-3">
          <span className="text-2xl">⚠️</span>
          <div>
            <h2 className="text-base font-bold text-amber-900 mb-1">
              Official Consular Advisory: Beware of Fraudulent Third-Party Portals
            </h2>
            <p className="text-xs text-amber-800 leading-relaxed">
              The Government of India processes all official electronic visa applications strictly through{' '}
              <a
                href="https://indianvisaonline.gov.in"
                target="_blank"
                rel="noopener noreferrer"
                className="font-bold underline text-amber-950 hover:text-black"
              >
                indianvisaonline.gov.in
              </a>
              . Fraudulent clone sites charge hidden premiums of $100–$300 USD and compromise confidential passport numbers.
              VisaSetu is an educational concept prototype demonstrating unified border UX and does not collect live visa fees.
            </p>
          </div>
        </div>
      </div>

      {/* SECTION 1: General Passport Requirements */}
      <section className="mb-12">
        <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          1. General Passport & Applicant Prerequisites
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 font-bold flex items-center justify-center mb-3">
              📅
            </div>
            <h3 className="font-bold text-slate-900 text-sm mb-1.5">
              Minimum 6 Months Validity
            </h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Your international passport must have at least 6 months validity remaining from the exact scheduled date of arrival in India.
            </p>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 font-bold flex items-center justify-center mb-3">
              📄
            </div>
            <h3 className="font-bold text-slate-900 text-sm mb-1.5">
              2 Blank Immigration Pages
            </h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              The physical passport booklet must have a minimum of two unused blank pages for stamping by immigration officers at ICP counters.
            </p>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
            <div className="w-10 h-10 rounded-lg bg-teal-50 text-teal-700 font-bold flex items-center justify-center mb-3">
              ✈️
            </div>
            <h3 className="font-bold text-slate-900 text-sm mb-1.5">
              Return / Onward Ticket & Funds
            </h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Travelers must hold a confirmed return journey ticket or onward flight ticket and demonstrate adequate financial funds for their stay.
            </p>
          </div>
        </div>

        {/* Restricted Origins clause */}
        <div className="mt-4 p-4 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-700">
          <strong className="text-slate-900">Restricted Passports & Origin Mandate:</strong> In accordance with statutory directives, applicants of Pakistani origin or holding Pakistani passports are ineligible for e-Visa facilities and must submit regular paper visa applications directly to the appropriate Indian Embassy or High Commission.
        </div>
      </section>

      {/* SECTION 2: Client-side Photo Validator */}
      <section className="mb-12 bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <div className="max-w-2xl">
          <h2 className="text-xl font-bold text-slate-900 mb-2 flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
            2. Interactive Passport Photo Validator
          </h2>
          <p className="text-xs text-slate-600 mb-6">
            Test your digital photograph against the Bureau of Immigration’s automated biometric ingest criteria before submitting your application.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
          {/* Upload Area */}
          <div>
            <div
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-teal-300 hover:border-teal-500 bg-teal-50/40 rounded-xl p-8 text-center cursor-pointer transition-colors"
            >
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/jpg"
                onChange={handlePhotoUpload}
                className="hidden"
              />
              <div className="text-3xl mb-3">📸</div>
              <p className="text-sm font-semibold text-slate-800 mb-1">
                Click or drag & drop photograph to validate
              </p>
              <p className="text-xs text-slate-500">
                JPEG format only • 10 KB to 1 MB • 350x350 to 1000x1000 px
              </p>
            </div>

            {photoError && (
              <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700">
                {photoError}
              </div>
            )}

            {/* Official specs list */}
            <div className="mt-5 space-y-2 text-xs text-slate-600">
              <p className="font-semibold text-slate-800 uppercase tracking-wider text-[11px]">
                Statutory Specs:
              </p>
              <div className="flex items-center gap-2">
                <span className="text-teal-700">✓</span> Square 1:1 Aspect ratio (Equal width & height)
              </div>
              <div className="flex items-center gap-2">
                <span className="text-teal-700">✓</span> Dimensions: Min 350 × 350 px, Max 1000 × 1000 px
              </div>
              <div className="flex items-center gap-2">
                <span className="text-teal-700">✓</span> Size: Min 10 KB, Max 1024 KB (1 MB)
              </div>
              <div className="flex items-center gap-2">
                <span className="text-teal-700">✓</span> Pure white or light background with no eyeglasses
              </div>
            </div>
          </div>

          {/* Validation Result Box */}
          <div className="bg-slate-50 rounded-xl border border-slate-200 p-6 min-h-[260px] flex flex-col justify-center">
            {photoResult ? (
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <img
                    src={photoResult.previewUrl}
                    alt="Uploaded preview"
                    className="w-20 h-20 rounded-lg object-cover border border-slate-300 shadow-sm"
                  />
                  <div>
                    <span
                      className={`inline-block px-3 py-1 rounded-full text-xs font-bold ${
                        photoResult.overallValid
                          ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                          : 'bg-red-100 text-red-800 border border-red-300'
                      }`}
                    >
                      {photoResult.overallValid ? '✓ Fully Compliant' : '✗ Non-Compliant Specifications'}
                    </span>
                    <p className="text-xs text-slate-500 mt-1">
                      {photoResult.file.name}
                    </p>
                  </div>
                </div>

                <div className="space-y-2 text-xs pt-3 border-t border-slate-200">
                  <div className="flex justify-between items-center">
                    <span className="text-slate-600">Format (JPEG):</span>
                    <span className={`font-semibold ${photoResult.isFormatValid ? 'text-emerald-700' : 'text-red-600'}`}>
                      {photoResult.isFormatValid ? '✓ Passed (JPEG)' : '✗ Failed (Not JPEG)'}
                    </span>
                  </div>

                  <div className="flex justify-between items-center">
                    <span className="text-slate-600">File Size:</span>
                    <span className={`font-semibold ${photoResult.isSizeValid ? 'text-emerald-700' : 'text-red-600'}`}>
                      {photoResult.isSizeValid ? `✓ ${photoResult.sizeKb} KB (Within 10KB–1MB)` : `✗ ${photoResult.sizeKb} KB (Out of range)`}
                    </span>
                  </div>

                  <div className="flex justify-between items-center">
                    <span className="text-slate-600">Aspect Ratio (1:1):</span>
                    <span className={`font-semibold ${photoResult.isRatioValid ? 'text-emerald-700' : 'text-red-600'}`}>
                      {photoResult.isRatioValid ? '✓ Square (1:1)' : `✗ Non-square (${photoResult.width}x${photoResult.height})`}
                    </span>
                  </div>

                  <div className="flex justify-between items-center">
                    <span className="text-slate-600">Resolution (350–1000px):</span>
                    <span className={`font-semibold ${photoResult.isResolutionValid ? 'text-emerald-700' : 'text-red-600'}`}>
                      {photoResult.isResolutionValid ? `✓ ${photoResult.width}×${photoResult.height} px` : `✗ ${photoResult.width}×${photoResult.height} px (Must be 350-1000)`}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center text-slate-400 py-6">
                <div className="text-3xl mb-2">📋</div>
                <p className="text-xs">
                  Upload an image to trigger instant client-side biometric validation
                </p>
              </div>
            )}
          </div>
        </div>
      </section>

      {/* SECTION 3: Personalized Document Checklist (Interactive & Printable) */}
      <section className="mb-12 bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              3. Personalized Document Checklist
            </h2>
            <p className="text-xs text-slate-600">
              Generate a custom checklist tailored to your nationality and purpose of travel.
            </p>
          </div>

          <button
            type="button"
            onClick={() => window.print()}
            className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold rounded-lg flex items-center gap-1.5 self-start sm:self-auto cursor-pointer"
          >
            <span>🖨️</span> Print Checklist
          </button>
        </div>

        <div className="flex flex-wrap gap-4 mb-6 p-4 bg-slate-50 rounded-xl border border-slate-200">
          <div>
            <label className="block text-[11px] font-semibold text-slate-500 uppercase mb-1">
              Select Passport Nationality
            </label>
            <select
              value={chkNationality}
              onChange={(e) => setChkNationality(e.target.value)}
              className="bg-white border border-slate-300 text-slate-800 text-xs rounded-lg p-2 font-medium"
            >
              <option value="JPN">🇯🇵 Japan (VoA Eligible)</option>
              <option value="USA">🇺🇸 United States</option>
              <option value="GBR">🇬🇧 United Kingdom</option>
              <option value="DEU">🇩🇪 Germany</option>
              <option value="FRA">🇫🇷 France</option>
              <option value="KOR">🇰🇷 South Korea (VoA Eligible)</option>
              <option value="AUS">🇦🇺 Australia</option>
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-semibold text-slate-500 uppercase mb-1">
              Travel Category
            </label>
            <select
              value={chkCategory}
              onChange={(e) => setChkCategory(e.target.value)}
              className="bg-white border border-slate-300 text-slate-800 text-xs rounded-lg p-2 font-medium"
            >
              <option value="TOURIST">Tourism / Sightseeing / Yoga</option>
              <option value="BUSINESS">Business / Corporate Meetings</option>
              <option value="MEDICAL">Medical Treatment / Surgery</option>
              <option value="CONFERENCE">Official Conference / Seminar</option>
            </select>
          </div>
        </div>

        {/* Dynamic Checklist Render */}
        <div className="space-y-3">
          <label className="flex items-start gap-3 p-3.5 bg-slate-50/70 rounded-lg border border-slate-200 text-xs text-slate-800 cursor-pointer hover:bg-slate-50">
            <input type="checkbox" defaultChecked className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
            <span>
              <strong>Original Passport Scan:</strong> High-resolution biographical page scan containing photograph and personal details (PDF or JPEG, 10 KB to 300 KB, min 6 months validity).
            </span>
          </label>

          <label className="flex items-start gap-3 p-3.5 bg-slate-50/70 rounded-lg border border-slate-200 text-xs text-slate-800 cursor-pointer hover:bg-slate-50">
            <input type="checkbox" defaultChecked className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
            <span>
              <strong>Digital Passport Photograph:</strong> Front-facing portrait with white background (JPEG, 350x350 to 1000x1000 px, 10 KB to 1 MB).
            </span>
          </label>

          <label className="flex items-start gap-3 p-3.5 bg-slate-50/70 rounded-lg border border-slate-200 text-xs text-slate-800 cursor-pointer hover:bg-slate-50">
            <input type="checkbox" className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
            <span>
              <strong>Return Flight Reservation:</strong> Proof of confirmed return journey or onward itinerary departing India.
            </span>
          </label>

          {chkCategory === 'BUSINESS' && (
            <>
              <label className="flex items-start gap-3 p-3.5 bg-teal-50/70 rounded-lg border border-teal-200 text-xs text-slate-800 cursor-pointer">
                <input type="checkbox" className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
                <span>
                  <strong>Business Card / Visiting Card:</strong> Scanned copy of applicant's corporate visiting card or company credential.
                </span>
              </label>
              <label className="flex items-start gap-3 p-3.5 bg-teal-50/70 rounded-lg border border-teal-200 text-xs text-slate-800 cursor-pointer">
                <input type="checkbox" className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
                <span>
                  <strong>Indian Host Invitation Letter:</strong> Formal invitation on Indian entity's letterhead with Corporate Identification Number (CIN) and authorized signatory.
                </span>
              </label>
            </>
          )}

          {chkCategory === 'MEDICAL' && (
            <label className="flex items-start gap-3 p-3.5 bg-teal-50/70 rounded-lg border border-teal-200 text-xs text-slate-800 cursor-pointer">
              <input type="checkbox" className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
              <span>
                <strong>Accredited Hospital Letter:</strong> Written diagnosis and treatment plan on letterhead from a recognized specialized hospital or NABH healthcare facility in India.
              </span>
            </label>
          )}

          {chkCategory === 'CONFERENCE' && (
            <label className="flex items-start gap-3 p-3.5 bg-teal-50/70 rounded-lg border border-teal-200 text-xs text-slate-800 cursor-pointer">
              <input type="checkbox" className="mt-0.5 rounded text-teal-600 focus:ring-teal-500" />
              <span>
                <strong>Government Event Clearances:</strong> Political Clearance letter from Ministry of External Affairs (MEA) and Security Clearance from Ministry of Home Affairs (MHA).
              </span>
            </label>
          )}

          {chkNationality === 'JPN' && chkCategory === 'TOURIST' && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-900 mt-2">
              💡 <strong>VoA Alternative Available:</strong> As a Japanese national, you may optionally bypass online document upload and obtain a Visa on Arrival directly upon landing at DEL, BOM, BLR, MAA, CCU, or HYD for ₹2000 INR (~$25 USD).
            </div>
          )}
        </div>
      </section>

      {/* SECTION 4: Application Window Rules */}
      <section className="mb-12 bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <h2 className="text-xl font-bold text-slate-900 mb-4 flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
          4. Statutory Application Windows & Timelines
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs text-slate-700">
          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200">
            <h3 className="font-bold text-slate-900 text-sm mb-2">
              e-Tourist Visa (30 Days)
            </h3>
            <ul className="space-y-2">
              <li>
                <strong>Earliest Submission:</strong> 30 calendar days before scheduled arrival in India.
              </li>
              <li>
                <strong>Latest Submission:</strong> Minimum 4 calendar days before embarkation date.
              </li>
              <li>
                <strong>Rule Note:</strong> Applications submitted less than 4 days before flight will not be accepted by the portal.
              </li>
            </ul>
          </div>

          <div className="p-4 bg-slate-50 rounded-xl border border-slate-200">
            <h3 className="font-bold text-slate-900 text-sm mb-2">
              1-Year / 5-Year e-Tourist, e-Business & e-Medical
            </h3>
            <ul className="space-y-2">
              <li>
                <strong>Earliest Submission:</strong> Up to 120 calendar days in advance of expected travel date.
              </li>
              <li>
                <strong>Latest Submission:</strong> Minimum 4 business days before arrival date.
              </li>
              <li>
                <strong>Rule Note:</strong> Multi-year visas start validity immediately upon grant of ETA.
              </li>
            </ul>
          </div>
        </div>
      </section>

      {/* SECTION 5: Searchable Entry Points List */}
      <section id="entry-points" className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
          <div>
            <h2 className="text-xl font-bold text-slate-900 mb-1 flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-teal-600" />
              5. Official Designated Entry & Exit Check Posts
            </h2>
            <p className="text-xs text-slate-600">
              Verified list of 37 international airports, 5 seaports, and authorized land border immigration check posts.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <input
              type="text"
              placeholder="Search port or state..."
              value={pointSearch}
              onChange={(e) => setPointSearch(e.target.value)}
              className="text-xs px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg focus:ring-teal-500 focus:border-teal-500"
            />

            <select
              value={pointTypeFilter}
              onChange={(e) => setPointTypeFilter(e.target.value)}
              className="text-xs px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg font-medium"
            >
              <option value="ALL">All Types ({entryPoints.length})</option>
              <option value="AIRPORT">Airports (37)</option>
              <option value="SEAPORT">Seaports (5)</option>
              <option value="LAND_ICP">Land ICPs</option>
            </select>
          </div>
        </div>

        {loadingPoints ? (
          <div className="py-12 text-center text-slate-500 text-xs">
            Loading verified entry points...
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
                  <th className="p-3">Port Name & Code</th>
                  <th className="p-3">Type</th>
                  <th className="p-3">State / UT</th>
                  <th className="p-3 text-center">e-Visa Entry</th>
                  <th className="p-3 text-center">e-Visa Exit</th>
                  <th className="p-3 text-center">VoA Allowed</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredEntryPoints.map((pt) => (
                  <tr key={pt.id || pt.name} className="hover:bg-slate-50/60 transition-colors">
                    <td className="p-3 font-medium text-slate-900">
                      {pt.name} {pt.code && <span className="text-slate-400 font-mono">({pt.code})</span>}
                    </td>
                    <td className="p-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-700">
                        {pt.type}
                      </span>
                    </td>
                    <td className="p-3 text-slate-600">{pt.state}</td>
                    <td className="p-3 text-center">
                      {pt.isEvisaEntry ? (
                        <span className="text-emerald-700 font-bold">✓</span>
                      ) : (
                        <span className="text-slate-300 font-bold">—</span>
                      )}
                    </td>
                    <td className="p-3 text-center">
                      {pt.isEvisaExit ? (
                        <span className="text-emerald-700 font-bold">✓</span>
                      ) : (
                        <span className="text-slate-300 font-bold">—</span>
                      )}
                    </td>
                    <td className="p-3 text-center">
                      {pt.isVoaAllowed ? (
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800">
                          VoA Active
                        </span>
                      ) : (
                        <span className="text-slate-300 font-bold">—</span>
                      )}
                    </td>
                  </tr>
                ))}
                {filteredEntryPoints.length === 0 && (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-slate-400">
                      No entry checkpoints matched your search query.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </PageTemplate>
  );
};
