package utils.detection;

import utils.general.StringUtils;
import utils.network.TransferProtocol;

/**
 * Represents the components of a local file path.
 * <p>
 * This class stores the distinct parts of a file path, such as the scheme (e.g., "file:"), drive letter, directories, file name, and extension. It provides methods to reconstruct
 * the path in various formats suitable for display or internal use.
 * <p>
 * This is an immutable class; once created, its components cannot be changed.
 */
public final class FilePath {

	private final boolean schemeFound;
	private final String  drive;
	private final String  directories;
	private final String  fileName;
	private final String  extension;

	/**
	 * Constructs a new, immutable {@code FilePath} instance.
	 *
	 * @param schemeFound Whether a "file:" scheme was detected in the original path.
	 * @param drive       The drive letter (e.g., "C:").
	 * @param directories The directory path (e.g., "/Users/Test/").
	 * @param fileName    The name of the file without its extension (e.g., "document").
	 * @param extension   The file extension without the dot (e.g., "txt").
	 */
	public FilePath(boolean schemeFound, String drive, String directories, String fileName, String extension) {
		this.schemeFound = schemeFound;
		this.drive = StringUtils.trimToNull(drive);
		this.directories = StringUtils.trimToNull(directories);
		this.fileName = StringUtils.trimToNull(fileName);
		this.extension = StringUtils.trimToNull(extension);
	}

	// --- Component Getters ---

	/**
	 * Returns whether a "file:" scheme was detected.
	 * 
	 * @return true if a scheme was found, false otherwise.
	 */
	public boolean hasScheme() {
		return schemeFound;
	}

	/**
	 * Returns the drive letter (e.g., "C:") or an empty string if not set.
	 * 
	 * @return The drive letter or an empty string.
	 */
	public String getDrive() {
		return drive != null ? drive : "";
	}

	/**
	 * Returns the directories path (e.g., "/Users/Test/") or an empty string if not set.
	 * 
	 * @return The directories path or an empty string.
	 */
	public String getDirectories() {
		return directories != null ? directories : "";
	}

	/**
	 * Returns the file name without extension (e.g., "document") or an empty string if not set.
	 * 
	 * @return The file name or an empty string.
	 */
	public String getFileName() {
		return fileName != null ? fileName : "";
	}

	/**
	 * Returns the file extension without the dot (e.g., "txt") or an empty string if not set.
	 * 
	 * @return The extension or an empty string.
	 */
	public String getExtension() {
		return extension != null ? extension : "";
	}

	// --- Assembled Path Getters ---

	/**
	 * Reconstructs and returns the complete, display-friendly file path, including the "file://" protocol if it was originally present.
	 *
	 * @return The full file path string.
	 */
	public String getFullPath() {
		return assemblePath(true);
	}

	/**
	 * Reconstructs and returns the file path without the "file://" protocol.
	 *
	 * @return The file path string without the protocol.
	 */
	public String getPathWithoutProtocol() {
		return assemblePath(false);
	}

	/**
	 * Returns the full file name including its extension (e.g., "document.txt").
	 *
	 * @return The full file name, or an empty string if the file name is not set.
	 */
	public String getFullName() {
		if (StringUtils.isBlank(fileName)) {
			return "";
		}
		return fileName + (StringUtils.isNotBlank(extension) ? "." + extension : "");
	}

	/**
	 * Core logic to assemble the path string.
	 * 
	 * @param withProtocol If true, includes the "file://" prefix.
	 * @return The assembled path string.
	 */
	private String assemblePath(boolean withProtocol) {
		StringBuilder sb = new StringBuilder();

		if (withProtocol && schemeFound) {
			sb.append(TransferProtocol.FILE_URL.getNotation());
		}

		if (StringUtils.isNotBlank(drive)) {
			sb.append(drive);
		}
		if (StringUtils.isNotBlank(directories)) {
			sb.append(directories);
		}

		sb.append(getFullName());

		return sb.toString();
	}

	/**
	 * Checks if the path has a structurally complete representation (i.e., contains a drive and some content). This is a helper method for debugging or logging, not for
	 * validation.
	 *
	 * @return true if the path has a drive and either directories or a filename, false otherwise.
	 */
	public boolean isStructurallyComplete() {
		return StringUtils.isNotBlank(drive) && (StringUtils.isNotBlank(directories) || StringUtils.isNotBlank(fileName));
	}

	@Override
	public String toString() {
		return String.format("FilePath[scheme=%b, drive='%s', dirs='%s', name='%s', ext='%s']", schemeFound, drive, directories, fileName, extension);
	}
}