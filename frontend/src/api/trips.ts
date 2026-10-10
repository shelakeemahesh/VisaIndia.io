import { apiClient } from './client';

export interface TripDraftDto {
  draftId: string;
  referenceId: string;
  applicantEmail: string;
  currentStep: number;
  sharedProfileJson: string;
  modulesJson: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface SharedProfileData {
  givenName?: string;
  surname?: string;
  email?: string;
  phone?: string;
  dob?: string;
  gender?: string;
  nationality?: string;
  passportNumber?: string;
  passportExpiry?: string;
}

export interface TripModulesData {
  eVisa?: {
    categoryName?: string;
    stayDuration?: string;
    arrivalDate?: string;
    portOfArrival?: string;
    previousVisit?: boolean;
  };
  eArrival?: {
    flightNumber?: string;
    airlineName?: string;
    hotelAddress?: string;
    cityInIndia?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
  };
  eSuvidha?: {
    healthDeclarationAccepted?: boolean;
    yellowFeverEndemicVisit?: boolean;
    feverSymptomFree?: boolean;
  };
}

export interface SubmitTripResponse {
  tripId: string;
  referenceId: string;
  status: string;
  amount: number;
  currency: string;
  applicantName: string;
  modules: Array<{
    id: string;
    moduleType: string;
    status: string;
  }>;
}

export interface PaymentOrderDto {
  orderId: string;
  amount: number;
  currency: string;
  status: string;
  idempotencyKey: string;
  razorpayKeyId: string;
  referenceId: string;
}

export interface TripDetailResponse {
  trip: {
    id: string;
    referenceId: string;
    applicantEmail: string;
    applicantName: string;
    nationality: string;
    categoryName: string;
    status: string;
    totalAmount: number;
    currency: string;
    arrivalDate: string;
    portOfArrival: string;
    etaNumber: string | null;
    qrToken: string | null;
    version: number;
    createdAt: string;
    updatedAt: string;
    submittedAt: string;
  };
  modules: Array<{
    id: string;
    moduleType: string;
    status: string;
    formDataJson: string;
  }>;
}

const LOCAL_STORAGE_KEY = 'visasetu_active_draft_v1';

export function getOfflineDraft(): { draftId?: string; version?: number; profile?: SharedProfileData; modules?: TripModulesData } | null {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveOfflineDraft(data: { draftId?: string; version?: number; profile?: SharedProfileData; modules?: TripModulesData }): void {
  try {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
  } catch {
    // Local storage quota or security error ignored
  }
}

export function clearOfflineDraft(): void {
  try {
    localStorage.removeItem(LOCAL_STORAGE_KEY);
  } catch {
    // Ignore
  }
}

/**
 * Initialize or resume draft for applicant email.
 */
export async function createOrGetDraft(
  email: string,
  nationality = 'JPN',
  categoryName = 'e-Tourist Visa (30 Days)'
): Promise<TripDraftDto> {
  const res = await apiClient.post<TripDraftDto>('/trips/drafts', {
    email: email.trim().toLowerCase(),
    nationality,
    categoryName,
  });
  return res.data;
}

/**
 * Autosave draft with version checking (optimistic locking).
 */
export async function autosaveDraft(
  draftId: string,
  version: number,
  profile: SharedProfileData,
  modules: TripModulesData,
  step: number
): Promise<TripDraftDto> {
  const res = await apiClient.put<TripDraftDto>(`/trips/drafts/${draftId}`, {
    version,
    sharedProfileJson: JSON.stringify(profile),
    modulesJson: JSON.stringify(modules),
    step,
  });
  // Mirror locally for offline resilience
  saveOfflineDraft({ draftId, version: res.data.version, profile, modules });
  return res.data;
}

/**
 * Submit draft atomically into persistent Trip application.
 */
export async function submitDraft(
  draftId: string,
  version: number,
  applicantName: string,
  arrivalDate: string,
  port: string
): Promise<SubmitTripResponse> {
  const res = await apiClient.post<SubmitTripResponse>(`/trips/drafts/${draftId}/submit`, {
    version,
    applicantName,
    arrivalDate,
    port,
  });
  clearOfflineDraft();
  return res.data;
}

/**
 * Fetch application details by reference ID.
 */
export async function getTripByReference(referenceId: string): Promise<TripDetailResponse> {
  const res = await apiClient.get<TripDetailResponse>(`/trips/${referenceId.trim()}`);
  return res.data;
}

/**
 * Initialize idempotent Razorpay payment order.
 */
export async function createPaymentOrder(
  referenceId: string,
  idempotencyKey = crypto.randomUUID()
): Promise<PaymentOrderDto> {
  const res = await apiClient.post<PaymentOrderDto>(
    '/payments/create-order',
    { referenceId: referenceId.trim() },
    {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
    }
  );
  return res.data;
}

/**
 * Reconcile / force re-sync payment state.
 */
export async function resyncPayment(referenceId: string): Promise<{
  status: string;
  referenceId: string;
  newTripStatus: string;
  message: string;
}> {
  const res = await apiClient.post('/payments/resync', {
    referenceId: referenceId.trim(),
  });
  return res.data;
}

/**
 * Helper URLs for document downloads
 */
export function getEtaPdfDownloadUrl(referenceId: string): string {
  const base = apiClient.defaults.baseURL || 'http://localhost:8080/api/v1';
  return `${base}/documents/${encodeURIComponent(referenceId)}/eta-pdf`;
}

export function getQrPassImageUrl(referenceId: string): string {
  const base = apiClient.defaults.baseURL || 'http://localhost:8080/api/v1';
  return `${base}/documents/${encodeURIComponent(referenceId)}/qr-pass`;
}
