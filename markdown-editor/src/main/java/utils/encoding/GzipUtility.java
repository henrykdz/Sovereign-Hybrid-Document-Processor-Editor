package utils.encoding;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javafx.scene.control.Alert;
import utils.logging.Log;
import utils.ui.WindowUtils;

/**
 * Utility class for Gzip compression and decompression operations. This class provides methods to compress and decompress files and strings using Gzip format.
 */
public class GzipUtility {

	// private constructor
	private GzipUtility() {
	}

	/**
	 * Compresses the input file into a Gzip file.
	 * 
	 * @param inputFile  the file to be compressed
	 * @param outputFile the compressed output file
	 * @return true if compression is successful, false otherwise
	 */
	public static boolean compressFile(File inputFile, File outputFile) {
		try (FileInputStream fis = new FileInputStream(inputFile); FileOutputStream fos = new FileOutputStream(outputFile); GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer)) > 0) {
				gzipOS.write(buffer, 0, length);
			}
			gzipOS.finish(); // Finish the compression
			return true;
		} catch (IOException e) {
			Log.exceptionShow(e, "Error during Gzip compression.");
			return false;
		}
	}

	public static boolean compressFile(File outputFile, String string) {
		Log.fine("Exporting as GZIP: " + outputFile.getName());

		try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(outputFile))) {
			// Write JSON data to GZIP stream
			gzipOutputStream.write(string.getBytes(StandardCharsets.UTF_8));
			Log.fine("Successfully wrote GZIP file: " + outputFile.getAbsolutePath() + "\n");
			return true;
		} catch (IOException ex) {
			String message = "Unable to write to GZIP file:\n" + outputFile.getPath();
			WindowUtils.showAlertDialog(Alert.AlertType.ERROR, null, "IOException", "Failed to write user data", message);
			Log.warn(ex, message);
			return false;
		}
	}

	/**
	 * Decompresses a Gzip file to a specified output file.
	 * 
	 * @param inputFile  the Gzip file to be decompressed
	 * @param outputFile the file to write the decompressed data to
	 * @return true if decompression is successful, false otherwise
	 */
	public static boolean decompressFile(File inputFile, File outputFile) {
		try (FileInputStream fis = new FileInputStream(inputFile); GZIPInputStream gzipIS = new GZIPInputStream(fis); FileOutputStream fos = new FileOutputStream(outputFile)) {

			byte[] buffer = new byte[1024];
			int length;
			while ((length = gzipIS.read(buffer)) > 0) {
				fos.write(buffer, 0, length);
			}
			return true;
		} catch (IOException e) {
			Log.exceptionShow(e, "Error during Gzip decompression.");
			return false;
		}
	}

	/**
	 * Compresses a String into a Gzip file.
	 * 
	 * @param content    the string content to be compressed
	 * @param outputFile the file to write the compressed content to
	 * @return true if compression is successful, false otherwise
	 */
	public static boolean compressString(String content, File outputFile) {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes());
		        FileOutputStream fos = new FileOutputStream(outputFile);
		        GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

			byte[] buffer = new byte[1024];
			int length;
			while ((length = bais.read(buffer)) > 0) {
				gzipOS.write(buffer, 0, length);
			}
			gzipOS.finish(); // Finish the compression
			return true;
		} catch (IOException e) {
			Log.exceptionShow(e, "Error during Gzip compression of String.");
			return false;
		}
	}

	// /**
	// * Compresses a string into a Gzip file.
	// * @param data the string to compress
	// * @param destinationFile the destination file for the compressed data
	// * @throws IOException if an I/O error occurs during compression
	// */
	// public static void compressStringToFile(String data, File destinationFile) throws IOException {
	// try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(destinationFile))) {
	// byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
	// gzipOutputStream.write(dataBytes);
	// gzipOutputStream.finish();
	// } catch (IOException e) {
	// // Log the exception and rethrow the exception to notify the caller
	// Log.exceptionShow(e, "Error during Gzip compression of String.");
	// throw e; // Rethrow the exception after logging it
	// }
	// }

	/**
	 * Decompresses a Gzip file and returns the decompressed content as a String.
	 * 
	 * @param inputFile the Gzip file to be decompressed
	 * @return the decompressed content as a String, or null if an error occurs
	 */
	public static String decompressToString(File inputFile) {
		try (FileInputStream fis = new FileInputStream(inputFile); GZIPInputStream gzipIS = new GZIPInputStream(fis); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			byte[] buffer = new byte[1024];
			int length;
			while ((length = gzipIS.read(buffer)) > 0) {
				baos.write(buffer, 0, length);
			}
			return baos.toString(); // Return decompressed content as String
		} catch (IOException e) {
			Log.exceptionShow(e, "Error during Gzip decompression to String.");
			return null;
		}
	}

	public static StringBuilder decompressGzipToStringBuilder(File compressedFile) {
		StringBuilder stringBuilder = new StringBuilder();

		// Error handling during GZIP decompression
		try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(compressedFile)); BufferedReader reader = new BufferedReader(new InputStreamReader(gis))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
		} catch (FileNotFoundException e) {
			Log.exceptionShow(e, "File not found: " + compressedFile.getAbsolutePath());
		} catch (IOException e) {
			Log.exceptionShow(e, "Error while reading GZIP file: " + compressedFile.getAbsolutePath());
		}

		return stringBuilder;
	}

	/**
	 * Checks whether a file is in Gzip format.
	 * 
	 * @param file the file to check
	 * @return true if the file is in Gzip format, false otherwise
	 */
	public static boolean isGzipFile(File file) {
		try (FileInputStream fis = new FileInputStream(file); GZIPInputStream gzipIS = new GZIPInputStream(fis)) {
			gzipIS.read(); // Try to read to check if it is a valid Gzip file
			return true;
		} catch (IOException e) {
			return false; // If an exception occurs, it's not a valid Gzip file
		}
	}
}
