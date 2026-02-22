package utils.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.girod.javafx.svgimage.SVGImage;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import utils.logging.Log;

/**
 * The ResMan class provides a centralized location for accessing resources, such as images and files, in a Java application. It uses a resource path to locate and load the
 * resources. This class uses a singleton pattern to ensure that only one instance is created throughout the application. This class supports caching of resources, which can help
 * to improve performance by reducing the number of times resources need to be loaded from disk.
 */
public class ResMan {

	// Define the file separator for the current operating system
	public static final Path FILE_SEPARATOR = Paths.get(System.getProperty("file.separator"));

	public static final String RESOURCE_DIRECTORY_NAME = "resources";

	// Define the name of the folder containing the resources
	public static final String RESOURCE_FOLDER     = FILE_SEPARATOR + RESOURCE_DIRECTORY_NAME + FILE_SEPARATOR;
	private static boolean     isResourceInsideJar = false;

	/**
	 * Checks whether the resources folder is located inside the JAR file
	 * 
	 * @return true if resources folder is in JAR file, false otherwise
	 */
	public static boolean isResourcesFolderInJar() {
		return isResourceInsideJar;
	}

	// Define a holder class for the singleton instance
	private static class ResourceCacheHolder {
		private static final ResMan INSTANCE = new ResMan(RESOURCE_DIRECTORY_NAME);
	}

	public static String getResourceDirectoryName() {
		return RESOURCE_DIRECTORY_NAME;
	}

	// Get the singleton instance of ResMan
	public static ResMan getInstance() {
		return ResourceCacheHolder.INSTANCE;
	}

	// private static final String TOOLTIP_DEBUG_STYLE_CLASS = ".tooltip {-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 7, 0.0 , 0 , 0 );}";
	public static String loadStyleSheet(Scene scene, String styleSheetName) {
		Log.info("loadStyleSheet: " + styleSheetName);

		try {
			String res = ResMan.getResolvedPathOf(styleSheetName);
			scene.getStylesheets().setAll(res);
			return res;
			// alternative: Application.setUserAgentStylesheet(res);
		} catch (NullPointerException ex) {
			Log.error(ex, ex.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves a resource from the classpath as a {@link URL}. This is the recommended method for loading resources like images, FXML files, or help files.
	 * <p>
	 * The method internally uses the file extension of {@code fileName} to determine the resource type via {@link ResType}. It then constructs the full, absolute classpath (e.g.,
	 * "/images/icon.png", "/fxml/dialog.fxml", "/helpfiles/help.html").
	 * <p>
	 * <b>Important:</b> For this method to work, the file extension (e.g., "html", "png") must be defined in the {@link ResType} enum. If the format is unknown, the resource will
	 * not be found.
	 *
	 * @param fileName The name of the resource file (e.g., "help_duplicates.html"). The path should be relative to the type-specific resource subdirectory. Do NOT include the
	 *                 subdirectory itself (e.g., use "my_help.html", not "helpfiles/my_help.html").
	 * @return The {@link URL} of the resource, or {@code null} if the resource could not be found.
	 */
	public static URL getResource(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			Log.warn("Cannot get resource for a null or blank file name.");
			return null;
		}

		// 1. Create a ResKey for the given file name.
		ResKey<URL> key = new ResKey<>(fileName);

		// 2. Resolve the URI using the central, reliable path resolution logic.
		// We get the singleton instance to call the private instance method.
		URI uri = ResMan.getInstance().resolveResourceUri(key);

		if (uri == null) {
			// The warning is already logged inside resolveResourceUri,
			// so we don't need to log it again here.
			return null;
		}

		// 3. Convert the safe URI to a URL.
		try {
			return uri.toURL();
		} catch (java.net.MalformedURLException e) {
			// This should theoretically never happen if resolveResourceUri returns a valid URI
			// from getResource(), but it's good practice to handle it.
			Log.error(e, "Failed to convert a valid URI to URL for resource: " + fileName);
			return null;
		}
	}

	/**
	 * Checks if a resource exists within the resources directory.
	 *
	 * @param resourcePath The path of the resource relative to the resource directory.
	 * @return true if the resource exists, false otherwise.
	 */
	public static boolean resourceExists(String resourcePath) {
		ResKey<String> key = new ResKey<>(resourcePath);

		try {
			URI uri = getInstance().getCheckedURIOf(key); // Resolves the URI
			if (uri == null) {
				Log.warn("resourceExists: URI could not be resolved for resource: " + resourcePath);
				return false;
			}

			if ("file".equals(uri.getScheme())) {
				// Local filesystem resource
				return Files.exists(Paths.get(uri));
			} else if ("jar".equals(uri.getScheme())) {
				// Resource inside a JAR file
				try (InputStream inputStream = uri.toURL().openStream()) {
					return inputStream != null;
				}
			} else {
				Log.warn("resourceExists: Unsupported URI scheme: " + uri.getScheme());
			}
		} catch (FileNotFoundException | NullPointerException ex) {
			Log.warn("resourceExists: Resource not found: " + resourcePath, ex);
		} catch (IOException ex) {
			Log.warn("resourceExists: Error accessing resource: " + resourcePath, ex);
		}

		return false;
	}

	/**
	 * Loads the flag image corresponding to a given Locale.
	 * <p>
	 * This method is high-performance and suitable for frequent calls (e.g., in UI list cells). It determines the correct resource path for the flag and then delegates the loading
	 * to the central, <strong>caching-enabled</strong> {@link #get(ResKey)} method.
	 * <p>
	 * Subsequent requests for the same locale will retrieve the image directly from ResMan's internal image cache, avoiding repeated disk access.
	 *
	 * @param locale The locale for which to load the flag. If null, the system's default locale will be used.
	 * @return The loaded {@link Image}, or {@code null} if the flag image could not be found or loaded.
	 * @see #get(ResKey)
	 */
	public static Image getCountryFlagImage(Locale locale) {
		Locale targetLocale = locale;

		if (targetLocale == null || targetLocale.toString().isBlank()) {
			// Fallback to the system default locale if no valid locale is provided.
			targetLocale = Locale.getDefault();
		}

		// Step 1: Translate the locale into a specific file name.
		String fileName = deriveFlagFilenameFromLocale(targetLocale);
		if (fileName == null) {
			return null; // If the locale format was invalid.
		}

		// Step 2: Build the full, relative resource path.
		String fullResourcePath = ResType.FLAGS.getDirectoryName() + "/" + fileName;

		// Step 3: Create a unique key for this resource.
		// Creating a new object here is correct and efficient, as the proper
		// equals/hashCode implementation in ResKey ensures that the main cache
		// recognizes it as the same logical key.
		ResKey<Image> key = new ResKey<>(fullResourcePath);

		// Step 4: Delegate loading and caching to the central get() method.
		// This will correctly utilize ResMan's internal image cache.
		return ResMan.get(key);
	}

	/**
	 * Derives the standard flag image filename from a given Locale object. This method is the canonical way to translate a Locale into a resource name.
	 *
	 * @param locale The Locale object, e.g., for Germany (de_DE). Must not be null.
	 * @return The filename, e.g., "DE.png", or null if the locale has no country part.
	 */
	private static String deriveFlagFilenameFromLocale(Locale locale) {
		// Use the built-in, robust method to get the country code (e.g., "DE").
		String countryCode = locale.getCountry();

		// Check if the country code is present.
		if (countryCode == null || countryCode.isBlank()) {
			Log.warn("Cannot derive flag filename: Locale '%s' does not contain a country part.", locale.toLanguageTag());
			return null;
		}

		// Construct the final filename. Using Locale.ROOT for toUpperCase is best practice.
		return countryCode.toUpperCase(Locale.ROOT) + ".png";
	}

	/**
	 * Get an image resource using a ResKey object
	 * 
	 * @param key the ResKey object representing the image resource
	 * @return the Image object for the resource
	 */
	public static Image get(ResKey<Image> key) {
		// Log.info(ResMan.class, "Image get(ResKey<Image> key):..key.getPathFile: " + key.getPathFile());

		Image result = key.getResource();
		if (null != result) {
			return result;
		}

		result = getInstance().getImage(key);
		if (null == result) {
			Log.warn("getImage(key): returned null");
		}
		return result;
	}

	/** Not caching, just loads the resource from URL */
	public static Image loadImageFromURL(URL url) {
		// Log.info(ResMan.class, "loadImageFromURL(URL url): " + url.toString());
		InputStream inputStream = null;
		try {
			inputStream = url.openStream();
		} catch (IOException ex) {
			Log.error(ex);
			return null;
		}
		return new Image(inputStream);
	}

	public static ImageView getImageView(ResKey<Image> key, double size) {
		Log.fine("getImageView(ResKey<Image> key, double size)");
		return getImageView(key, size, size);
	}

	public static ImageView getImageView(ResKey<Image> key, double width, double height) {
		ImageView imageView = new ImageView(getInstance().getImage(key));
		imageView.setFitWidth(width);
		imageView.setFitHeight(height);
		return imageView;
	}

	public static Object[] loadLayoutAndController(String subPathToFile) throws IOException {
		return loadLayoutAndController(new ResKey<>(subPathToFile));
	}

	public static Object[] loadLayoutAndController(ResKey<?> key) throws IOException {
		Object[] result = new Object[2];
		FXMLLoader loader = new FXMLLoader();
		// try {
		loader.setLocation(getInstance().getCheckedURIOf(key).toURL());
		result[0] = loader.load();
		result[1] = loader.getController();
		// }

		// catch (IOException e) {
		// // Log the error here
		// Log.severe(e, ResMan.class, "loadLayoutAndController", "Failed to load resource: " + key.getFileName());
		// // Rethrow the exception to propagate it to the caller
		// throw e;
		// }

		return result;
	}

	/**
	 * TODO: debug: returns with a file protocol of locaal pathes 'file:\C:\' Gets the checked path of a resource file without file protocol
	 * 
	 * @param fileName the name of the resource file
	 * @return the path of the resource file
	 */
	public static String getResolvedPathOf(String fileName) {
		ResKey<String> key = new ResKey<>(fileName);
		return getInstance().getCheckedPathStringOf(key);
	}

	/**
	 * Retrieves a resource as a {@link URI}. This method is useful for operations that require a URI object, such as loading FXML files with {@link FXMLLoader}.
	 * <p>
	 * Like {@link #getResource(String)}, this method relies on the file extension to determine the correct resource path via the {@link ResType} enum. It performs the same path
	 * resolution logic.
	 * <p>
	 * <b>Note:</b> A key difference between URL and URI is how they handle special characters. This method returns a raw URI object, which may need to be converted to a URL via
	 * {@code .toURL()} for certain stream operations.
	 *
	 * @param path The name of the resource file (e.g., "MyDialog.fxml"), relative to its type-specific subdirectory (e.g., "fxml/").
	 * @return The {@link URI} of the resource, or {@code null} if not found.
	 * @see #getResource(String) for more details on path resolution.
	 */
	public static URI getResolvedURIOf(String path) {
		ResKey<String> key = new ResKey<>(path);
		return getInstance().getCheckedURIOf(key);
	}

	/**
	 * Get the URL of a resource file
	 * 
	 * @param key the ResKey object representing the resource file
	 * @return the URL of the resource file
	 */
	public static URI getURIOf(ResKey<?> key) {
		return getInstance().getCheckedURIOf(key);
	}

	/**
	 * Loads and caches the resource. Sets Image resource to the ResKey.
	 **/
	public Image getImage(ResKey<Image> key) {
		// Check if the resource has already been loaded and cached
		ResEntry<Image> cachedEntry = imageCache.get(key);
		if (cachedEntry != null) {
			return cachedEntry.getValue();
		}

		URI uri = getCheckedURIOf(key);

		// if (!Files.exists(Paths.get(uri))) {
		// Log.warning("getImage(key): uri path does not exists uri: " + Paths.get(uri));
		// }

		// Check if the resource has already been buffered
		Image image = key.getResource();
		// If the resource is not cached, load it from the URL
		if (null == image && null != uri) {
			image = loadImage(uri);
			key.setResource(image);
		}

		if (null == image) {
			return null;
		}

		// Cache the resource and return it
		ResEntry<Image> newEntry = new ResEntry<>(key, uri, image);
		getCacheImage().cache(newEntry);
		return image;
	}

	/**
	 * Loads an Image resource from the specified URI.
	 * 
	 * @param uri the URI of the image resource
	 * @return the loaded Image, or null if the image cannot be loaded
	 */
	public Image loadImage(URI uri) {
		if (null == uri) {
			return null;
		}

		try (InputStream inputStream = uri.toURL().openStream()) {
			return new Image(inputStream);
		} catch (FileNotFoundException ex) {
			Log.warn("File not found: " + uri);
			return null;
		} catch (IOException ex) {
			Log.warn(ex);
			return null;
		}
	}

	/**
	 * Resolves the full, absolute classpath URI for a given resource key.
	 * <p>
	 * This is the central and definitive method for all resource path resolutions within the framework. It is designed to be platform-independent and works reliably whether the
	 * application is run from an IDE or a packaged JAR file.
	 * <p>
	 * The method employs a flexible strategy to construct the final resource path:
	 * <ol>
	 * <li><b>Path Provided in Key:</b> If the {@code ResKey} was created with a path (e.g., {@code new ResKey<>("logo/splash.png")}), that path is trusted and used directly.</li>
	 * <li><b>No Path in Key:</b> If the {@code ResKey} contains only a filename (e.g., {@code new ResKey<>("help.html")}), the method determines the correct subdirectory from the
	 * file's extension via the {@link ResType} enum.</li>
	 * </ol>
	 * The final constructed path follows the absolute classpath format:
	 * 
	 * <pre>
	 * /{@value #RESOURCE_DIRECTORY_NAME}/[resolved_path]/[file_name]
	 *
	 * Example 1 (Path in Key):
	 * ResKey("logo/splash.png") -> /resources/logo/splash.png
	 *
	 * Example 2 (Path from ResType):
	 * ResKey("help_duplicates.html") -> /resources/helpfiles/help_duplicates.html
	 * </pre>
	 * 
	 * <b>Important:</b> This method's success depends on the {@link ResType} enum being correctly configured with all necessary file extensions and their corresponding directory
	 * names.
	 *
	 * @param key The ResKey representing the resource. Must not be null.
	 * @return The resolved {@link URI} for the resource, or {@code null} if the resource could not be found, the key was invalid, or the resource type was unknown.
	 */
	private URI resolveResourceUri(ResKey<?> key) {
		if (key == null) {
			Log.warn("ResKey parameter is null in resolveResourceUri.");
			return null;
		}

		// 1. Determine the resource type to validate the file extension.
		ResType type = ResType.getTypeOf(key);
		if (type == ResType.UNKNOWN_TYPE_FORMAT) {
			// Warning is already logged inside ResType
			return null;
		}

		// 2. Determine the relative path for the resource.
		String relativePath;
		if (key.hasPrefixPath()) {
			// CASE A: The key itself provides the path (e.g., from "logo/splash.jpg").
			// We build a platform-independent path using its components.
			relativePath = key.getPrefixPath().replace('\\', '/') + "/" + key.getFileName();
		} else {
			// CASE B: The key is just a file name (e.g., "help_duplicates.html").
			// We derive the path from its ResType.
			String typeDirectory = type.getDirectoryName();
			String fileName = key.getFileName();
			relativePath = typeDirectory + "/" + fileName;
		}

		// 3. Construct the final, absolute classpath.
		// Classpath resources MUST use forward slashes ("/") as separators.
		String expectedClasspath = "/" + RESOURCE_DIRECTORY_NAME + "/" + relativePath;

		Log.fine("ResMan: Attempting to locate resource at constructed classpath: %s", expectedClasspath);

		// 4. Try to load the resource via the Classloader.
		try {
			URL resourceUrl = ResMan.class.getResource(expectedClasspath);
			if (resourceUrl != null) {
				return resourceUrl.toURI();
			} else {
				Log.warn("Resource not found at expected classpath: %s", expectedClasspath);
				return null;
			}
		} catch (URISyntaxException e) {
			Log.error(e, "The resolved resource URL could not be converted to a URI for path: " + expectedClasspath);
			return null;
		}
	}

	/**
	 * Get the checked URL of a resource file and check if it exists.
	 * 
	 * @param key the ResKey object representing the resource file
	 * @return the URL of the resource file if it exists, null otherwise
	 */
	public URI getCheckedURIOf(ResKey<?> key) {
		return resolveResourceUri(key);
	}

	/**
	 * Get the path of a resource file and check if it exists.
	 * 
	 * @param key the ResKey object representing the resource file
	 * @return the path of the resource file if it exists, null otherwise
	 */
	public String getCheckedPathStringOf(ResKey<?> key) {
		URI uri = resolveResourceUri(key);
		return (uri != null) ? uri.toString() : null;
	}

	/** The map to cache ResEntrys */
	ResCache<Image>    imageCache = new ResCache<>();
	ResCache<SVGImage> soundCache = new ResCache<>();

	protected ResCache<Image> getCacheImage() {
		return imageCache;
	}

	// Root-Resourse-folder
	private String  resourcePath;
	private boolean useResourceFolder = false;

	/**
	 * Constructor for the ResMan class
	 * 
	 * @param rootResourceFolderName the name of the root resource folder to use
	 */
	private ResMan(String rootResourceFolderName) {
		this.resourcePath = rootResourceFolderName;
		this.resourcePath = updateResourcePath();

		this.useResourceFolder = !checkNullOrEmpty(resourcePath);
	}

	/**
	 * Check if the resource folder is being used
	 * 
	 * @return true if the resource folder is being used, false otherwise
	 */
	public boolean isResourceFolderUsed() {
		return useResourceFolder;
	}

	/**
	 * Updates the resource path based on whether the resources folder is located inside a jar file.
	 * 
	 * @return the updated resource path as a string
	 */
	// Existing method for updating resource path
	private String updateResourcePath() {
		String resourceFolderPath = this.resourcePath;
		boolean resourceExists = false;
		String resourceLocation = "";
		String resourceUrlPath = "";

		try {
			ClassLoader classLoader = ResMan.class.getClassLoader();
			URL resourceURL = classLoader.getResource(RESOURCE_DIRECTORY_NAME);

			if (resourceURL != null) {
				resourceUrlPath = resourceURL.toString();
				resourceLocation = getResourceLocation(resourceURL);
				resourceExists = checkResourceExistence(classLoader);

				// Update the flag when the resource is found in the classpath
				isResourceInsideJar = resourceURL.toString().startsWith("jar:");
			} else {
				// If the resources folder does not exist in the classpath, check if it's in a JAR
				String jarPath = ResMan.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				int lastSlash = jarPath.lastIndexOf("/");
				jarPath = jarPath.substring(0, lastSlash + 1);

				// Construct the resource folder path assuming it is located inside the JAR
				resourceLocation = "JAR file";
				resourceFolderPath = "jar:" + jarPath + resourcePath;

				// Update the flag indicating that the resource is inside a JAR
				isResourceInsideJar = true;

				// Check if resource exists in the JAR or on the filesystem
				resourceExists = checkResourceExistence(classLoader);
			}

			Log.fine("Resource path updated: '%s' (Resource exists: %s, Location: %s)", resourceFolderPath, resourceExists, resourceLocation);
			Log.fine("Resource Url: '%s'", resourceUrlPath);
		} catch (IOException ex) {
			Log.exceptionShow(ex, null, "Error updating resource path: " + resourceFolderPath);
			System.exit(1);
		}
		return resourceFolderPath;
	}

	private String getResourceLocation(URL resourceURL) {
		if (!resourceURL.getProtocol().equals("jar")) {
			return "file system";
		}
		return "JAR file";
	}

	private boolean checkResourceExistence(ClassLoader classLoader) throws IOException {
		try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_DIRECTORY_NAME)) {
			return inputStream != null;
		}
	}

	/**
	 * Checks whether a given string is null or empty
	 * 
	 * @param arg the string to be checked
	 * @return true if string is null or empty, false otherwise
	 */
	private static boolean checkNullOrEmpty(String arg) {
		return (arg == null || arg.trim().isEmpty());
	}

}
