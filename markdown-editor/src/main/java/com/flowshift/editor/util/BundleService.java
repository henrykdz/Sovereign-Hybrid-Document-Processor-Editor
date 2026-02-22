package com.flowshift.editor.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowshift.editor.model.BundleManifest;

public class BundleService {

	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Entpackt ein .fsb Bundle und integriert die Ressourcen (Markdown, HTML, Preview) in die lokale Library in user.dir.
	 * 
	 * @param bundlePath    Pfad zur .fsb Quelldatei.
	 * @param templatesRoot Zielpfad für das Markdown-Template (meist /templates/premium).
	 * @param headerRoot    Zielpfad für das Header-Design (meist /templates/header_templates).
	 * @param previewRoot   Zielpfad für das Vorschaubild (Cache-Ordner für die Galerie).
	 * @return Das geladene BundleManifest für die UI-Bestätigung.
	 */
	public BundleManifest importBundle(Path bundlePath, Path templatesRoot, Path headerRoot, Path previewRoot) throws IOException {
		BundleManifest manifest = null;
		byte[] mdContent = null;
		byte[] htmlContent = null;
		byte[] thumbData = null;

		// 1. EXTRAKTION: Wir lesen den verschlüsselten/gepackten Stream
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(bundlePath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				byte[] bytes = zis.readAllBytes();
				switch (entry.getName()) {
				case "manifest.json" -> manifest = mapper.readValue(bytes, BundleManifest.class);
				case "template.md"   -> mdContent = bytes;
				case "header.html"   -> htmlContent = bytes;
				case "preview.png"   -> thumbData = bytes;
				}
				zis.closeEntry();
			}
		}

		// 2. VALIDIERUNG: Ohne Manifest und Blueprint ist das Bundle wertlos
		if (manifest == null || mdContent == null) {
			throw new IOException("Invalid flowShift Bundle: Manifest or Template missing.");
		}

		// 3. INTEGRATION: Ressourcen in den App-Nexus einordnen
		// Dateisystem-Sicherheit: Wir säubern den Namen für den Pfad
		String safeBaseName = manifest.name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");

		// A. Markdown Blueprint speichern
		Files.createDirectories(templatesRoot);
		Files.write(templatesRoot.resolve(safeBaseName + ".md"), mdContent);

		// B. Header-Style integrieren (Falls vorhanden)
		if (htmlContent != null && manifest.headerStyle != null) {
			Files.createDirectories(headerRoot);
			// Wir nutzen den vom Creator definierten Stil-Namen
			Files.write(headerRoot.resolve(manifest.headerStyle + ".html"), htmlContent);
		}

		// C. Vorschaubild in den App-Cache spiegeln (Wichtig für die Galerie!)
		if (thumbData != null) {
			Files.createDirectories(previewRoot);
			Files.write(previewRoot.resolve(safeBaseName + ".png"), thumbData);
		}

		return manifest;
	}

	/**
	 * Erzeugt ein .fsb Bundle inklusive Vorschaubild.
	 */
	public void createBundle(Path targetPath, BundleManifest manifest, String mdContent, String htmlContent, byte[] thumbData) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetPath))) {
			addToZip(zos, "manifest.json", mapper.writeValueAsBytes(manifest));
			addToZip(zos, "template.md", mdContent.getBytes(StandardCharsets.UTF_8));

			if (htmlContent != null)
				addToZip(zos, "header.html", htmlContent.getBytes(StandardCharsets.UTF_8));

			// NEU: Das Bild wandert mit ins Paket
			if (thumbData != null)
				addToZip(zos, "preview.png", thumbData);
		}
	}

	private void addToZip(ZipOutputStream zos, String fileName, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(fileName);
		zos.putNextEntry(entry);
		zos.write(data);
		zos.closeEntry();
	}
}