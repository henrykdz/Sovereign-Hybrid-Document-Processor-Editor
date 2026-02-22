package utils.detection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import utils.general.StringUtils;
import utils.logging.Log;

/**
 * Eine statische Utility-Klasse für die Interaktion mit der System-Zwischenablage.
 * <p>
 * Diese Klasse ist zustandslos und bietet Methoden, um den Inhalt der Zwischenablage zu prüfen, strukturierte Daten (Pathment) zu extrahieren und URLs in die Zwischenablage zu
 * schreiben.
 * </p>
 * Sie ist als {@code final} deklariert und hat einen privaten Konstruktor, um eine Instanziierung zu verhindern.
 */
public final class ClipboardUtil {

	/**
	 * Privater Konstruktor, um die Instanziierung dieser Utility-Klasse zu verhindern.
	 */
	private ClipboardUtil() {
		// Diese Klasse soll nicht instanziiert werden.
	}

	/**
	 * Prüft, ob die System-Zwischenablage Inhalte enthält, die für die Anwendung potenziell relevant sind (URL, Dateien oder String).
	 *
	 * @return true, wenn die Zwischenablage eine URL, Dateien oder einen String enthält, andernfalls false.
	 */
	public static boolean isContentInClipboard() {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		return clipboard.hasUrl() || clipboard.hasFiles() || clipboard.hasString();
	}

	/**
	 * Analyzes the clipboard for supported content (URL, String, or Files) and attempts to parse it into a list of {@link Pathment} objects.
	 * <p>
	 * This method now robustly handles plain strings by using {@code PathmentExtractor.parse()}, which ensures that a {@code Pathment} object (even if {@code UNSPECIFIED}) is
	 * created, allowing the UI to react to any string content in the clipboard.
	 *
	 * @return A {@code List} of {@link Pathment} objects extracted from the clipboard. Returns an empty list if no parsable content is found.
	 */
	public static List<Pathment> getClipboardContent() {
		Log.info("getClipboardContent");
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final List<Pathment> resultList = new ArrayList<>();

		if (clipboard.hasUrl()) {
			Log.fine("Clipboard has a URL.");
			String url = clipboard.getUrl();
			if (StringUtils.isNotBlank(url)) {
				resultList.add(PathmentExtractor.parse(url));
			}

		} else if (clipboard.hasString()) {
			Log.fine("Clipboard has a String.");
			String content = clipboard.getString();

			if (StringUtils.isNotBlank(content)) {
				// --- HIER IST DIE INTELLIGENTE ENTSCHEIDUNG ---

				// Heuristik: Wenn der Text Zeilenumbrüche enthält oder sehr lang ist,
				// behandeln wir ihn als Textblock, der gescannt werden muss.
				boolean isBlockOfText = content.contains("\n") || content.length() > 200;

				if (isBlockOfText) {
					// Es ist ein Textblock -> benutze perform(), um ihn zu scannen.
					Log.fine("Content appears to be a block of text. Using perform() to scan for multiple paths.");
					List<Pathment> parsedFromString = PathmentExtractor.perform(content);
					if (parsedFromString != null && !parsedFromString.isEmpty()) {
						resultList.addAll(parsedFromString);
					}
				} else {
					// Es ist eine einzelne Zeile -> benutze parse(), um sie als Ganzes zu behandeln.
					Log.fine("Content is a single line. Using parse() to treat it as one entity.");
					Pathment pathment = PathmentExtractor.parse(content);
					if (pathment != null) {
						// Wichtig: Wir fügen es hinzu, auch wenn es UNSPECIFIED ist, damit der
						// Controller den Clipboard-Button anzeigen kann.
						resultList.add(pathment);
					}
				}
			}

		} else if (clipboard.hasFiles()) {
			Log.fine("Clipboard has Files.");
			List<File> files = clipboard.getFiles();
			if (files != null && !files.isEmpty()) {
				files.forEach(file -> {
					Pathment p = PathmentConverter.from(file);
					if (p != null) {
						resultList.add(p);
					}
				});
			}
		}

		Log.info("End of getClipboardContent(): resultList size: " + resultList.size());
		return resultList;
	}

	/**
	 * Platziert eine gegebene URL in der System-Zwischenablage. Der Inhalt wird sowohl als reiner Text als auch als URL-Typ gesetzt, um die Kompatibilität mit anderen Anwendungen
	 * zu verbessern.
	 *
	 * @param url Die URL, die in die Zwischenablage kopiert werden soll.
	 */
	public static void putUrlIntoSystemClipboard(String url) {
		if (url == null) {
			return;
		}
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(url);
		content.putUrl(url);
		clipboard.setContent(content);
	}
}