-- V2: Seed Visa Rules
-- Sample data - verify with official source

INSERT INTO visa_rule (
    id, nationality, purpose, route_type, title, description, fee, currency,
    processing_days_min, processing_days_max, validity_days, max_stay_days,
    entries, allowed_ports, documents, source_url,
    last_verified_at, effective_from, effective_to, is_recommended
) VALUES
-- JAPAN (JPN) - TOURISM
(
    'a0000000-0000-0000-0000-000000000001', 'JPN', 'TOURISM', 'VISA_ON_ARRIVAL',
    'Visa on Arrival (VoA)',
    'Available for Japanese nationals arriving at 6 designated international airports. Sample data — verify with official source.',
    25.00, 'USD', 0, 1, 60, 60, 'DOUBLE',
    '["Delhi (DEL)", "Mumbai (BOM)", "Kolkata (CCU)", "Chennai (MAA)", "Bengaluru (BLR)", "Hyderabad (HYD)"]'::jsonb,
    '["Passport with minimum 6 months validity", "Confirmed return/onward flight ticket", "Sufficient financial means for stay", "Completed VoA application form"]'::jsonb,
    'https://indianvisaonline.gov.in/visa/visa-on-arrival.html',
    NOW(), '2024-01-01', NULL, FALSE
),
(
    'a0000000-0000-0000-0000-000000000002', 'JPN', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (30 Days)',
    'Fast electronic visa for recreation, sightseeing, and casual visits. Sample data — verify with official source.',
    25.00, 'USD', 3, 4, 30, 30, 'DOUBLE',
    '["29 Designated Airports (DEL, BOM, BLR, etc.)", "5 Major Seaports (Cochin, Goa, Mangalore, Mumbai, Chennai)"]'::jsonb,
    '["Recent front-facing color photo (JPEG, 10KB-1MB)", "Passport biographical page copy (PDF/JPEG, 10KB-300KB)"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),
(
    'a0000000-0000-0000-0000-000000000003', 'JPN', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (1 Year)',
    'Long-term electronic tourist visa allowing multiple entries throughout one year. Sample data — verify with official source.',
    40.00, 'USD', 3, 4, 365, 90, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Recent front-facing color photo", "Passport biographical page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, FALSE
),
(
    'a0000000-0000-0000-0000-000000000004', 'JPN', 'TOURISM', 'EMBASSY_VISA',
    'Regular Embassy Visa (Tourist)',
    'Physical sticker visa stamped at the Embassy of India in Tokyo or Consulate in Osaka. Sample data — verify with official source.',
    40.00, 'USD', 7, 10, 180, 90, 'MULTIPLE',
    '["All authorized Indian Immigration Check Posts (Airports, Seaports, Land Borders)"]'::jsonb,
    '["Physical passport with at least 2 blank pages", "Printed online application form with signature", "2 passport photos (5cm x 5cm)", "Proof of residential address in Japan"]'::jsonb,
    'https://www.indembassy-tokyo.gov.in/visa_services.html',
    NOW(), '2024-01-01', NULL, FALSE
),

-- JAPAN (JPN) - BUSINESS
(
    'a0000000-0000-0000-0000-000000000005', 'JPN', 'BUSINESS', 'E_VISA',
    'e-Business Visa (1 Year)',
    'Electronic visa for meetings, trade, exhibitions, and technical discussions. Sample data — verify with official source.',
    80.00, 'USD', 3, 4, 365, 180, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport bio page scan", "Recent photo", "Business card or company invitation letter"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),
(
    'a0000000-0000-0000-0000-000000000006', 'JPN', 'BUSINESS', 'EMBASSY_VISA',
    'Regular Embassy Business Visa',
    'Physical sticker visa for long-term commercial activities. Sample data — verify with official source.',
    120.00, 'USD', 7, 12, 365, 180, 'MULTIPLE',
    '["All authorized Indian Immigration Check Posts"]'::jsonb,
    '["Physical passport", "Invitation letter from Indian firm", "Letter of guarantee from sending Japanese firm"]'::jsonb,
    'https://www.indembassy-tokyo.gov.in/visa_services.html',
    NOW(), '2024-01-01', NULL, FALSE
),

-- UNITED STATES (USA) - TOURISM
(
    'a0000000-0000-0000-0000-000000000010', 'USA', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (30 Days)',
    'Short stay electronic tourist visa for US citizens. Sample data — verify with official source.',
    25.00, 'USD', 3, 4, 30, 30, 'DOUBLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, FALSE
),
(
    'a0000000-0000-0000-0000-000000000011', 'USA', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (1 Year)',
    '1-Year Multiple Entry electronic visa for US citizens. Sample data — verify with official source.',
    40.00, 'USD', 3, 4, 365, 180, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),
(
    'a0000000-0000-0000-0000-000000000012', 'USA', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (5 Years)',
    '5-Year Multiple Entry electronic visa for frequent US travelers. Sample data — verify with official source.',
    80.00, 'USD', 3, 4, 1825, 180, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, FALSE
),

-- UNITED KINGDOM (GBR) - TOURISM
(
    'a0000000-0000-0000-0000-000000000020', 'GBR', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (1 Year)',
    '1-Year Multiple Entry electronic tourist visa for UK citizens. Sample data — verify with official source.',
    40.00, 'USD', 3, 4, 365, 180, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),

-- GERMANY (DEU) - TOURISM
(
    'a0000000-0000-0000-0000-000000000030', 'DEU', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (1 Year)',
    'Electronic tourist visa for German citizens. Sample data — verify with official source.',
    40.00, 'USD', 3, 4, 365, 90, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),

-- FRANCE (FRA) - TOURISM
(
    'a0000000-0000-0000-0000-000000000040', 'FRA', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (1 Year)',
    'Electronic tourist visa for French citizens. Sample data — verify with official source.',
    40.00, 'USD', 3, 4, 365, 90, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),

-- SOUTH KOREA (KOR) - TOURISM (Also eligible for VoA!)
(
    'a0000000-0000-0000-0000-000000000050', 'KOR', 'TOURISM', 'VISA_ON_ARRIVAL',
    'Visa on Arrival (VoA)',
    'VoA facility for citizens of Republic of Korea at 6 designated airports. Sample data — verify with official source.',
    25.00, 'USD', 0, 1, 60, 60, 'DOUBLE',
    '["Delhi (DEL)", "Mumbai (BOM)", "Kolkata (CCU)", "Chennai (MAA)", "Bengaluru (BLR)", "Hyderabad (HYD)"]'::jsonb,
    '["Valid Passport (6+ months)", "Return ticket", "VoA Form"]'::jsonb,
    'https://indianvisaonline.gov.in/visa/visa-on-arrival.html',
    NOW(), '2024-01-01', NULL, FALSE
),
(
    'a0000000-0000-0000-0000-000000000051', 'KOR', 'TOURISM', 'E_VISA',
    'e-Tourist Visa (1 Year)',
    '1-Year Multiple Entry electronic tourist visa for South Korean citizens. Sample data — verify with official source.',
    40.00, 'USD', 3, 4, 365, 90, 'MULTIPLE',
    '["29 Designated Airports", "5 Major Seaports"]'::jsonb,
    '["Passport photo", "Passport bio page scan"]'::jsonb,
    'https://indianvisaonline.gov.in/evisa/tvoa.html',
    NOW(), '2024-01-01', NULL, TRUE
),

-- NEPAL (NPL) - TOURISM (VISA FREE)
(
    'a0000000-0000-0000-0000-000000000060', 'NPL', 'TOURISM', 'VISA_FREE',
    'Freedom of Movement (Visa-Free)',
    'Citizens of Nepal do not require a visa to enter India under the 1950 Indo-Nepal Treaty of Peace and Friendship. Sample data — verify with official source.',
    0.00, 'USD', 0, 0, 0, 0, 'FREEDOM_OF_MOVEMENT',
    '["All open land borders, airports, and seaports"]'::jsonb,
    '["Valid Nepali Passport or Election Commission Voter ID Card"]'::jsonb,
    'https://www.mea.gov.in',
    NOW(), '2024-01-01', NULL, TRUE
);
