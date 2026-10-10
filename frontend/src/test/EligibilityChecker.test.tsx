import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { EligibilityChecker } from '../components/EligibilityChecker';
import * as api from '../api/eligibility';

// Mock the API module
vi.mock('../api/eligibility', () => ({
  fetchNationalities: vi.fn(),
  fetchPurposes: vi.fn(),
  checkEligibility: vi.fn(),
}));

describe('EligibilityChecker Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(api.fetchNationalities).mockResolvedValue([
      { code: 'JPN', name: 'Japan', flagEmoji: '🇯🇵' },
      { code: 'USA', name: 'United States', flagEmoji: '🇺🇸' },
    ]);

    vi.mocked(api.fetchPurposes).mockResolvedValue([
      { code: 'TOURISM', displayName: 'Tourism & Recreation' },
      { code: 'BUSINESS', displayName: 'Business & Commerce' },
    ]);
  });

  it('renders and lists all routes including Visa on Arrival for Japan + Tourism', async () => {
    const mockResponse: api.EligibilityResponse = {
      nationality: 'JPN',
      nationalityName: 'Japan',
      purpose: 'TOURISM',
      arrivalDate: '2026-11-01',
      routesFound: 2,
      routes: [
        {
          id: 'route-1',
          routeType: 'VISA_ON_ARRIVAL',
          routeTypeName: 'Visa on Arrival (VoA)',
          title: 'Visa on Arrival (VoA)',
          description: 'Available for Japanese nationals at 6 designated ports.',
          fee: 25,
          currency: 'USD',
          processingDaysMin: 0,
          processingDaysMax: 1,
          processingTimeText: 'Instant at designated port (0–1 days)',
          validityDays: 60,
          maxStayDays: 60,
          entries: 'DOUBLE',
          allowedPorts: ['Delhi (DEL)', 'Mumbai (BOM)'],
          documents: ['Passport valid for 6 months', 'Return ticket'],
          sourceUrl: 'https://indianvisaonline.gov.in',
          lastVerifiedAt: '2026-10-01T00:00:00Z',
          applyByDate: null,
          isRecommended: false,
          dataDisclaimer: 'Sample data — verify with official source',
        },
        {
          id: 'route-2',
          routeType: 'E_VISA',
          routeTypeName: 'Electronic Visa (e-Visa)',
          title: 'e-Tourist Visa (30 Days)',
          description: 'Fast electronic visa.',
          fee: 25,
          currency: 'USD',
          processingDaysMin: 3,
          processingDaysMax: 4,
          processingTimeText: '3–4 business days',
          validityDays: 30,
          maxStayDays: 30,
          entries: 'DOUBLE',
          allowedPorts: ['29 Airports'],
          documents: ['Passport bio page', 'Photo'],
          sourceUrl: 'https://indianvisaonline.gov.in',
          lastVerifiedAt: '2026-10-01T00:00:00Z',
          applyByDate: '2026-10-27',
          isRecommended: true,
          dataDisclaimer: 'Sample data — verify with official source',
        },
      ],
      fallbackMessage: null,
      generalDisclaimer: 'Concept project — not affiliated with the Government of India.',
    };

    vi.mocked(api.checkEligibility).mockResolvedValue(mockResponse);

    render(<EligibilityChecker />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByTestId('route-card-visa_on_arrival')).toBeInTheDocument();
      expect(screen.getByTestId('route-card-e_visa')).toBeInTheDocument();
    });

    // Check VoA specifics
    expect(screen.getAllByText('Visa on Arrival (VoA)').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Instant at designated port (0–1 days)')).toBeInTheDocument();

    // Check e-Visa specifics
    expect(screen.getByText('e-Tourist Visa (30 Days)')).toBeInTheDocument();
    expect(screen.getByText('3–4 business days')).toBeInTheDocument();
    expect(screen.getByText('Recommended')).toBeInTheDocument();
  });

  it('renders embassy fallback when no route is found', async () => {
    const mockEmptyResponse: api.EligibilityResponse = {
      nationality: 'XYZ',
      nationalityName: 'XYZ',
      purpose: 'TOURISM',
      arrivalDate: null,
      routesFound: 0,
      routes: [],
      fallbackMessage: 'No direct route found. Please contact the nearest Indian Embassy or Consulate.',
      generalDisclaimer: 'Sample data — verify with official source.',
    };

    vi.mocked(api.checkEligibility).mockResolvedValue(mockEmptyResponse);

    render(<EligibilityChecker />);

    await waitFor(() => {
      expect(screen.getByText(/Embassy Route Required/i)).toBeInTheDocument();
      expect(screen.getByText(/Please contact the nearest Indian Embassy or Consulate/i)).toBeInTheDocument();
    });
  });
});
