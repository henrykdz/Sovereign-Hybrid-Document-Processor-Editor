package utils;


import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * FileOpeningService (Minimalist Edition)
 * Ein asynchroner Dienst zum Öffnen von Dateien und Verzeichnissen
 * im Standard-Dateimanager oder Browser des Betriebssystems.
 * Verhindert UI-Freezes durch Thread-Entkopplung.
 */
public class FileOpeningService {

    private static final FileOpeningService INSTANCE = new FileOpeningService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FileOpeningService() {}

    public static FileOpeningService getInstance() {
        return INSTANCE;
    }

    /**
     * Öffnet eine Datei oder einen Ordner im System-Standard (Explorer/Finder/Browser).
     * Die Operation läuft im Hintergrund.
     */
    public void openSystemDefault(File target) {
        if (target == null) return;

        executor.execute(() -> {
            try {
                if (!Desktop.isDesktopSupported()) {
                    showError("Not Supported", "System integration is not supported on this platform.");
                    return;
                }

                Desktop desktop = Desktop.getDesktop();
                if (!target.exists()) {
                    showError("File Not Found", "The path does not exist:\n" + target.getAbsolutePath());
                    return;
                }

                // Öffnet die Datei (oder den Ordner) mit der Standard-Anwendung
                desktop.open(target);

            } catch (IOException e) {
                showError("Launch Error", "Could not open file:\n" + e.getMessage());
            } catch (SecurityException e) {
                showError("Permission Error", "Access denied to:\n" + target.getAbsolutePath());
            }
        });
    }

    /**
     * Zeigt einen Fehler-Dialog auf dem UI-Thread an.
     */
    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            // Nutze dein Styling, falls global verfügbar, sonst Standard
            if (alert.getDialogPane().getScene().getStylesheets().isEmpty()) {
                 var css = getClass().getResource("/com/flowshift/editor/editor-style.css");
                 if (css != null) alert.getDialogPane().getStylesheets().add(css.toExternalForm());
            }
            alert.getDialogPane().getStyleClass().add("nexus-dialog");
            alert.showAndWait();
        });
    }
}