import { apiClient } from './client';

export interface VisaCategoryDetailDto {
  id: string;
  slug: string;
  name: string;
  subCategoryName: string;
  code: string;
  purposeCode: string;
  description: string;
  whoIsItFor: string;
  validityText: string;
  validityDays: number;
  entries: string;
  maxStayText: string;
  maxStayDays: number;
  feeBandSummary: string;
  processingDaysMin: number;
  processingDaysMax: number;
  isExtendable: boolean;
  isConvertible: boolean;
  requiredDocuments: string[];
  entryPointLimits: string;
  officialSourceUrl: string;
  lastVerifiedAt: string;
}

export interface EntryPointDto {
  id: string;
  name: string;
  code: string;
  type: 'AIRPORT' | 'SEAPORT' | 'LAND_ICP';
  state: string;
  isEvisaEntry: boolean;
  isEvisaExit: boolean;
  isVoaAllowed: boolean;
  officialSourceUrl: string;
}

export interface NoticeDto {
  id: string;
  noticeId: string;
  title: string;
  summary: string;
  body: string;
  category: string;
  severity: 'CRITICAL' | 'HIGH' | 'NORMAL' | 'LOW';
  publishedAt: string;
  sourceUrl: string;
  isPinnedHome: boolean;
}

export interface FaqDto {
  id: string;
  topic: string;
  question: string;
  answer: string;
  orderNum: number;
  officialSourceUrl: string;
  lastVerifiedAt: string;
}

export interface ApplicationTimelineStep {
  step: string;
  label: string;
  state: 'COMPLETED' | 'CURRENT' | 'PENDING' | 'FAILED';
  description: string;
  timestamp: string | null;
}

export interface TrackApplicationResponse {
  referenceId: string;
  applicantName: string;
  maskedEmail: string;
  nationality: string;
  categoryName: string;
  status: 'DRAFT' | 'SUBMITTED' | 'PAYMENT_PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'PAYMENT_FAILED';
  orderId?: string;
  paymentId?: string;
  amount?: number;
  arrivalDate?: string;
  portOfArrival?: string;
  qrData?: string;
  createdAt: string;
  updatedAt: string;
  timeline: ApplicationTimelineStep[];
}

export interface HelpTicketPayload {
  category: string;
  subject: string;
  description: string;
  email: string;
  referenceId?: string;
}

export interface HelpTicketResponse {
  ticketId: string;
  status: string;
  message: string;
  receivedAt: string;
  category: string;
}

export async function fetchVisaCategories(purpose?: string): Promise<VisaCategoryDetailDto[]> {
  const params = purpose ? { purpose } : {};
  const res = await apiClient.get<VisaCategoryDetailDto[]>('/content/categories', { params });
  return res.data;
}

export async function fetchVisaCategoryBySlug(slug: string): Promise<VisaCategoryDetailDto> {
  const res = await apiClient.get<VisaCategoryDetailDto>(`/content/categories/${slug}`);
  return res.data;
}

export async function fetchEntryPoints(type?: string, state?: string): Promise<EntryPointDto[]> {
  const params: Record<string, string> = {};
  if (type) params.type = type;
  if (state) params.state = state;
  const res = await apiClient.get<EntryPointDto[]>('/content/entry-points', { params });
  return res.data;
}

export async function fetchNotices(category?: string, pinnedOnly?: boolean): Promise<NoticeDto[]> {
  const params: Record<string, any> = {};
  if (category) params.category = category;
  if (pinnedOnly) params.pinnedOnly = true;
  const res = await apiClient.get<NoticeDto[]>('/content/notices', { params });
  return res.data;
}

export async function fetchNoticeById(id: string): Promise<NoticeDto> {
  const res = await apiClient.get<NoticeDto>(`/content/notices/${id}`);
  return res.data;
}

export async function fetchFaqs(topic?: string, query?: string): Promise<FaqDto[]> {
  const params: Record<string, string> = {};
  if (topic) params.topic = topic;
  if (query) params.query = query;
  const res = await apiClient.get<FaqDto[]>('/content/faqs', { params });
  return res.data;
}

export async function trackApplication(
  referenceId: string,
  email: string,
  otp?: string
): Promise<any> {
  const res = await apiClient.post('/applications/track', { referenceId, email, otp });
  return res.data;
}

export async function resyncPayment(referenceId: string): Promise<any> {
  const res = await apiClient.post('/applications/resync-payment', { referenceId });
  return res.data;
}

export async function submitHelpTicket(payload: HelpTicketPayload): Promise<HelpTicketResponse> {
  const res = await apiClient.post<HelpTicketResponse>('/help/report-problem', payload);
  return res.data;
}
