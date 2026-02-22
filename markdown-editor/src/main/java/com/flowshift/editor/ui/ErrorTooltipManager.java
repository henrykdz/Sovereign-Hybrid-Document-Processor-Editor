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

package com.flowshift.editor.ui;

import java.util.Comparator;
import java.util.List;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.event.MouseOverTextEvent;

import com.flowshift.editor.util.MarkdownHighlighter;
import com.flowshift.editor.util.MarkdownLinter;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import utils.tooltip.CustomPopupControl;
import utils.tooltip.CustomPopupControl.AnimationProfile;
import utils.tooltip.CustomPopupControl.Position;

/**
 * Manages context-aware diagnostic tooltips for three interaction zones:
 * <ul>
 * <li>Status bar label - shows summary of all errors
 * <li>Editor text/gutter/diagnostics stripe - shows detailed single error
 * </ul>
 */
public class ErrorTooltipManager {
	// --- Classpath-Pfad als Konstante (kein Laden, nur der Pfad!) ---
	private static final String CSS_PATH = "/com/flowshift/editor/editor-style.css";

	// --- Text constants ---
	private static final String SURGICAL_HEADER = "Structural Issue Detected";
	private static final String SUMMARY_HEADER  = "Structural Errors Detected (%d)";
	private static final String MORE_ISSUES     = "... and %d more issues.";
	private static final String LINE            = "Line";
	private static final String TYPE            = "Type";
	private static final String DETAILS         = "Details";

	// --- CSS classes ---
	private static final String CSS_CONTAINER   = "error-tooltip-container";
	private static final String CSS_HEADER      = "error-tooltip-surgical-header";
	private static final String CSS_META_TITLE  = "error-tooltip-surgical-meta-title";
	private static final String CSS_META_VALUE  = "error-tooltip-surgical-meta-value";
	private static final String CSS_LINE        = "error-tooltip-line";
	private static final String CSS_TYPE        = "error-tooltip-type";
	private static final String CSS_DETAILS     = "error-tooltip-surgical-details";
	private static final String CSS_SEPARATOR   = "error-tooltip-separator";
	private static final String CSS_GRID_HEADER = "error-tooltip-summary-grid-header";
	private static final String CSS_MORE_ERRORS = "error-tooltip-summary-more-errors";

	private final CustomPopupControl  summaryTooltip;
	private final CustomPopupControl  editorTooltip;
	private final MarkdownHighlighter highlighter;
	private final CodeArea            editor;
	private final Label               ownerLabel;
	private final Pane                diagnosticsStripe;

	/**
	 * Creates and installs a new ErrorTooltipManager.
	 * 
	 * @param owner             status bar label that triggers summary tooltip
	 * @param editor            code area for surgical tooltips
	 * @param highlighter       provides error data
	 * @param diagnosticsStripe visual marker bar for errors
	 */
	public static ErrorTooltipManager install(Label owner, CodeArea editor, MarkdownHighlighter highlighter, Pane diagnosticsStripe) {
		return new ErrorTooltipManager(owner, editor, highlighter, diagnosticsStripe);
	}

	private ErrorTooltipManager(Label owner, CodeArea editor, MarkdownHighlighter highlighter, Pane diagnosticsStripe) {
		this.ownerLabel = owner;
		this.editor = editor;
		this.highlighter = highlighter;
		this.diagnosticsStripe = diagnosticsStripe;

		// 1. Popup for Status Bar (Summary View)
		this.summaryTooltip = CustomPopupControl.createFor(owner).unmanaged().position(Position.ABOVE).animation(AnimationProfile.SHARP).maxWidth(550).paddingDefault(false)
		        .addStylesheet(CSS_PATH) // ← RICHTIG: Classpath-Pfad als Konstante
		        .build();

		// 2. Popup for Editor Hover (Surgical View)
		this.editorTooltip = CustomPopupControl.createFor(editor).unmanaged().position(Position.CURSOR_BOTTOM_RIGHT).animation(AnimationProfile.SHARP).maxWidth(500)
		        .paddingDefault(false).addStylesheet(CSS_PATH) // ← RICHTIG: Classpath-Pfad als Konstante
		        .build();

		setupSummaryHover();
		setupSurgicalHovers();
	}

	private void setupSummaryHover() {
		ownerLabel.setOnMouseEntered(event -> {
			if (highlighter != null && !highlighter.getLastErrors().isEmpty()) {
				summaryTooltip.content(buildSummaryContent(highlighter.getLastErrors()));
				summaryTooltip.updatePosition();
				summaryTooltip.show(ownerLabel, 0, 0);
			}
		});

		ownerLabel.setOnMouseExited(event -> {
			summaryTooltip.hideFaded();
		});
	}

	/**
	 * Shows tooltip for gutter line number click.
	 * 
	 * @param anchor    line number label
	 * @param lineIndex zero-based line number
	 */
	public void showGutterTooltip(Node anchor, int lineIndex) {
		MarkdownLinter.TagError error = findErrorOnLine(lineIndex);
		if (error != null) {
			editorTooltip.content(buildSurgicalContent(error));
			editorTooltip.updatePosition();
			Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
			editorTooltip.show(anchor, bounds.getMaxX() + 10, bounds.getMinY());
		}
	}

	private void setupSurgicalHovers() {
		editor.setMouseOverTextDelay(java.time.Duration.ofMillis(400));

		editor.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
			MarkdownLinter.TagError error = findErrorAtChar(e.getCharacterIndex());
			if (error != null) {
				showSurgicalTooltip(error, e.getScreenPosition(), editor);
			}
		});

		editor.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, e -> editorTooltip.hideFaded());

		if (diagnosticsStripe != null) {
			PauseTransition stripeExitTimer = new PauseTransition(Duration.seconds(1.5));
			stripeExitTimer.setOnFinished(e -> editorTooltip.hideFaded());

			diagnosticsStripe.setOnMouseMoved(e -> {
				double y = e.getY();
				double stripeHeight = diagnosticsStripe.getHeight();
				double docLength = editor.getLength();

				if (stripeHeight > 0 && docLength > 0) {
					int estimatedCharIdx = (int) ((y / stripeHeight) * docLength);
					MarkdownLinter.TagError error = findClosestError(estimatedCharIdx);

					if (error != null) {
						double errorY = ((double) error.start / docLength) * stripeHeight;
						if (Math.abs(y - errorY) > 10) {
							if (stripeExitTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) {
								stripeExitTimer.playFromStart();
							}
						} else {
							stripeExitTimer.stop();
							Point2D screenPos = diagnosticsStripe.localToScreen(e.getX(), e.getY());
							showSurgicalTooltip(error, new Point2D(screenPos.getX() - 15, screenPos.getY()), diagnosticsStripe);
						}
					}
				}
			});

			diagnosticsStripe.setOnMouseExited(e -> {
				stripeExitTimer.stop();
				editorTooltip.hideFaded();
			});
		}
	}

	private void showSurgicalTooltip(MarkdownLinter.TagError error, Point2D screenPos, Node anchor) {
		editorTooltip.content(buildSurgicalContent(error));
		editorTooltip.updatePosition();
		editorTooltip.show(anchor, screenPos.getX(), screenPos.getY());
	}

	// --- Error lookup helpers ---

	private MarkdownLinter.TagError findErrorAtChar(int charIdx) {
		if (charIdx < 0)
			return null;
		return highlighter.getLastErrors().stream().filter(err -> charIdx >= err.start && charIdx <= err.end).findFirst().orElse(null);
	}

	private MarkdownLinter.TagError findErrorOnLine(int lineIdx) {
		if (lineIdx < 0)
			return null;
		return highlighter.getLastErrors().stream().filter(err -> editor.offsetToPosition(err.start, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() == lineIdx)
		        .findFirst().orElse(null);
	}

	private MarkdownLinter.TagError findClosestError(int charIdx) {
		return highlighter.getLastErrors().stream().min(Comparator.comparingInt(err -> Math.abs(err.start - charIdx))).orElse(null);
	}

	// --- Tooltip content builders ---

	private Node buildSurgicalContent(MarkdownLinter.TagError error) {
		VBox container = new VBox(6);
		container.setPadding(new Insets(8, 10, 8, 10));
		container.getStyleClass().add(CSS_CONTAINER);

		Label header = new Label(SURGICAL_HEADER);
		header.getStyleClass().add(CSS_HEADER);
		// INLINE-STYLE als Fallback für die Farbe
		header.setStyle("-fx-text-fill: #D79050; -fx-font-weight: bold;");

		int lineNum = editor.offsetToPosition(error.start, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;

		HBox metaRow = new HBox(12);
		metaRow.setAlignment(Pos.CENTER_LEFT);

		// Line Box
		HBox lineBox = new HBox(4);
		Label lTitle = new Label(LINE + ":");
		lTitle.getStyleClass().add(CSS_META_TITLE);
		Label lValue = new Label(String.valueOf(lineNum));
		lValue.getStyleClass().addAll(CSS_LINE, CSS_META_VALUE);
		lineBox.getChildren().addAll(lTitle, lValue);

		// Type Box
		HBox typeBox = new HBox(4);
		Label tTitle = new Label(TYPE + ":");
		tTitle.getStyleClass().add(CSS_META_TITLE);
		Label tValue = new Label(error.id.name());
		tValue.getStyleClass().addAll(CSS_TYPE, CSS_META_VALUE);
		typeBox.getChildren().addAll(tTitle, tValue);

		metaRow.getChildren().addAll(lineBox, typeBox);

		Region sep = new Region();
		sep.setPrefHeight(1);
		sep.getStyleClass().add(CSS_SEPARATOR);

		Label detailsTitle = new Label(DETAILS + ":");
		detailsTitle.getStyleClass().add(CSS_META_TITLE);

		Label descLabel = new Label(MarkdownLinter.getErrorDescription(error));
		descLabel.setWrapText(true);
		descLabel.setMaxWidth(380);
		descLabel.getStyleClass().add(CSS_DETAILS);

		container.getChildren().addAll(header, metaRow, sep, detailsTitle, descLabel);
		return container;
	}

	private Node buildSummaryContent(List<MarkdownLinter.TagError> errors) {
		VBox container = new VBox(8);
		container.setPadding(new Insets(10, 12, 10, 12));
		container.getStyleClass().add(CSS_CONTAINER);

		Label header = new Label(String.format(SUMMARY_HEADER, errors.size()));
		header.getStyleClass().add(CSS_HEADER);
		container.getChildren().add(header);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(2);

		ColumnConstraints colLine = new ColumnConstraints();
		colLine.setMinWidth(45);
		colLine.setPrefWidth(45);
		// KEINE Halignment - Standard linksbündig!

		ColumnConstraints colType = new ColumnConstraints();
		colType.setMinWidth(160);
		colType.setPrefWidth(160);
		colType.setMaxWidth(220);

		ColumnConstraints colDetails = new ColumnConstraints();
		colDetails.setHgrow(Priority.ALWAYS);
		colDetails.setMaxWidth(360);

		grid.getColumnConstraints().addAll(colLine, colType, colDetails);

		// Grid Headers
		Label hLine = new Label(LINE);
		hLine.getStyleClass().add(CSS_GRID_HEADER);

		Label hType = new Label(TYPE);
		hType.getStyleClass().add(CSS_GRID_HEADER);

		Label hDesc = new Label(DETAILS);
		hDesc.getStyleClass().add(CSS_GRID_HEADER);

		grid.add(hLine, 0, 0);
		grid.add(hType, 1, 0);
		grid.add(hDesc, 2, 0);

		int limit = Math.min(errors.size(), 3);
		for (int i = 0; i < limit; i++) {
			MarkdownLinter.TagError error = errors.get(i);
			int line = editor.offsetToPosition(error.start, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor() + 1;

			Label lineLabel = new Label(String.valueOf(line));
			lineLabel.getStyleClass().addAll(CSS_LINE, CSS_META_VALUE);
			// KEINE Alignment-Änderung - Standard linksbündig!

			Label typeLabel = new Label(error.id.name());
			typeLabel.getStyleClass().addAll(CSS_TYPE, CSS_META_VALUE);
			typeLabel.setWrapText(true);
			typeLabel.setMaxWidth(200);

			Label descLabel = new Label(MarkdownLinter.getErrorDescription(error));
			descLabel.setWrapText(true);
			descLabel.setMaxWidth(350);
			descLabel.getStyleClass().add(CSS_DETAILS);

			grid.add(lineLabel, 0, i + 1);
			grid.add(typeLabel, 1, i + 1);
			grid.add(descLabel, 2, i + 1);
		}
		container.getChildren().add(grid);

		if (errors.size() > limit) {
			Label moreErrors = new Label(String.format(MORE_ISSUES, errors.size() - limit));
			moreErrors.getStyleClass().add(CSS_MORE_ERRORS);
			container.getChildren().add(moreErrors);
		}

		return container;
	}

}
