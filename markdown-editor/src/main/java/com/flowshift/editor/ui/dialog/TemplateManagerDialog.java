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

package com.flowshift.editor.ui.dialog;

import com.flowshift.editor.model.DocumentSettings;
import com.flowshift.editor.util.MarkdownTemplateManager;
import com.flowshift.editor.util.SnapshotService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TemplateManagerDialog extends Dialog<TemplateManagerDialog.TemplateResult> {

    public record TemplateResult(ActionType action, String templateName) {}
    public enum ActionType { LOAD, IMPORT_REQUEST, EXPORT_REQUEST }

    private final MarkdownTemplateManager manager;
    private final Function<String, String> htmlProvider;
    private final BiConsumer<String, String> saveHandler;

    private final ListView<String>  templateList   = new ListView<>();
    private final ImageView         previewDisplay = new ImageView();
    private final TextField         nameField      = new TextField();
    private final ToggleGroup       categoryGroup  = new ToggleGroup();
    private final Button            btnRefresh     = new Button("Refresh Preview");

    public TemplateManagerDialog(MarkdownTemplateManager manager, DocumentSettings settings, 
                                 Function<String, String> htmlProvider, BiConsumer<String, String> saveHandler) {
        this.manager = manager;
        this.htmlProvider = htmlProvider;
        this.saveHandler = saveHandler;

        setTitle("FlowShift Template Library");
        getDialogPane().setPrefWidth(900);
        getDialogPane().getStyleClass().add("nexus-dialog");
        getDialogPane().getStylesheets().add(getClass().getResource("/com/flowshift/editor/editor-style.css").toExternalForm());
        
        // --- HAUPT-BUTTONS (Schließen den Dialog) ---
        ButtonType loadBtn = new ButtonType("Load Selected", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(loadBtn, ButtonType.CANCEL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // KATEGORIEN-FILTER
        HBox filterBox = new HBox(10, new Label("Category:"), createCategoryToggle("All", true), createCategoryToggle("Custom", false), createCategoryToggle("Premium", false));
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("segmented-button-bar");

        // BROWSER (Liste & Vorschau)
        HBox browserBox = new HBox(20);
        VBox.setVgrow(browserBox, Priority.ALWAYS);
        
        Button btnDelete = new Button("Delete selected");
        btnDelete.setId("btnDelete");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        VBox listBox = new VBox(8, new Label("Available Blueprints:"), templateList, btnDelete);
        HBox.setHgrow(listBox, Priority.ALWAYS);
        VBox.setVgrow(templateList, Priority.ALWAYS);
        templateList.setMaxHeight(Double.MAX_VALUE);

        previewDisplay.setFitWidth(400); 
        previewDisplay.setPreserveRatio(true);
        StackPane imageFrame = new StackPane(previewDisplay);
        imageFrame.setStyle("-fx-border-color: #404040; -fx-background-color: #1a1c24;");
        VBox.setVgrow(imageFrame, Priority.ALWAYS);
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        VBox imageBox = new VBox(8, new Label("Visual Preview:"), imageFrame, btnRefresh);

        browserBox.getChildren().addAll(listBox, imageBox);

        // BUNDLE ACTIONS
        HBox bundleBox = new HBox(10);
        Button btnImport = new Button("Import FlowShift Bundle...");
        Button btnExport = new Button("Export Current as Bundle...");
        bundleBox.getChildren().addAll(btnImport, btnExport);
        
        // SPEICHER-WIDGET
        VBox saveBox = new VBox(10);
        Label lbSave = new Label("Save current document as blueprint:");
        lbSave.setStyle("-fx-font-weight: bold; -fx-text-fill: #D79050;");
        Button btnSaveAs = new Button("Save as New");
        btnSaveAs.getStyleClass().add("save-as-button");
        nameField.setText(settings.getDocumentTitle());
        HBox nameAndSave = new HBox(10, nameField, btnSaveAs);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        saveBox.getChildren().addAll(lbSave, nameAndSave);
        
        root.getChildren().addAll(filterBox, browserBox, bundleBox, new Separator(), saveBox);
        getDialogPane().setContent(root);
        
        // --- LOGIK ---
        setupLogic(loadBtn, btnImport, btnExport, btnDelete, btnSaveAs);
        refreshList("All");
    }

    private void setupLogic(ButtonType loadBtn, Button importBtn, Button exportBtn, Button deleteBtn, Button saveBtn) {
        // Filter & Preview
        categoryGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) refreshList(((ToggleButton) newVal).getText());
        });
        templateList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> updatePreviewImage(newVal));
        
        // AKTIONEN, DIE DEN DIALOG SCHLIESSEN
        importBtn.setOnAction(e -> triggerAction(ActionType.IMPORT_REQUEST, null));
        exportBtn.setOnAction(e -> triggerAction(ActionType.EXPORT_REQUEST, null));
        
        // AKTIONEN, DIE DEN DIALOG OFFEN LASSEN
        deleteBtn.setOnAction(e -> handleDeleteAction());
        saveBtn.setOnAction(e -> handleSaveAction());
        btnRefresh.setOnAction(e -> handleManualRefresh());

        // RESULT-CONVERTER FÜR HAUPT-BUTTONS
        setResultConverter(btn -> {
            if (btn == loadBtn) {
                return new TemplateResult(ActionType.LOAD, templateList.getSelectionModel().getSelectedItem());
            }
            return null; // Cancel schließt einfach
        });
    }

    private void handleSaveAction() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        
        saveHandler.accept(name, null);
        
        nameField.setPromptText("Saved as: " + name);
        nameField.clear();
        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> refreshList("Custom"));
        }).start();
    }
    
    private void handleDeleteAction() {
        String sel = templateList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Permanently remove '" + sel + "'?", ButtonType.OK, ButtonType.CANCEL);
        confirm.getDialogPane().getStyleClass().add("nexus-dialog");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                manager.deleteTemplate(sel);
                refreshList(((ToggleButton) categoryGroup.getSelectedToggle()).getText());
            } catch (IOException e) { /* Fehler-Alert */ }
        }
    }

    private void handleManualRefresh() {
        String sel = templateList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        
        btnRefresh.setDisable(true);
        previewDisplay.setOpacity(0.5);

        String fullHtml = htmlProvider.apply(manager.getTemplateContent(sel));
        Path target = manager.getRootPath().resolve("previews").resolve(sel + ".png");

        SnapshotService.create(fullHtml, target, (data) -> Platform.runLater(() -> {
            updatePreviewImage(sel);
            previewDisplay.setOpacity(1.0);
            btnRefresh.setDisable(false);
        }));
    }

    private void updatePreviewImage(String name) {
        if (name == null) { showPlaceholderImage(); return; }
        Path p = manager.getRootPath().resolve("previews").resolve(name + ".png");
        if (Files.exists(p)) previewDisplay.setImage(new Image(p.toUri().toString()));
        else showPlaceholderImage();
    }

    private void showPlaceholderImage() {
        java.net.URL url = getClass().getResource("/com/flowshift/editor/images/no-preview.png");
        if (url != null) previewDisplay.setImage(new Image(url.toExternalForm()));
        else previewDisplay.setImage(null);
    }

    private void triggerAction(ActionType type, String name) {
        this.setResult(new TemplateResult(type, name));
        this.close();
    }

    private ToggleButton createCategoryToggle(String text, boolean selected) {
        ToggleButton tb = new ToggleButton(text);
        tb.setToggleGroup(categoryGroup);
        tb.setSelected(selected);
        return tb;
    }

    private void refreshList(String category) {
        templateList.getItems().setAll(manager.getTemplatesByCategory(category));
        if (!templateList.getItems().isEmpty()) templateList.getSelectionModel().select(0);
        else updatePreviewImage(null);
    }

}
