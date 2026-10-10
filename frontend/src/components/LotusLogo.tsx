import React from 'react';

interface LotusLogoProps {
  className?: string;
}

export const LotusLogo: React.FC<LotusLogoProps> = ({ className = 'h-9 w-auto' }) => {
  return (
    <div className="flex items-center space-x-2.5 cursor-pointer select-none">
      {/* Exact Atithi multi-petal stylized lotus mark */}
      <svg
        viewBox="0 0 100 85"
        className={className}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-label="Atithi Lotus Logo"
      >
        {/* Left outer petal - Soft rose pink */}
        <path
          d="M20 54C10 46 8 32 15 24C22 16 34 26 33 42C33 46 26 51 20 54Z"
          fill="#FB7185"
          opacity="0.9"
        />
        {/* Left inner petal - Bright coral */}
        <path
          d="M34 50C28 38 30 22 40 14C49 7 53 22 47 38C44 44 39 48 34 50Z"
          fill="#F43F5E"
        />
        {/* Center petal - Warm saffron / orange */}
        <path
          d="M50 48C45 32 44 14 50 6C56 14 55 32 50 48Z"
          fill="#F97316"
        />
        {/* Right inner petal - Deep violet / purple */}
        <path
          d="M66 50C72 38 70 22 60 14C51 7 47 22 53 38C56 44 61 48 66 50Z"
          fill="#8B5CF6"
        />
        {/* Right outer petal - Soft lilac / lavender */}
        <path
          d="M80 54C90 46 92 32 85 24C78 16 66 26 67 42C67 46 74 51 80 54Z"
          fill="#C084FC"
          opacity="0.9"
        />
        {/* Small base accent dots */}
        <circle cx="50" cy="56" r="3.5" fill="#EA580C" />
        <circle cx="41" cy="58" r="2.5" fill="#FB7185" />
        <circle cx="59" cy="58" r="2.5" fill="#A855F7" />
      </svg>

      {/* Typography: "Atithi" in elegant serif, with subtitle "E-VISA" */}
      <div className="flex flex-col leading-none">
        <span className="font-serif text-2xl tracking-tight text-slate-900 font-semibold">
          Atithi
        </span>
        <span className="text-[9px] tracking-[0.25em] text-slate-400 font-bold uppercase mt-0.5">
          E-VISA
        </span>
      </div>
    </div>
  );
};
