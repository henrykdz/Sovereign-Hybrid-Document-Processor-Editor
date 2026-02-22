package com.flowshift.editor.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.flowshift.editor.model.DocumentSettings;

/**
 * Manages reading and writing of project-specific settings (settings.json). These settings are stored in the same directory as the currently loaded Markdown file. Uses Jackson
 * Databind for reliable JSON serialization.
 */
public class ProjectSettingsManager {

	private static final String SETTINGS_FILE_NAME = "settings.json";
	private final ObjectMapper  mapper;

	public ProjectSettingsManager() {
		this.mapper = new ObjectMapper();
		// Optionale Einstellung für lesbare JSON-Dateien
		this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	/**
	 * Attempts to load settings.json from the given project directory. If the file does not exist, returns a default ProjectSettings object.
	 * 
	 * @param projectPath The root directory of the project (where the .md file is located).
	 * @return Loaded settings or defaults.
	 */
	public DocumentSettings loadSettings(Path projectPath) {
		Path filePath = projectPath.resolve(SETTINGS_FILE_NAME);
		File settingsFile = filePath.toFile();

		if (settingsFile.exists() && settingsFile.length() > 0) {
			try {
				return mapper.readValue(settingsFile, DocumentSettings.class);
			} catch (IOException e) {
				System.err.println("Error loading project settings from " + SETTINGS_FILE_NAME + ". Using defaults. Error: " + e.getMessage());
				// Fallback to default on read failure
				return new DocumentSettings();
			}
		}

		// If file does not exist, return defaults
		return new DocumentSettings();
	}

	/**
	 * Saves the current ProjectSettings object to the project directory. Creates the file if it does not exist.
	 * 
	 * @param projectPath The root directory of the project.
	 * @param settings    The settings object to save.
	 * @return true if successful, false otherwise.
	 */
	public boolean saveSettings(Path projectPath, DocumentSettings settings) {
		Path filePath = projectPath.resolve(SETTINGS_FILE_NAME);
		File settingsFile = filePath.toFile();

		try {
			// Jackson serializes the object to the file
			mapper.writeValue(settingsFile, settings);
			return true;
		} catch (IOException e) {
			System.err.println("Error saving project settings to " + SETTINGS_FILE_NAME + ". Error: " + e.getMessage());
			return false;
		}
	}

	/**
	 * NEUE METHODE: Lädt IMMER aus dem zentralen Anwendungs-Root. Dies ist der souveräne Weg, da die Konfiguration an die App gebunden ist.
	 */
	public DocumentSettings loadSettings() {
		// Berechnet den zentralen Pfad und delegiert an die Ladelogik
		Path centralPath = Paths.get(System.getProperty("user.dir"));
		return loadSettings(centralPath);
	}

	// Auch die saveSettings braucht eine universelle Version
	public void saveSettings(DocumentSettings settings) {
		Path centralPath = Paths.get(System.getProperty("user.dir"));
		saveSettings(centralPath, settings);
	}
}