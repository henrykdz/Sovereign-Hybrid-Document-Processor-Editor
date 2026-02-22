/*
 * =============================================================================
 * Project: FlowShift - The Sovereign Content Engine
 * Component: MarkdownLinter
 * 
 * Copyright (c) 2026 FlowShift. All rights reserved.
 * Author: Henryk Daniel Zschuppan
 *
 * This source code is proprietary and confidential. Unauthorized copying 
 * of this file, via any medium, is strictly prohibited.
 *
 * DESIGN PHILOSOPHY: High-performance, context-aware structural validation
 * utilizing a single-pass Oracle-Backtick-Protocol for real-time processing.
 * =============================================================================
 */

package com.flowshift.editor.util;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * SnapshotService v2.0 - Sovereign Sequential Engine.
 * Garantiert Systemsicherheit durch sequentielle Abarbeitung und Ressourcen-Management.
 */
public class SnapshotService {

    // Die Warteschlange für anstehende Aufgaben
    private static final Queue<SnapshotTask> taskQueue = new LinkedList<>();
    private static boolean isProcessing = false;

    private static class SnapshotTask {
        String html;
        Path targetPath;
        Consumer<byte[]> callback;

        SnapshotTask(String html, Path targetPath, Consumer<byte[]> callback) {
            this.html = html;
            this.targetPath = targetPath;
            this.callback = callback;
        }
    }

    /**
     * Reiht einen Snapshot-Auftrag in die Warteschlange ein.
     */
    public static void create(String html, Path targetPath, Consumer<byte[]> callback) {
        if (html == null || html.isEmpty()) {
            if (callback != null) callback.accept(null);
            return;
        }

        synchronized (taskQueue) {
            taskQueue.add(new SnapshotTask(html, targetPath, callback));
            if (!isProcessing) {
                processNext();
            }
        }
    }

    private static void processNext() {
        synchronized (taskQueue) {
            if (taskQueue.isEmpty()) {
                isProcessing = false;
                return;
            }
            isProcessing = true;
            SnapshotTask task = taskQueue.poll();
            executeTask(task);
        }
    }

    private static void executeTask(SnapshotTask task) {
        Platform.runLater(() -> {
            WebView ghostWeb = new WebView();
            ghostWeb.setPrefSize(850, 1100);

            Stage ghostStage = new Stage(StageStyle.UTILITY);
            ghostStage.setOpacity(0);
            ghostStage.setX(-20000); // Weit außerhalb des Sichtbereichs
            ghostStage.setScene(new Scene(new StackPane(ghostWeb)));
            ghostStage.show();

            // Timeout-Schutz: Falls der Load-Worker hängen bleibt (max 5 Sekunden)
            PauseTransition timeoutGuard = new PauseTransition(Duration.seconds(5));
            timeoutGuard.setOnFinished(e -> {
                if (ghostStage.isShowing()) {
                    ghostStage.close();
                    if (task.callback != null) task.callback.accept(null);
                    processNext(); // Weiter zum nächsten Auftrag
                }
            });
            timeoutGuard.play();

            ghostWeb.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                if (state == Worker.State.SUCCEEDED) {
                    timeoutGuard.stop(); // Alles ok, Timeout abbrechen

                    // Rendering-Puffer für CSS-Effekte
                    PauseTransition paintDelay = new PauseTransition(Duration.millis(800));
                    paintDelay.setOnFinished(e -> {
                        try {
                            WritableImage img = ghostWeb.snapshot(null, null);
                            byte[] data = convertImageToBytes(img);
                            
                            if (task.targetPath != null && data != null) {
                                saveBytesToDisk(data, task.targetPath);
                            }
                            if (task.callback != null) task.callback.accept(data);
                        } finally {
                            ghostStage.close();
                            // WICHTIG: Rekursiver Aufruf für die Queue
                            processNext();
                        }
                    });
                    paintDelay.play();
                } else if (state == Worker.State.FAILED) {
                    timeoutGuard.stop();
                    ghostStage.close();
                    if (task.callback != null) task.callback.accept(null);
                    processNext();
                }
            });

            // Base-URL Injektion für lokale Ressourcen
            String baseUrl = new File(System.getProperty("user.dir")).toURI().toString();
            String htmlWithBase = task.html.replace("<head>", "<head><base href=\"" + baseUrl + "/\">");
            ghostWeb.getEngine().loadContent(htmlWithBase);
        });
    }

    private static byte[] convertImageToBytes(WritableImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(img, null), "png", baos);
            return baos.toByteArray();
        } catch (IOException e) { return null; }
    }

    private static void saveBytesToDisk(byte[] data, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException ignored) {}
    }

}
