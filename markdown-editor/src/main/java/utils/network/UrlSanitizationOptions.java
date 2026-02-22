package utils.network;


/**
 * Encapsulates the configuration for the "Nexus Port-Hygiene" system.
 * This record defines which automated cleaning and privacy-preserving rules are applied to incoming URLs
 * during Paste, Drag & Drop, or Scraping operations.
 * 
 * @param masterActive    The global gatekeeper. If {@code false}, all specific sanitization rules are bypassed,
 *                        and only basic technical decoding (UTF-8 normalization) is performed.
 * @param amazonActive    Enables Amazon-specific normalization. Transforms complex paths (gp/product, gp/aw/d, etc.)
 *                        into the canonical {@code /dp/ASIN} format and strips all referral and tracking tags.
 * @param ebayActive      Enables eBay-specific cleaning. Specifically targets item URLs ({@code /itm/}) to remove
 *                        extensive tracking parameters while preserving the core product identity.
 * @param canonicalActive Authorizes the MetaDataScraper to respect the {@code <link rel="canonical">} tag found in 
 *                        HTML headers. This ensures that the "official" version of a web resource is stored, 
 *                        preventing duplicates caused by session IDs or marketing parameters.
 * 
 * @author Henryk Daniel Zschuppan / Kassandra AGI 2025
 */
public record UrlSanitizationOptions(
    boolean masterActive,
    boolean amazonActive,
    boolean ebayActive,
    boolean canonicalActive
) {
    /**
     * Factory method for a "neutral" configuration. 
     * Use this when technical integrity (decoding) is required, but architectural 
     * or content-based modifications are undesirable.
     * 
     * @return A configuration where all sanitization features are disabled.
     */
    public static UrlSanitizationOptions disabled() {
        return new UrlSanitizationOptions(false, false, false, false);
    }

    /**
     * Factory method for a full "Nexus Standard" configuration.
     * 
     * @return A configuration where all sanitization features are enabled.
     */
    public static UrlSanitizationOptions allEnabled() {
        return new UrlSanitizationOptions(true, true, true, true);
    }
}