package com.example.f1_kotlin.util

/**
 * Jolpica / Ergast nationality & country names → ISO 3166-1 alpha-2,
 * then regional-indicator emoji (e.g. `gb` → 🇬🇧).
 */
object CountryFlagCodes {
    fun resolve(raw: String?): String? {
        val key = raw?.trim()?.lowercase().orEmpty()
        if (key.isEmpty()) return null
        return CODES[key]
    }

    fun toEmoji(isoCode: String): String {
        val upper = isoCode.uppercase()
        if (upper.length != 2) return ""
        val base = 0x1F1E6
        return String(
            intArrayOf(
                base + upper[0].code - 0x41,
                base + upper[1].code - 0x41,
            ),
            0,
            2,
        )
    }

    fun emojiFor(raw: String?): String? {
        val code = resolve(raw) ?: return null
        val emoji = toEmoji(code)
        return emoji.takeIf { it.isNotEmpty() }
    }

    private val CODES = mapOf(
        // Nationalities
        "american" to "us",
        "argentine" to "ar",
        "australian" to "au",
        "austrian" to "at",
        "azerbaijani" to "az",
        "bahraini" to "bh",
        "belgian" to "be",
        "brazilian" to "br",
        "british" to "gb",
        "canadian" to "ca",
        "chinese" to "cn",
        "colombian" to "co",
        "czech" to "cz",
        "danish" to "dk",
        "dutch" to "nl",
        "east german" to "de",
        "emirati" to "ae",
        "finnish" to "fi",
        "french" to "fr",
        "german" to "de",
        "hungarian" to "hu",
        "indian" to "in",
        "indonesian" to "id",
        "irish" to "ie",
        "italian" to "it",
        "japanese" to "jp",
        "korean" to "kr",
        "liechtensteiner" to "li",
        "malaysian" to "my",
        "mexican" to "mx",
        "monegasque" to "mc",
        "moroccan" to "ma",
        "new zealander" to "nz",
        "polish" to "pl",
        "portuguese" to "pt",
        "qatari" to "qa",
        "rhodesian" to "zw",
        "russian" to "ru",
        "saudi" to "sa",
        "singaporean" to "sg",
        "south african" to "za",
        "spanish" to "es",
        "swedish" to "se",
        "swiss" to "ch",
        "thai" to "th",
        "turkish" to "tr",
        "uruguayan" to "uy",
        "venezuelan" to "ve",
        // Countries
        "argentina" to "ar",
        "australia" to "au",
        "austria" to "at",
        "azerbaijan" to "az",
        "bahrain" to "bh",
        "belgium" to "be",
        "brazil" to "br",
        "britain" to "gb",
        "canada" to "ca",
        "china" to "cn",
        "england" to "gb",
        "finland" to "fi",
        "france" to "fr",
        "germany" to "de",
        "great britain" to "gb",
        "hungary" to "hu",
        "india" to "in",
        "italy" to "it",
        "japan" to "jp",
        "korea" to "kr",
        "malaysia" to "my",
        "mexico" to "mx",
        "monaco" to "mc",
        "morocco" to "ma",
        "netherlands" to "nl",
        "new zealand" to "nz",
        "portugal" to "pt",
        "qatar" to "qa",
        "russia" to "ru",
        "saudi arabia" to "sa",
        "singapore" to "sg",
        "south africa" to "za",
        "spain" to "es",
        "sweden" to "se",
        "switzerland" to "ch",
        "thailand" to "th",
        "turkey" to "tr",
        "uae" to "ae",
        "united arab emirates" to "ae",
        "uk" to "gb",
        "united kingdom" to "gb",
        "usa" to "us",
        "united states" to "us",
    )
}
