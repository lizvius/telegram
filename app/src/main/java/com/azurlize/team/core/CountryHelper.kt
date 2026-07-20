package com.azurlize.team.core

data class Country(
    val name: String,
    val dialCode: String, // e.g. "+62"
    val flagEmoji: String, // e.g. "🇮🇩"
    val code: String // ISO 2-letter code e.g. "ID"
)

object CountryHelper {
    val countries = listOf(
        Country("Indonesia", "+62", "🇮🇩", "ID"),
        Country("Malaysia", "+60", "🇲🇾", "MY"),
        Country("Singapore", "+65", "🇸🇬", "SG"),
        Country("United States", "+1", "🇺🇸", "US"),
        Country("United Kingdom", "+44", "🇬🇧", "GB"),
        Country("India", "+91", "🇮🇳", "IN"),
        Country("Saudi Arabia", "+966", "🇸🇦", "SA"),
        Country("Australia", "+61", "🇦🇺", "AU"),
        Country("Japan", "+81", "🇯🇵", "JP"),
        Country("South Korea", "+82", "🇰🇷", "KR"),
        Country("Germany", "+49", "🇩🇪", "DE"),
        Country("France", "+33", "🇫🇷", "FR"),
        Country("Canada", "+1", "🇨🇦", "CA"),
        Country("Australia", "+61", "🇦🇺", "AU"),
        Country("Philippines", "+63", "🇵🇭", "PH"),
        Country("Thailand", "+66", "🇹🇭", "TH"),
        Country("Vietnam", "+84", "🇻🇳", "VN"),
        Country("United Arab Emirates", "+971", "🇦🇪", "AE"),
        Country("Brazil", "+55", "🇧🇷", "BR"),
        Country("Russia", "+7", "🇷🇺", "RU"),
        Country("China", "+86", "🇨🇳", "CN"),
        Country("Turkey", "+90", "🇹🇷", "TR"),
        Country("Bangladesh", "+880", "🇧🇩", "BD"),
        Country("Pakistan", "+92", "🇵🇰", "PK"),
        Country("Egypt", "+20", "🇪🇬", "EG"),
        Country("Netherlands", "+31", "🇳🇱", "NL"),
        Country("Spain", "+34", "🇪🇸", "ES"),
        Country("Italy", "+39", "🇮🇹", "IT"),
        Country("South Africa", "+27", "🇿🇦", "ZA"),
        Country("Ukraine", "+380", "🇺🇦", "UA"),
        Country("Mexico", "+52", "🇲🇽", "MX"),
        Country("Nigeria", "+234", "🇳🇬", "NG"),
        Country("New Zealand", "+64", "🇳🇿", "NZ"),
        Country("Switzerland", "+41", "🇨🇭", "CH"),
        Country("Sweden", "+46", "🇸🇪", "SE"),
        Country("Norway", "+47", "🇳🇴", "NO"),
        Country("Poland", "+48", "🇵🇱", "PL"),
        Country("Belgium", "+32", "🇧🇪", "BE"),
        Country("Austria", "+43", "🇦🇹", "AT"),
        Country("Hong Kong", "+852", "🇭🇰", "HK"),
        Country("Taiwan", "+886", "🇹🇼", "TW")
    ).sortedBy { it.name }

    /**
     * Detects the country from a typed phone number string.
     * Checks starting from the longest dial code prefix down to shortest.
     */
    fun detectCountry(phoneNumber: String): Country? {
        val cleanPhone = phoneNumber.replace(" ", "").replace("-", "")
        if (!cleanPhone.startsWith("+")) return null
        
        // Sort dialCodes descending by length so e.g. "+852" is checked before "+8"
        val sortedByDialLength = countries.sortedByDescending { it.dialCode.length }
        for (country in sortedByDialLength) {
            if (cleanPhone.startsWith(country.dialCode)) {
                return country
            }
        }
        return null
    }
}
