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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import com.flowshift.editor.util.MarkdownLinter.ErrorID;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.CodeBlock;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.util.ast.Node;

import javafx.scene.control.IndexRange;

/**
 * Hilfsklasse für Markdown-Syntax-Highlighting mit FlexMark. HTML-Highlighting wird zusätzlich zu Markdown angewendet.
 */
public class MarkdownHighlighter {
	// Im Kopf der Klasse
	private static final String HL_UI_SANCTUARY = "style-sanctuary";
	// =============================================================================================
	// LOGICAL MARKERS & PREFIXES (Refactored for Performance)
	// =============================================================================================

	// --- Namespace Prefixes (für combineSpans) ---
	private static final String PF_MD   = "md-";
	private static final String PF_HTML = "html-";
	private static final String PF_TBL  = "tbl-";
	private static final String PF_YAML = "yaml-";
	private static final String PF_CSS  = "css-";
	private static final String PF_LINT = "lint-";

	// --- HTML & Markdown Syntax Logic ---
//	private static final String MARKER_YAML_START = "---";
	private static final String MARKER_TAG_CLOSE = "</";
	private static final String MARKER_LINK_MID  = "](";
//	private static final String MARKER_IMG_START  = "![";
	private static final String TAG_STYLE = "style";

	// --- Attribute Keys ---
	private static final String ATTR_SRC   = "src";
	private static final String ATTR_HREF  = "href";
	private static final String ATTR_CLASS = "class";
	private static final String ATTR_ID    = "id";
	private static final String ATTR_STYLE = "style";

	// --- 1. NAMESPACE: MD (Markdown Core) ---
	private static final String HL_MD_TEXT = "md-text";

	private static final String HL_MD_HEADING = "md-heading";
	// Im Namespace MD
	private static final String HL_MD_HEADING_TEXT = "md-heading-text";

	// Im Namespace HTML
	private static final String HL_HTML_HEADING_TAG = "html-heading-tag";

	private static final String HL_MD_BOLD          = "md-bold";
	private static final String HL_MD_ITALIC        = "md-italic";
	private static final String HL_MD_STRIKETHROUGH = "md-strikethrough";
	private static final String HL_MD_BLOCKQUOTE    = "md-blockquote";
	private static final String HL_MD_LIST          = "md-list";
	private static final String HL_MD_LIST_ITEM     = "md-list-item";    // Für .md-list-item (optional im CSS)
	private static final String HL_MD_HR            = "md-hr";
	private static final String HL_MD_LINK          = "md-link";         // Passend zu .md-link
	private static final String HL_MD_LINK_URL      = "md-link-url";
	private static final String HL_MD_IMAGE         = "md-image";
	private static final String HL_MD_CODE_INLINE   = "md-inline-code";  // Passend zu .md-inline-code
	private static final String HL_MD_CODE_BLOCK    = "md-code-block";
	private static final String HL_MD_EMOJI         = "md-emoji";
	private static final String HL_MD_PLACEHOLDER   = "md-placeholder";

	// --- 2. NAMESPACE: TBL (Tables) ---
	private static final String HL_TBL_BLOCK  = "tbl-block";
	private static final String HL_TBL_HEADER = "tbl-header";
	private static final String HL_TBL_ROW    = "tbl-row";
	private static final String HL_TBL_CELL   = "tbl-cell";
	private static final String HL_TBL_PIPE   = "tbl-pipe";

	// --- 3. NAMESPACE: HTML (Hypertext) ---
	private static final String HL_HTML_BRACKET    = "html-tag-bracket";      // .html-tag-bracket
	private static final String HL_HTML_TAG_NAME   = "html-tag-name";         // .html-tag-name
	private static final String HL_HTML_TAG_CLOSE  = "html-tag-name-closing"; // .html-tag-name-closing
	private static final String HL_HTML_TAG_STYLE  = "html-tag-style";        // .html-tag-style
	private static final String HL_HTML_ATTR_NAME  = "html-attr-name";        // .html-attr-name
	private static final String HL_HTML_ATTR_VAL   = "html-attr-val";         // .html-attr-val
	private static final String HL_HTML_ATTR_CLASS = "html-attr-class";       // .html-attr-class
	private static final String HL_HTML_ATTR_ID    = "html-attr-id";          // .html-attr-id
	private static final String HL_HTML_ATTR_URL   = "html-attr-url";         // .html-attr-url
	private static final String HL_HTML_ATTR_STYLE = "html-attr-style";       // .html-attr-style
	private static final String HL_HTML_COMMENT    = "html-comment";          // .html-comment
	private static final String HL_HTML_ENTITY     = "html-entity";           // .html-entity
	private static final String HL_HTML_PAGE_BREAK = "html-page-break";       // .html-page-break

	// --- 4. NAMESPACE: CSS (Styles) ---
	private static final String HL_CSS_SELECTOR  = "css-selector";
	private static final String HL_CSS_PROPERTY  = "css-property"; // .css-property
	private static final String HL_CSS_VAL       = "css-val";      // .css-val
	private static final String HL_CSS_AT_RULE   = "css-at-rule";
	private static final String HL_CSS_IMPORTANT = "css-important";
	private static final String HL_CSS_PUNCT     = "css-punct";
	private static final String HL_CSS_COMMENT   = "css-comment";

	// --- 5. NAMESPACE: YAML (Frontmatter) ---
	private static final String HL_YAML_DELIM   = "yaml-delim";
	private static final String HL_YAML_KEY     = "yaml-key";
	private static final String HL_YAML_VAL     = "yaml-val";
	private static final String HL_YAML_CONST   = "yaml-const";
	private static final String HL_YAML_COMMENT = "yaml-comment";

	// --- 6. NAMESPACE: LINT (Feedback) ---
	private static final String HL_LINT_ERROR = "lint-error";
	// --- 7. NAMESPACE: UI (Interaktion) ---
	private static final String HL_SEARCH_MATCH = "search-highlight"; // .search-highlight
	// Im Kopf der Klasse (Namespace UI)
//	private static final String HL_UI_SELECTION = "ui-selection";

	private final Map<Class<?>, String> styleMap;
//	private final CodeArea              codeArea;

	// =============================================================================================
	// REGEX PATTERNS (Pre-compiled for Performance)
	// =============================================================================================
	/** Erkennt HTML-Header-Tags (h1-h6), case-insensitive */
	private static final Pattern HTML_HEADER_TAG_PATTERN = Pattern.compile("(?i)h[1-6]");

	/** Pre-compiled Pattern für die URL-Erkennung in Attributen */
	private static final Pattern URL_PROTOCOL_PATTERN = Pattern.compile("^(https?|ftp)://.*", Pattern.CASE_INSENSITIVE);
	// --- 1. HTML PATTERNS ---
	/** Extrahiert HTML-Kommentare: <!-- Kommentar --> */
	private static final Pattern HTML_COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

	/** Extrahiert HTML-Entities: &nbsp;, &#123;, &auml; */
	private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[a-zA-Z0-9#]+;");

	/**
	 * Der zentrale HTML-Tag-Parser. Gruppe 1: Öffner (< oder </) Gruppe 2: Tag-Name (z.B. div) Gruppe 3: Roher Attribut-String Gruppe 4: Schließer (> oder />)
	 */
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(<\\/?)([a-zA-Z][a-zA-Z0-9]*)(\\s*[^>]*?)(\\/?>)", Pattern.DOTALL);

	/** Erkennt das spezifische Page-Break Tag für den PDF-Export */
	private static final Pattern PAGE_BREAK_PATTERN = Pattern.compile("<div class=\"page-break\"></div>", Pattern.CASE_INSENSITIVE);

	/** Parst Attribut-Paare innerhalb eines Tags: key="value" */
	private static final Pattern HTML_ATTR_PATTERN = Pattern.compile("([a-zA-Z_:-][a-zA-Z0-9_:-]*)\\s*(=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+))?");

	// --- 2. CSS PATTERNS (Nested Grammar) ---
	/** Isoliert den Inhalt zwischen <style> Tags für den CSS-Parser */
	private static final Pattern CSS_BLOCK_PATTERN = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);

	/** Erkennt CSS-Selektoren: .class, #id, tag (via Lookahead auf { ) */
	private static final Pattern CSS_SELECTOR = Pattern.compile("[.#a-zA-Z][a-zA-Z0-9\\-_.]*(?=\\s*\\{)");

	/** Erkennt CSS-Eigenschaften: color, margin-top (via Lookahead auf : ) */
	private static final Pattern CSS_PROPERTY = Pattern.compile("[a-zA-Z\\- ]+(?=\\s*:)");

	/** Erkennt CSS-Werte: alles nach dem : bis zum ; oder } */
	private static final Pattern CSS_VALUE = Pattern.compile("(?<=:\\s*)[^;{}]+");

	/** Erkennt CSS-Kommentare: /* Kommentar * / */
	private static final Pattern CSS_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/");

	/** Erkennt die !important Regel */
	private static final Pattern CSS_IMPORTANT = Pattern.compile("!important\\b", Pattern.CASE_INSENSITIVE);

	/** Erkennt @-Regeln wie @media oder @font-face */
	private static final Pattern CSS_AT_RULE = Pattern.compile("@[a-zA-Z\\-]+");

	/** Erkennt geschweifte Klammern zur Strukturierung */
	private static final Pattern CSS_BRACKETS = Pattern.compile("[{}]");

	// --- 3. YAML PATTERNS (Frontmatter) ---
	/**
	 * Isoliert den gesamten YAML-Block am Dokumentanfang. \A garantiert, dass NUR am absoluten Start des Dokuments gesucht wird. Erlaubt führende Leerzeichen, um Konsistenz mit
	 * Linter und Parser zu gewährleisten.
	 */
	private static final Pattern YAML_BLOCK_PATTERN = Pattern.compile("\\A\\s*---\\s*\\n([\\s\\S]*?)\\n---", Pattern.MULTILINE); // NEU: \\s* am Anfang
	/** Erkennt YAML-Schlüssel: key (via Lookahead auf : ) */
	private static final Pattern YAML_KEY           = Pattern.compile("^\\s*([\\w\\-]+)(?=:)", Pattern.MULTILINE);

	/** Erkennt YAML-Werte: alles nach dem : bis zum Zeilenende */
	private static final Pattern YAML_VALUE = Pattern.compile("(?<=:)\\s*(.*)$", Pattern.MULTILINE);

	/** Erkennt die YAML-Trennerlinien (---) */
	private static final Pattern YAML_DELIMITER = Pattern.compile("^---", Pattern.MULTILINE);

	/** Erkennt numerische Konstanten (Integer/Float) zur speziellen Färbung */
	private static final Pattern YAML_NUMERIC_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

	// --- 4. MISC & METADATA PATTERNS ---
	/** Erkennt doppelte geschweifte Klammern für Platzhalter: {{variable}} */
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{[a-zA-Z0-9_-]+\\}\\}");

	/** Erkennt Emojis über einen breiten Unicode-Bereich für die korrekte Font-Darstellung */
	private static final Pattern EMOJI_PATTERN = Pattern.compile("[" + "\\x{1F300}-\\x{1F5FF}" // Symbols & Pictographs
	        + "\\x{1F600}-\\x{1F64F}" // Emoticons
	        + "\\x{1F680}-\\x{1F6FF}" // Transport & Map
	        + "\\x{1F700}-\\x{1F77F}" // Alchemical Symbols
	        + "\\x{1F780}-\\x{1F7FF}" // Geometric Shapes Extended
	        + "\\x{1F800}-\\x{1F8FF}" // Supplemental Arrows
	        + "\\x{1F900}-\\x{1F9FF}" // Supplemental Symbols
	        + "\\x{1FA00}-\\x{1FAFF}" // Symbols Extended
	        + "\\x{2600}-\\x{26FF}" // Misc Symbols
	        + "\\x{2700}-\\x{27BF}" // Dingbats
	        + "]");

	private List<MarkdownLinter.TagError> lastErrors = new ArrayList<>();

	public List<MarkdownLinter.TagError> getLastErrors() {
		return lastErrors;
	}

	/**
	 * Konstruktor v7.8.5 - Vollständig entkoppelt. Initialisiert die Engine ohne Abhängigkeit von einer CodeArea.
	 */
	public MarkdownHighlighter() {
		this.styleMap = createStyleMap();
	}

	public StyleSpans<Collection<String>> applyHighlighting(String text, Node document, boolean includeErrors, boolean isDesignMode, int sanctuaryStart, int sanctuaryEnd,
	        List<IndexRange> searchMatches, java.util.Set<String> definedClasses) { // NEU
		if (text == null || text.isEmpty())
			return createEmptySpans();

		try {
			List<TextSpan> markdownSpans = new ArrayList<>();
			collectMarkdownSpans(text, document, markdownSpans);

			List<TextSpan> overlaySpans = new ArrayList<>();
			overlaySpans.addAll(extractHtmlSpans(text));
			overlaySpans.addAll(extractCssSpans(text));
			overlaySpans.addAll(extractEmojiSpans(text));
			overlaySpans.addAll(extractYamlSpans(text));
			overlaySpans.addAll(extractPlaceholderSpans(text));
			overlaySpans.addAll(extractPageBreakSpans(text));

			if (searchMatches != null) {
				for (IndexRange match : searchMatches) {
					overlaySpans.add(new TextSpan(HL_SEARCH_MATCH, match.getStart(), match.getEnd()));
				}
			}

			// HEILUNG: Wir weisen die spezifischen Klassen für das CSS zu
			if (includeErrors) {
			    List<MarkdownLinter.TagError> errors = MarkdownLinter.checkHierarchy(text, definedClasses);
			    for (MarkdownLinter.TagError error : errors) {
			        
			        String style;
			        // 1. Missing CSS Classes -> Cyan
			        if (error.id == ErrorID.MISSING_CSS_CLASS) {
			            style = "lint-design-integrity";
			        } 
			        // 2. Deine Struktur-Warnung (Fehlende Leerzeile) -> Magenta
			        else if (error.id == ErrorID.MISSING_BLANK_LINE_AFTER_HTML) {
			            style = "lint-structure-warning";
			        } 
			        // 3. Alles andere (Syntax-Fehler) -> Rot
			        else {
			            style = "lint-error";
			        }
			        
			        overlaySpans.add(new TextSpan(style, error.start, error.end));
			    }
			    this.lastErrors = errors; 
			}

			return combineSpans(markdownSpans, overlaySpans, text.length());
		} catch (Exception e) {
			System.err.println("Highlighting failed: " + e.getMessage());
			return createEmptySpans();
		}
	}

//Und passe den Wrapper an, damit die Forge (SettingsDialog) nicht bricht:
	public StyleSpans<Collection<String>> applyHighlighting(String text, Node document, boolean includeErrors, boolean isDesignMode, int sanctuaryStart, int sanctuaryEnd) {
		return applyHighlighting(text, document, includeErrors, isDesignMode, sanctuaryStart, sanctuaryEnd, null, null);
	}

	private void collectMarkdownSpans(String text, Node node, List<TextSpan> spans) {
		if (node instanceof Heading heading) {
			int start = heading.getStartOffset();
			int textStart = heading.getText().getStartOffset();
			spans.add(new TextSpan(HL_MD_HEADING, start, textStart));
			spans.add(new TextSpan(HL_MD_HEADING_TEXT, textStart, heading.getEndOffset()));
			return;
		}
		handleTableNodes(node, spans);
		String style = styleMap.get(node.getClass());
		if (style != null)
			spans.add(new TextSpan(style, node.getStartOffset(), node.getEndOffset()));

		// HEILUNG: Übergabe des 'text' Strings
		if (node instanceof Link link)
			handleLink(text, link, spans);
		if (node instanceof Image image)
			handleImage(text, image, spans);

		for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
			collectMarkdownSpans(text, child, spans);
		}
	}

	private List<TextSpan> extractYamlSpans(String text) {
		List<TextSpan> yamlSpans = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return yamlSpans;
		}

		Matcher blockMatcher = YAML_BLOCK_PATTERN.matcher(text);

		if (blockMatcher.find()) {
			int blockStart = blockMatcher.start();
			String yamlContent = blockMatcher.group();

			// In extractYamlSpans:
			Matcher hashCommentMatcher = Pattern.compile("#.*").matcher(yamlContent);
			while (hashCommentMatcher.find()) {
				yamlSpans.add(new TextSpan(HL_YAML_COMMENT, blockStart + hashCommentMatcher.start(), blockStart + hashCommentMatcher.end()));
			}

			// Trenner (---)
			Matcher delimiterMatcher = YAML_DELIMITER.matcher(yamlContent);
			while (delimiterMatcher.find()) {
				yamlSpans.add(new TextSpan(HL_YAML_DELIM, blockStart + delimiterMatcher.start(), blockStart + delimiterMatcher.end()));
			}

			// Schlüssel (Keys)
			Matcher keyMatcher = YAML_KEY.matcher(yamlContent);
			while (keyMatcher.find()) {
				yamlSpans.add(new TextSpan(HL_YAML_KEY, blockStart + keyMatcher.start(), blockStart + keyMatcher.end()));
			}

			// Werte (Values) - inklusive Constant-Check
			Matcher valueMatcher = YAML_VALUE.matcher(yamlContent);
			while (valueMatcher.find()) {
				int valStart = blockStart + valueMatcher.start();
				int valEnd = blockStart + valueMatcher.end();
				String val = valueMatcher.group().trim();

				String style = HL_YAML_VAL;
				// Der optimierte Check:
				if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false") || YAML_NUMERIC_PATTERN.matcher(val).matches()) {
					style = HL_YAML_CONST;
				}
				yamlSpans.add(new TextSpan(style, valStart, valEnd));

			}
		}
		return yamlSpans;
	}

	private List<TextSpan> extractPlaceholderSpans(String text) {
		List<TextSpan> placeholderSpans = new ArrayList<>();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
		while (matcher.find()) {
			placeholderSpans.add(new TextSpan(HL_MD_PLACEHOLDER, matcher.start(), matcher.end()));
		}
		return placeholderSpans;
	}

	private void handleTableNodes(Node node, List<TextSpan> spans) {
		if (node instanceof TableBlock) {
			spans.add(new TextSpan(HL_TBL_BLOCK, node.getStartOffset(), node.getEndOffset()));

			Node child = node.getFirstChild();
			while (child != null) {
				if (child instanceof TableHead) {
					spans.add(new TextSpan(HL_TBL_HEADER, child.getStartOffset(), child.getEndOffset()));
				} else if (child instanceof TableRow) {
					spans.add(new TextSpan(HL_TBL_ROW, child.getStartOffset(), child.getEndOffset()));
				} else if (child instanceof TableSeparator) {
					spans.add(new TextSpan(HL_TBL_PIPE, child.getStartOffset(), child.getEndOffset()));
				}

				if (child instanceof TableRow || child instanceof TableHead) {
					Node cellChild = child.getFirstChild();
					while (cellChild != null) {
						if (cellChild instanceof TableCell) {
							spans.add(new TextSpan(HL_TBL_CELL, cellChild.getStartOffset(), cellChild.getEndOffset()));
						}
						cellChild = cellChild.getNext();
					}
				}
				child = child.getNext();
			}
		}
	}

	private List<TextSpan> extractHtmlSpans(String text) {
		List<TextSpan> htmlSpans = new ArrayList<>();

		// --- PHASE 1: TAG-ANALYSE (Orchestrierung) ---
		Matcher tagMatcher = HTML_TAG_PATTERN.matcher(text);
		while (tagMatcher.find()) {
			String tagName = tagMatcher.group(2);

			// Der Orchestrator fragt: Bist du ein Spezialfall?
			if (HTML_HEADER_TAG_PATTERN.matcher(tagName).matches()) {
				handleHtmlHeader(tagMatcher, text, htmlSpans);
			} else {
				// Wenn nicht, behandle es als generischen Tag
				handleGenericHtmlTag(tagMatcher, htmlSpans);
			}
		}

		// --- PHASE 2: DELEGIERUNG (Kommentare & Entities) ---
		htmlSpans.addAll(extractCommentsAndEntities(text));

		return htmlSpans;
	}

	/**
	 * Spezialist für die semantische Analyse von HTML-Header-Tags (h1-h6). Behandelt Tag-Name, Attribute und Inhalt als getrennte, souveräne Einheiten.
	 */
	private void handleHtmlHeader(Matcher tagMatcher, String text, List<TextSpan> spans) {
		String openingBracket = tagMatcher.group(1);
		String tagName = tagMatcher.group(2);
		String attrs = tagMatcher.group(3);
		String closingBracket = tagMatcher.group(4);

		boolean isOpeningTag = !openingBracket.equals("</");

		// --- TEIL 1: DER TAG SELBST (Name & Klammern) ---
		// A. Klammern
		spans.add(new TextSpan(HL_HTML_BRACKET, tagMatcher.start(), tagMatcher.start() + openingBracket.length()));

		// B. Tag-Name (h1-h6) bekommt den speziellen Header-Stil
		spans.add(new TextSpan(HL_HTML_HEADING_TAG, tagMatcher.start() + openingBracket.length(), tagMatcher.start() + openingBracket.length() + tagName.length()));

		// C. Schließende Klammer
		spans.add(new TextSpan(HL_HTML_BRACKET, tagMatcher.end() - closingBracket.length(), tagMatcher.end()));

		// --- TEIL 2: DIE ATTRIBUTE (Delegierung an den Spezialisten) ---
		// Wir übergeben die Attribute an deine bewährte, intelligente parseAttributes-Methode.
		// Sie kümmert sich um die korrekte Färbung von 'style', 'class', etc.
		if (attrs != null && !attrs.trim().isEmpty()) {
			parseAttributes(tagMatcher.start() + openingBracket.length() + tagName.length(), attrs, spans);
		}

		// --- TEIL 3: DER INHALT (Der semantische Text) ---
		if (isOpeningTag && !tagMatcher.group(0).endsWith("/>")) {
			Matcher endTagMatcher = Pattern.compile("</" + tagName + ">", Pattern.CASE_INSENSITIVE).matcher(text);
			if (endTagMatcher.find(tagMatcher.end())) {
				spans.add(new TextSpan(HL_MD_HEADING_TEXT, tagMatcher.end(), endTagMatcher.start()));
			}
		}
	}

	/**
	 * Generalist für die Analyse aller HTML-Tags, die keine Header sind.
	 */
	private void handleGenericHtmlTag(Matcher tagMatcher, List<TextSpan> spans) {
		String openingBracket = tagMatcher.group(1);
		String tagName = tagMatcher.group(2);
		String attrs = tagMatcher.group(3);
		String closingBracket = tagMatcher.group(4);

		// A. Klammern
		spans.add(new TextSpan(HL_HTML_BRACKET, tagMatcher.start(), tagMatcher.start() + openingBracket.length()));

		// B. Tag-Name
		String tagStyle;
		if (tagName.equalsIgnoreCase(TAG_STYLE))
			tagStyle = HL_HTML_TAG_STYLE;
		else if (openingBracket.equals(MARKER_TAG_CLOSE))
			tagStyle = HL_HTML_TAG_CLOSE;
		else
			tagStyle = HL_HTML_TAG_NAME;
		spans.add(new TextSpan(tagStyle, tagMatcher.start() + openingBracket.length(), tagMatcher.start() + openingBracket.length() + tagName.length()));

		// C. Attribute
		if (attrs != null && !attrs.trim().isEmpty()) {
			parseAttributes(tagMatcher.start() + openingBracket.length() + tagName.length(), attrs, spans);
		}

		// D. Schließende Klammer
		spans.add(new TextSpan(HL_HTML_BRACKET, tagMatcher.end() - closingBracket.length(), tagMatcher.end()));
	}

	/**
	 * Extrahiert HTML-Kommentare und Entities aus dem Text. Dient als spezialisierter Helfer für extractHtmlSpans.
	 */
	private List<TextSpan> extractCommentsAndEntities(String text) {
		List<TextSpan> spans = new ArrayList<>();

		// 1. Kommentare finden
		Matcher commentMatcher = HTML_COMMENT_PATTERN.matcher(text);
		while (commentMatcher.find()) {
			spans.add(new TextSpan(HL_HTML_COMMENT, commentMatcher.start(), commentMatcher.end()));
		}

		// 2. Entities finden
		Matcher entityMatcher = HTML_ENTITY_PATTERN.matcher(text);
		while (entityMatcher.find()) {
			spans.add(new TextSpan(HL_HTML_ENTITY, entityMatcher.start(), entityMatcher.end()));
		}

		return spans;
	}

	private List<TextSpan> extractCssSpans(String text) {
		List<TextSpan> cssSpans = new ArrayList<>();
		Matcher blockMatcher = CSS_BLOCK_PATTERN.matcher(text);
		while (blockMatcher.find()) {
			cssSpans.addAll(extractCssSpansInternal(blockMatcher.group(1), blockMatcher.start(1)));
		}
		return cssSpans;
	}

	/**
	 * Spezialist: Analysiert einen CSS-String (rein oder eingebettet). Dies ist die einzige Quelle der Wahrheit für CSS-Syntax-Highlighting.
	 */
	private List<TextSpan> extractCssSpansInternal(String cssContent, int offset) {
		List<TextSpan> cssSpans = new ArrayList<>();

		// 1. Kommentare (Haben Vorrang vor Syntax)
		Matcher commentMatcher = CSS_COMMENT_PATTERN.matcher(cssContent);
		while (commentMatcher.find())
			cssSpans.add(new TextSpan(HL_CSS_COMMENT, offset + commentMatcher.start(), offset + commentMatcher.end()));

		// 2. Selektoren, Eigenschaften, At-Rules & Punktuation
		Matcher atMatcher = CSS_AT_RULE.matcher(cssContent);
		while (atMatcher.find())
			cssSpans.add(new TextSpan(HL_CSS_AT_RULE, offset + atMatcher.start(), offset + atMatcher.end()));

		Matcher bracketMatcher = CSS_BRACKETS.matcher(cssContent);
		while (bracketMatcher.find())
			cssSpans.add(new TextSpan(HL_CSS_PUNCT, offset + bracketMatcher.start(), offset + bracketMatcher.end()));

		Matcher selectorMatcher = CSS_SELECTOR.matcher(cssContent);
		while (selectorMatcher.find())
			cssSpans.add(new TextSpan(HL_CSS_SELECTOR, offset + selectorMatcher.start(), offset + selectorMatcher.end()));

		Matcher propMatcher = CSS_PROPERTY.matcher(cssContent);
		while (propMatcher.find())
			cssSpans.add(new TextSpan(HL_CSS_PROPERTY, offset + propMatcher.start(), offset + propMatcher.end()));

		// 3. Werte (inklusive !important-Spezialbehandlung)
		Matcher valueMatcher = CSS_VALUE.matcher(cssContent);
		while (valueMatcher.find()) {
			int valStart = offset + valueMatcher.start();
			int valEnd = offset + valueMatcher.end();
			String fullValue = valueMatcher.group();

			Matcher importantMatcher = CSS_IMPORTANT.matcher(fullValue);
			if (importantMatcher.find()) {
				int impStartRel = importantMatcher.start();
				if (impStartRel > 0)
					cssSpans.add(new TextSpan(HL_CSS_VAL, valStart, valStart + impStartRel));

				cssSpans.add(new TextSpan(HL_CSS_IMPORTANT, valStart + impStartRel, valStart + importantMatcher.end()));

				if (importantMatcher.end() < fullValue.length()) {
					cssSpans.add(new TextSpan(HL_CSS_VAL, valStart + importantMatcher.end(), valEnd));
				}
			} else {
				cssSpans.add(new TextSpan(HL_CSS_VAL, valStart, valEnd));
			}
		}
		return cssSpans;
	}

	/**
	 * Spezial-Highlighting für den souveränen CSS-Editor (Sanktuarium). Arbeitet rein auf CSS-Syntax und integriert den CSS-Linter.
	 */
	public StyleSpans<Collection<String>> highlightPureCss(String cssText, boolean includeErrors) {
		if (cssText == null || cssText.isEmpty()) {
			return createEmptySpans();
		}

		try {
			// 1. Nutzt den bereits perfektionierten CSS-Spezialisten
			// Wir setzen den Offset auf 0, da das Sanktuarium nur CSS enthält
			List<TextSpan> overlaySpans = extractCssSpansInternal(cssText, 0);

			// 2. Integration des CSS-Linters (falls angefordert)
			if (includeErrors) {
				List<MarkdownLinter.TagError> errors = MarkdownLinter.lintPureCss(cssText);
				for (MarkdownLinter.TagError error : errors) {
					overlaySpans.add(new TextSpan(HL_LINT_ERROR, error.start, error.end));
				}
			}

			// 3. Zusammenbau (Markdown-Spans sind hier leer)
			return combineSpans(new ArrayList<>(), overlaySpans, cssText.length());

		} catch (Exception e) {
			System.err.println("CSS Highlighting failed: " + e.getMessage());
			return createEmptySpans();
		}
	}

	private List<TextSpan> extractPageBreakSpans(String text) {
		List<TextSpan> spans = new ArrayList<>();
		Matcher matcher = PAGE_BREAK_PATTERN.matcher(text);
		while (matcher.find()) {
			spans.add(new TextSpan(HL_HTML_PAGE_BREAK, matcher.start(), matcher.end()));
		}
		return spans;
	}

	private List<TextSpan> extractEmojiSpans(String text) {
		List<TextSpan> emojiSpans = new ArrayList<>();
		Matcher matcher = EMOJI_PATTERN.matcher(text);
		while (matcher.find()) {
			emojiSpans.add(new TextSpan(HL_MD_EMOJI, matcher.start(), matcher.end()));
		}
		return emojiSpans;
	}

//	private List<TextSpan> extractLinterSpans(String text) {
//	    List<TextSpan> spans = new ArrayList<>();
//	    
//	    // HEILUNG: Wir rufen die neue Signatur mit 'null' auf. 
//	    // Der Linter ist so programmiert, dass er bei 'null' einfach 
//	    // die Klassen-Prüfung überspringt, aber alle anderen Tests 
//	    // (HTML-Struktur, YAML, etc.) weiterhin ausführt.
//	    this.lastErrors = MarkdownLinter.checkHierarchy(text, null);
//	    
//	    for (MarkdownLinter.TagError error : lastErrors) {
//	        spans.add(new TextSpan(HL_LINT_ERROR, error.start, error.end));
//	    }
//	    return spans;
//	}

	/**
	 * Combines Markdown AST spans with Regex overlay spans (HTML, CSS, YAML, Linter). Implements a strict priority hierarchy for visual sovereignty, ensuring that specific tags
	 * (like search matches or comments) correctly override general syntax styles.
	 */
	private StyleSpans<Collection<String>> combineSpans(List<TextSpan> markdownSpans, List<TextSpan> overlaySpans, int textLength) {
		TreeSet<Integer> boundaries = new TreeSet<>(Arrays.asList(0, textLength));
		List<TextSpan> allSpans = new ArrayList<>(markdownSpans.size() + overlaySpans.size());
		allSpans.addAll(markdownSpans);
		allSpans.addAll(overlaySpans);

		for (TextSpan s : allSpans) {
			if (s.start < textLength) boundaries.add(s.start);
			if (s.end < textLength) boundaries.add(s.end);
		}

		StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		List<Integer> sortedBoundaries = new ArrayList<>(boundaries);

		for (int i = 0; i < sortedBoundaries.size() - 1; i++) {
			int start = sortedBoundaries.get(i);
			int end = sortedBoundaries.get(i + 1);
			int length = end - start;
			if (length <= 0) continue;

			// --- DIE HEILUNG: ZWEI SLOTS ---
			String colorStyle = HL_MD_TEXT; // Startet mit deinem gedimmten Weiß
			String lintStyle = null;        // Slot für die Linie
			int maxColorPriority = -1;

			for (TextSpan s : allSpans) {
				if (start >= s.start && end <= s.end) {
					String style = s.style;
					
					// 1. Wenn es ein Linter-Stil ist, kommt er in den Linien-Slot
					if (style.startsWith(PF_LINT)) {
						lintStyle = style;
					} 
					// 2. Ansonsten berechnen wir die Priorität für die Textfarbe
					else {
						int p = 0;
						if (style.startsWith(PF_MD)) p = 5;
						if (style.startsWith(PF_HTML)) p = 20;
						if (style.startsWith(PF_TBL)) p = 25;
						if (style.startsWith(PF_YAML)) p = 30;
						if (style.equals(HL_YAML_COMMENT)) p = 35;
						if (style.equals(HL_HTML_COMMENT)) p = 99;
						if (style.startsWith(PF_CSS)) p = 40;
						if (style.equals(HL_MD_PLACEHOLDER)) p = 50;
						if (style.equals(HL_MD_EMOJI)) p = 55;
						if (style.equals(HL_HTML_PAGE_BREAK)) p = 70;
						if (style.equals(HL_SEARCH_MATCH)) p = 110;
						if (style.equals(HL_UI_SANCTUARY)) p = 130;

						if (p > maxColorPriority) {
							maxColorPriority = p;
							colorStyle = style;
						}
					}
				}
			}

			// --- ZUSAMMENFÜHREN OHNE LÖSCHEN ---
			List<String> combined = new ArrayList<>();
			combined.add(colorStyle); // Fügt z.B. .md-text oder .md-bold hinzu
			
			if (lintStyle != null) {
				combined.add(lintStyle); // Fügt .lint-error (die Linie) hinzu
			}

			builder.add(combined, length);
		}

		return builder.create();
	}
	/**
	 * Parst HTML-Attribute innerhalb eines Tags und wendet spezifisches Highlighting an. Nutzt das vorkompilierte HTML_ATTR_PATTERN für maximale Performance.
	 */
	private void parseAttributes(int attrStart, String attrString, List<TextSpan> spans) {
		Matcher attrMatcher = HTML_ATTR_PATTERN.matcher(attrString);

		while (attrMatcher.find()) {
			String attrName = attrMatcher.group(1);
			String attrValue = attrMatcher.group(3); // Der eigentliche Wert (ohne Anführungszeichen)

			// A. Attribut-NAME (z.B. class, href)
			spans.add(new TextSpan(HL_HTML_ATTR_NAME, attrStart + attrMatcher.start(1), attrStart + attrMatcher.start(1) + attrName.length()));

			// B. Gleichheitszeichen (Zuweisung) -> Nutzt den Bracket-Style für optische Ruhe
			if (attrMatcher.group(2) != null) {
				spans.add(new TextSpan(HL_HTML_BRACKET, attrStart + attrMatcher.start(2), attrStart + attrMatcher.start(3)));
			}

			// C. Attribut-WERT (Typ-spezifisches Highlighting)
			if (attrValue != null) {
				int valueStart = attrStart + attrMatcher.start(3);
				int valueEnd = valueStart + attrValue.length();

				// Delegiert die Style-Entscheidung an die spezialisierte Helper-Methode
				String style = getAttributeValueStyle(attrName, attrValue);
				spans.add(new TextSpan(style, valueStart, valueEnd));
			}
		}
	}

	/**
	 * Bestimmt den Style für einen Attributwert basierend auf Name und Inhalt. Nutzt vorkompilierte Patterns und Konstanten für maximale Performance.
	 */
	private String getAttributeValueStyle(String attrName, String attrValue) {
		// 1. URL-Check (Kombination aus Attribut-Name und Inhalts-Muster)
		if (attrName.equalsIgnoreCase(ATTR_SRC) || attrName.equalsIgnoreCase(ATTR_HREF) || URL_PROTOCOL_PATTERN.matcher(attrValue).matches()) {
			return HL_HTML_ATTR_URL;
		}

		// 2. CSS-Check (Inline-Styles)
		if (attrName.equalsIgnoreCase(ATTR_STYLE)) {
			return HL_HTML_ATTR_STYLE;
		}

		// 3. Struktur-Check (Klassen)
		if (attrName.equalsIgnoreCase(ATTR_CLASS)) {
			return HL_HTML_ATTR_CLASS;
		}

		// 4. Identitäts-Check (IDs)
		if (attrName.equalsIgnoreCase(ATTR_ID)) {
			return HL_HTML_ATTR_ID;
		}

		// 5. Fallback für alle anderen Attributwerte
		return HL_HTML_ATTR_VAL;
	}

	private void handleLink(String fullText, Link link, List<TextSpan> spans) {
		Node textNode = link.getFirstChild();
		if (textNode != null)
			spans.add(new TextSpan(HL_MD_LINK, textNode.getStartOffset(), textNode.getEndOffset()));
		String url = link.getUrl().toString();
		if (url != null && !url.isEmpty()) {
			int urlStart = findUrlPosition(fullText, link.getStartOffset(), link.getEndOffset());
			if (urlStart > 0) {
				int urlEnd = Math.min(urlStart + url.length(), link.getEndOffset());
				spans.add(new TextSpan(HL_MD_LINK_URL, urlStart, urlEnd));
			}
		}
	}

	private void handleImage(String fullText, Image image, List<TextSpan> spans) {
		Node textNode = image.getFirstChild();
		if (textNode != null)
			spans.add(new TextSpan(HL_MD_IMAGE, textNode.getStartOffset(), textNode.getEndOffset()));
		String url = image.getUrl().toString();
		if (url != null && !url.isEmpty()) {
			int urlStart = findUrlPosition(fullText, image.getStartOffset(), image.getEndOffset());
			if (urlStart > 0) {
				int urlEnd = Math.min(urlStart + url.length(), image.getEndOffset());
				spans.add(new TextSpan(HL_MD_LINK_URL, urlStart, urlEnd));
			}
		}
	}

	private int findUrlPosition(String text, int start, int end) {
		if (text == null || start < 0 || end > text.length())
			return -1;
		String linkText = text.substring(start, end);
		int urlStartMarker = linkText.indexOf(MARKER_LINK_MID); // nutzt "]("
		return (urlStartMarker != -1) ? start + urlStartMarker + 2 : -1;
	}

	private StyleSpans<Collection<String>> createEmptySpans() {
		StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		builder.add(Collections.emptyList(), 0);
		return builder.create();
	}

	/**
	 * Initialisiert die statische Mapping-Tabelle zwischen Flexmark-Knotenklassen und unseren konsolidierten CSS-Highlighter-Konstanten.
	 */
	private static Map<Class<?>, String> createStyleMap() {
		Map<Class<?>, String> map = new HashMap<>();

		// --- Markdown Core Layer ---
		map.put(Heading.class, HL_MD_HEADING);
		map.put(StrongEmphasis.class, HL_MD_BOLD);
		map.put(Emphasis.class, HL_MD_ITALIC);
		map.put(Strikethrough.class, HL_MD_STRIKETHROUGH);
		map.put(Code.class, HL_MD_CODE_INLINE); // "md-inline-code"
		map.put(CodeBlock.class, HL_MD_CODE_BLOCK);
		map.put(BlockQuote.class, HL_MD_BLOCKQUOTE);
		map.put(ThematicBreak.class, HL_MD_HR);

		// --- Listen (Bullets & Numbers) ---
		map.put(BulletList.class, HL_MD_LIST);
		map.put(OrderedList.class, HL_MD_LIST);
		map.put(ListItem.class, HL_MD_LIST_ITEM);

		// HINWEIS: Tabellen werden manuell in handleTableNodes() verarbeitet,
		// da sie eine tiefere strukturelle Analyse erfordern als dieses flache Mapping.

		return Collections.unmodifiableMap(map);
	}

	/**
	 * Innere Klasse für TextSpans.
	 */
	private static final class TextSpan {
		final String style;
		final int    start;
		final int    end;

		TextSpan(String style, int start, int end) {
			this.style = style;
			this.start = start;
			this.end = end;
		}

	}

}
