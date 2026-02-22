package com.flowshift.editor.webview;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fxmisc.richtext.CodeArea;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import utils.logging.Log;

public class SovereignNavigator {

	private final CodeArea              editor;
	private final SovereignSourceMapper sourceMapper;

	public SovereignNavigator(CodeArea editor, SovereignSourceMapper sourceMapper) {
		this.editor = Objects.requireNonNull(editor);
		this.sourceMapper = Objects.requireNonNull(sourceMapper);
	}

	private record SelectionRange(int start, int end) {
	}

	public void smartJumpToSource(String tagName, String textContent, String openingTag, String uidHint, int occurrenceIndex) {
		// HEILUNG 1: <br> ignorieren, um Fehlsprünge zu vermeiden
		if (tagName.equalsIgnoreCase("br"))
			return;

		Platform.runLater(() -> {
			try {
				String editorText = editor.getText();
				int targetOffset = sourceMapper.getOffsetForFsId(uidHint);

				if (targetOffset == -1 && openingTag != null && openingTag.length() > 3) {
					String searchTag = openingTag.endsWith(">") ? openingTag.substring(0, openingTag.length() - 1) : openingTag;
					targetOffset = findNthOccurrence(editorText, searchTag, occurrenceIndex);
				}

				if (targetOffset == -1)
					return;

				int lineIndex = editor.offsetToPosition(targetOffset, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
				if (lineIndex >= editor.getParagraphs().size())
					return;

				SelectionRange range = determineSelectionRange(lineIndex, targetOffset, textContent, editorText, tagName);
				applyCenteredSelection(range, lineIndex);

			} catch (Exception e) {
				Log.error(e, "Navigator: Precision Jump failed.");
			}
		});
	}

	private SelectionRange determineSelectionRange(int lineIndex, int knownOffset, String searchText, String fullText, String tagName) {
		String tag = tagName.toLowerCase();
		int lineStart = editor.getAbsolutePosition(lineIndex, 0);

		// 1. CODE
		if (tag.equals("pre") || tag.equals("code")) {
			SelectionRange fenced = calculateFencedCodeBlockRange(fullText, knownOffset, tag.equals("pre"));
			if (fenced != null)
				return fenced;
			if (tag.equals("code"))
				return calculateSurgicalRange(fullText, lineIndex, lineStart, knownOffset, searchText);
		}

		// 2. HTML
		int searchStart = Math.max(0, knownOffset);
		int textLength = fullText.length();
		String lookAhead = fullText.substring(searchStart, Math.min(searchStart + 10, textLength));
		if (lookAhead.contains("<") || isBlockContainer(tag) || isHtmlStructure(tag)) {
			int actualTagStart = fullText.indexOf("<", searchStart);
			if (actualTagStart != -1 && actualTagStart - searchStart < 10) {
				return calculateHtmlRange(fullText, actualTagStart);
			}
		}

		// 3. HEILUNG 2: LISTEN (li) - Strikte Zeilen-Logik (verhindert Selektion der Vorzeile)
		if (tag.equals("li")) {
			String lineContent = editor.getParagraph(lineIndex).getText();
			return new SelectionRange(lineStart, lineStart + lineContent.length());
		}

		// 4. TEXT-BLÖCKE (p, h1-h6)
		if (tag.matches("h[1-6]|p|blockquote|header|section|article")) {
			return calculateGreedyProseRange(fullText, lineIndex, lineStart);
		}

		// 5. INLINE
		if (isInlineStylingTag(tag) || tag.equals("span")) {
			return calculateSurgicalRange(fullText, lineIndex, lineStart, knownOffset, searchText);
		}

		return calculateGreedyProseRange(fullText, lineIndex, lineStart);
	}

	private SelectionRange calculateFencedCodeBlockRange(String fullText, int startOffset, boolean includeBackticks) {
		int pos = 0;
		int lastBlockStart = -1;
		String activeMarker = null;
		while (pos < fullText.length()) {
			int nextNewLine = fullText.indexOf("\n", pos);
			int endOfLine = (nextNewLine == -1) ? fullText.length() : nextNewLine;
			String line = fullText.substring(pos, endOfLine).trim();
			if (line.startsWith("```") || line.startsWith("~~~")) {
				String marker = line.substring(0, 3);
				if (activeMarker == null) {
					activeMarker = marker;
					lastBlockStart = pos;
				} else if (marker.equals(activeMarker)) {
					int blockEnd = endOfLine;
					if (startOffset >= lastBlockStart && startOffset <= blockEnd)
						return finalizeBlockSelection(fullText, lastBlockStart, blockEnd, pos, includeBackticks);
					activeMarker = null;
					lastBlockStart = -1;
				}
			}
			if (nextNewLine == -1)
				break;
			pos = nextNewLine + 1;
			if (pos > startOffset && activeMarker == null)
				return null;
		}
		return null;
	}

	private SelectionRange finalizeBlockSelection(String fullText, int start, int end, int fenceStart, boolean includeBackticks) {
		if (includeBackticks)
			return new SelectionRange(start, end);
		int contentStart = fullText.indexOf("\n", start);
		if (contentStart == -1 || contentStart >= end)
			return new SelectionRange(start, end);
		contentStart += 1;
		int contentEnd = fenceStart;
		if (contentEnd > contentStart && fullText.charAt(contentEnd - 1) == '\n')
			contentEnd--;
		if (contentEnd > contentStart && fullText.charAt(contentEnd - 1) == '\r')
			contentEnd--;
		return new SelectionRange(contentStart, contentEnd);
	}

	private SelectionRange calculateHtmlRange(String fullText, int start) {
		Matcher m = Pattern.compile("<([a-zA-Z0-9]+)").matcher(fullText.substring(start));
		if (!m.find())
			return new SelectionRange(start, start);
		String tagName = m.group(1).toLowerCase();
		if (isSelfClosingTag(tagName)) {
			int end = fullText.indexOf(">", start);
			return new SelectionRange(start, (end != -1) ? end + 1 : start + 1);
		}
		String openP = "<" + tagName;
		String closeP = "</" + tagName + ">";
		int depth = 1, currentPos = start + openP.length(), end = -1;
		while (depth > 0 && currentPos < fullText.length()) {
			int nextO = fullText.toLowerCase().indexOf(openP, currentPos);
			int nextC = fullText.toLowerCase().indexOf(closeP, currentPos);
			if (nextC != -1 && (nextO == -1 || nextC < nextO)) {
				depth--;
				currentPos = nextC + closeP.length();
				if (depth == 0)
					end = currentPos;
			} else if (nextO != -1) {
				depth++;
				currentPos = nextO + openP.length();
			} else
				break;
		}
		return new SelectionRange(start, (end != -1) ? end : fullText.indexOf(">", start) + 1);
	}

	private SelectionRange calculateSurgicalRange(String fullText, int lineIndex, int lineStart, int start, String searchText) {
		String lineContent = editor.getParagraph(lineIndex).getText();
		int lineEnd = lineStart + lineContent.length();
		int s = start;

		if (searchText != null && !searchText.trim().isEmpty()) {
			int localIdx = lineContent.toLowerCase().indexOf(searchText.trim().toLowerCase());
			if (localIdx != -1)
				s = lineStart + localIdx;
		}

		while (s > lineStart && isSyntaxMarker(fullText.charAt(s - 1)))
			s--;
		// Header # überspringen für saubere Textselektion
		while (s < lineEnd && fullText.charAt(s) == '#')
			s++;
		while (s < lineEnd && Character.isWhitespace(fullText.charAt(s)))
			s++;

		char first = fullText.charAt(s);
		int e = -1;

		// HEILUNG 3: Unterstützung für nackte {{ Stempel
		if (fullText.startsWith("`{{", s)) {
			e = fullText.indexOf("}}`", s);
			if (e != -1)
				e += 3;
		} else if (fullText.startsWith("{{", s)) {
			e = fullText.indexOf("}}", s);
			if (e != -1)
				e += 2;
		}
		else if (first == '*' || first == '`' || first == '_' || first == '[') {
			char closing = (first == '[') ? ']' : first;
			boolean isDouble = (first == '*' && fullText.startsWith("**", s));
			int searchStart = isDouble ? s + 2 : s + 1;
			e = fullText.indexOf(closing, searchStart);
			if (e != -1)
				e += (isDouble) ? 2 : 1;
		} else if (isSyntaxMarker(first)) {
			e = s;
			while (e < lineEnd && fullText.charAt(e) == first)
				e++;
		}

		if (e == -1 || e <= s)
			e = s + (searchText != null ? searchText.trim().length() : 1);
		return cleanupRange(fullText, s, e);
	}

	private SelectionRange calculateGreedyProseRange(String fullText, int lineIndex, int lineStart) {
		String lineContent = editor.getParagraph(lineIndex).getText();
		return cleanupRange(fullText, lineStart, lineStart + lineContent.length());
	}

	private SelectionRange cleanupRange(String fullText, int s, int e) {
		while (s < e && Character.isWhitespace(fullText.charAt(s)))
			s++;
		while (e > s && Character.isWhitespace(fullText.charAt(e - 1)))
			e--;
		return new SelectionRange(s, e);
	}

	private boolean isSelfClosingTag(String tagName) {
		return tagName.matches("area|base|br|col|embed|hr|img|input|link|meta");
	}

	private boolean isBlockContainer(String tag) {
		return tag.toLowerCase().matches("div|header|main|section|article|pre");
	}

	private boolean isHtmlStructure(String tag) {
		return tag.toLowerCase().matches("table|thead|tbody|tr|td|th");
	}

	private boolean isInlineStylingTag(String tag) {
		return tag.toLowerCase().matches("strong|b|em|i|a|code");
	}

	private boolean isSyntaxMarker(char c) {
		return "*_`{}[]#=-|".indexOf(c) != -1;
	}

	private void applyCenteredSelection(SelectionRange range, int lineIndex) {
		editor.moveTo(range.start());
		editor.selectRange(range.start(), range.end());
		int visibleLines = Math.max(1, editor.getVisibleParagraphs().size());
		int startPar = editor.offsetToPosition(range.start(), org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
		int endPar = editor.offsetToPosition(range.end(), org.fxmisc.richtext.model.TwoDimensional.Bias.Backward).getMajor();
		int scrollTo = Math.max(0, startPar - (visibleLines / 3));
		if ((endPar - startPar + 1) > (visibleLines / 2))
			scrollTo = Math.max(0, ((startPar + endPar) / 2) - (visibleLines / 2));
		final int finalScroll = scrollTo;
		PauseTransition delay = new PauseTransition(Duration.millis(50));
		delay.setOnFinished(ev -> {
			if (finalScroll < editor.getParagraphs().size())
				editor.showParagraphAtTop(finalScroll);
			editor.requestFocus();
		});
		delay.play();
	}

	private int findNthOccurrence(String text, String search, int n) {
		int pos = -1;
		for (int i = 0; i <= n; i++) {
			pos = text.indexOf(search, pos + 1);
			if (pos == -1)
				return -1;
		}
		return pos;
	}
}