package com.flowshift.editor.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import utils.logging.Log;

/**
 * MarkdownTemplateManager v7.7.2
 * Verwaltet Templates UND Dokumente aus dem Vault.
 */
public class MarkdownTemplateManager {

	private final Map<String, String> templates = new LinkedHashMap<>();
	private final Path rootPath;
	private final Path vaultPath;
	private final Path premiumPath;
	private final Path customPath;
	
	private static final List<String> BUILT_IN_FILES = List.of("Betrieb");

	public MarkdownTemplateManager() {
		this.rootPath = Paths.get(System.getProperty("user.dir"), "templates");
		this.vaultPath = Paths.get(System.getProperty("user.dir"), "documents");
		this.premiumPath = rootPath.resolve("premium");
		this.customPath = rootPath.resolve("custom");

		ensureDirectoriesExist();
		ensureVaultDirectoryExists();
		loadTemplates();
	}

	private void ensureDirectoriesExist() {
		try { Files.createDirectories(premiumPath); Files.createDirectories(customPath); } 
		catch (IOException ignored) {}
	}

	private void ensureVaultDirectoryExists() {
		try { Files.createDirectories(vaultPath); } 
		catch (IOException ignored) {}
	}
	
	public Path getRootPath() { return rootPath; }
	
	public void loadTemplates() {
		templates.clear();
		loadBuiltIn();
		loadFromDir(premiumPath);
		loadFromDir(customPath);
	}

	private void loadBuiltIn() {
		for (String name : BUILT_IN_FILES) {
			String resPath = "/com/flowshift/editor/templates/" + name.toLowerCase() + ".md";
			try (InputStream is = getClass().getResourceAsStream(resPath)) {
				if (is != null) templates.put(name, new String(is.readAllBytes(), StandardCharsets.UTF_8));
			} catch (IOException ignored) {}
		}
	}

	private void loadFromDir(Path dir) {
		if (!Files.exists(dir)) return;
		try (var stream = Files.list(dir)) {
			stream.filter(p -> p.toString().endsWith(".md")).forEach(p -> {
				try {
					String name = p.getFileName().toString();
					templates.put(name, Files.readString(p, StandardCharsets.UTF_8));
				} catch (IOException ignored) {}
			});
		} catch (IOException ignored) {}
	}

	public void saveCustomTemplate(String name, String content) throws IOException {
		Path file = customPath.resolve(name + ".md");
		Files.writeString(file, content, StandardCharsets.UTF_8);
		templates.put(name, content);
	}

	public List<String> getTemplateNames() {
		return new ArrayList<>(templates.keySet()).stream().sorted().toList();
	}

	public List<String> getDocumentsFromVault() {
		if (!Files.exists(vaultPath)) return new ArrayList<>();
		try (var stream = Files.list(vaultPath)) {
			return stream.filter(p -> p.toString().endsWith(".md"))
			             .map(p -> p.getFileName().toString())
			             .sorted().toList();
		} catch (IOException e) { return new ArrayList<>(); }
	}

	public void deleteDocumentFromVault(String fileName) throws IOException {
		Path file = vaultPath.resolve(fileName);
		Files.deleteIfExists(file);
	}

	public List<String> getTemplatesByCategory(String category) {
		return switch (category.toLowerCase()) {
			case "custom"         -> loadNamesFromDir(customPath);
			case "premium"        -> loadNamesFromDir(premiumPath);
			case "document vault" -> getDocumentsFromVault(); // Dein Case "Documents" angepasst
			default               -> getTemplateNames(); // All
		};
	}

	/**
	 * Lädt Dateinamen aus einem Verzeichnis. 
	 * Akzeptiert .md, .txt UND Dateien ohne Endung (Souveräne Resilienz).
	 */
	private List<String> loadNamesFromDir(Path dir) {
		if (!Files.exists(dir)) return new ArrayList<>();
		try (var stream = Files.list(dir)) {
			return stream.filter(p -> {
			    String name = p.getFileName().toString().toLowerCase();
			    // HEILUNG: Wir lassen auch Dateien ohne Punkt durch,
			    // damit sie im System sichtbar bleiben und repariert werden können.
			    return name.endsWith(".md") || name.endsWith(".txt") || !name.contains(".");
			})
			.map(p -> p.getFileName().toString())
			.sorted().toList();
		} catch (IOException e) { 
			return new ArrayList<>(); 
		}
	}


	public void deleteTemplate(String name) throws IOException {
		Path customFile = customPath.resolve(name);
		Path premiumFile = premiumPath.resolve(name);
		Path previewFile = rootPath.resolve("previews").resolve(name.replace(".md", ".png"));

		if (Files.deleteIfExists(customFile)) Log.info("Deleted custom template: " + name);
		else if (Files.deleteIfExists(premiumFile)) Log.info("Deleted premium template: " + name);

		Files.deleteIfExists(previewFile);
		loadTemplates();
	}

	public String getTemplateContent(String name) {
		return templates.getOrDefault(name, "");
	}
	
	/**
	 * Liest den Inhalt eines Dokuments aus dem Vault.
	 * Benötigt für den "Manual Refresh" der Vorschau.
	 */
	public String getVaultDocumentContent(String fileName) {
		Path file = vaultPath.resolve(fileName);
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			return "Error: Could not load document " + fileName;
		}
	}
}