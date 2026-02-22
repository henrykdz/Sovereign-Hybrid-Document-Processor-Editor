package utils.resources;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import utils.logging.Log;

/**
 * ResKey represents a resource loader, provides methods to get the name, format, full file name, path+file, and resource type. It checks for invalid file names and formats. Used
 * keys for images, audio, data files.
 * 
 * @param <T>
 */
public class ResKey<T> {

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	/**
	 * Splits the given file name string into two parts: the name and the format. The format is determined by the last occurrence of the '.' character in the string. If the string
	 * does not contain a '.' character, the format is set to null.
	 * 
	 * @param fileName the file name string to split
	 * @return an array of two strings: the file name and the format
	 */
	public static String[] splitFileNameFromFormat(String filename) {
		if (filename == null || filename.isBlank()) {
			return null;
		}
		return splitFileName(filename);
	}

	public static String[] splitFileNameFromPath(String pathToFile) {
		if (pathToFile == null || pathToFile.isBlank()) {
			return null;
		}

		Path path = Path.of(pathToFile);
		String filename = path.getFileName().toString();
		return splitFileName(filename);
	}

	public static String[] splitFileNameExtensionFromFile(File file) {
		if (file == null) {
			return null; // Return null if the file is invalid or a directory
		}
//		if (file.isDirectory()) {
//			Log.warn("File is a directory:" + file.getAbsolutePath());
//			return null; // Return null if the file is invalid or a directory
//		}

		return splitFileName(file.getName());
	}

	private static String[] splitFileName(String filename) {
		int lastDotIndex = filename.lastIndexOf('.');
		return (lastDotIndex >= 0) ? new String[] { filename.substring(0, lastDotIndex), filename.substring(lastDotIndex + 1).toLowerCase() } : new String[] { filename, null };
	}

	public static String removeExtension(String fileName, boolean lastIndex) {
		if (fileName == null || fileName.isEmpty()) {
			return fileName; // Return the original value for null or empty input
		}

		int pos = lastIndex ? fileName.lastIndexOf('.') : fileName.indexOf('.');

		// Check if the dot is valid and not at the start or end
		if (pos > 0 && pos < fileName.length() - 1) {
			return fileName.substring(0, pos);
		}

		return fileName;
	}

	// public static ResKey fromFilename(String filename) {
	// int lastDotIndex = filename.lastIndexOf('.');
	// if (lastDotIndex == -1 || lastDotIndex == 0 || lastDotIndex == filename.length() - 1) {
	// throw new IllegalArgumentException("Invalid filename: " + filename);
	// }
	// String name = filename.substring(0, lastDotIndex);
	// String extension = filename.substring(lastDotIndex + 1);
	//
	// return new ResKey<>(name, extension);
	// }
	/**
	 * The directory path suffix string of the resource
	 */
	private final String path;    // contains normalized separator
	/**
	 * The file name string of the resource
	 */
	private final String fileName;

	/**
	 * The file format string of the resource, or null if not set
	 */
	private final String format;

	private T resource = null;

	/**
	 * Constructs a new ResKey object with the given key and file name. The file format is set to null, except the file name contains a file format definition separated by a dot
	 * *.* then it will be extracted at the last dot.
	 * 
	 * @param key      the key string of the resource
	 * @param fileName the file name string of the resource
	 */
	public ResKey(String fileName) {
		this(fileName, null);
	}

	/**
	 * A ResKey object represents a resource key that consists of a file name string with an optional file format. The file name strings is required and cannot be null or empty.
	 * The file format string is optional and can be null or empty. If it is null or empty, and the file name does not contain a format definition separated by a dot *.*, then the
	 * format of the file is set to null, otherwise it is extracted from the last dot in the file name. This class ensures that the file name parameter is not null or empty by
	 * throwing an IllegalArgumentException if either is null or empty. It also checks if the file name contains invalid characters, such as backslash (\), forward slash (/), colon
	 * (:), asterisk (*), question mark (?), quote ("), less than (<), greater than (>), or pipe (|).
	 * 
	 * @param fileName the file name string of the resource, cannot be null or empty
	 * @param format   the file format string of the resource, can be null or empty
	 * @throws IllegalArgumentException if file name is null or empty, file name contains invalid characters
	 * @implSpec This constructor sets the fileName and format fields based on the input parameters. It also checks if the format parameter is empty and the file name does not
	 *           contain a format definition, in which case it sets the format field to null.
	 */
	public ResKey(String fileName, String format) {
		checkNullOrEmpty(fileName, "fileName");

		/* Alternativ: // String formattesSlash = result[1].replace("/", sep).replace("\\", sep); */
		String formattedSlash = new File(fileName).getPath();
		String[] pathFileSplit = splitFilePath(formattedSlash);
		this.path = pathFileSplit[0];
		String[] parts = splitFileNameFromFormat(pathFileSplit[1]);

		checkHasInvalidCharacters(parts[0], "fileName");

		this.fileName = parts[0];
		// Check if format is empty and fileName does not contain a format definition
		if ((format == null || format.isEmpty()) && parts[1] == null) {
			this.format = null;
		} else {
			this.format = format != null ? format : parts[1];
		}
	}

	/**
	 * [0]path [1]file or directory
	 */
	private static String[] splitFilePath(String path) {
		String[] result = new String[2];

		String normalizedPath = path.replace('/', File.separatorChar).replace('\\', File.separatorChar);

		int index = normalizedPath.lastIndexOf(File.separatorChar);

		if (index > 0) {
			result[0] = normalizedPath.substring(0, index); // Pfad
			result[1] = normalizedPath.substring(index + 1); // Datei oder Ordnername

		} else {
			result[0] = "";
			result[1] = normalizedPath;
		}

		return result;
	}

	/**
	 * Checks if the given string argument is null or empty. If it is, a warning message is logged and an IllegalArgumentException is thrown with a message containing the argument
	 * name.
	 * 
	 * @param arg     the string argument to check for null or empty
	 * @param argName the name of the argument, used in the error message
	 * @return false if the argument is not null or empty
	 * @throws IllegalArgumentException if the argument is null or empty
	 */
	private static boolean checkNullOrEmpty(String arg, String argName) throws IllegalArgumentException {
		if (arg == null) {
			String message = String.format("%s is null, cannot be null or empty", argName);
			Log.warn(message);
			throw new IllegalArgumentException(message);
		} else if (arg.trim().isEmpty()) {
			String message = String.format("%s is empty, cannot be null or empty", argName);
			Log.warn(message);
			throw new IllegalArgumentException(message);
		}
		return false;
	}

	/**
	 * Checks if the given string argument contains any invalid characters. If it does, a warning message is logged and true is returned.
	 * 
	 * @param arg     the string argument to check for invalid characters
	 * @param argName the name of the argument, used in the error message
	 * @return true if the argument contains invalid characters
	 */
	private static boolean checkHasInvalidCharacters(String arg, String argName) {
		// Check if the arg contains any invalid characters
		if (arg.matches(".*[\\\\/:*?\"<>|].*")) {
			String message = String.format("Invalid %s: %s contains invalid characters.", argName, arg);
			Log.warn(message);
			return true;
		}
		return false;
	}

	/**
	 * To get the complete Filename with file format, use getFileName.
	 * 
	 * @return the file name string of the resource without the format suffix
	 */
	public String getName() {
		return fileName;
	}

	/**
	 * @return the file format string of the resource, or null if not set
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Returns a boolean indicating whether the ResKey object has a file format or not.
	 * 
	 * @return true if the ResKey object has a file format, false otherwise
	 */
	public boolean hasFileFormat() {
		return format != null;
	}

	/**
	 * Returns the full file name string of the resource, consisting of the file name and the file format (if set). If the file format is null, only the file name is returned.
	 * 
	 * @return the full file name string of the resource
	 */
	public String getFileName() {
		if (format == null) {
			return fileName;
		} else {
			return fileName + "." + format;
		}
	}

	/**
	 * Returns the suffixed path with file name, consisting of the file name and the file format (if set). If the file format is null, only the file name is returned.
	 * 
	 * @return the full file name string of the resource
	 */
	public String getPathFile() {

		if (path.isEmpty()) {
			return getFileName();
		} else {
			return path + FILE_SEPARATOR + getFileName();
		}
	}

	public boolean hasPrefixPath() {
		return !path.isEmpty();
	}

	public String getPrefixPath() {
		return path;
	}

	public ResType getType() {
		return ResType.getTypeByFormat(format);
	}

	public void setResource(T resource) {
		this.resource = resource;
	}

	public T getResource() {
		return resource;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ResKey<?> resKey = (ResKey<?>) o;
		return Objects.equals(path, resKey.path) && Objects.equals(fileName, resKey.fileName) && Objects.equals(format, resKey.format);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, fileName, format);
	}

	@Override
	public String toString() {
		return "ResKey{path='" + path + '\'' + ", fileName='" + fileName + '\'' + ", format=" + format + '}';
	}
}
