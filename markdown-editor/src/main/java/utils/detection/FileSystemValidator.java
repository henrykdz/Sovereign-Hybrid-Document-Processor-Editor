package utils.detection;

import java.io.File;

import utils.logging.Log;

/**
 * Handles all filesystem validation with the same checks as the original Pathment method. Stateless and thread-safe.
 */
public final class FileSystemValidator {

	/**
	 * Replica of the original Pathment's checkFileExists() logic.
	 * 
	 * @param pathment Must be non-null.
	 * @return true if the file exists and is accessible, with identical validation rules.
	 */
	public static boolean checkFileExists(Pathment pathment) {
		// Early exit for non-file types (original check 1:1)
		if (!pathment.getType().isFileBased()) {
			Log.fine("Not a file-based Pathment type: " + pathment.getType());
			return false;
		}

		// Null check (original check 1:1)
		FilePath filePath = pathment.getFilePath();
		if (filePath == null) {
			Log.warn("FilePath is null for file-based Pathment");
			return false;
		}

		try {
			String systemPath = filePath.getPathWithoutProtocol();

			// Empty path check (original check 1:1)
			if (systemPath == null || systemPath.trim().isEmpty()) {
				Log.warn("Empty filesystem path");
				return false;
			}

			// Relative path handling (original logic 1:1)
			if (pathment.getType() == Pathment.Type.RELATIVE_PATH) {
				return resolveRelativePath(systemPath).exists();
			}

			// Standard absolute path check (original logic 1:1)
			return new File(systemPath).exists();

		} catch (SecurityException e) {
			Log.warn("Access denied to: " + filePath.getFullPath());
			return false;
		} catch (Exception e) {
			Log.error("Unexpected error checking path existence: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Original relative path resolution logic.
	 */
	private static File resolveRelativePath(String relativePath) {
		return new File(System.getProperty("user.dir"), relativePath);
	}
}