import React, { useState, useRef, useEffect } from 'react';
import { Search, ChevronDown, Check, X } from 'lucide-react';
import { ALL_COUNTRIES, CountryItem } from '../data/countries';

interface SearchableCountrySelectProps {
  value: string;
  onChange: (countryCode: string) => void;
  options?: CountryItem[];
  id?: string;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}

export const SearchableCountrySelect: React.FC<SearchableCountrySelectProps> = ({
  value,
  onChange,
  options = ALL_COUNTRIES,
  id,
  placeholder = 'Search country or region...',
  className = '',
  disabled = false,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Auto-focus search input when opening
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);
    } else {
      setSearchQuery('');
    }
  }, [isOpen]);

  const selectedCountry = options.find(
    (c) => c.code.toUpperCase() === (value || '').toUpperCase()
  ) || ALL_COUNTRIES.find((c) => c.code.toUpperCase() === (value || '').toUpperCase());

  const filtered = options.filter((c) => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return true;
    return (
      c.name.toLowerCase().includes(q) ||
      c.code.toLowerCase().includes(q)
    );
  });

  const handleSelect = (code: string) => {
    onChange(code);
    setIsOpen(false);
    setSearchQuery('');
  };

  return (
    <div className={`relative ${className}`} ref={containerRef}>
      {/* Trigger Button */}
      <button
        id={id}
        type="button"
        disabled={disabled}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        className={`w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-medium text-slate-900 flex items-center justify-between text-left transition-all focus:bg-white focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 ${
          disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:border-slate-400'
        } ${isOpen ? 'bg-white ring-2 ring-teal-500/20 border-teal-500' : ''}`}
        aria-expanded={isOpen}
      >
        <span className="flex items-center gap-2 truncate">
          {selectedCountry ? (
            <>
              <span className="text-base select-none">{selectedCountry.flagEmoji}</span>
              <span className="truncate font-semibold text-slate-800">{selectedCountry.name}</span>
              <span className="text-[10px] font-mono text-slate-400 font-semibold px-1 py-0.5 rounded bg-slate-200/60">
                {selectedCountry.code}
              </span>
            </>
          ) : (
            <span className="text-slate-400">{placeholder}</span>
          )}
        </span>
        <ChevronDown
          className={`w-4 h-4 text-slate-400 flex-shrink-0 transition-transform duration-200 ${
            isOpen ? 'rotate-180 text-teal-600' : ''
          }`}
        />
      </button>

      {/* Dropdown Panel with Search Bar */}
      {isOpen && (
        <div className="absolute left-0 right-0 mt-1.5 bg-white rounded-2xl shadow-xl border border-slate-200 py-2 z-50 animate-in fade-in slide-in-from-top-1 duration-150">
          {/* Search Bar Input */}
          <div className="px-3 pb-2 pt-1 border-b border-slate-100">
            <div className="relative flex items-center">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 pointer-events-none" />
              <input
                ref={searchInputRef}
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Type country name or code..."
                className="w-full pl-8 pr-7 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:bg-white focus:outline-none focus:ring-1 focus:ring-teal-500 focus:border-teal-500 font-medium placeholder:text-slate-400"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2 text-slate-400 hover:text-slate-600 p-0.5"
                  title="Clear search"
                >
                  <X className="w-3 h-3" />
                </button>
              )}
            </div>
            <div className="flex items-center justify-between text-[10px] text-slate-400 mt-1.5 px-0.5 font-medium">
              <span>Showing {filtered.length} of {options.length} countries</span>
              {searchQuery && <span className="text-teal-700">Filtered</span>}
            </div>
          </div>

          {/* Results List */}
          <div className="max-h-60 overflow-y-auto px-1 py-1 space-y-0.5">
            {filtered.length > 0 ? (
              filtered.map((country) => {
                const isSelected = country.code.toUpperCase() === (value || '').toUpperCase();
                return (
                  <button
                    key={country.code}
                    type="button"
                    onClick={() => handleSelect(country.code)}
                    className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs transition-colors text-left cursor-pointer ${
                      isSelected
                        ? 'bg-teal-50 text-teal-900 font-bold'
                        : 'text-slate-700 hover:bg-slate-50 hover:text-slate-900'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 truncate">
                      <span className="text-base select-none">{country.flagEmoji}</span>
                      <span className="truncate">{country.name}</span>
                    </div>
                    <div className="flex items-center gap-1.5 flex-shrink-0 ml-2">
                      <span className="font-mono text-[10px] text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded">
                        {country.code}
                      </span>
                      {isSelected && <Check className="w-3.5 h-3.5 text-teal-600 flex-shrink-0" />}
                    </div>
                  </button>
                );
              })
            ) : (
              <div className="px-4 py-6 text-center text-xs text-slate-400">
                <p className="font-semibold text-slate-600 mb-1">No matching countries found</p>
                <p className="text-[11px]">Try checking the spelling or searching by 3-letter ISO code.</p>
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="mt-2.5 px-3 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[11px] font-semibold transition-colors"
                >
                  Clear search
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
