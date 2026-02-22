package com.flowshift.editor;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import utils.localize.LangMap;
import utils.logging.Log;

/**
 * Launcher v1.1 - The Entry Point of the FlowShift Markdown Editor.
 * 
 * This class is responsible for initializing the JavaFX environment,
 * loading the primary FXML layout, applying global stylesheets, 
 * and establishing the main application window (Stage).
 */
public class Launcher extends Application {

	// --- GEOMETRY CONSTANTS ---
	private static final double INITIAL_WIDTH  = 1400.0;
	private static final double INITIAL_HEIGHT = 940.0;
	private static final double MIN_WIDTH      = 900.0; // Adjusted for sidebar + editor usability
	private static final double MIN_HEIGHT     = 600.0;

	// --- RESOURCE PATHS ---
	private static final String FXML_PATH     = "/com/flowshift/editor/MarkdownEditor.fxml";
	private static final String CSS_MAIN_PATH = "/com/flowshift/editor/editor-style.css";

	@Override
	public void start(Stage primaryStage) {
		try {
		    LangMap.init();

			// 1. Load the Primary UI Structure
			URL fxmlUrl = getClass().getResource(FXML_PATH);
			if (fxmlUrl == null) {
				throw new IOException("Critical Resource Missing: FXML file not found at " + FXML_PATH);
			}

			FXMLLoader loader = new FXMLLoader(fxmlUrl);
			// We use Parent to keep the root generic, although it's physically a BorderPane
			Parent root = loader.load();

			// 2. Global Styling Integration
			applyGlobalStyles(root);

			// 3. Scene Initialization
			Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);

			// 4. Primary Stage Configuration
			primaryStage.setTitle("FlowShift Markdown Editor (JavaFX 23)");
			primaryStage.setScene(scene);
			
			// Enforce minimum window dimensions for layout stability
			primaryStage.setMinWidth(MIN_WIDTH);
			primaryStage.setMinHeight(MIN_HEIGHT);

			// 5. Lifecycle & Focus Handlers
			MarkdownEditorController controller = loader.getController();
			if (controller != null) {
				Log.info("Launcher: Controller successfully attached to main stage.");
			}

			primaryStage.show();
			
			// Ensure the window is active and visible to the user
			primaryStage.toFront();
			root.requestFocus();

		} catch (IOException e) {
			Log.error(e, "Launcher: Critical initialization failure.");
			showErrorAndExit("Initialization Error", "The user interface could not be loaded.", e);
		}
	}

	/**
	 * Locates and applies the main stylesheet to the root node.
	 * Degrades gracefully with a console warning if the CSS resource is missing.
	 */
	private void applyGlobalStyles(Parent root) {
		URL cssUrl = getClass().getResource(CSS_MAIN_PATH);
		if (cssUrl != null) {
			root.getStylesheets().add(cssUrl.toExternalForm());
		} else {
			System.err.println("WARNING: Global stylesheet not found at " + CSS_MAIN_PATH + ". Falling back to default UI skin.");
		}
	}

	/**
	 * Displays a critical error dialog and terminates the application process.
	 * Ensures the error is reported on the JavaFX application thread.
	 */
	private void showErrorAndExit(String title, String message, Exception e) {
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle(title);
			alert.setHeaderText(message);
			alert.setContentText("Internal Details: " + e.getMessage());
			
			alert.showAndWait();
			
			// Forceful exit to prevent hanging processes
			Platform.exit();
			System.exit(1);
		});
	}

	/**
	 * Main execution entry point. 
	 * Configures hardware-accelerated text rendering before launching the toolkit.
	 */
	public static void main(String[] args) {
		// Optimization: Enhance text rendering quality for high-resolution displays
		System.setProperty("prism.lcdtext", "true"); 
		System.setProperty("prism.text", "t2k"); 
		
		launch(args);
	}
}