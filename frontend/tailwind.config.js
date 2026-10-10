/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0f7ff',
          100: '#e0effe',
          200: '#bae0fd',
          300: '#7cc5fb',
          400: '#36a6f6',
          500: '#0c87eb',
          600: '#006bc9',
          700: '#0155a3',
          800: '#064986',
          900: '#0a3c6f',
          950: '#07264a',
        },
        navy: {
          900: '#070D1E',
          950: '#030712',
          800: '#0B1530',
        },
        accent: {
          50: '#fff7ed',
          100: '#ffedd5',
          200: '#fed7aa',
          500: '#E86A33',
          600: '#D9531E',
          700: '#C2410C',
        }
      },
      fontFamily: {
        serif: [
          '"Playfair Display"',
          'Georgia',
          'serif',
        ],
        sans: [
          '"Plus Jakarta Sans"',
          'Inter',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          'Roboto',
          'system-ui',
          'sans-serif',
        ],
      },
    },
  },
  plugins: [],
}
