package utils.detection;

import java.util.Set;

/**
 * A utility class holding constant data sets used for heuristic parsing, such as curated Top-Level Domains (TLDs) and common file extensions. Using immutable Sets provides optimal
 * performance for 'contains' checks. All entries are stored in lower case for case-insensitive matching.
 */
public final class PathmentHeuristics {

	private PathmentHeuristics() {
	} // Prevent instantiation

	/**
	 * A curated, extensive set of common and notable Top-Level Domains (TLDs). This list is not exhaustive of all ~1500+ TLDs but is designed to cover the vast majority of
	 * encountered domains, improving parsing accuracy. New or emerging TLDs can be easily added here.
	 */
	public static final Set<String> CURATED_TLDS = Set.of(
	        // --- Original Generic TLDs (gTLDs) ---
	        "com", "org", "net", "edu", "gov", "mil", "int",

	        // --- Common Modern gTLDs ---
	        "app", "ai", "biz", "info", "io", "dev", "tech", "xyz", "online", "site", "tv", "cloud", "shop", "store", "blog", "news", "art", "design", "wiki", "agency",
	        "consulting", "expert", "foundation", "global", "group", "studio",

	        // --- Geographic & Regional ---
	        "asia", "berlin", "boston", "london", "nyc", "paris", "tokyo", "earth", "lat",

	        // --- Most Common Country Code TLDs (ccTLDs) ---
	        "ac", "ad", "ae", "af", "ag", "al", "am", "at", "au", "az", "ba", "be", "bg", "bh", "bi", "bj", "bo", "br", "bs", "by", "ca", "cc", "cd", "ch", "ci", "cl", "cm", "cn",
	        "co", "cr", "cu", "cv", "cy", "cz", "de", "dk", "dm", "do", "dz", "ec", "ee", "eg", "es", "et", "eu", "fi", "fm", "fr", "ga", "ge", "gg", "gh", "gl", "gm", "gr", "gt",
	        "gy", "hk", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in", "ir", "is", "it", "je", "jm", "jo", "jp", "ke", "kg", "kh", "kr", "kw", "kz", "la", "lb", "li", "lk",
	        "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mk", "ml", "mn", "mo", "mt", "mu", "mv", "mx", "my", "na", "ng", "ni", "nl", "no", "np", "nz", "om", "pa", "pe",
	        "ph", "pk", "pl", "pr", "pt", "py", "qa", "ro", "rs", "ru", "rw", "sa", "se", "sg", "si", "sk", "sm", "sn", "so", "st", "sv", "sy", "tc", "td", "tg", "th", "tn", "to",
	        "tr", "tt", "tw", "ua", "ug", "uk", "us", "uy", "uz", "vc", "ve", "vn", "vu", "ws", "ye", "za", "zm", "zw");

	/**
	 * A set of common file extensions that are unlikely to be TLDs. Used as an exclusion filter to prevent misclassifying filenames (e.g., "archive.zip") as domain names. Stored
	 * without the leading dot.
	 */
	public static final Set<String> COMMON_FILE_EXTENSIONS = Set.of(
	        // --- Documents & Text ---
	        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt", "rtf", "csv", "md", "tex", "epub", "mobi",

	        // --- Archives ---
	        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso",

	        // --- Images ---
	        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "tiff", "psd", "ai", "ico",

	        // --- Audio ---
	        "mp3", "wav", "flac", "aac", "ogg", "m4a",

	        // --- Video ---
	        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",

	        // --- Code & Scripts ---
	        "java", "js", "ts", "html", "htm", "css", "scss", "php", "py", "rb", "go", "rs", "c", "cpp", "h", "cs", "swift", "kt", "kts", "sh", "bat", "ps1", "vbs",

	        // --- Executables & Libraries ---
	        "exe", "dll", "so", "dmg", "app", "jar", "msi",

	        // --- Data & Configuration ---
	        "xml", "json", "yml", "yaml", "ini", "conf", "cfg", "log", "db", "sqlite", "sql", "bak", "tmp",

	        // --- Web Specific ---
	        "asp", "aspx", "jsp", "xhtml",

	        // --- Other common but ambiguous ---
	        "class", "method", "obj", "dat", "bin");
}