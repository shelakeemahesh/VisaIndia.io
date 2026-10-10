package com.visasetu.service;

import java.util.*;

public final class CountryRegistry {

    public record CountryInfo(String name, String flagEmoji) {}

    private static final Map<String, CountryInfo> REGISTRY = new HashMap<>();

    static {
        register("AFG", "Afghanistan", "🇦🇫");
        register("ALB", "Albania", "🇦🇱");
        register("DZA", "Algeria", "🇩🇿");
        register("AND", "Andorra", "🇦🇩");
        register("AGO", "Angola", "🇦🇴");
        register("ATG", "Antigua and Barbuda", "🇦🇬");
        register("ARG", "Argentina", "🇦🇷");
        register("ARM", "Armenia", "🇦🇲");
        register("AUS", "Australia", "🇦🇺");
        register("AUT", "Austria", "🇦🇹");
        register("AZE", "Azerbaijan", "🇦🇿");
        register("BHS", "Bahamas", "🇧🇸");
        register("BHR", "Bahrain", "🇧🇭");
        register("BGD", "Bangladesh", "🇧🇩");
        register("BRB", "Barbados", "🇧🇧");
        register("BLR", "Belarus", "🇧🇾");
        register("BEL", "Belgium", "🇧🇪");
        register("BLZ", "Belize", "🇧🇿");
        register("BEN", "Benin", "🇧🇯");
        register("BTN", "Bhutan", "🇧🇹");
        register("BOL", "Bolivia", "🇧🇴");
        register("BIH", "Bosnia and Herzegovina", "🇧🇦");
        register("BWA", "Botswana", "🇧🇼");
        register("BRA", "Brazil", "🇧🇷");
        register("BRN", "Brunei", "🇧🇳");
        register("BGR", "Bulgaria", "🇧🇬");
        register("BFA", "Burkina Faso", "🇧🇫");
        register("BDI", "Burundi", "🇧🇮");
        register("CPV", "Cabo Verde", "🇨🇻");
        register("KHM", "Cambodia", "🇰🇭");
        register("CMR", "Cameroon", "🇨🇲");
        register("CAN", "Canada", "🇨🇦");
        register("CAF", "Central African Republic", "🇨🇫");
        register("TCD", "Chad", "🇹🇩");
        register("CHL", "Chile", "🇨🇱");
        register("CHN", "China", "🇨🇳");
        register("COL", "Colombia", "🇨🇴");
        register("COM", "Comoros", "🇰🇲");
        register("COG", "Congo", "🇨🇬");
        register("COD", "Congo (DRC)", "🇨🇩");
        register("CRI", "Costa Rica", "🇨🇷");
        register("CIV", "Cote d'Ivoire", "🇨🇮");
        register("HRV", "Croatia", "🇭🇷");
        register("CUB", "Cuba", "🇨🇺");
        register("CYP", "Cyprus", "🇨🇾");
        register("CZE", "Czech Republic", "🇨🇿");
        register("DNK", "Denmark", "🇩🇰");
        register("DJI", "Djibouti", "🇩🇯");
        register("DMA", "Dominica", "🇩🇲");
        register("DOM", "Dominican Republic", "🇩🇴");
        register("ECU", "Ecuador", "🇪🇨");
        register("EGY", "Egypt", "🇪🇬");
        register("SLV", "El Salvador", "🇸🇻");
        register("GNQ", "Equatorial Guinea", "🇬🇶");
        register("ERI", "Eritrea", "🇪🇷");
        register("EST", "Estonia", "🇪🇪");
        register("SWZ", "Eswatini", "🇸🇿");
        register("ETH", "Ethiopia", "🇪🇹");
        register("FJI", "Fiji", "🇫🇯");
        register("FIN", "Finland", "🇫🇮");
        register("FRA", "France", "🇫🇷");
        register("GAB", "Gabon", "🇬🇦");
        register("GMB", "Gambia", "🇬🇲");
        register("GEO", "Georgia", "🇬🇪");
        register("DEU", "Germany", "🇩🇪");
        register("GHA", "Ghana", "🇬🇭");
        register("GRC", "Greece", "🇬🇷");
        register("GRD", "Grenada", "🇬🇩");
        register("GTM", "Guatemala", "🇬🇹");
        register("GIN", "Guinea", "🇬🇳");
        register("GNB", "Guinea-Bissau", "🇬🇼");
        register("GUY", "Guyana", "🇬🇾");
        register("HTI", "Haiti", "🇭🇹");
        register("HND", "Honduras", "🇭🇳");
        register("HKG", "Hong Kong", "🇭🇰");
        register("HUN", "Hungary", "🇭🇺");
        register("ISL", "Iceland", "🇮🇸");
        register("IND", "India", "🇮🇳");
        register("IDN", "Indonesia", "🇮🇩");
        register("IRN", "Iran", "🇮🇷");
        register("IRQ", "Iraq", "🇮🇶");
        register("IRL", "Ireland", "🇮🇪");
        register("ISR", "Israel", "🇮🇱");
        register("ITA", "Italy", "🇮🇹");
        register("JAM", "Jamaica", "🇯🇲");
        register("JPN", "Japan", "🇯🇵");
        register("JOR", "Jordan", "🇯🇴");
        register("KAZ", "Kazakhstan", "🇰🇿");
        register("KEN", "Kenya", "🇰🇪");
        register("KIR", "Kiribati", "🇰🇮");
        register("PRK", "North Korea", "🇰🇵");
        register("KOR", "South Korea", "🇰🇷");
        register("KWT", "Kuwait", "🇰🇼");
        register("KGZ", "Kyrgyzstan", "🇰🇬");
        register("LAO", "Laos", "🇱🇦");
        register("LVA", "Latvia", "🇱🇻");
        register("LBN", "Lebanon", "🇱🇧");
        register("LSO", "Lesotho", "🇱🇸");
        register("LBR", "Liberia", "🇱🇷");
        register("LBY", "Libya", "🇱🇾");
        register("LIE", "Liechtenstein", "🇱🇮");
        register("LTU", "Lithuania", "🇱🇹");
        register("LUX", "Luxembourg", "🇱🇺");
        register("MDG", "Madagascar", "🇲🇬");
        register("MWI", "Malawi", "🇲🇼");
        register("MYS", "Malaysia", "🇲🇾");
        register("MDV", "Maldives", "🇲🇻");
        register("MLI", "Mali", "🇲🇱");
        register("MLT", "Malta", "🇲🇹");
        register("MHL", "Marshall Islands", "🇲🇭");
        register("MRT", "Mauritania", "🇲🇷");
        register("MUS", "Mauritius", "🇲🇺");
        register("MEX", "Mexico", "🇲🇽");
        register("FSM", "Micronesia", "🇫🇲");
        register("MDA", "Moldova", "🇲🇩");
        register("MCO", "Monaco", "🇲🇨");
        register("MNG", "Mongolia", "🇲🇳");
        register("MNE", "Montenegro", "🇲🇪");
        register("MAR", "Morocco", "🇲🇦");
        register("MOZ", "Mozambique", "🇲🇿");
        register("MMR", "Myanmar", "🇲🇲");
        register("NAM", "Namibia", "🇳🇦");
        register("NRU", "Nauru", "🇳🇷");
        register("NPL", "Nepal", "🇳🇵");
        register("NLD", "Netherlands", "🇳🇱");
        register("NZL", "New Zealand", "🇳🇿");
        register("NIC", "Nicaragua", "🇳🇮");
        register("NER", "Niger", "🇳🇪");
        register("NGA", "Nigeria", "🇳🇬");
        register("MKD", "North Macedonia", "🇲🇰");
        register("NOR", "Norway", "🇳🇴");
        register("OMN", "Oman", "🇴🇲");
        register("PAK", "Pakistan", "🇵🇰");
        register("PLW", "Palau", "🇵🇼");
        register("PSE", "Palestine", "🇵🇸");
        register("PAN", "Panama", "🇵🇦");
        register("PNG", "Papua New Guinea", "🇵🇬");
        register("PRY", "Paraguay", "🇵🇾");
        register("PER", "Peru", "🇵🇪");
        register("PHL", "Philippines", "🇵🇭");
        register("POL", "Poland", "🇵🇱");
        register("PRT", "Portugal", "🇵🇹");
        register("QAT", "Qatar", "🇶🇦");
        register("ROU", "Romania", "🇷🇴");
        register("RUS", "Russia", "🇷🇺");
        register("RWA", "Rwanda", "🇷🇼");
        register("KNA", "Saint Kitts and Nevis", "🇰🇳");
        register("LCA", "Saint Lucia", "🇱🇨");
        register("VCT", "Saint Vincent and the Grenadines", "🇻🇨");
        register("WSM", "Samoa", "🇼🇸");
        register("SMR", "San Marino", "🇸🇲");
        register("STP", "Sao Tome and Principe", "🇸🇹");
        register("SAU", "Saudi Arabia", "🇸🇦");
        register("SEN", "Senegal", "🇸🇳");
        register("SRB", "Serbia", "🇷🇸");
        register("SYC", "Seychelles", "🇸🇨");
        register("SLE", "Sierra Leone", "🇸🇱");
        register("SGP", "Singapore", "🇸🇬");
        register("SVK", "Slovakia", "🇸🇰");
        register("SVN", "Slovenia", "🇸🇮");
        register("SLB", "Solomon Islands", "🇸🇧");
        register("SOM", "Somalia", "🇸🇴");
        register("ZAF", "South Africa", "🇿🇦");
        register("SSD", "South Sudan", "🇸🇸");
        register("ESP", "Spain", "🇪🇸");
        register("LKA", "Sri Lanka", "🇱🇰");
        register("SDN", "Sudan", "🇸🇩");
        register("SUR", "Suriname", "🇸🇷");
        register("SWE", "Sweden", "🇸🇪");
        register("CHE", "Switzerland", "🇨🇭");
        register("SYR", "Syria", "🇸🇾");
        register("TWN", "Taiwan", "🇹🇼");
        register("TJK", "Tajikistan", "🇹🇯");
        register("TZA", "Tanzania", "🇹🇿");
        register("THA", "Thailand", "🇹🇭");
        register("TLS", "Timor-Leste", "🇹🇱");
        register("TGO", "Togo", "🇹🇬");
        register("TON", "Tonga", "🇹🇴");
        register("TTO", "Trinidad and Tobago", "🇹🇹");
        register("TUN", "Tunisia", "🇹🇳");
        register("TUR", "Turkey", "🇹🇷");
        register("TKM", "Turkmenistan", "🇹🇲");
        register("TUV", "Tuvalu", "🇹🇻");
        register("UGA", "Uganda", "🇺🇬");
        register("UKR", "Ukraine", "🇺🇦");
        register("ARE", "United Arab Emirates", "🇦🇪");
        register("GBR", "United Kingdom", "🇬🇧");
        register("USA", "United States", "🇺🇸");
        register("URY", "Uruguay", "🇺🇾");
        register("UZB", "Uzbekistan", "🇺🇿");
        register("VUT", "Vanuatu", "🇻🇺");
        register("VAT", "Vatican City", "🇻🇦");
        register("VEN", "Venezuela", "🇻🇪");
        register("VNM", "Vietnam", "🇻🇳");
        register("YEM", "Yemen", "🇾🇪");
        register("ZMB", "Zambia", "🇿🇲");
        register("ZWE", "Zimbabwe", "🇿🇼");
    }

    private static void register(String code, String name, String flagEmoji) {
        REGISTRY.put(code.toUpperCase(), new CountryInfo(name, flagEmoji));
    }

    public static CountryInfo get(String code) {
        if (code == null) return new CountryInfo("Unknown", "🌐");
        return REGISTRY.getOrDefault(code.trim().toUpperCase(), new CountryInfo(code.trim().toUpperCase(), "🌐"));
    }

    public static Map<String, CountryInfo> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static String resolveIso3Code(String input) {
        if (input == null || input.isBlank()) return "USA";
        String clean = input.trim();
        if (REGISTRY.containsKey(clean.toUpperCase())) {
            return clean.toUpperCase();
        }
        for (Map.Entry<String, CountryInfo> entry : REGISTRY.entrySet()) {
            if (entry.getValue().name().equalsIgnoreCase(clean)) {
                return entry.getKey();
            }
        }
        return clean.length() == 3 ? clean.toUpperCase() : "USA";
    }

    private CountryRegistry() {}
}
