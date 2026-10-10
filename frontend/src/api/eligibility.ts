import { apiClient } from './client';

export interface RouteDto {
  id: string;
  routeType: 'VISA_FREE' | 'E_VISA' | 'VISA_ON_ARRIVAL' | 'EMBASSY_VISA';
  routeTypeName: string;
  title: string;
  description: string;
  fee: number;
  currency: string;
  processingDaysMin: number;
  processingDaysMax: number;
  processingTimeText: string;
  validityDays: number;
  maxStayDays: number;
  entries: string;
  allowedPorts: string[];
  documents: string[];
  sourceUrl: string;
  lastVerifiedAt: string;
  applyByDate: string | null;
  isRecommended: boolean;
  dataDisclaimer: string;
}

export interface EligibilityResponse {
  nationality: string;
  nationalityName: string;
  purpose: string;
  arrivalDate: string | null;
  routesFound: number;
  routes: RouteDto[];
  fallbackMessage: string | null;
  generalDisclaimer: string;
}

export interface NationalityOption {
  code: string;
  name: string;
  flagEmoji: string;
}

export interface PurposeOption {
  code: string;
  displayName: string;
}

export async function fetchNationalities(): Promise<NationalityOption[]> {
  const res = await apiClient.get<NationalityOption[]>('/eligibility/nationalities');
  return res.data;
}

export async function fetchPurposes(): Promise<PurposeOption[]> {
  const res = await apiClient.get<PurposeOption[]>('/eligibility/purposes');
  return res.data;
}

export async function checkEligibility(
  nationality: string,
  purpose: string,
  arrivalDate?: string
): Promise<EligibilityResponse> {
  const params: Record<string, string> = { nationality, purpose };
  if (arrivalDate) {
    params.arrivalDate = arrivalDate;
  }
  const res = await apiClient.get<EligibilityResponse>('/eligibility', { params });
  return res.data;
}
