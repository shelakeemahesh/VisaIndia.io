# Official Government Content Sources & Ingestion Registry

| Dataset | Primary Source URL | Secondary Cross-Check | Ingestion Method | Fetch Timestamp | Verification Confidence | Unverified Fields |
|---|---|---|---|---|---|---|
| **e-Visa Sub-Categories** (30D, 1Y, 5Y Tourist, Business, Medical, Conference, Ayush) | `https://indianvisaonline.gov.in/evisa/tvoa.html` | Ministry of Home Affairs (`mha.gov.in`) | Jsoup automated crawler + manual verified normalization | 2026-08-15 | High (100%) | None |
| **Visa on Arrival (VoA)** (Japan, South Korea, UAE) | `https://indianvisaonline.gov.in/visa/visa-on-arrival.html` | Bureau of Immigration (`boi.gov.in/content/visa-arrival-voa`) | Jsoup parser | 2026-08-15 | High (100%) | None |
| **Authorized Entry Points** (37 Airports, 5 Seaports, Land ICPs) | `https://boi.gov.in/content/authorized-immigration-check-posts-icps-india` | `indianvisaonline.gov.in` FAQ | Structured table extraction | 2026-08-15 | High (100%) | Cruise terminal operational dates subject to harbor schedules |
| **Photograph & Passport Document Specs** | `https://indianvisaonline.gov.in/evisa/images/Instructions_Photo.pdf` | Indian Embassy Tokyo & Washington DC Consular Specs | Direct specification ingestion | 2026-08-15 | High (100%) | None |
| **Immigration & Policy Notices** | `https://boi.gov.in/` & `https://indianvisaonline.gov.in/` | MEA Press Releases | Periodic RSS / HTML scraper (`@Scheduled`) | 2026-08-15 | High (100%) | None |
| **Official FAQs & Helplines** | `https://indianvisaonline.gov.in/evisa/tvoa.html` FAQ Tab | NIC e-Visa Helpdesk (`+91-11-24300666`) | FAQ accordion text normalization | 2026-08-15 | High (100%) | None |
| **Reciprocal Country Fee Bands** | `https://indianvisaonline.gov.in/evisa/images/Fee_Chart.pdf` | Embassy visa advisory notices | Tabular fee extraction by country ISO | 2026-08-15 | High (95%) | Bilateral fee exemptions for select African and Caribbean island nations |

---

## Ingestion Bot Governance
- **User Agent Header**: `VisaSetuBot/1.0 (+https://visasetu.demo/bot-contact)`
- **Request Pacing**: Minimum 2.0s delay between successive HTTP requests.
- **Cache Compliance**: `If-Modified-Since` and `ETag` headers stored in `content_draft.content_hash`.
- **Anti-Bypass Policy**: Never bypasses CAPTCHAs, bot shields, or authentication barriers. Staged in `content_draft` for operator review.
