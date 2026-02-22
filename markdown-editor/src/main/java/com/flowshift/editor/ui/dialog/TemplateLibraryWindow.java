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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.kordamp.ikonli.javafx.FontIcon;

import com.flowshift.editor.model.DocumentSettings;
import com.flowshift.editor.util.MarkdownTemplateManager;
import com.flowshift.editor.util.SnapshotService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * TemplateLibraryWindow v7.7.2 - Das "Nexus Command Center". Priorisiert den "Document Vault" für einen souveränen Workflow.
 */
public class TemplateLibraryWindow extends Stage {

	private final MarkdownTemplateManager  manager;
	private final Function<String, String> htmlProvider;
	private final LibraryActions           actions;

	private final ListView<String> listView       = new ListView<>();
	private final ImageView        previewDisplay = new ImageView();
	private final TextField        nameField      = new TextField();
	private final ToggleGroup      categoryGroup  = new ToggleGroup();
	private final Button           btnRefresh     = new Button("Refresh Preview");

	public TemplateLibraryWindow(Stage owner, MarkdownTemplateManager manager, DocumentSettings settings, Function<String, String> htmlProvider, LibraryActions actions) {
		this.manager = manager;
		this.htmlProvider = htmlProvider;
		this.actions = actions;

		initOwner(owner);
		initModality(Modality.NONE);
		setTitle("FlowShift Document & Asset Manager");

		BorderPane root = new BorderPane();
		root.setPadding(new Insets(15));
		root.getStyleClass().add("nexus-dialog");

		Scene scene = new Scene(root, 950, 900);
		scene.getStylesheets().add(getClass().getResource("/com/flowshift/editor/editor-style.css").toExternalForm());
		setScene(scene);

		// --- TOP SECTION ---
		BorderPane topBarContent = new BorderPane();
		topBarContent.setPadding(new Insets(0, 0, 10, 0));

		// LINKS: Priorisierte Filter-Leiste
		HBox filterBox = new HBox(10);
		filterBox.setAlignment(Pos.CENTER_LEFT);
		filterBox.getStyleClass().add("segmented-button-bar");

		ToggleButton btnVault = createCategoryToggle("Document Vault", true); // Default
		ToggleButton btnAll = createCategoryToggle("All Blueprints", false);
		ToggleButton btnCustom = createCategoryToggle("Custom", false);
		ToggleButton btnPremium = createCategoryToggle("Premium", false);
		HBox blueprintFilters = new HBox(btnAll, btnCustom, btnPremium);

		filterBox.getChildren().addAll(btnVault, new Separator(Orientation.VERTICAL), blueprintFilters);
		topBarContent.setLeft(filterBox);

		// RECHTS: Bundle Actions
		HBox bundleBox = new HBox(10);
		bundleBox.setAlignment(Pos.CENTER_RIGHT);
		Button btnImport = createIconButton("Import Bundle...", "fas-file-import", false);
		Button btnExport = createIconButton("Export Current...", "fas-file-export", false);
		bundleBox.getChildren().addAll(btnImport, btnExport);
		topBarContent.setRight(bundleBox);

		Label headerLabel = new Label("Manage your documents and reusable blueprint assets.");
		headerLabel.getStyleClass().add("header-text");
		HBox headerWrapper = new HBox(headerLabel);
		headerWrapper.getStyleClass().add("header-panel");

		VBox topSection = new VBox(10, headerWrapper, topBarContent);
		root.setTop(topSection);

		// --- CENTER: Browser ---
		HBox browserBox = new HBox(20);
		VBox.setVgrow(browserBox, Priority.ALWAYS);
		Button btnDelete = createIconButton("Delete Selected", "fas-trash-alt", false);
		btnDelete.setId("btnDelete");
		btnDelete.setMaxWidth(Double.MAX_VALUE);
		VBox listBox = new VBox(8, new Label("Available Items:"), listView, btnDelete);
		HBox.setHgrow(listBox, Priority.ALWAYS);
		VBox.setVgrow(listView, Priority.ALWAYS);

		previewDisplay.setFitWidth(400);
		previewDisplay.setPreserveRatio(true);
		StackPane imageFrame = new StackPane(previewDisplay);
		imageFrame.setStyle("-fx-border-color: #404040; -fx-background-color: #1a1c24;");
		btnRefresh.setMaxWidth(Double.MAX_VALUE);
		VBox imageBox = new VBox(8, new Label("Visual Preview:"), imageFrame, btnRefresh);
		VBox.setVgrow(imageFrame, Priority.ALWAYS); // <--- DIESE ZEILE IST ENTSCHEIDEND

		browserBox.getChildren().addAll(listBox, imageBox);
		root.setCenter(browserBox);

		// C. BOTTOM: Erstellung & Haupt-Aktionen
		VBox bottomContainer = new VBox(15);
		// DER FIX: Wir geben dem Container ein Padding nach oben,
		// damit er sich sauber vom Browser-Bereich abhebt.
		bottomContainer.setPadding(new Insets(15, 0, 0, 0));

		// Speicher-Widget
		VBox saveBox = new VBox(10);
		saveBox.setPadding(new Insets(15));
		saveBox.setStyle("-fx-background-color: rgba(215, 144, 80, 0.08); -fx-border-color: #D7905066; -fx-border-radius: 5;");
		Label lbSave = new Label("Save content FROM THE EDITOR as a new blueprint:");
		lbSave.setStyle("-fx-font-weight: bold; -fx-text-fill: #D79050;");
		Button btnSaveAs = new Button("Save as New");
		btnSaveAs.getStyleClass().add("save-as-button");
		nameField.setText(settings.getDocumentTitle());
		HBox nameAndSave = new HBox(10, nameField, btnSaveAs);
		HBox.setHgrow(nameField, Priority.ALWAYS);
		saveBox.getChildren().addAll(lbSave, nameAndSave);

		// Haupt-Buttonleiste
		HBox mainActions = new HBox(10);
		mainActions.setAlignment(Pos.CENTER_RIGHT);
		Button btnLoad = new Button("Load/Open Selected");
		btnLoad.setDefaultButton(true);
		Button btnClose = new Button("Close");
		btnClose.setStyle("-fx-min-width:100;");
		mainActions.getChildren().addAll(btnLoad, btnClose);

		// Die korrekte Reihenfolge MIT dem Abstand
		bottomContainer.getChildren().addAll(new Separator(), saveBox, new Separator(), mainActions);
		root.setBottom(bottomContainer);

		setupLogic(btnImport, btnExport, btnDelete, btnSaveAs, btnLoad, btnClose);
		refreshList("Document Vault");

	}

	private void setupLogic(Button importBtn, Button exportBtn, Button deleteBtn, Button saveBtn, Button loadBtn, Button closeBtn) {
		categoryGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
			if (newVal != null)
				refreshList(((ToggleButton) newVal).getText());
		});
		listView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> updatePreviewImage(newVal));

		listView.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2)
				handleLoadOrOpen();
		});

		importBtn.setOnAction(e -> actions.onBundleImport());
		exportBtn.setOnAction(e -> actions.onBundleExport());
		deleteBtn.setOnAction(e -> handleDeleteAction());
		saveBtn.setOnAction(e -> handleSaveAction());
		loadBtn.setOnAction(e -> handleLoadOrOpen());
		btnRefresh.setOnAction(e -> handleManualRefresh());
		closeBtn.setOnAction(e -> this.close());
	}

	private void handleLoadOrOpen() {
		String selected = listView.getSelectionModel().getSelectedItem();
		if (selected == null)
			return;
		String category = ((ToggleButton) categoryGroup.getSelectedToggle()).getText();
		if ("Document Vault".equalsIgnoreCase(category)) {
			actions.onVaultDocumentOpen(selected);
		} else {
			if (actions.onTemplateLoad(selected))
				this.close();
		}
	}

	private void handleSaveAction() {
		if (actions.onTemplateSave(nameField.getText())) {
			nameField.setText(nameField.getText() + " (Saved)");
			refreshList("Custom");
		}
	}

	private void handleDeleteAction() {
		String sel = listView.getSelectionModel().getSelectedItem();
		if (sel == null)
			return;
		String cat = ((ToggleButton) categoryGroup.getSelectedToggle()).getText();
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Permanently remove '" + sel + "'?", ButtonType.OK, ButtonType.CANCEL);
		confirm.getDialogPane().getStyleClass().add("nexus-dialog");
		if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			if ("Document Vault".equalsIgnoreCase(cat))
				actions.onVaultDocumentDelete(sel);
			else
				actions.onTemplateDelete(sel);
			refreshList(cat);
		}
	}

	private void handleManualRefresh() {
		String sel = listView.getSelectionModel().getSelectedItem();
		if (sel == null)
			return;
		String cat = ((ToggleButton) categoryGroup.getSelectedToggle()).getText();

		btnRefresh.setDisable(true);
		previewDisplay.setOpacity(0.5);
		String content = "Document Vault".equalsIgnoreCase(cat) ? manager.getVaultDocumentContent(sel) : manager.getTemplateContent(sel);
		String fullHtml = htmlProvider.apply(content);
		Path target = manager.getRootPath().resolve("previews").resolve(sel + ".png");

		SnapshotService.create(fullHtml, target, (data) -> Platform.runLater(() -> {
			updatePreviewImage(sel);
			previewDisplay.setOpacity(1.0);
			btnRefresh.setDisable(false);
		}));
	}

	private void updatePreviewImage(String name) {
		if (name == null) {
			showPlaceholderImage();
			return;
		}
		Path p = manager.getRootPath().resolve("previews").resolve(name + ".png");
		if (Files.exists(p))
			previewDisplay.setImage(new Image(p.toUri().toString()));
		else
			showPlaceholderImage();
	}

	private void showPlaceholderImage() {
		var url = getClass().getResource("/com/flowshift/editor/images/no-preview.png");
		if (url != null)
			previewDisplay.setImage(new Image(url.toExternalForm()));
		else
			previewDisplay.setImage(null);
	}

	private Button createIconButton(String text, String iconCode, boolean primary) {
		Button button = new Button(text);
		FontIcon icon = new FontIcon(iconCode);
		icon.setIconSize(16);
		button.setGraphic(icon);
		button.getStyleClass().add("action-button");
		if (primary)
			button.getStyleClass().add("button-primary");
		return button;
	}

	private ToggleButton createCategoryToggle(String text, boolean selected) {
		ToggleButton tb = new ToggleButton(text);
		tb.setToggleGroup(categoryGroup);
		tb.setSelected(selected);
		String iconCode = switch (text) {
		case "All Blueprints" -> "fas-list";
		case "Custom"         -> "fas-user-edit";
		case "Premium"        -> "fas-crown";
		case "Document Vault" -> "fas-file-alt";
		default               -> null;
		};
		if (iconCode != null) {
			FontIcon icon = new FontIcon(iconCode);
			icon.setIconSize(16);
			tb.setGraphic(icon);
			tb.getStyleClass().add("action-button");
			tb.setContentDisplay(ContentDisplay.LEFT);
		}
		return tb;
	}

	public void refreshContent() {
		refreshList(((ToggleButton) categoryGroup.getSelectedToggle()).getText());
	}

	private void refreshList(String category) {
		listView.getItems().setAll(manager.getTemplatesByCategory(category));
		if (!listView.getItems().isEmpty())
			listView.getSelectionModel().select(0);
		else
			updatePreviewImage(null);
	}

}
