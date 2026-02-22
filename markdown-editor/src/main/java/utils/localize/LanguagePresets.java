package utils.localize;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class LanguagePresets {
	public enum LanguageInfoKey {
		LOCALE("locale"),
		LANGUAGE_ENGLISH("language_english"),
		LANGUAGE_NATIVE("language_native"),
		COUNTRY("country"),
		CURRENCY("currency"),
		TIMEZONE("timezone"),
		DATE_FORMAT("date_format"),
		TIME_FORMAT("time_format"),
		DECIMAL_SEPARATOR("decimal_separator"),
		THOUSANDS_SEPARATOR("thousands_separator");

		private final String key;

		LanguageInfoKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}
	}

	/**
	 * Represents a comprehensive set of information for a specific language and region (locale).
	 * <p>
	 * This record bundles together not only the locale code and display names but also common regional settings like currency, date/time formats, and number separators. It also
	 * includes a crucial flag to indicate the level of localization support within the core JavaFX framework itself.
	 *
	 * @param localeCode          The unique identifier for the locale, e.g., "de_DE" or "en_US".
	 * @param languageInEnglish   The name of the language in English, e.g., "German".
	 * @param languageInNative    The native name of the language, e.g., "Deutsch".
	 * @param country             The name of the primary country where the language is spoken.
	 * @param currency            The ISO 4217 currency code, e.g., "EUR".
	 * @param timezone            The IANA time zone identifier, e.g., "Europe/Berlin".
	 * @param dateFormat          A common date format string for the locale.
	 * @param timeFormat          A common time format string for the locale.
	 * @param decimalSeparator    The character used for the decimal point.
	 * @param thousandsSeparator  The character used for separating thousands.
	 * @param isFxSystemSupported Indicates whether the JavaFX runtime provides complete, built-in translations for this locale. While your application can be fully localized,
	 *                            certain standard JavaFX controls and dialogs (e.g., {@link javafx.scene.control.ColorPicker}, {@link javafx.stage.FileChooser}) have internal,
	 *                            hard-coded text. If a language is not supported by the JavaFX runtime itself, these specific UI elements will fall back to English. This flag
	 *                            allows you to inform the user about this potential inconsistency.
	 */
	public static record LanguageInfo(String localeCode, String languageInEnglish, String languageInNative, String country, String currency, // Währung (z. B. "EUR" für Euro)
	        String timezone, // Zeitzone (z. B. "Europe/Berlin")
	        String dateFormat, // Datumsformat (z. B. "dd.MM.yyyy")
	        String timeFormat, // Zeitformat (z. B. "HH:mm:ss")
	        String decimalSeparator, // Dezimaltrennzeichen (z. B. ",")
	        String thousandsSeparator, // Tausendertrennzeichen (z. B. ".")
	        boolean isFxSystemSupported // Das wichtige Feld
	) {

		public Locale getLocaleConstruct() {
			return constructLocale(localeCode);
		}

		/**
		 * Checks if the JavaFX runtime provides complete, built-in translations for this language's locale.
		 * <p>
		 * While an application can be localized using its own resource bundles, some standard JavaFX controls contain their own internal text that is only available for a limited
		 * set of languages. For example, the buttons in a {@link javafx.scene.control.ColorPicker} or a standard {@link javafx.scene.control.Alert} dialog are translated by the
		 * JavaFX framework itself.
		 * <p>
		 * If this method returns {@code false}, it indicates that some of these system-level UI elements may appear in English, even if the rest of the application is correctly
		 * translated into the selected language.
		 *
		 * @return {@code true} if standard JavaFX controls and dialogs are fully localized for this locale, {@code false} if they might fall back to English.
		 */
		public boolean isFxSystemSupported() {
			return isFxSystemSupported;
		}
	}

	public static final Map<String, LanguageInfo> LANGUAGE_MAP = new HashMap<>();

	static {
		// Europa
		LANGUAGE_MAP.put("bs_BA", new LanguageInfo("bs_BA", "Bosnian", "Bosanski", "Bosnia and Herzegovina", "BAM", "Europe/Sarajevo", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("da_DK", new LanguageInfo("da_DK", "Danish", "Dansk", "Denmark", "DKK", "Europe/Copenhagen", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("de_DE", new LanguageInfo("de_DE", "German", "Deutsch", "Germany", "EUR", "Europe/Berlin", "dd.MM.yyyy", "HH:mm:ss", ",", ".", true));
		LANGUAGE_MAP.put("en_GB", new LanguageInfo("en_GB", "English (UK)", "English", "United Kingdom", "GBP", "Europe/London", "dd/MM/yyyy", "HH:mm", ".", ",", true));
		LANGUAGE_MAP.put("en_US", new LanguageInfo("en_US", "English (US)", "English", "United States", "USD", "America/New_York", "MM/dd/yyyy", "h:mm a", ".", ",", true));
		LANGUAGE_MAP.put("es_ES", new LanguageInfo("es_ES", "Spanish", "Español", "Spain", "EUR", "Europe/Madrid", "dd/MM/yyyy", "HH:mm:ss", ",", ".", true));
		LANGUAGE_MAP.put("fi_FI", new LanguageInfo("fi_FI", "Finnish", "Suomi", "Finland", "EUR", "Europe/Helsinki", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("fr_FR", new LanguageInfo("fr_FR", "French", "Français", "France", "EUR", "Europe/Paris", "dd/MM/yyyy", "HH:mm:ss", ",", " ", true));
		LANGUAGE_MAP.put("hr_HR", new LanguageInfo("hr_HR", "Croatian", "Hrvatski", "Croatia", "HRK", "Europe/Zagreb", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("hu_HU", new LanguageInfo("hu_HU", "Hungarian", "Magyar", "Hungary", "HUF", "Europe/Budapest", "yyyy.MM.dd", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("it_IT", new LanguageInfo("it_IT", "Italian", "Italiano", "Italy", "EUR", "Europe/Rome", "dd/MM/yyyy", "HH:mm:ss", ",", ".", true));
		LANGUAGE_MAP.put("pl_PL", new LanguageInfo("pl_PL", "Polish", "Polski", "Poland", "PLN", "Europe/Warsaw", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("ru_RU", new LanguageInfo("ru_RU", "Russian", "Русский", "Russia", "RUB", "Europe/Moscow", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("sv_SE", new LanguageInfo("sv_SE", "Swedish", "Svenska", "Sweden", "SEK", "Europe/Stockholm", "yyyy-MM-dd", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("no_NO", new LanguageInfo("no_NO", "Norwegian", "Norsk", "Norway", "NOK", "Europe/Oslo", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("nl_NL", new LanguageInfo("nl_NL", "Dutch", "Nederlands", "Netherlands", "EUR", "Europe/Amsterdam", "dd-MM-yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("pt_BR", new LanguageInfo("pt_BR", "Portuguese", "Português", "Brazil", "BRL", "America/Sao_Paulo", "dd/MM/yyyy", "HH:mm:ss", ",", ".", true));
		LANGUAGE_MAP.put("pt_PT", new LanguageInfo("pt_PT", "Portuguese", "Português", "Portugal", "EUR", "Europe/Lisbon", "dd/MM/yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("cs_CZ", new LanguageInfo("cs_CZ", "Czech", "Čeština", "Czech Republic", "CZK", "Europe/Prague", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("sk_SK", new LanguageInfo("sk_SK", "Slovak", "Slovenčina", "Slovakia", "EUR", "Europe/Bratislava", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("bg_BG", new LanguageInfo("bg_BG", "Bulgarian", "Български", "Bulgaria", "BGN", "Europe/Sofia", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("ro_RO", new LanguageInfo("ro_RO", "Romanian", "Română", "Romania", "RON", "Europe/Bucharest", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("el_GR", new LanguageInfo("el_GR", "Greek", "Ελληνικά", "Greece", "EUR", "Europe/Athens", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("tr_TR", new LanguageInfo("tr_TR", "Turkish", "Türkçe", "Turkey", "TRY", "Europe/Istanbul", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("lt_LT", new LanguageInfo("lt_LT", "Lithuanian", "Lietuvių", "Lithuania", "EUR", "Europe/Vilnius", "yyyy.MM.dd", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("lv_LV", new LanguageInfo("lv_LV", "Latvian", "Latviešu", "Latvia", "EUR", "Europe/Riga", "yyyy.MM.dd", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("et_EE", new LanguageInfo("et_EE", "Estonian", "Eesti", "Estonia", "EUR", "Europe/Tallinn", "dd.MM.yyyy", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("is_IS", new LanguageInfo("is_IS", "Icelandic", "Íslenska", "Iceland", "ISK", "Atlantic/Reykjavik", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("sq_AL", new LanguageInfo("sq_AL", "Albanian", "Shqip", "Albania", "ALL", "Europe/Tirane", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("mk_MK", new LanguageInfo("mk_MK", "Macedonian", "Македонски", "North Macedonia", "MKD", "Europe/Skopje", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("sr_RS", new LanguageInfo("sr_RS", "Serbian", "Српски", "Serbia", "RSD", "Europe/Belgrade", "dd.MM.yyyy", "HH:mm:ss", ",", ".", false));

		// Asien
		LANGUAGE_MAP.put("zh_CN", new LanguageInfo("zh_CN", "Chinese (Simplified)", "简体中文", "China", "CNY", "Asia/Shanghai", "yyyy-MM-dd", "HH:mm:ss", ".", ",", true));
		LANGUAGE_MAP.put("zh_TW", new LanguageInfo("zh_TW", "Chinese (Traditional)", "繁體中文", "Taiwan", "TWD", "Asia/Taipei", "yyyy/MM/dd", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("ja_JP", new LanguageInfo("ja_JP", "Japanese", "日本語", "Japan", "JPY", "Asia/Tokyo", "yyyy/MM/dd", "HH:mm:ss", ".", ",", true));
		LANGUAGE_MAP.put("ko_KR", new LanguageInfo("ko_KR", "Korean", "한국어", "South Korea", "KRW", "Asia/Seoul", "yyyy-MM-dd", "HH:mm:ss", ".", ",", true));
		LANGUAGE_MAP.put("hi_IN", new LanguageInfo("hi_IN", "Hindi", "हिन्दी", "India", "INR", "Asia/Kolkata", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("ta_IN", new LanguageInfo("ta_IN", "Tamil", "தமிழ்", "India", "INR", "Asia/Kolkata", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("th_TH", new LanguageInfo("th_TH", "Thai", "ไทย", "Thailand", "THB", "Asia/Bangkok", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("vi_VN", new LanguageInfo("vi_VN", "Vietnamese", "Tiếng Việt", "Vietnam", "VND", "Asia/Ho_Chi_Minh", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("id_ID", new LanguageInfo("id_ID", "Indonesian", "Bahasa Indonesia", "Indonesia", "IDR", "Asia/Jakarta", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("ms_MY", new LanguageInfo("ms_MY", "Malay", "Bahasa Melayu", "Malaysia", "MYR", "Asia/Kuala_Lumpur", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));

		// Afrika
		LANGUAGE_MAP.put("af_ZA", new LanguageInfo("af_ZA", "Afrikaans", "Afrikaans", "South Africa", "ZAR", "Africa/Johannesburg", "yyyy/MM/dd", "HH:mm:ss", ",", " ", false));
		LANGUAGE_MAP.put("sw_KE", new LanguageInfo("sw_KE", "Swahili", "Kiswahili", "Kenya", "KES", "Africa/Nairobi", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("yo_NG", new LanguageInfo("yo_NG", "Yoruba", "Yorùbá", "Nigeria", "NGN", "Africa/Lagos", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("ha_NG", new LanguageInfo("ha_NG", "Hausa", "Hausa", "Nigeria", "NGN", "Africa/Lagos", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("zu_ZA", new LanguageInfo("zu_ZA", "Zulu", "isiZulu", "South Africa", "ZAR", "Africa/Johannesburg", "yyyy/MM/dd", "HH:mm:ss", ",", " ", false));

		// Naher Osten
		LANGUAGE_MAP.put("ar_SA", new LanguageInfo("ar_SA", "Arabic (Saudi Arabia)", "العربية", "Saudi Arabia", "SAR", "Asia/Riyadh", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("he_IL", new LanguageInfo("he_IL", "Hebrew", "עברית", "Israel", "ILS", "Asia/Jerusalem", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("fa_IR", new LanguageInfo("fa_IR", "Persian", "فارسی", "Iran", "IRR", "Asia/Tehran", "yyyy/MM/dd", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("ur_PK", new LanguageInfo("ur_PK", "Urdu", "اردو", "Pakistan", "PKR", "Asia/Karachi", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));

		// Nord- und Südamerika
		LANGUAGE_MAP.put("es_MX", new LanguageInfo("es_MX", "Spanish (Mexico)", "Español", "Mexico", "MXN", "America/Mexico_City", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("pt_BR", new LanguageInfo("pt_BR", "Portuguese (Brazil)", "Português", "Brazil", "BRL", "America/Sao_Paulo", "dd/MM/yyyy", "HH:mm:ss", ",", ".", false));
		LANGUAGE_MAP.put("en_CA", new LanguageInfo("en_CA", "English (Canada)", "English", "Canada", "CAD", "America/Toronto", "MM/dd/yyyy", "h:mm a", ".", ",", false));
		LANGUAGE_MAP.put("fr_CA", new LanguageInfo("fr_CA", "French (Canada)", "Français", "Canada", "CAD", "America/Montreal", "yyyy-MM-dd", "HH:mm:ss", ",", " ", false));

		// Ozeanien
		LANGUAGE_MAP.put("en_AU", new LanguageInfo("en_AU", "English (Australia)", "English", "Australia", "AUD", "Australia/Sydney", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
		LANGUAGE_MAP.put("mi_NZ", new LanguageInfo("mi_NZ", "Maori", "Māori", "New Zealand", "NZD", "Pacific/Auckland", "dd/MM/yyyy", "HH:mm:ss", ".", ",", false));
	}

	private static boolean isValidLocaleFormat(String localeCode) {
		return localeCode != null && localeCode.matches("^[a-z]{2}[-_][A-Z]{2}$");
	}

	/**
	 * Retrieves the complete information for a given locale.
	 * 
	 * The locale code follows the standard format "xx_XX": - "xx" is the **language code** (ISO 639-1) in lowercase letters. - "XX" is the **country code** (ISO 3166-1) in
	 * uppercase letters.
	 * 
	 * Example: - "bs_BA" for Bosnian in Bosnia and Herzegovina - "en_US" for English in the United States - "de_DE" for German in Germany
	 * 
	 * This format is widely used in international applications and platforms to define language, country, and regional settings.
	 * 
	 * @param locale The locale code in the format "xx_XX", e.g., "bs_BA" for Bosnian in Bosnia and Herzegovina.
	 * @return A {@link LanguageInfo} instance containing the regional settings for the specified locale.
	 * @throws IllegalArgumentException If the locale code is invalid or does not exist.
	 */
	public static LanguageInfo getLanguageInfo(String localeCode) {
		// Validierung des Formats des localeCode
		if (!isValidLocaleFormat(localeCode)) {
			throw new IllegalArgumentException("Invalid locale format. Expected format: 'xx_XX' (e.g., 'bs_BA')");
		}

		// Convert hyphen to underscore
		localeCode = localeCode.replace('-', '_');

		// Prüfen, ob die Sprache im Map existiert
		LanguageInfo languageInfo = LANGUAGE_MAP.get(localeCode);

		if (languageInfo == null) {
			throw new IllegalArgumentException("Locale not found: " + localeCode);
		}

		return languageInfo;
	}

	/**
	 * Retrieves specific information for a given locale based on the provided key.
	 * 
	 * This method allows you to query specific information about a locale (e.g., language, country, currency, etc.) by providing the locale code and the corresponding key from the
	 * {@link LanguageInfoKey} enum.
	 * 
	 * The locale code must follow the standard format "xx_XX", where: - "xx" represents the **language code** (ISO 639-1, in lowercase letters). - "XX" represents the **country
	 * code** (ISO 3166-1, in uppercase letters).
	 * 
	 * @param localeCode The locale code in the format "xx_XX", e.g., "bs_BA" for Bosnian in Bosnia and Herzegovina.
	 * @param key        The key from the {@link LanguageInfoKey} enum that specifies which piece of information to retrieve.
	 * @return A {@link String} representing the value for the given key and locale.
	 * @throws IllegalArgumentException If the locale code is not found or if the key is unknown.
	 */
	public static String getLanguageInfo(String localeCode, LanguageInfoKey key) {
		// Check if the language exists in the map
		LanguageInfo languageInfo = LANGUAGE_MAP.get(localeCode);

		if (languageInfo == null) {
			throw new IllegalArgumentException("Locale not found: " + localeCode);
		}

		// Return the corresponding value based on the provided key
		switch (key) {
		case LOCALE:
			return languageInfo.localeCode;
		case LANGUAGE_ENGLISH:
			return languageInfo.languageInEnglish;
		case LANGUAGE_NATIVE:
			return languageInfo.languageInNative;
		case COUNTRY:
			return languageInfo.country;
		case CURRENCY:
			return languageInfo.currency;
		case TIMEZONE:
			return languageInfo.timezone;
		case DATE_FORMAT:
			return languageInfo.dateFormat;
		case TIME_FORMAT:
			return languageInfo.timeFormat;
		case DECIMAL_SEPARATOR:
			return languageInfo.decimalSeparator;
		case THOUSANDS_SEPARATOR:
			return languageInfo.thousandsSeparator;
		default:
			throw new IllegalArgumentException("Unknown key: " + key);
		}
	}

//	public static Locale parseLocaleCode(String localeCode) {
//		if (localeCode == null) {
//			Log.warn("Null locale code provided. Using default locale.");
//			return Locale.getDefault();
//		}
//
//		localeCode = localeCode.trim(); // Remove any leading/trailing spaces
//
//		// Enforce lowercase for language and country codes for consistency
//		localeCode = localeCode.toLowerCase(Locale.ROOT);
//
//		try {
//			// Attempt to create a Locale object with the given code
//			return Locale.forLanguageTag(localeCode);
//		} catch (IllformedLocaleException e) {
//			// Handle malformed locale codes gracefully
//			Log.warn("Malformed locale code: %s. Using default locale.", localeCode);
//			return Locale.getDefault();
//		}
//	}

	/**
	 * Constructs a Locale object from the given locale string. The locale string can be in the form "en", "EN", "en-US", "en_US", or "en-US_UK". The method will first convert the
	 * language part to lowercase, the region part (if present) to uppercase, and then reformat the string with the appropriate delimiter, using a hyphen ('-') as the standard
	 * separator.
	 * 
	 * The input can use either underscores ('_') or hyphens ('-') as delimiters, but the resulting Locale will standardize the format using hyphens.
	 *
	 * @param localeString The locale string, which can include language, region, and variant parts.
	 * @return A Locale object representing the provided locale string.
	 * @throws IllegalArgumentException If the locale string is in an invalid format.
	 */
	public static Locale constructLocale(String localeString) {
		Objects.requireNonNull(localeString, "Locale string must not be null");

		// Stelle sicher, dass der Sprachcode in Kleinbuchstaben und der Regionsteil in Großbuchstaben ist
		String[] parts = localeString.split("[-_]", 3); // Split on either "-" or "_"

		if (parts.length < 1 || parts.length > 3) {
			throw new IllegalArgumentException("Invalid locale format: " + localeString);
		}

		Locale.Builder builder = new Locale.Builder();

		// Setze die Sprache in Kleinbuchstaben
		builder.setLanguage(parts[0].toLowerCase());

		if (parts.length > 1) {
			// Setze die Region in Großbuchstaben, falls vorhanden
			builder.setRegion(parts[1].toUpperCase());
		}

		if (parts.length > 2) {
			// Setze die Variante, falls vorhanden
			builder.setVariant(parts[2]);
		}

		return builder.build();
	}

}
