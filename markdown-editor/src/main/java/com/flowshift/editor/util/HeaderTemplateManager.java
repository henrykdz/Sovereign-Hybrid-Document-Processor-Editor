package com.flowshift.editor.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class HeaderTemplateManager {
	private final Path templateDir;

	public HeaderTemplateManager(Path unused) { // Pfad-Parameter wird ignoriert für globale Konsistenz
		// Wir erden den Manager fest im Anwendungsverzeichnis
		Path appRootDir = Paths.get(System.getProperty("user.dir"));

		// Die Hierarchie: templates/header_templates
		this.templateDir = appRootDir.resolve("templates").resolve("header_templates");

		ensureDirectoryExists();
		seedDefaultsIfEmpty(); // Schreibt die Dateien beim ersten Start
	}

	private void seedDefaultsIfEmpty() {
		if (getAvailableStyles().isEmpty()) {
			saveTemplate("Professional Clean", """
			        <div style="margin-bottom: 10mm; padding-bottom: 5px; border-bottom: 1px solid #ccc; display: flex; justify-content: space-between; align-items: center;">
			            <div style="display: flex; align-items: center;">
			                <img src="{{logoUrl}}" style="max-height: 30px; margin-right: 10px;">
			                <span style="font-size: 14pt; font-weight: bold; color: #003366;">{{companyName}}</span>
			            </div>
			            <div style="text-align: right; font-size: 10pt; color: #555;">{{date}} | {{time}}</div>
			        </div>""");

			saveTemplate("Modern Compact", """
			        <div style="border-left: 5px solid #20BFDF; padding-left: 15px; margin-bottom: 8mm;">
			            <h1 style="margin: 0; font-size: 18pt;">{{documentTitle}}</h1>
			            <div style="color: #666;">{{authorName}} &bull; {{companyName}} &bull; {{date}}</div>
			        </div>""");

			saveTemplate("Minimalist (Logo Only)", """
			        <div style="text-align: center; margin-bottom: 15mm;">
			            <img src="{{logoUrl}}" style="max-height: 60px;">
			        </div>""");
		}
	}

	private void ensureDirectoryExists() {
		try {
			Files.createDirectories(templateDir);
		} catch (IOException ignored) {
		}
	}

	public List<String> getAvailableStyles() {
		try (var stream = Files.list(templateDir)) {
			return stream.filter(p -> p.toString().endsWith(".html")).map(p -> p.getFileName().toString().replace(".html", "")).collect(Collectors.toList());
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}

	public String loadTemplate(String name) {
		Path file = templateDir.resolve(name + ".html");
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "";
		}
	}

	public void saveTemplate(String name, String html) {
		Path file = templateDir.resolve(name + ".html");
		try {
			Files.writeString(file, html, StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}

	/**
	 * Entfernt einen Header-Stil permanent aus der Bibliothek.
	 * 
	 * @param name Der Name des Stils (ohne .html Endung).
	 */
	public void deleteTemplate(String name) {
		if (name == null || "NONE".equals(name))
			return;

		Path file = templateDir.resolve(name + ".html");
		try {
			// Wir nutzen deleteIfExists, um robust gegen nicht existierende Dateien zu sein
			Files.deleteIfExists(file);
		} catch (IOException e) {
			// Im Fehlerfall loggen wir es, um die Souveränität zu wahren
			System.err.println("Failed to delete header template: " + file.toAbsolutePath());
			e.printStackTrace();
		}
	}
}