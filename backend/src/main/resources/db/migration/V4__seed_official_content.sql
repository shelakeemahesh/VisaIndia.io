-- V4: Seed Authentic Official Government Visa Categories, Entry Points, Notices, FAQs and Demo Applications
-- Data sources: indianvisaonline.gov.in, boi.gov.in, mha.gov.in

-- 1. VISA CATEGORY DETAILS
INSERT INTO visa_category_detail (
    slug, name, sub_category_name, code, purpose_code, description, who_is_it_for,
    validity_text, validity_days, entries, max_stay_text, max_stay_days,
    fee_band_summary, processing_days_min, processing_days_max,
    is_extendable, is_convertible, required_documents, entry_point_limits,
    official_source_url, last_verified_at
) VALUES
(
    'e-tourist-30-days', 'e-Tourist Visa (30 Days)', 'e-Tourist Visa (01 Month)', 'ETV_30D', 'TOURISM',
    'Short-duration electronic tourist visa for holidaying, sightseeing, and casual visits.',
    'International travelers visiting India for recreation, sightseeing, yoga programs, or visiting family and friends for up to 30 days.',
    '30 days from date of first arrival in India', 30, 'DOUBLE',
    '30 continuous days per entry', 30,
    '$10 USD (April to June) / $25 USD (July to March)', 3, 4,
    FALSE, FALSE,
    '["Recent passport-size color photograph (350x350 to 1000x1000 px, 10KB-1MB, white background)", "Passport biographical page scan (PDF or JPEG, 10KB-300KB) containing min 6 months validity", "Confirmed onward/return travel ticket", "Proof of sufficient financial funds"]'::jsonb,
    'Entry permitted at 37 designated airports and 5 seaports. Exit permitted at all authorized ICPs.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-tourist-1-year', 'e-Tourist Visa (1 Year)', 'e-Tourist Visa (01 Year)', 'ETV_1Y', 'TOURISM',
    'Multiple-entry electronic tourist authorization valid for one full calendar year from the date of grant.',
    'Frequent leisure travelers and diaspora visiting relatives across multiple trips throughout the year.',
    '365 days from the date of Electronic Travel Authorization (ETA) issuance', 365, 'MULTIPLE',
    'Continuous stay during each visit shall not exceed 90 days (180 days for USA, UK, Japan, Canada)', 90,
    '$40 USD for most nationalities (reciprocal variations apply)', 3, 4,
    FALSE, FALSE,
    '["Recent color passport photograph (white background)", "Clear copy of passport biographical information page", "Sufficient financial means"]'::jsonb,
    'Entry at 37 designated international airports and 5 designated seaports.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-tourist-5-years', 'e-Tourist Visa (5 Years)', 'e-Tourist Visa (05 Years)', 'ETV_5Y', 'TOURISM',
    'Long-term multiple-entry electronic travel authorization valid for five years.',
    'Long-term international tourists and regular holidaymakers visiting India periodically over a 5-year span.',
    '5 calendar years from the date of ETA grant', 1825, 'MULTIPLE',
    'Max 90 continuous days per visit (180 days for nationals of USA, UK, Japan, and Canada)', 90,
    '$80 USD for most eligible nationalities', 3, 4,
    FALSE, FALSE,
    '["Recent color passport photo (white background)", "Color scan of original passport bio page", "Valid return or onward ticket"]'::jsonb,
    'Entry permitted at 37 designated airports and 5 designated seaports.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-business-visa', 'e-Business Visa', 'e-Business Visa (1 Year)', 'EBV_1Y', 'BUSINESS',
    'Multiple-entry electronic commercial visa covering corporate meetings, industrial ventures, and trade exhibitions.',
    'Business professionals attending commercial meetings, trade fairs, setting up ventures, recruiting staff, or delivering lectures under GIAN.',
    '365 days from the date of ETA grant', 365, 'MULTIPLE',
    'Continuous stay up to 180 days per calendar year. (FRRO registration required if cumulative stay exceeds 180 days)', 180,
    '$80 USD to $100 USD (UK $100 USD, USA $100 USD)', 3, 4,
    FALSE, FALSE,
    '["Recent passport-size color photograph", "Color copy of passport bio page", "Visiting card / Business card of applicant", "Invitation letter from Indian organization/firm with company letterhead and CIN"]'::jsonb,
    'Entry permitted at 37 designated airports and 5 designated seaports.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-medical-visa', 'e-Medical Visa', 'e-Medical Visa (60 Days)', 'EMV_60D', 'MEDICAL',
    'Triple-entry electronic visa granted for receiving specialized medical treatment in recognized Indian hospitals.',
    'Patients seeking medical treatment, advanced surgical procedures, or organ transplants at accredited super-specialty hospitals in India.',
    '60 days from the date of first arrival in India', 60, 'TRIPLE',
    'Up to 60 days per trip with triple entry permissions within validity window', 60,
    '$80 USD for most nationalities', 3, 4,
    TRUE, FALSE,
    '["Recent color photograph", "Passport biographical page scan", "Official letter from accredited hospital in India stating medical diagnosis and treatment schedule on hospital letterhead"]'::jsonb,
    'Entry permitted at 37 designated airports and 5 designated seaports.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-medical-attendant', 'e-Medical Attendant Visa', 'e-Medical Attendant Visa', 'EMAV_60D', 'MEDICAL',
    'Electronic visa issued to family members or caregivers accompanying an e-Medical visa holder.',
    'Up to two attendants/caregivers traveling together with the principal e-Medical patient.',
    'Co-terminus with the principal e-Medical Visa holder (60 days from arrival)', 60, 'TRIPLE',
    'Matches the principal patient’s authorized duration of stay', 60,
    '$80 USD per attendant', 3, 4,
    TRUE, FALSE,
    '["Color photo and passport bio page", "Copy of the principal e-Medical patient’s application reference or ETA approval letter"]'::jsonb,
    'Must travel with or immediately follow the principal medical patient.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-conference-visa', 'e-Conference Visa', 'e-Conference Visa (30 Days)', 'ECV_30D', 'CONFERENCE',
    'Single-entry electronic visa granted to international delegates attending seminars and government-cleared conferences.',
    'Academicians, scientists, industry delegates, and speakers invited to official workshops and international conferences held in India.',
    '30 days from date of arrival in India', 30, 'SINGLE',
    '30 continuous days', 30,
    '$80 USD for most nationalities', 3, 4,
    FALSE, FALSE,
    '["Passport photo and bio page", "Formal invitation letter from conference organizer in India", "Political Clearance from Ministry of External Affairs (MEA)", "Event Security Clearance from Ministry of Home Affairs (MHA)"]'::jsonb,
    'Entry permitted at 37 designated airports and 5 seaports.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
),
(
    'e-ayush-visa', 'e-Ayush Visa', 'e-Ayush Visa (Traditional Medicine)', 'EAV_1Y', 'MEDICAL',
    'Specialized electronic visa for receiving therapeutic care in Ayurveda, Yoga, Naturopathy, Unani, Siddha, and Homeopathy.',
    'International patients seeking holistic treatments at accredited Ayush hospitals and wellness institutions in India.',
    'Up to 1 year with multiple entries as per medical recommendation', 365, 'MULTIPLE',
    'Up to 180 days per visit', 180,
    '$80 USD', 3, 4,
    TRUE, FALSE,
    '["Passport bio page and photo", "Letter from NABH/accredited Ayush wellness center or hospital in India specifying treatment program"]'::jsonb,
    'Entry at 37 designated airports and 5 designated seaports.',
    'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()
);

-- 2. ENTRY POINTS (37 Designated Airports, 5 Major Seaports, Land Ports)
INSERT INTO entry_points (name, code, type, state, is_evisa_entry, is_evisa_exit, is_voa_allowed, official_source_url) VALUES
('Indira Gandhi International Airport, Delhi', 'DEL', 'AIRPORT', 'Delhi', TRUE, TRUE, TRUE, 'https://boi.gov.in/'),
('Chhatrapati Shivaji Maharaj International Airport, Mumbai', 'BOM', 'AIRPORT', 'Maharashtra', TRUE, TRUE, TRUE, 'https://boi.gov.in/'),
('Kempegowda International Airport, Bengaluru', 'BLR', 'AIRPORT', 'Karnataka', TRUE, TRUE, TRUE, 'https://boi.gov.in/'),
('Chennai International Airport, Chennai', 'MAA', 'AIRPORT', 'Tamil Nadu', TRUE, TRUE, TRUE, 'https://boi.gov.in/'),
('Netaji Subhash Chandra Bose International Airport, Kolkata', 'CCU', 'AIRPORT', 'West Bengal', TRUE, TRUE, TRUE, 'https://boi.gov.in/'),
('Rajiv Gandhi International Airport, Hyderabad', 'HYD', 'AIRPORT', 'Telangana', TRUE, TRUE, TRUE, 'https://boi.gov.in/'),
('Sardar Vallabhbhai Patel International Airport, Ahmedabad', 'AMD', 'AIRPORT', 'Gujarat', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Cochin International Airport, Kochi', 'COK', 'AIRPORT', 'Kerala', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Dabolim Airport, Goa', 'GOI', 'AIRPORT', 'Goa', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Manohar International Airport, Mopa, Goa', 'GOX', 'AIRPORT', 'Goa', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Sri Guru Ram Dass Jee International Airport, Amritsar', 'ATQ', 'AIRPORT', 'Punjab', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Jaipur International Airport, Jaipur', 'JAI', 'AIRPORT', 'Rajasthan', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Chaudhary Charan Singh International Airport, Lucknow', 'LKO', 'AIRPORT', 'Uttar Pradesh', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Trivandrum International Airport, Thiruvananthapuram', 'TRV', 'AIRPORT', 'Kerala', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Lal Bahadur Shastri Airport, Varanasi', 'VNS', 'AIRPORT', 'Uttar Pradesh', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Coimbatore International Airport, Coimbatore', 'CJB', 'AIRPORT', 'Tamil Nadu', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Lokpriya Gopinath Bordoloi International Airport, Guwahati', 'GAU', 'AIRPORT', 'Assam', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Mangaluru International Airport, Mangalore', 'IXE', 'AIRPORT', 'Karnataka', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Dr. Babasaheb Ambedkar International Airport, Nagpur', 'NAG', 'AIRPORT', 'Maharashtra', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Pune International Airport, Pune', 'PNQ', 'AIRPORT', 'Maharashtra', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Tiruchirappalli International Airport, Trichy', 'TRZ', 'AIRPORT', 'Tamil Nadu', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Visakhapatnam Airport, Visakhapatnam', 'VTZ', 'AIRPORT', 'Andhra Pradesh', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Chandigarh International Airport, Chandigarh', 'IXC', 'AIRPORT', 'Chandigarh', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Madurai Airport, Madurai', 'IXM', 'AIRPORT', 'Tamil Nadu', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Biju Patnaik International Airport, Bhubaneswar', 'BBI', 'AIRPORT', 'Odisha', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Veer Savarkar International Airport, Port Blair', 'IXZ', 'AIRPORT', 'Andaman and Nicobar', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Bagdogra Airport, Siliguri', 'IXB', 'AIRPORT', 'West Bengal', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Calicut International Airport, Kozhikode', 'CCJ', 'AIRPORT', 'Kerala', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Gaya Airport, Gaya', 'GAY', 'AIRPORT', 'Bihar', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Devi Ahilyabai Holkar Airport, Indore', 'IDR', 'AIRPORT', 'Madhya Pradesh', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Kannur International Airport, Kannur', 'CNN', 'AIRPORT', 'Kerala', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Surat International Airport, Surat', 'STV', 'AIRPORT', 'Gujarat', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Vijayawada International Airport, Vijayawada', 'VGA', 'AIRPORT', 'Andhra Pradesh', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Sheikh ul-Alam International Airport, Srinagar', 'SXR', 'AIRPORT', 'Jammu and Kashmir', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),

-- SEAPORTS (Designated for e-Visa cruises)
('Cochin Seaport, Kochi', 'INCOK', 'SEAPORT', 'Kerala', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Mormugao Seaport, Goa', 'INMRM', 'SEAPORT', 'Goa', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('New Mangalore Seaport, Mangaluru', 'INNML', 'SEAPORT', 'Karnataka', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Mumbai Seaport, Mumbai', 'INBOM', 'SEAPORT', 'Maharashtra', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),
('Chennai Seaport, Chennai', 'INMAA', 'SEAPORT', 'Tamil Nadu', TRUE, TRUE, FALSE, 'https://boi.gov.in/'),

-- LAND PORTS (Permitted for Exit with valid e-Visa)
('Attari Integrated Check Post, Wagah Border', 'ATTARI', 'LAND_ICP', 'Punjab', FALSE, TRUE, FALSE, 'https://boi.gov.in/'),
('Haridaspur / Petrapole Integrated Check Post', 'PETRAPOLE', 'LAND_ICP', 'West Bengal', FALSE, TRUE, FALSE, 'https://boi.gov.in/'),
('Raxaul Integrated Check Post', 'RAXAUL', 'LAND_ICP', 'Bihar', FALSE, TRUE, FALSE, 'https://boi.gov.in/');

-- 3. OFFICIAL NOTICES & POLICY ADVISORIES
INSERT INTO notices (notice_id, title, summary, body, category, severity, published_at, source_url, is_pinned_home) VALUES
(
    'NOT-2026-08',
    'Mandatory Pre-Arrival e-Arrival Card for All Incoming Foreign Nationals',
    'All international travelers and OCI cardholders must complete the digital e-Arrival registration before boarding flights to India.',
    'In accordance with Bureau of Immigration guidelines, the paper disembarkation card has been phased out. Incoming travelers must file their personal, flight, and baggage declaration electronically through the unified travel portal prior to embarkation. Verification QR is scanned at designated biometric e-gates upon landing.',
    'POLICY', 'CRITICAL', '2026-08-15 00:00:00+00', 'https://boi.gov.in/', TRUE
),
(
    'NOT-2026-05',
    'Visa on Arrival (VoA) Facility Active for Nationals of Japan, South Korea and UAE',
    'Eligible citizens can obtain double-entry VoA directly at 6 major international airports in India without prior electronic application.',
    'Citizens of Japan, Republic of Korea, and the United Arab Emirates (subject to conditions) may avail the Visa-on-Arrival facility for tourism, business, and medical visits up to 60 days. The facility is operational 24x7 at Delhi, Mumbai, Kolkata, Chennai, Bengaluru, and Hyderabad international airports for a fee of 2000 INR / $25 USD.',
    'ENTRY_POINTS', 'HIGH', '2026-05-10 00:00:00+00', 'https://indianvisaonline.gov.in/visa/visa-on-arrival.html', FALSE
),
(
    'NOT-2026-02',
    'Public Fraud Warning: Clone & Intermediary Scam Portals',
    'Government of India cautions travelers that official e-Visa applications are processed strictly via official government endpoints.',
    'Multiple third-party clone websites impersonating the official Indian e-Visa portal charge exorbitant exorbitant fees or compromise passport data. Travelers are advised that the authentic government domain is indianvisaonline.gov.in. VisaSetu operates exclusively as a concept demonstration and does not collect live fees on behalf of the Government.',
    'SYSTEM', 'NORMAL', '2026-02-01 00:00:00+00', 'https://indianvisaonline.gov.in/evisa/tvoa.html', FALSE
),
(
    'NOT-2026-01',
    'Yellow Fever & Oral Polio Health Declaration Advisory',
    'Mandatory Yellow Fever vaccination card required for passengers arriving from or transiting through endemic countries.',
    'Any traveler arriving in India within 6 days of departure from an endemic Yellow Fever zone without a valid international vaccination certificate is liable to quarantine for up to 6 days. Health declarations must be filed in advance under e-Suvidha.',
    'POLICY', 'NORMAL', '2026-01-15 00:00:00+00', 'https://www.mohfw.gov.in/', FALSE
);

-- 4. OFFICIAL FAQS (Grouped by 6 Topics)
INSERT INTO faqs (topic, question, answer, order_num, official_source_url, last_verified_at) VALUES
('BEFORE_APPLYING', 'Who is eligible to apply for an Indian e-Visa?', 'Nationals of 172+ eligible countries whose sole objective of visiting India is recreation, sightseeing, casual visit to meet friends or relatives, short duration medical treatment, traditional Ayush therapy, commercial business, or attending an authorized international conference. Passports must have at least 6 months validity from the date of arrival and at least 2 blank pages.', 1, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('BEFORE_APPLYING', 'How early can I submit an e-Visa application?', 'For the 30-day e-Tourist Visa, applications can be submitted between 30 days and 4 days prior to your arrival date. For 1-Year and 5-Year e-Tourist, e-Business, and e-Medical visas, applications can be submitted up to 120 days in advance of your expected travel date.', 2, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('APPLICATION', 'What are the exact specifications for the passport photograph?', 'The photograph must be recent, in color, in JPEG format between 10 KB and 1 MB. Dimensions must be equal (square, 1:1 aspect ratio) with minimum 350x350 pixels up to 1000x1000 pixels. The background must be pure white or light, with your full face centered covering 70% to 80% of the frame with no eyeglasses or shadows.', 3, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('APPLICATION', 'Can I change my port of arrival after my e-Visa is approved?', 'Yes. You may arrive at ANY of the 37 designated international airports or 5 designated seaports regardless of the port of arrival indicated on your application. You can exit from any authorized Indian Immigration Check Post (ICP).', 4, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('PAYMENT', 'What should I do if my bank account was debited but the application status still shows Pending?', 'VisaSetu includes an automatic payment reconciliation engine that queries the payment gateway every 5 minutes. You can also use the "Payment debited but not showing?" button on the Existing Applications page to trigger an instantaneous re-sync against the gateway.', 5, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('PAYMENT', 'How many payment attempts am I allowed before the application locks?', 'A maximum of 3 payment attempts are permitted per application. If all 3 attempts fail, you may regenerate a fresh checkout session with a new idempotency key on VisaSetu without having to re-enter your applicant profile.', 6, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('AFTER_APPROVAL', 'What document do I receive once my application is approved?', 'You will receive an Electronic Travel Authorization (ETA) grant letter via email and in your VisaSetu dashboard. You must print a physical copy of the ETA and carry it with you for travel and presentation at the airline check-in desk and Indian immigration.', 7, 'https://indianvisaonline.gov.in/evisa/tvoa.html', NOW()),
('ARRIVAL', 'What is the procedure upon landing at the airport in India?', 'Upon arrival, follow signs to the dedicated "e-Visa Counters". Hand over your passport, printed ETA letter, and e-Arrival QR pass. The immigration officer will capture your biometric fingerprints and facial photo, and stamp a physical visa endorsement in your passport.', 8, 'https://boi.gov.in/', NOW()),
('TECHNICAL', 'Can an e-Visa be extended or converted to another visa type while in India?', 'e-Visas are strictly non-extendable and non-convertible to any other visa type (such as Employment or Student Visa), except in rare emergencies requiring prolonged hospitalization certified by an accredited state medical board via the FRRO.', 9, 'https://boi.gov.in/', NOW());

-- 5. SEEDED DEMO APPLICATIONS ACROSS EVERY STATE
INSERT INTO demo_applications (
    reference_id, applicant_email, applicant_name, nationality, category_name,
    status, order_id, payment_id, amount, arrival_date, port_of_arrival, qr_data, created_at
) VALUES
(
    'VS-2026-8821', 'traveler.draft@example.com', 'Liam Henderson', 'GBR', 'e-Tourist Visa (1 Year)',
    'DRAFT', NULL, NULL, 40.00, '2026-11-20', 'Delhi (DEL)', NULL, NOW() - INTERVAL '1 hour'
),
(
    'VS-2026-7492', 'elena.rodriguez@example.com', 'Elena Rodriguez', 'ESP', 'e-Tourist Visa (30 Days)',
    'SUBMITTED', 'order_demo_7492_sub', NULL, 25.00, '2026-11-15', 'Mumbai (BOM)', NULL, NOW() - INTERVAL '3 hours'
),
(
    'VS-2026-6134', 'david.kim@example.com', 'David Min-Soo Kim', 'KOR', 'e-Business Visa',
    'PAYMENT_PENDING', 'order_demo_6134_pend', NULL, 80.00, '2026-12-01', 'Bengaluru (BLR)', NULL, NOW() - INTERVAL '15 minutes'
),
(
    'VS-2026-5509', 'akiko.tanaka@example.com', 'Akiko Tanaka', 'JPN', 'e-Tourist Visa (30 Days)',
    'UNDER_REVIEW', 'order_demo_5509_rev', 'pay_demo_5509_capture', 25.00, '2026-11-05', 'Delhi (DEL)', NULL, NOW() - INTERVAL '1 day'
),
(
    'VS-2026-4011', 'sarah.jenkins@example.com', 'Sarah Jane Jenkins', 'USA', 'e-Tourist Visa (5 Years)',
    'APPROVED', 'order_demo_4011_app', 'pay_demo_4011_capture', 80.00, '2026-10-25', 'Delhi (DEL)',
    'ETA:VS-2026-4011|NAME:SARAH JANE JENKINS|NAT:USA|CAT:ETOURIST_5Y|VALID_UNTIL:2031-10-25', NOW() - INTERVAL '2 days'
),
(
    'VS-2026-3199', 'marcus.dupont@example.com', 'Marcus Dupont', 'FRA', 'e-Conference Visa',
    'REJECTED', 'order_demo_3199_rej', 'pay_demo_3199_capture', 80.00, '2026-10-30', 'Hyderabad (HYD)', NULL, NOW() - INTERVAL '4 days'
),
(
    'VS-2026-2048', 'claudia.muller@example.com', 'Claudia Müller', 'DEU', 'e-Tourist Visa (1 Year)',
    'PAYMENT_FAILED', 'order_demo_2048_fail', NULL, 40.00, '2026-11-10', 'Cochin (COK)', NULL, NOW() - INTERVAL '6 hours'
);
