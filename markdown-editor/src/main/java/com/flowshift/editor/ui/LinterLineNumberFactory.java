package com.flowshift.editor.ui;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.fxmisc.richtext.CodeArea;

import com.flowshift.editor.util.MarkdownLinter;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Universelle Factory für Zeilennummern mit Support für Fehler-Markierungen (Linter) und Hervorhebung der aktuellen Zeile.
 */
public class LinterLineNumberFactory implements IntFunction<Node> {

	/**
	 * STATISCHE HILFSMETHODE: Sucht einen Fehlertext für eine spezifische Zeile. Diese Methode wird von den Controllern als "errorProvider" genutzt.
	 */
	public static String findErrorAtLine(CodeArea area, List<MarkdownLinter.TagError> errors, int lineIndex) {
		if (errors == null || errors.isEmpty())
			return null;

		for (MarkdownLinter.TagError err : errors) {
			try {
				// Die einzige Stelle, die Offset in Zeilennummer umrechnet
				int errLine = area.offsetToPosition(err.start, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
				if (errLine == lineIndex) {
					return MarkdownLinter.getErrorDescription(err);
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	private final CodeArea                  editor;
	private final Function<Integer, String> errorProvider;

	private int               currentLine         = -1;
	private final PseudoClass CURRENT_LINE_PSEUDO = PseudoClass.getPseudoClass("current");
	private final PseudoClass ERROR_LINE_PSEUDO   = PseudoClass.getPseudoClass("error");

	// Callback für Hover-Events
	private BiConsumer<Node, Integer> hoverHandler;

	public LinterLineNumberFactory(CodeArea editor, Function<Integer, String> errorProvider) {
		this.editor = editor;
		this.errorProvider = errorProvider;
	}

	// NEU: Setter für den Handler
	public void setHoverHandler(BiConsumer<Node, Integer> handler) {
		this.hoverHandler = handler;
	}

	@Override
	public Node apply(int lineNumber) {
		Label label = new Label(String.valueOf(lineNumber + 1));
		label.getStyleClass().add("lineno");

		String errorMsg = errorProvider.apply(lineNumber);
		label.pseudoClassStateChanged(ERROR_LINE_PSEUDO, errorMsg != null);

		if (errorMsg != null) {
			label.setOnMouseEntered(e -> {
				if (hoverHandler != null)
					hoverHandler.accept(label, lineNumber);
			});
			// Wichtig: Auch das Verlassen behandeln, um das Popup zu schließen!
			label.setOnMouseExited(e -> {
				// Wir senden -1 oder null, um zu signalisieren: Hide
				if (hoverHandler != null)
					hoverHandler.accept(null, -1);
			});
		}

		label.pseudoClassStateChanged(CURRENT_LINE_PSEUDO, lineNumber == currentLine);
		return label;
	}

	public void updateCurrentLine(int newLine) {
		int oldLine = this.currentLine;
		if (oldLine != newLine) {
			this.currentLine = newLine;
			if (oldLine >= 0)
				updatePseudoClass(oldLine, false);
			if (newLine >= 0)
				updatePseudoClass(newLine, true);
		}
	}

	private void updatePseudoClass(int lineIndex, boolean active) {
		try {
			Node graphic = editor.getParagraphGraphic(lineIndex);
			if (graphic != null) {
				graphic.pseudoClassStateChanged(CURRENT_LINE_PSEUDO, active);
			}
		} catch (Exception ignored) {
		}
	}
}