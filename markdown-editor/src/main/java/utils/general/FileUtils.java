package utils.general;


import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import utils.logging.Log;

public class FileUtils {

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

	public static String getFileExtension(String filename) {
		if (filename == null || filename.isBlank()) {
			return null;
		}
		// Hier wird nur der Dateityp (Erweiterung) extrahiert
		String[] parts = splitFileName(filename);
		return parts[1]; // parts[1] enthält die Dateiendung
	}

	public static String getFileExtensionFromFile(File file) {
		if (file == null || !file.exists()) {
			return null; // Return null if the file is invalid or does not exist
		}

		return getFileExtension(file.getName()); // Ruft die Erweiterung ab
	}

	public static String getFileExtensionFromPath(Path pathToFile) {
		if (pathToFile == null || !Files.exists(pathToFile)) {
			return null; // Return null if the path is invalid or does not exist
		}

		String filename = pathToFile.getFileName().toString();
		return getFileExtension(filename); // Ruft die Erweiterung ab
	}

	/**
	 * Moves a file to a specified directory, optionally overwriting the target file if it exists. This method also removes the `.temp` extension from the file name if it exists.
	 * 
	 * @param srcFile   the source file to move
	 * @param destDir   the destination directory where the file should be moved
	 * @param overwrite whether to overwrite the target file if it exists
	 * @return true if the file was successfully moved, false otherwise
	 * @throws IOException if an I/O error occurs
	 */
	public static boolean moveFileToDirectory(File srcFile, File destDir, boolean overwrite) throws IOException {
		Path sourcePath = srcFile.toPath();

		// Handle .temp extension by removing it if exists
		String fileName = srcFile.getName();
		if (fileName.endsWith(".temp")) {
			fileName = fileName.substring(0, fileName.length() - 5); // Remove ".temp"
		}

		Path targetPath = destDir.toPath().resolve(fileName); // Resolve the file name in the destination directory

		// If overwrite is true and the file exists, delete it first
		if (overwrite && Files.exists(targetPath)) {
			Files.delete(targetPath);
		}

		// Move the file to the destination directory
		Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
		return Files.exists(targetPath);
	}

	/**
	 * Moves a file from its current location to the specified target directory. This method also removes the `.temp` extension from the file name if it exists before moving.
	 *
	 * @param option     the copy option(s) (e.g., REPLACE_EXISTING, ATOMIC_MOVE) to specify how the move should be performed.
	 * @param file       the source file to be moved. Must not be null and must be a file.
	 * @param targetPath the path of the target directory where the file should be moved. Must not be blank and must be a valid directory.
	 * @return true if the file was moved successfully, false otherwise.
	 */
	public static boolean moveFileToDirectory(StandardCopyOption option, File file, String targetPath) {
		// --- Input Validation ---
		if (option == null) {
			Log.warn("The provided copy option is null.");
			return false;
		}

		if (file == null) {
			Log.warn("The provided source file is null.");
			return false;
		}

		if (!file.exists()) {
			Log.warn("The provided source file does not exist: " + file.getPath());
			return false;
		}

		if (!file.isFile()) {
			Log.warn("The provided source path is not a file: " + file.getPath());
			return false;
		}

		if (StringUtils.isBlank(targetPath)) {
			Log.warn("The target directory path is null or empty.");
			return false;
		}

		Path targetDirectory;
		try {
			targetDirectory = FileSystems.getDefault().getPath(targetPath);
		} catch (InvalidPathException ipe) {
			Log.warn("The target directory path is invalid: " + targetPath, ipe);
			return false;
		}

		if (!Files.isDirectory(targetDirectory)) {
			Log.warn("The target path is not a valid directory: " + targetPath);
			return false;
		}

		// --- Prepare Paths ---
		// Determine target filename (remove .temp if present)
		String sourceFileName = file.getName();
		String targetFileName = sourceFileName; // Default to original name
		final String tempSuffix = ".temp";
		if (sourceFileName.endsWith(tempSuffix)) {
			// Remove ".temp" suffix
			targetFileName = sourceFileName.substring(0, sourceFileName.length() - tempSuffix.length());
		}

		// Create full source and target paths using NIO Path API
		Path moveFrom = file.toPath();
		Path target = targetDirectory.resolve(targetFileName);

		Log.fine("Attempting to move file from: " + moveFrom + " to target: " + target + " with option: " + option);

		// --- Perform Move ---
		try {
			// Perform the move operation using Files.move
			Files.move(moveFrom, target, option);
			Log.fine("File moved successfully to: " + target);
			return true;
		} catch (FileAlreadyExistsException faee) {
			// Log specifically if the file exists and REPLACE_EXISTING wasn't used or failed
			Log.warn("Target file already exists (and REPLACE_EXISTING might not have been specified or applicable): " + target, faee);
			return false;
		} catch (AtomicMoveNotSupportedException amnse) {
			Log.warn("Atomic move not supported between source and target locations (different file systems?): " + moveFrom + " -> " + target, amnse);
			// Optionally: Retry with non-atomic move? Or just fail as per contract.
			return false;
		} catch (AccessDeniedException ade) {
			Log.warn("Access denied during move operation. Check permissions for source/target: " + moveFrom + " -> " + target, ade);
			return false;
		} catch (IOException ex) {
			// Log other IO exceptions with a detailed message
			Log.warn("IOException while moving file from " + moveFrom + " to " + target + ": " + ex.getMessage(), ex);
			return false;
		} catch (SecurityException se) {
			// Catch potential security manager restrictions
			Log.warn("SecurityException during move operation: " + moveFrom + " -> " + target, se);
			return false;
		}
	}

	/**
	 * Formats a file size in bytes into a human-readable string (e.g., "1.23 MB"). Handles Bytes, KB, MB, and GB.
	 *
	 * @param size The file size in bytes.
	 * @return A formatted string representing the size with the appropriate unit.
	 */
	public static String formatFileSize(long size) {
		if (size < 1024) {
			return size + " Bytes";
		}
		int exp = (int) (Math.log(size) / Math.log(1024));
		char pre = "KMGTPE".charAt(exp - 1);
		return String.format("%.2f %sB", size / Math.pow(1024, exp), pre);
	}
}
