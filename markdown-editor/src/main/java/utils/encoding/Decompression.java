package utils.encoding;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//import com.aayushatharva.brotli4j.decoder.BrotliInputStream;

import javafx.scene.control.Alert;
import utils.logging.Log;
import utils.ui.WindowUtils;

/**
 * A utility class for handling various decompression formats (GZIP, ZIP, Brotli) and for managing the encoding of compressed and decompressed data streams.
 * <p>
 * It leverages both the standard Java libraries for common formats and specific external dependencies (like Brotli4j) for modern compression algorithms. Such external dependencies
 * must be correctly configured in the {@code pom.xml}.
 * <p>
 * This class provides methods to reliably detect file formats based on their "magic numbers" and ensures that all internal string-to-byte conversions are explicitly handled with
 * UTF-8 to prevent character encoding issues.
 *
 * <h3>Revision History</h3>
 * <ul>
 * <li><b>12.11.2025 - Reviewed by Henryk & Kassandra:</b>
 * <ul>
 * <li><b>UTF-8 Hardening:</b> Verified that all string-to-byte and byte-to-string conversions explicitly use {@code StandardCharsets.UTF_8} to prevent encoding errors with special
 * characters.</li>
 * <li><b>Robust Format Detection:</b> Replaced unreliable stream-reading checks with robust "magic number" validation for {@code isZipFile()} and {@code isGzipFile()}, eliminating
 * false positives.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author Henryk Daniel Zschuppan
 * @version 2025-11-12
 */
public class Decompression {

	/**
	 * Represents the supported HTTP Content-Encoding types. Provides a type-safe way to handle different compression formats.
	 */
	public enum ContentEncoding {
		/** Brotli compression. */
		BROTLI("br"),
		/** GZIP compression. */
		GZIP("gzip"),
		/** Deflate compression. */
		DEFLATE("deflate"),
		/** Represents the absence of a specific content encoding. */
		NONE("");

		private final String encoding;

		ContentEncoding(String encoding) {
			this.encoding = encoding;
		}

		/**
		 * @return The string representation of the encoding (e.g., "gzip").
		 */
		public String getEncoding() {
			return encoding;
		}

		/**
		 * Creates a {@code ContentEncoding} from its string representation, ignoring case. If the provided string does not match any known encoding, this method safely returns
		 * {@link #NONE} as a default.
		 *
		 * @param encoding The string representation from an HTTP header (e.g., "gzip").
		 * @return The corresponding {@code ContentEncoding} constant, or {@code NONE} if no match is found.
		 */
		public static ContentEncoding fromString(String encoding) {
			if (encoding != null) {
				for (ContentEncoding enc : values()) {
					if (enc.getEncoding().equalsIgnoreCase(encoding.trim())) {
						return enc;
					}
				}
			}
			// Fallback for null or non-matching encodings.
			return NONE;
		}
	}

	/**
	 * Decompresses the response body based on the Content-Encoding header and interprets the bytes using the provided Charset.
	 *
	 * @param responseBodyBytes     The compressed or uncompressed response body.
	 * @param contentEncodingHeader The value of the "Content-Encoding" header (e.g., "gzip", "br"). Can be null or empty.
	 * @param charset               The Charset determined from the "Content-Type" header or a default (e.g., UTF-8). Must not be null.
	 * @return The decompressed response body as a String, interpreted with the given Charset.
	 * @throws IOException          If an IO error occurs during decompression.
	 * @throws NullPointerException if responseBodyBytes or charset is null.
	 */
	public static String decompressResponseBody(byte[] responseBodyBytes, String contentEncodingHeader, Charset charset) throws IOException {
		Objects.requireNonNull(responseBodyBytes, "Response body bytes cannot be null");
		Objects.requireNonNull(charset, "Charset cannot be null");

		final int THRESHOLD_SIZE = 1 * 1024 * 1024; // 1 MB
		String encodingStr = (contentEncodingHeader != null) ? contentEncodingHeader.trim() : "";
		ContentEncoding encoding = ContentEncoding.fromString(encodingStr);

		String logMessage = "Decompressing body. Encoding: " + encoding + ", Charset: " + charset.name() + ", Size: " + responseBodyBytes.length + " bytes";

		InputStream decompressedStream = null;
		ByteArrayInputStream inputStream = new ByteArrayInputStream(responseBodyBytes);

		try {
			switch (encoding) {
			case BROTLI:
				Log.fine(logMessage + " (Brotli)");
				// NOTE: BrotliInputStream requires the 'brotli4j' library.
				// This dependency must be included in the pom.xml for this code to compile and run.
				// See dependency: groupId 'com.aayushatharva.brotli4j'
//				decompressedStream = new BrotliInputStream(inputStream);
				break;
			case GZIP:
				Log.fine(logMessage + " (Gzip)");
				decompressedStream = new GZIPInputStream(inputStream);
				break;
			case DEFLATE:
				Log.fine(logMessage + " (Deflate)");
				decompressedStream = new InflaterInputStream(inputStream); // Standard Deflate
				break;
			case NONE:
			default: // Handle unknown or no encoding
				Log.fine(logMessage + " (None/Unknown)");
				decompressedStream = inputStream; // Use original stream
				break;
			}

			// Read from the (potentially decompressed) stream using the correct charset.
			if (responseBodyBytes.length <= THRESHOLD_SIZE || encoding == ContentEncoding.NONE) {
				// For smaller files or uncompressed data, read all bytes directly.
				Log.fine("Reading decompressed data using readAllBytes...");
				byte[] decompressedBytes = decompressedStream.readAllBytes();
				return new String(decompressedBytes, charset);
			} else {
				// For larger decompressed streams, read line by line to avoid large byte arrays.
				Log.fine("Reading large decompressed data using BufferedReader...");
				return readStreamWithBufferedReader(decompressedStream, charset);
			}
		} finally {
			// Clean up resources. The `readStreamWithBufferedReader` method manages its own streams.
			// For other paths, we only close the `decompressedStream` if it is a wrapper.
			// Closing the wrapper stream also closes the underlying stream it wraps.
			if (decompressedStream != null && decompressedStream != inputStream) {
				try {
					decompressedStream.close();
				} catch (IOException e) {
					Log.warn(e, "Error closing decompression stream");
				}
			}
		}
	}

	/**
	 * Reads the entire content of an InputStream into a String using a BufferedReader with the specified charset.
	 * <p>
	 * Note: This method reads line by line and is generally memory-efficient, but can consume significant memory if the stream contains extremely long lines without line breaks.
	 *
	 * @param inputStream The input stream to read from. Will be closed by this method.
	 * @param charset     The character encoding to use for interpreting the stream.
	 * @return The full content of the stream as a single String.
	 * @throws IOException If an I/O error occurs while reading from the stream.
	 */
	private static String readStreamWithBufferedReader(InputStream inputStream, Charset charset) throws IOException {
		// The try-with-resources block ensures that the reader and underlying streams are closed automatically.
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {

			StringBuilder output = new StringBuilder();
			String line;
			// Read line by line - careful with memory for huge single lines!
			while ((line = bufferedReader.readLine()) != null) {
				output.append(line);
				// Note: readLine() strips newline characters. If they are needed in the final
				// string, they must be appended manually, e.g., output.append(System.lineSeparator());
			}
			return output.toString();
		}
	}

	/**
	 * Saves a given string content into a single-entry ZIP file.
	 * <p>
	 * The created ZIP archive will contain one entry named after the {@code archiveName} parameter with a {@code .json} extension. The content is written using UTF-8 encoding.
	 * This method will fail if any parameter is null or if the provided content is empty.
	 *
	 * @param outputFile  The target ZIP file to be created or overwritten. Must not be null.
	 * @param archiveName The base name for the entry inside the ZIP file (e.g., "Example"). Must not be null.
	 * @param content     The string content to be written into the ZIP entry. Must not be null or empty.
	 * @return {@code true} if the file was saved successfully, {@code false} otherwise.
	 */
	public static boolean saveToZipFile(File outputFile, String archiveName, String content) {
		// --- Validation Block 1: Check for null parameters (Technical Error) ---
		if (outputFile == null || archiveName == null || content == null) {
			Log.warn("Failed to save ZIP file: One or more required parameters were null.");
			return false;
		}

		// --- Validation Block 2: Check for empty content (Logical Error) ---
		if (content.isEmpty()) {
			Log.warn("Failed to save ZIP file: The provided content was empty. Aborting to prevent creating an empty archive.");
			return false;
		}

		Log.fine("Exporting as ZIP: " + outputFile.getName());

		try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile); ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

			// Create a new ZIP entry with the specified name and .json extension.
			ZipEntry zipEntry = new ZipEntry(archiveName + ".json");
			zipOutputStream.putNextEntry(zipEntry);

			// Write the content to the ZIP entry using UTF-8 encoding.
			zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();

			return true;

		} catch (IOException ex) {
			String message = "Unable to write to ZIP file:\n" + outputFile.getPath();
			WindowUtils.showAlertDialog(Alert.AlertType.ERROR, null, "IOException", "Failed to write ZIP file", message);
			Log.warn(ex, message);
			return false;
		}
	}

	/**
	 * Reads the content of a specific entry from a ZIP file into a String.
	 * <p>
	 * This method safely handles file validation and ensures the content is decoded using UTF-8. It returns {@code null} if the file doesn't exist, the entry is not found, or an
	 * I/O error occurs during reading.
	 *
	 * @param zipFile   The ZIP file to read from. Must not be null and must exist.
	 * @param entryName The exact name of the entry to read (e.g., "Example.json"). Must not be null or empty.
	 * @return The content of the ZIP entry as a String, or {@code null} if the operation fails.
	 */
	public static String readFromZipFile(File zipFile, String entryName) {
		// --- Validation Block 1: Check for null parameters ---
		if (zipFile == null || entryName == null) {
			Log.warn("Failed to read from ZIP: The zipFile or entryName parameter was null.");
			return null;
		}
		// --- Validation Block 2: Check for logical errors ---
		if (entryName.isEmpty()) {
			Log.warn("Failed to read from ZIP: The entryName parameter cannot be empty.");
			return null;
		}
		if (!zipFile.exists()) {
			Log.warn("Failed to read from ZIP: The file does not exist at path: " + zipFile.getAbsolutePath());
			return null;
		}

		Log.fine("Reading from ZIP: " + zipFile.getName() + ", Entry: " + entryName);

		try (FileInputStream fileInputStream = new FileInputStream(zipFile); ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {

			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				if (entryName.equals(zipEntry.getName())) {
					Log.fine("Found matching entry: " + zipEntry.getName());

					// Read the content from the entry using a buffer.
					StringBuilder contentBuilder = new StringBuilder();
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = zipInputStream.read(buffer)) != -1) {
						contentBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
					}

					zipInputStream.closeEntry();
					Log.fine("Successfully read content from ZIP entry.");
					return contentBuilder.toString();
				}
			}

			// If the loop completes without finding the entry.
			Log.warn("Specified entry '" + entryName + "' was not found in ZIP file: " + zipFile.getName());

		} catch (IOException ex) {
			String message = "Unable to read from ZIP file:\n" + zipFile.getPath();
			WindowUtils.showAlertDialog(Alert.AlertType.ERROR, null, "IOException", "Failed to read ZIP file", message);
			Log.warn(ex, message);
		}

		// Return null if the entry was not found or an exception occurred.
		return null;
	}

	/**
	 * Checks if a file is a GZIP stream by verifying its "magic number". A GZIP file must start with the bytes 0x1F 0x8B.
	 *
	 * @param file The file to check.
	 * @return true if the file is a valid GZIP stream, false otherwise.
	 */
	public static boolean isGzipFile(File file) {
		if (file == null || !file.exists() || file.length() < 2) {
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			// GZIP magic number is 0x1f8b (big-endian)
			return raf.readShort() == 0x1f8b;
		} catch (IOException e) {
			Log.warn(e, "Could not read file to check for GZIP magic number: " + file.getName());
			return false;
		}
	}

	/**
	 * Checks if a file is a ZIP archive by verifying its "magic number". A ZIP file must start with the bytes 0x50 0x4B 0x03 0x04.
	 *
	 * @param file The file to check.
	 * @return true if the file is a valid ZIP archive, false otherwise.
	 */
	public static boolean isZipFile(File file) {
		if (file == null || !file.exists() || file.length() < 4) {
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			int magic = raf.readInt();
			// The magic number for a ZIP file is 0x504B0304 (little-endian)
			// readInt() reads it as big-endian, so we need to compare to the reversed byte order.
			return magic == 0x504B0304;
		} catch (IOException e) {
			// Log the error for debugging but return false as it's not a valid/readable ZIP.
			Log.warn(e, "Could not read file to check for ZIP magic number: " + file.getName());
			return false;
		}
	}

}