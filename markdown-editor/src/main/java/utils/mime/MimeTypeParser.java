package utils.mime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.scene.input.DataFormat;
import utils.general.StringUtils;

public class MimeTypeParser {
	private static final String EXTERNAL_BODY_MIME = "message/external-body";
//	private static final String RENDER_MIME        = "chromium/x-renderer-tain";

	/**
	 * Parses a set of DataFormat objects to identify and extract MIME data, returning the results as a list of MimeData objects.
	 * 
	 * @param dataFormats the set of DataFormat objects to inspect
	 * @return a list of MimeData objects extracted from the DataFormat set
	 */
	public static List<MimeData> parseMimeDataWithNames(Set<DataFormat> dataFormats) {
		List<MimeData> mimeDataList = new ArrayList<>();
		if (dataFormats == null || dataFormats.isEmpty()) {
			return mimeDataList; // No data available
		}

		// Instantiate the parser
		MimeTypeParser parser = new MimeTypeParser();

		// Iterate through the provided DataFormat set
		for (DataFormat format : dataFormats) { // Files/HTML/URL
			if (format.getIdentifiers() == null || format.getIdentifiers().isEmpty()) {
				continue; // should never happen
			}
			// Die id ist also ein String, der typischerweise den MIME-Typ (oder einen Identifier) darstellt "text/html", "image/jpeg"
			// Log.info("DataFormat: " + format.toString() + "- format size:" + format.getIdentifiers().size());
			for (String id : format.getIdentifiers()) {
				// Log.fine("id of dataformat: " + id);
				MimeData mimeData = null;

				// Check if the identifier matches specific MIME types
				if (id.startsWith(EXTERNAL_BODY_MIME)) {
					mimeData = parser.parse(id);
					if (mimeData != null && mimeData.getName() != null) {
						mimeDataList.add(mimeData);
					}
				}
				// currently no need
				// else if (id.startsWith(RENDER_MIME)) {
				// mimeData = parser.parse(id);
				// if (mimeData != null) {
				// mimeData.setRendered(true); // Set the rendered flag
				// }
				// } // else {}

			}
		}

		return mimeDataList;
	}

	//
	///**
	// * Parses a set of DataFormat objects to identify and extract specific MIME types
	// * and configures a MimeTypeParser instance accordingly.
	// * <p>This method is used when a URL drop (e.g., from a browser) may contain data
	// * with specific MIME type characteristics. It identifies two key MIME types:
	// * <ul>
	// * <li>"message/external-body": Indicates external data access</li>
	// * <li>"chromium/x-renderer-taint": Indicates a renderer taint specific to Chromium-based browsers</li>
	// * </ul>
	// * The extracted MIME type is then parsed and returned as a MimeTypeParser object.
	// * @param set the set of DataFormat objects to be inspected
	// * @return a MimeTypeParser instance initialized with the parsed MIME type
	// */
	// public static MimeTypeParser getParsedMimeObject(Set<DataFormat> set) {
	// // Create a new MimeTypeParser instance to store the parsed information
	// MimeTypeParser mtp = new MimeTypeParser();
	//
	// // Initialize MIME type strings to check for specific types
	// String externalBodyMime = "message/external-body";
	// String renderMime = "chromium/x-renderer-taint";
	//
	// // Variable to store the MIME type if identified
	// String mime = "";
	//
	// // Iterate through the provided DataFormat set
	// Iterator<DataFormat> iterator = set.iterator();
	// while (iterator.hasNext()) {
	// // Get the current DataFormat block
	// DataFormat block = iterator.next();
	//
	// // Extract its identifiers
	// Set<String> idSet = block.getIdentifiers();
	// Iterator<String> idIt = idSet.iterator();
	//
	// // Check each identifier for specific MIME patterns
	// while (idIt.hasNext()) {
	// String id = idIt.next();
	//
	// // If the identifier matches Chromium renderer taint, mark the parser as rendered
	// if (id.startsWith(renderMime)) {
	// mtp.setRendered(); // Marks the MIME type as associated with a rendered taint
	// }
	// // If the identifier matches external body MIME, save the MIME type for parsing
	// else if (id.startsWith(externalBodyMime)) {
	// mime = id;
	// }
	// }
	// }
	//
	// // Parse the MIME type if it was found
	// mtp.parse(mime);
	//
	// // Return the initialized MimeTypeParser object
	// return mtp;
	// }

	/**
	 * Parses the given MIME type string and extracts relevant information such as access type, index, and name based on the `message/external-body` format. The parser handles
	 * extensions described in RFC 1521 for `message/external-body`, particularly for clipboard access types. This format can include additional parameters separated by semicolons
	 * (e.g., `;index=1;access-type=clipboard`).
	 * 
	 * @param mimeFull the full MIME type string to parse.
	 */
	public MimeData parse(String mimetype) {
		if (StringUtils.isBlank(mimetype)) {
			return null;
		}

		// Check for the "message/external-body" MIME type
		if (!mimetype.startsWith(EXTERNAL_BODY_MIME)) {
			// Log.warn("mime is not of external type :" + mimetype);
			return new MimeData(mimetype);
		}

		// Log.info("mimetype to parse : " + mimetype);
		MimeData data = new MimeData(mimetype);

		// Split the MIME type and its parameters (e.g., ";access-type=clipboard")
		String[] mimeParts = mimetype.split(";");
		String accessType = "";
		int indexValue = -1;
		String nameValue = "";

		for (int i = 1; i < mimeParts.length; ++i) {
			String[] params = mimeParts[i].split("=");

			if (params.length == 2) {
				String key = params[0].trim();
				String value = params[1].trim();
				if (key.equalsIgnoreCase("index")) {
					indexValue = Integer.parseInt(value);
				} else if (key.equalsIgnoreCase("access-type")) {
					accessType = value;
				} else if (key.equalsIgnoreCase("name")) {
					nameValue = value.replaceAll("^\"|\"$", ""); // Remove quotes
				}
			}
		}

		// Populate the MimeData object
		data.setAccessType(accessType);
		data.setIndex(indexValue);
		data.setName(nameValue);
		return data;
	}
}