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
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarkdownLinter - A high-performance, context-aware structural and syntactic validator.
 * 
 * DESIGN PHILOSOPHY & EFFICIENCY: 1. Single-Pass O(n) Analysis: The linter scans the document in a linear fashion, ensuring that processing time scales predictably even with large
 * files.
 * 
 * 2. Static Pattern Pre-compilation: All regular expressions are pre-compiled into static constants to avoid the heavy overhead of repeated pattern compilation during live typing
 * sessions.
 * 
 * 3. Indentation-Aware Heuristics: Instead of building a heavy, memory-intensive DOM tree, the linter uses geometric heuristics (solitary tag detection) to differentiate between
 * structural block elements and inline text formatting.
 * 
 * 4. Stream-Based Validation: Supports multi-line CSS values and HTML attributes by treating content as a continuous stream, using non-greedy lookaheads to maintain precision
 * without sacrificing performance.
 * 
 * 5. Low Memory Footprint: Utilizes lightweight stack-based parity checks for hierarchy validation, minimizing object allocation and garbage collection pressure.
 * 
 * @author Henryk Daniel Zschuppan / Kassandra AGI
 * @version 1.0.0 (Professional Grade)
 */
public class MarkdownLinter {

	// --- YAML-Begrenzer-Patterns ---
	// Dieser Pattern prüft exakt den START des DOKUMENTS auf '---', erlaubt aber führende Leerzeichen.
	private static final Pattern YAML_START_DELIMITER_AT_BEGINNING = Pattern.compile("\\A\\s*---\\s*$", Pattern.MULTILINE);
	// Dieser Pattern prüft ein '---' als Zeile, das irgendwo im Dokument stehen kann (für den End-Delimiter).
	private static final Pattern YAML_LINE_DELIMITER = Pattern.compile("^---\\s*$", Pattern.MULTILINE);

	/** Isoliert den YAML-Block am Dokumentanfang und fängt den Inhalt in Gruppe 1 */
	// HEILUNG: Erlaubt führende Leerzeichen vor dem Start-Delimiter
	private static final Pattern YAML_BLOCK_PATTERN = Pattern.compile("\\A\\s*---\\s*\\n([\\s\\S]*?)\\n^---\\s*$", Pattern.MULTILINE);

	private static final String MARKER_YAML_DELIM = "---";
	private static final String MARKER_YAML_SEP   = ":";
	// =============================================================================================
	// LOGICAL CONTEXTS (Error Scopes & Reporting)
	// =============================================================================================
	private static final String CTX_CSS      = "css";
	private static final String CTX_CSS_RULE = "css-rule";
	private static final String CTX_CSS_VAL  = "css-value";
	private static final String CTX_HTML     = "html";
	private static final String CTX_YAML     = "yaml";
	private static final String CTX_MD       = "markdown";
	private static final String CTX_FRAGMENT = "fragment";

	// =============================================================================================
	// SYNTAX MARKERS & PREFIXES (String Literals)
	// =============================================================================================

	private static final String MARKER_STYLE_ATTR_EQ     = "style=";
	private static final String MARKER_TAG_SELF_CLOSE    = "/>";
	private static final String MARKER_HTML_COMMENT      = "!--";
	private static final String MARKER_MD_COMMENT_OPEN   = "<!--";
	private static final String MARKER_MD_COMMENT_CLOSE  = "-->";
	private static final String MARKER_CSS_COMMENT_OPEN  = "/*";
	private static final String MARKER_CSS_COMMENT_CLOSE = "*/";
	private static final String MARKER_HTTP              = "<http:";
	private static final String MARKER_HTTPS             = "<https:";
	private static final String MARKER_STAR              = "*";
	private static final String MARKER_COMMENT_ALT       = "/";
	private static final String AT_PREFIX                = "@";

	// --- Markdown Block Prefixes & Special Characters for Linter Logic ---
//	private static final String MARKER_MD_HEADER_PREFIX      = "#";
//	private static final String MARKER_MD_BULLET_LIST_PREFIX = "* ";
//	private static final String MARKER_MD_DASH_LIST_PREFIX   = "- ";
//	private static final String MARKER_MD_BLOCKQUOTE_PREFIX  = "> ";

	/** All characters that signal the start of a Markdown block or inline element. */
	private static final String MARKDOWN_START_CHARS = "#*->+_|"; // Including pipe for tables

	/** Pre-compiled pattern for extracting the tag name from an HTML line. Moved out for performance. */
	private static final Pattern HTML_TAG_NAME_EXTRACTOR = Pattern.compile("<([a-zA-Z0-9]+)");

	// =============================================================================================
	// ATOMIC LITERALS (Character Constants for Performance)
	// =============================================================================================
	private static final char CHAR_MASK         = ' ';
	private static final char CHAR_RETURN       = '\r';
	private static final char CHAR_LINE_BREAK   = '\n';
	private static final char CHAR_EQUALS       = '=';
	private static final char CHAR_COLON        = ':';
	private static final char CHAR_SEMICOLON    = ';';
	private static final char CHAR_COMMA        = ',';
	private static final char CHAR_QUOTE_DOUBLE = '"';
	private static final char CHAR_QUOTE_SINGLE = '\'';
	private static final char CHAR_BRACE_OPEN   = '{';
	private static final char CHAR_BRACE_CLOSE  = '}';
	private static final char CHAR_SLASH        = '/';
	private static final char CHAR_TAG_OPEN     = '<';
	private static final char CHAR_TAG_CLOSE    = '>';
	private static final char CHAR_BACKTICK     = '`';

	// =============================================================================================
	// SYNTAX MARKERS & PREFIXES (Link Integrity)
	// =============================================================================================
	private static final String MARKER_IMG_INTENT   = "![";
	private static final String MARKER_LINK_INTENT  = "](";
	private static final String MARKER_BRACKET_OPEN = "[";

	// =============================================================================================
	// ATOMIC LITERALS (Link Integrity)
	// =============================================================================================
	private static final char CHAR_BRACKET_CLOSE = ']';
//	private static final char CHAR_PAREN_OPEN    = '(';
	private static final char CHAR_PAREN_CLOSE = ')';

	// STRUCTURAL LOOKUP SETS (O(1) Efficiency)
	// =============================================================================================
	/** Globale Tags, die in HTML-Fragmenten strikt verboten sind */
	private static final List<String> FORBIDDEN_GLOBAL_TAGS = List.of("<html", "<head", "<body", "<title", "<meta");

	/** Menge aller selbstschließenden HTML-Tags zur Hierarchie-Validierung */
	private static final Set<String> HTML_SELF_CLOSING_TAGS = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr");

	/** Menge der erlaubten CSS At-Rules für die Syntax-Prüfung */
	private static final Set<String> CSS_AT_RULES = Set.of("media", "keyframes", "font-face", "import", "container", "supports", "layer", "page");

	/** Hilfs-Pattern zum Parsen von CSS At-Rules */
	private static final Pattern AT_RULE_SPLIT_PATTERN = Pattern.compile("[\\s:{]");

	// =============================================================================================
	// REGEX ENGINES (Pre-compiled Pattern Library)
	// =============================================================================================

	// --- 2. HTML VALIDATION ---
	// 1. DER SCANNER (Scanner):
	// Bleibt diszipliniert mit [^<>], um nicht in den nächsten Tag hineinzufressen.
	// Erlaubt Whitespace und Attribute, erwartet aber zwingend ein abschließendes '>'.
	private static final Pattern TAG_PATTERN = Pattern.compile("<(/?[a-zA-Z][a-zA-Z0-9]*)(\\s+[^<>]*)?/?>");

	// Matches a line that contains ONLY an HTML tag (potentially with attributes and whitespace).
	// This is the precise trigger for CommonMark HTML Block Type 6 behavior.
//	private static final Pattern SOLITARY_HTML_TAG_LINE_PATTERN = Pattern.compile("^\\s*<([a-zA-Z][a-zA-Z0-9]*)([^>]*?)?>\\s*$", Pattern.CASE_INSENSITIVE);
	/** Pattern to iterate through lines of text, preserving their original content and offsets. */
	private static final Pattern LINE_ITERATOR_PATTERN = Pattern.compile("(?m)^.*$");

	// 2. DER FINDER (Probe):
	// HEILUNG: Wir entfernen das '/' aus dem Ausschluss!
	// (?!:) bedeutet: "Ignoriere nur, wenn ein Doppelpunkt folgt (URLs)."
	// Alles andere (Space, Zeilenumbruch, Slash) wird als potenzieller Tag-Start erkannt.
	private static final Pattern TAG_START_PROBE = Pattern.compile("<(/?[a-zA-Z][a-zA-Z0-9]*)(?!:)");

	/** Detektiert Start- und End-Marker von HTML-Kommentaren */
	private static final Pattern HTML_COMMENT_MARKER = Pattern.compile("<!--|-->");

	/** Erkennt HTML-Pre-Blöcke zur Maskierung (DOTALL für Mehrzeiler) */
//	private static final Pattern PRE_BLOCK_PATTERN = Pattern.compile("(?s)<pre>.*?</pre>");

	// --- 3. CSS VALIDATION (Nested Grammar) ---
	/** Findet CSS-Kommentare innerhalb von Style-Blöcken */
	private static final Pattern CSS_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/");

	/** Detektiert Start- und End-Marker von CSS-Kommentaren */
	private static final Pattern CSS_COMMENT_MARKER = Pattern.compile("/\\*|\\*/");

	/** Teilt CSS-Blöcke in einzelne Anweisungen (Statements) */
	private static final Pattern CSS_STATEMENT_PATTERN = Pattern.compile("([^;\\n]+)");

	/** Trennt CSS-Eigenschaft und Wert mit Lookahead für das nächste Statement */
	private static final Pattern CSS_PROP_VALUE_PATTERN = Pattern.compile("([a-zA-Z\\-]+)\\s*:\\s*([\\s\\S]+?)(?=\\s*[a-zA-Z\\-]+\\s*:|[;{}]|$)");

	// --- NEU: DESIGN-INTEGRITÄT (v9.5) ---
//		private static final String CTX_DESIGN = "design-integrity";

	/** Erkennt Klassen-Selektoren in CSS: .klasse { ... } */
	private static final Pattern CSS_CLASS_DEF_PATTERN = Pattern.compile("\\.([a-zA-Z0-9_-]+)(?=\\s*\\{)");

	/** Erkennt Klassen-Attribute in HTML: class="meine-klasse" */
//	private static final Pattern HTML_CLASS_ATTR_PATTERN = Pattern.compile("class\\s*=\\s*\"([^\"]+)\"");
	private static final Pattern HTML_CLASS_ATTR_PATTERN = Pattern.compile("class\\s*=\\s*[\"']([^\"']+)[\"']");

	// --- 4. MARKDOWN VALIDATION & MASKING ---
	// =============================================================================================
	// REGEX ENGINES (Restored to Heritage Stability)
	// =============================================================================================

	/** Isoliert <style> Blöcke für die CSS-Validierung */
	private static final Pattern STYLE_BLOCK_PATTERN = Pattern.compile("<style[^>]*>([\\s\\S]*?)(?:</style>|$)", Pattern.CASE_INSENSITIVE);

	/** Universeller Schild für HTML-Container (pre, code, script, style) */
	private static final Pattern HTML_VERBATIM_PATTERN = Pattern.compile("(?is)<(pre|code|script|style)[^>]*>.*?</\\1>");

	/** Das Herzstück des Oracles: Findet Gruppen von Backticks, die nicht escaped sind */
	private static final Pattern BACKTICK_GROUP_PATTERN = Pattern.compile("(?<!\\\\)(`+)");

	/** Erkennt die Absicht von Links und Bildern im maskierten Text */
	private static final Pattern LINK_INTENT_PATTERN = Pattern.compile("(?<!\\\\)(\\!\\[|\\]\\()|(?<!\\\\|\\!)\\[");

	// --- HILFSKLASSEN ---
	public static class TagError {
		public final int     start;
		public final int     end;
		public final ErrorID id;
		public final String  tagName;

		public TagError(int s, int e, ErrorID i, String t) {
			this.start = s;
			this.end = e;
			this.id = i;
			this.tagName = t;
		}
	}

	private static class TagAnchor {
		String name;
		int    start;
		int    end;

		TagAnchor(String n, int s, int e) {
			this.name = n;
			this.start = s;
			this.end = e;
		}
	}

	// --- NEU (Schlank & Fokussiert) ---
	private static class BraceAnchor {
		final int start; // Macht es final für Stabilität

		BraceAnchor(int s) {
			this.start = s;
		}
	}

	public static String getErrorDescription(TagError err) {
		if (err == null || err.id == null)
			return "Unknown Linter Error";

		return switch (err.id) {
		case MISSING_YAML_START_DELIMITER -> "Critical: Missing YAML start delimiter '---' at the beginning of the document.";
		case MISSING_YAML_END_DELIMITER   -> "Critical: Missing YAML end delimiter '---' after the frontmatter block.";
		case REDUNDANT_SEMICOLON          -> "Redundant semicolon in YAML (not used in values)";
		case ILLEGAL_FRAGMENT_TAG         -> "Fragment Integrity Error: Global tags (<html/head/body>) are not allowed.";
		case REDUNDANT_CLOSING            -> "Redundant closing tag: </" + err.tagName + ">";
		case MISMATCHED_CLOSING           -> "Tag mismatch: Unexpected </" + err.tagName + ">";
		case UNCLOSED_OPENING             -> "Unclosed tag: <" + err.tagName + ">";
		case MALFORMED_TAG                ->
		    err.tagName.startsWith(AT_PREFIX) ? "Missing '@' prefix for CSS at-rule: " + err.tagName : "Malformed or unclosed tag: <" + err.tagName + " (missing '>')";
		case UNCLOSED_CODEBLOCK           -> "Unclosed Markdown code block (```)";
		case UNCLOSED_COMMENT             -> "Unclosed comment (missing end marker)";
		case REDUNDANT_COMMENT_CLOSING    -> "Redundant comment closing (missing start marker)";
		case UNCLOSED_BRACE               -> "Unclosed curly brace '{' in CSS";
		case REDUNDANT_BRACE              -> "Redundant closing brace '}' in CSS";
		case MISSING_EQUALS               -> "Missing '=' in attribute of <" + err.tagName + ">";
		case MISSING_SEMICOLON            -> err.tagName.equals(CTX_CSS_RULE) ? "Missing semicolon in CSS rule" : "Missing semicolon in style attribute of <" + err.tagName + ">";
		case MISSING_COLON                -> err.tagName.equals(CTX_CSS_RULE) ? "Missing colon in CSS rule"
		        : err.tagName.equals(CTX_YAML) ? "Missing colon in YAML frontmatter" : "Missing colon in style attribute of <" + err.tagName + ">";
		case MALFORMED_ATTRIBUTE          -> err.tagName.equals(CTX_CSS_VAL) ? "Unclosed quote in CSS value"
		        : err.tagName.equals(CTX_YAML) ? "Unclosed quote in YAML frontmatter" : "Malformed attribute in <" + err.tagName + ">";
		case UNCLOSED_LINK_BRACKET        -> "Unclosed Markdown bracket '[' (missing ']')";
		case UNCLOSED_LINK_PAREN          -> "Unclosed link target URL '(' (missing ')')";
		case MALFORMED_IMAGE_TAG          -> "Unclosed image alt-text '![' (missing ']')";
		// Innerhalb des switch-Blocks:
		case ILLEGAL_DUPLICATE_STYLE_BLOCK -> "Duplicate <style> block. Only one is allowed to ensure design consistency.";
		case MISSING_CSS_CLASS             -> "Missing CSS definition for: ." + err.tagName;
		case MISSING_BLANK_LINE_AFTER_HTML -> {
			String tag = err.tagName.toLowerCase();
			// Die souveräne Wahrheit: Kurz, knapp und ohne falsche Versprechen.
			yield "Markdown requires a blank line after the HTML block '<" + tag + ">' to correctly resume parsing subsequent Markdown elements.";
		}
		default                            -> "Unhandled Linter Error: " + err.id.name();
		};
	}

	public enum ErrorID {
		MISSING_YAML_START_DELIMITER, // NEU: YAML-Block beginnt nicht mit '---'
		MISSING_YAML_END_DELIMITER, // NEU: YAML-Block wird nicht mit '---' geschlossen
		REDUNDANT_CLOSING,
		MISMATCHED_CLOSING,
		UNCLOSED_OPENING,
		MALFORMED_TAG,
		MALFORMED_ATTRIBUTE,
		MISSING_SEMICOLON,
		MISSING_COLON,
		UNCLOSED_BRACE,
		REDUNDANT_BRACE,
		UNCLOSED_CODEBLOCK,
		UNCLOSED_COMMENT,
		REDUNDANT_COMMENT_CLOSING,
		REDUNDANT_SEMICOLON,
		ILLEGAL_FRAGMENT_TAG,
		UNCLOSED_LINK_BRACKET, // [Text ohne ]
		UNCLOSED_LINK_PAREN, // [Text](url ohne )
		MALFORMED_IMAGE_TAG, // ![Alt ohne ]
		MISSING_EQUALS,
		ILLEGAL_DUPLICATE_STYLE_BLOCK,
		MISSING_CSS_CLASS,
		MISSING_BLANK_LINE_AFTER_HTML // NEU: Warnung für fehlende Leerzeile
	}

	/** Erkennt Klassen-Attribute in HTML: class="meine-klasse" ODER class='meine-klasse' */

	public static List<TagError> checkHierarchy(String text, java.util.Set<String> definedClasses) {
		List<TagError> errors = new ArrayList<>();
		if (text == null)
			return errors;

		lintYamlFrontmatter(text, errors);
		lintStyleBlockUniqueness(text, errors);

		// 1. ZUERST MASKIEREN: Schützt vor Code-Blöcken (Backticks)
		String shieldedText = maskSafeZones(text, errors);

		// 2. JETZT DIE KLASSEN PRÜFEN (auf dem geschützten Text!)
		if (definedClasses != null && !definedClasses.isEmpty()) {
			lintMissingClasses(shieldedText, definedClasses, errors);
		}

		// Restliche Prüfungen auf shieldedText...
		lintHtmlComments(shieldedText, errors);
		lintCssBlocks(text, errors);
		lintHtmlTagsAndHierarchy(shieldedText, errors);
		lintMarkdownLinks(shieldedText, errors);
		lintMissingBlankLineAfterHtml(text, errors); // NEU: Auf dem Originaltext prüfen!
		return errors;
	}

	/**
	 * Stellt sicher, dass nur ein einziger, souveräner <style>-Block im Dokument existiert. Dies ist eine architektonische Regel zur Gewährleistung der Design-Konsistenz.
	 */
	private static void lintStyleBlockUniqueness(String text, List<TagError> errors) {
		// Wir nutzen das bereits vorhandene STYLE_BLOCK_PATTERN, das den öffnenden Tag findet.
		// Wir passen es leicht an, um nur den Tag selbst zu finden.
		Pattern styleTagFinder = Pattern.compile("<style[^>]*>", Pattern.CASE_INSENSITIVE);
		Matcher m = styleTagFinder.matcher(text);

		int count = 0;
		while (m.find()) {
			count++;
			// Wenn wir einen zweiten (oder dritten, etc.) Block finden...
			if (count > 1) {
				// ...markieren wir ihn als illegalen Duplikat-Fehler.
				errors.add(new TagError(m.start(), m.end(), ErrorID.ILLEGAL_DUPLICATE_STYLE_BLOCK, CTX_HTML));
			}
		}
	}

	private static String maskSafeZones(String text, List<TagError> errors) {
		if (text == null || text.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder(text);

		// 1. SCHRITT: HTML-Schutz (pre, code, style, script einfrieren)
		// Das verhindert, dass Backticks in <code> den Markdown-Linter verwirren.
		applyMask(sb, HTML_VERBATIM_PATTERN.matcher(text));

		// 2. SCHRITT: Backtick-Souveränität (Das Oracle)
		// Es maskiert valide Paare im 'sb' und trägt Fehler in 'errors' ein.
		lintBacktickSovereignty(text, sb, errors);

		return sb.toString();
	}

	private static void lintBacktickSovereignty(String originalText, StringBuilder shielded, List<TagError> errors) {
		Matcher m = BACKTICK_GROUP_PATTERN.matcher(originalText);
		int searchPos = 0;

		while (m.find(searchPos)) {
			int openerStart = m.start();
			int openerEnd = m.end();
			String ticks = m.group(); // z.B. "```" oder "`"

			// Wir prüfen, ob dieser Opener bereits durch den HTML-Schild maskiert wurde
			if (shielded.charAt(openerStart) == CHAR_MASK) {
				searchPos = openerEnd;
				continue;
			}

			// Suche nach dem exakt gleichen Gegenstück
			int closerStart = originalText.indexOf(ticks, openerEnd);

			// Verifizierung des Gegenstücks (keine Flankierung durch weitere Backticks)
			while (closerStart != -1) {
				boolean precededByBacktick = originalText.charAt(closerStart - 1) == CHAR_BACKTICK;
				boolean followedByBacktick = (closerStart + ticks.length() < originalText.length()) && originalText.charAt(closerStart + ticks.length()) == CHAR_BACKTICK;

				// Wenn der Closer auch schon maskiert ist, ist er ungültig
				boolean alreadyMasked = shielded.charAt(closerStart) == CHAR_MASK;

				if (!precededByBacktick && !followedByBacktick && !alreadyMasked)
					break;
				closerStart = originalText.indexOf(ticks, closerStart + 1);
			}

			if (closerStart != -1) {
				// VALIDES PAAR: Alles dazwischen maskieren
				maskInternal(shielded, openerStart, closerStart + ticks.length());
				searchPos = closerStart + ticks.length();
			} else {
				// FEHLER: Punktgenaue Markierung des Openers!
				addFullLineError(originalText, openerStart, ErrorID.UNCLOSED_CODEBLOCK, CTX_MD, errors);
				// Wir maskieren nur den Opener selbst, um den Rest weiter prüfen zu können
				maskInternal(shielded, openerStart, openerEnd);
				searchPos = openerEnd;
			}
		}
	}

	private static void maskInternal(StringBuilder sb, int start, int end) {
		for (int i = start; i < end; i++) {
			char c = sb.charAt(i);
			if (c != CHAR_LINE_BREAK && c != CHAR_RETURN) {
				sb.setCharAt(i, CHAR_MASK);
			}
		}
	}

	private static void addFullLineError(String text, int pos, ErrorID id, String ctx, List<TagError> errors) {
		int lineStart = text.lastIndexOf(CHAR_LINE_BREAK, pos) + 1;
		int lineEnd = text.indexOf(CHAR_LINE_BREAK, pos);
		if (lineEnd == -1)
			lineEnd = text.length();
		errors.add(new TagError(lineStart, lineEnd, id, ctx));
	}

	/**
	 * Interne Hilfsmethode: Wendet die Maskierung auf alle Matches eines Matchers an. Bewahrt Zeilenumbrüche, um die Zeilennummern für den Linter nicht zu korrumpieren.
	 */
	private static void applyMask(StringBuilder sb, Matcher matcher) {
		while (matcher.find()) {
			for (int i = matcher.start(); i < matcher.end(); i++) {
				char c = sb.charAt(i);
				if (c != CHAR_LINE_BREAK && c != CHAR_RETURN)
					sb.setCharAt(i, CHAR_MASK);
			}
		}
	}

	/**
	 * TODO integrate this with forge header: Prüft, ob ein HTML-Fragment globale Seiten-Tags enthält, die die Integrität des exportierten Dokuments gefährden würden.
	 */
	public static void validateFragmentIntegrity(String html, List<TagError> errors) {
		if (html == null || html.isEmpty())
			return;

		// Einmalige Konvertierung in Kleinbuchstaben für den Vergleich
		String lowerHtml = html.toLowerCase();

		for (String forbiddenTag : FORBIDDEN_GLOBAL_TAGS) {
			int start = lowerHtml.indexOf(forbiddenTag);

			if (start != -1) {
				// Wir markieren exakt das verbotene Wort (z.B. "<html")
				int end = start + forbiddenTag.length();

				// Nutzt CTX_FRAGMENT statt des Magic-Strings "fragment"
				errors.add(new TagError(start, end, ErrorID.ILLEGAL_FRAGMENT_TAG, CTX_FRAGMENT));
			}
		}
	}

	/**
	 * Validiert die Integrität von Markdown-Links und Bildern in drei Stufen. Schützt vor ungeschlossenen eckigen Klammern und runden URL-Klammern.
	 */
	private static void lintMarkdownLinks(String shieldedText, List<TagError> errors) {
		if (shieldedText == null || shieldedText.isEmpty())
			return;

		Matcher m = LINK_INTENT_PATTERN.matcher(shieldedText);
		while (m.find()) {
			String marker = m.group();
			int pos = m.start();

			// FALL 1: Bild-Absicht (![) -> Prüfe auf schließendes ]
			if (marker.equals(MARKER_IMG_INTENT)) {
				int end = shieldedText.indexOf(CHAR_BRACKET_CLOSE, pos);
				if (end == -1 || isLineBreakBetween(shieldedText, pos, end)) {
					errors.add(new TagError(pos, pos + 2, ErrorID.MALFORMED_IMAGE_TAG, CTX_MD));
				}
			}

			// FALL 2: Link-Übergang (]( ) -> Prüfe auf schließendes )
			else if (marker.equals(MARKER_LINK_INTENT)) {
				int end = shieldedText.indexOf(CHAR_PAREN_CLOSE, pos);
				if (end == -1 || isLineBreakBetween(shieldedText, pos, end)) {
					// Fehlerposition ist die öffnende runde Klammer
					errors.add(new TagError(pos + 1, pos + 2, ErrorID.UNCLOSED_LINK_PAREN, CTX_MD));
				}
			}

			// FALL 3: Einfache öffnende Klammer ([) -> Prüfe auf schließendes ]
			else if (marker.equals(MARKER_BRACKET_OPEN)) {
				int end = shieldedText.indexOf(CHAR_BRACKET_CLOSE, pos);
				if (end == -1 || isLineBreakBetween(shieldedText, pos, end)) {
					errors.add(new TagError(pos, pos + 1, ErrorID.UNCLOSED_LINK_BRACKET, CTX_MD));
				}
			}
		}
	}

	/**
	 * Hilfsmethode: Prüft, ob zwischen zwei Positionen ein Zeilenumbruch liegt. Dies verhindert, dass der Linter Klammern über Absätze hinweg verknüpft.
	 */
	private static boolean isLineBreakBetween(String text, int start, int end) {
		return text.substring(start, end).indexOf(CHAR_LINE_BREAK) != -1;
	}

	/**
	 * Analysiert den YAML-Frontmatter-Block am Dokumentanfang. Prüft auf fehlende Doppelpunkte, redundante Semikolons und korrupte Anführungszeichen.
	 */
//	private static void lintYamlFrontmatter(String text, List<TagError> errors) {
//		if (text == null)
//			return;
//
//		Matcher m = YAML_BLOCK_PATTERN.matcher(text);
//		if (m.find()) {
//			int blockStart = m.start();
//			String content = m.group();
//
//			// Wir splitten nur den kleinen YAML-Block, um die Zeilenstruktur zu wahren
//			String[] lines = content.split(String.valueOf(CHAR_LINE_BREAK));
//			int currentOffset = blockStart;
//
//			for (String line : lines) {
//				String trimmed = line.trim();
//
//				// Überspringe Leerzeilen und die Trenner (---)
//				if (!trimmed.isEmpty() && !trimmed.equals(MARKER_YAML_DELIM)) {
//
//					// 1. Check: Fehlender Doppelpunkt
//					if (!trimmed.contains(MARKER_YAML_SEP)) {
//						int errStart = currentOffset + line.indexOf(trimmed);
//						errors.add(new TagError(errStart, errStart + trimmed.length(), ErrorID.MISSING_COLON, CTX_YAML));
//					} else {
//						// Wir teilen die Zeile am ersten Doppelpunkt in Key und Value
//						String[] parts = trimmed.split(MARKER_YAML_SEP, 2);
//						String valuePart = parts[1].trim();
//
//						if (valuePart.length() > 0) {
//							// 2. Check: Redundantes Semikolon (Präferenz: YAML braucht keine Semikolons)
//							if (valuePart.endsWith(String.valueOf(CHAR_SEMICOLON)) && !isProperlyQuoted(valuePart)) {
//
//								int scPos = currentOffset + line.lastIndexOf(CHAR_SEMICOLON);
//								errors.add(new TagError(scPos, scPos + 1, ErrorID.REDUNDANT_SEMICOLON, CTX_YAML));
//
//								// Bereinige den Wert für den folgenden Quote-Check
//								valuePart = valuePart.substring(0, valuePart.length() - 1).trim();
//							}
//
//							// 3. Check: Malformed Quotes (Ungepaarte Anführungszeichen)
//							if (valuePart.length() > 0) {
//								char first = valuePart.charAt(0);
//								char last = valuePart.charAt(valuePart.length() - 1);
//
//								boolean isStartQuote = (first == CHAR_QUOTE_DOUBLE || first == CHAR_QUOTE_SINGLE);
//								boolean isEndQuote = (last == CHAR_QUOTE_DOUBLE || last == CHAR_QUOTE_SINGLE);
//
//								if ((isStartQuote || isEndQuote) && first != last) {
//									int errStart = currentOffset + line.indexOf(valuePart);
//									errors.add(new TagError(errStart, errStart + valuePart.length(), ErrorID.MALFORMED_ATTRIBUTE, CTX_YAML));
//								}
//							}
//						}
//					}
//				}
//				// Offset für die nächste Zeile berechnen (+1 für den Linebreak)
//				currentOffset += line.length() + 1;
//			}
//		}
//	}

	/**
	 * Analyzes the YAML frontmatter block at the start of the document for both delimiter integrity and content syntax. This linter treats YAML as an **optional** block. It only
	 * reports delimiter errors if there is an attempt to define a YAML block (i.e., a start delimiter is present). If no start delimiter is present at all, no YAML-related errors
	 * are reported.
	 *
	 * @param text   The complete Markdown text from the editor.
	 * @param errors The list to which detected errors will be added.
	 */
	private static void lintYamlFrontmatter(String text, List<TagError> errors) {
		if (text == null)
			return;

		// --- PHASE 1: PRÜFUNG DER DELIMITER-INTEGRITÄT ---
		Matcher startMatcher = YAML_START_DELIMITER_AT_BEGINNING.matcher(text);
		boolean hasStartDelimiter = startMatcher.find(0);
		int startDelimiterEndOffset = hasStartDelimiter ? startMatcher.end() : -1;

		// Wenn KEIN Start-Delimiter am absoluten Anfang gefunden wird, dann ist es KEIN YAML-Frontmatter.
		// In diesem Fall gibt es auch keine Delimiter-Fehler zu melden.
		if (!hasStartDelimiter) {
			return; // Hier ist das YAML optional: Wenn kein Start, dann kein Linting.
		}

		// Wenn ein Start-Delimiter vorhanden ist, MUSS auch ein End-Delimiter vorhanden sein.
		// Suche nach dem End-Delimiter erst NACH dem Start-Delimiter.
		Matcher endMatcher = YAML_LINE_DELIMITER.matcher(text);
		boolean hasEndDelimiter = false;
		int endDelimiterStartOffset = -1;

		if (startDelimiterEndOffset != -1) {
			if (endMatcher.find(startDelimiterEndOffset)) {
				endDelimiterStartOffset = endMatcher.start();
				hasEndDelimiter = true;
			}
		}

		// ERROR: Start-Delimiter da, aber Ende fehlt.
		if (hasStartDelimiter && !hasEndDelimiter) {
			errors.add(new TagError(startDelimiterEndOffset, startDelimiterEndOffset, ErrorID.MISSING_YAML_END_DELIMITER, "---"));
		}
		// ERROR: Start-Delimiter da, Ende ist da, aber falsch positioniert (vor/auf Start).
		else if (hasStartDelimiter && hasEndDelimiter && endDelimiterStartOffset <= startDelimiterEndOffset) {
			errors.add(new TagError(endDelimiterStartOffset, endDelimiterStartOffset, ErrorID.MISSING_YAML_END_DELIMITER, "---"));
		}

		// --- PHASE 2: PRÜFUNG DES INHALTS (NUR WENN DER BLOCK VOLLSTÄNDIG IST) ---
		// Hier nutzen wir den YAML_BLOCK_PATTERN, der den Inhalt in Gruppe 1 fängt.
		// Wichtig: m.find() MUSS den gesamten gültigen Block finden, inklusive beider Delimiter.
		Matcher m = YAML_BLOCK_PATTERN.matcher(text);

		// Nur wenn ein VOLLSTÄNDIGER, syntaktisch korrekter YAML-Block gefunden wurde,
		// prüfen wir den INHALT auf fehlende Doppelpunkte etc.
		if (m.find(0) && m.start() == 0) { // m.find(0) um zu gewährleisten, dass es am Anfang des Dokuments ist
			String content = m.group(1); // Der reine Inhalt zwischen den Delimitern

			String[] lines = content.split(String.valueOf(CHAR_LINE_BREAK));
			int currentOffset = m.start(1); // Start des Inhalts (Gruppe 1) relativ zum Dokument

			for (String line : lines) {
				String trimmed = line.trim();

				if (!trimmed.isEmpty() && !trimmed.equals(MARKER_YAML_DELIM)) {
					if (trimmed.startsWith("#") || trimmed.startsWith(MARKER_MD_COMMENT_OPEN)) {
						// Kommentare ignorieren
					} else if (!trimmed.contains(MARKER_YAML_SEP)) {
						int errStart = currentOffset + line.indexOf(trimmed);
						errors.add(new TagError(errStart, errStart + trimmed.length(), ErrorID.MISSING_COLON, CTX_YAML));
					} else {
						String[] parts = trimmed.split(MARKER_YAML_SEP, 2);
						String valuePart = parts[1].trim();

						if (valuePart.length() > 0) {
							if (valuePart.endsWith(String.valueOf(CHAR_SEMICOLON)) && !isProperlyQuoted(valuePart)) {
								int scPos = currentOffset + line.lastIndexOf(CHAR_SEMICOLON);
								errors.add(new TagError(scPos, scPos + 1, ErrorID.REDUNDANT_SEMICOLON, CTX_YAML));
								valuePart = valuePart.substring(0, valuePart.length() - 1).trim();
							}
							if (valuePart.length() > 0) {
								char first = valuePart.charAt(0);
								char last = valuePart.charAt(valuePart.length() - 1);
								boolean isStartQuote = (first == CHAR_QUOTE_DOUBLE || first == CHAR_QUOTE_SINGLE);
								boolean isEndQuote = (last == CHAR_QUOTE_DOUBLE || last == CHAR_QUOTE_SINGLE);
								if ((isStartQuote || isEndQuote) && first != last) {
									int errStart = currentOffset + line.indexOf(valuePart);
									errors.add(new TagError(errStart, errStart + valuePart.length(), ErrorID.MALFORMED_ATTRIBUTE, CTX_YAML));
								}
							}
						}
					}
				}
				currentOffset += line.length() + 1;
			}
		}
	}

	/**
	 * Hilfsmethode: Prüft, ob ein YAML-Wert bereits sauber in Quotes eingeschlossen ist.
	 */
	private static boolean isProperlyQuoted(String val) {
		if (val.length() < 2)
			return false;
		char f = val.charAt(0);
		char l = val.charAt(val.length() - 1);
		// Es ist sauber gequoted, wenn Anfang und Ende übereinstimmen und Quotes sind
		return (f == CHAR_QUOTE_DOUBLE || f == CHAR_QUOTE_SINGLE) && (f == l);
	}

	private static void lintCssBlocks(String text, List<TagError> errors) {
		Matcher sbm = STYLE_BLOCK_PATTERN.matcher(text);
		while (sbm.find()) {
			int bStart = sbm.start(1);
			String css = sbm.group(1);

			// 1. Kommentare validieren
			lintCssComments(css, bStart, errors);

			// 2. Kommentare maskieren, um die anderen Linter nicht zu stören
			StringBuilder masked = new StringBuilder(css);
			Matcher cm = CSS_COMMENT_PATTERN.matcher(css);
			while (cm.find()) {
				for (int i = cm.start(); i < cm.end(); i++)
					masked.setCharAt(i, CHAR_MASK);
			}
			String proc = masked.toString();

			// --- DIE ENTKOPPELTEN SPEZIALISTEN ---
			// 3. Syntaktische Prüfung (At-Rules)
			lintCssAtRules(proc, bStart, errors);

			// 4. Strukturelle Prüfung (Braces & Properties)
			lintCssBraces(proc, bStart, errors);
		}
	}

	private static void lintCssBraces(String proc, int bStart, List<TagError> errors) {
		Stack<BraceAnchor> braceStack = new Stack<>();
		for (int i = 0; i < proc.length(); i++) {
			char c = proc.charAt(i);
			if (c == CHAR_BRACE_OPEN) {
				braceStack.push(new BraceAnchor(i));
			} else if (c == CHAR_BRACE_CLOSE) {
				if (!braceStack.isEmpty()) {
					BraceAnchor open = braceStack.pop();
					String blockContent = proc.substring(open.start + 1, i);
					if (!blockContent.contains(String.valueOf(CHAR_BRACE_OPEN))) {
						lintCssProperties(blockContent, bStart + open.start + 1, CTX_CSS_RULE, errors);
					}
				} else {
					errors.add(new TagError(bStart + i, bStart + i + 1, ErrorID.REDUNDANT_BRACE, CTX_CSS));
				}
			}
		}
		while (!braceStack.isEmpty()) {
			BraceAnchor unclosed = braceStack.pop();
			errors.add(new TagError(bStart + unclosed.start, bStart + unclosed.start + 1, ErrorID.UNCLOSED_BRACE, CTX_CSS));
		}
	}

	/**
	 * Überprüft die syntaktische Korrektheit von CSS At-Rules. Stellt sicher, dass auf Schlüsselwörter wie 'media' ein '@' folgt.
	 */
	private static void lintCssAtRules(String proc, int bStart, List<TagError> errors) {
		// Finde jeden Textblock, dem eine '{' folgt (ein potenzieller Selektor)
		Pattern selectorProbe = Pattern.compile("([^;{}]+)(?=\\{)");
		Matcher m = selectorProbe.matcher(proc);

		while (m.find()) {
			String selector = m.group(1).trim().toLowerCase();
			if (selector.isEmpty())
				continue;

			String firstWord = AT_RULE_SPLIT_PATTERN.split(selector)[0];

			// Wenn das erste Wort eine At-Rule ist, aber kein '@' am Anfang steht...
			if (CSS_AT_RULES.contains(firstWord)) {
				// ... dann haben wir einen Fehler gefunden.
				int errorPos = proc.indexOf(firstWord, m.start(1));
				errors.add(new TagError(bStart + errorPos, bStart + errorPos + firstWord.length(), ErrorID.MALFORMED_TAG, AT_PREFIX + firstWord));
			}
		}
	}

	public static List<TagError> lintPureCss(String cssText) {
		List<TagError> errors = new ArrayList<>();
		if (cssText == null || cssText.isEmpty())
			return errors;

		// 1. Kommentare validieren (Parität prüfen)
		lintCssComments(cssText, 0, errors);

		// 2. HEILUNG: Kommentare für die restliche Prüfung MASKIEREN
		// Wir nutzen ein lokales StringBuilder-Objekt für die Analyse
		StringBuilder masked = new StringBuilder(cssText);
		Matcher cm = CSS_COMMENT_PATTERN.matcher(cssText); // Nutzt das /*...*/ Pattern
		while (cm.find()) {
			for (int i = cm.start(); i < cm.end(); i++) {
				char c = cssText.charAt(i);
				// Wir behalten nur Zeilenumbrüche, alles andere wird zu Space
				if (c != CHAR_LINE_BREAK && c != CHAR_RETURN)
					masked.setCharAt(i, CHAR_MASK);
			}
		}

		String cleanCss = masked.toString();

		// 3. Jetzt die restlichen Prüfungen auf dem sauberen Text
		lintCssAtRules(cleanCss, 0, errors);
		lintCssBraces(cleanCss, 0, errors);

		return errors;
	}

	private static void lintHtmlComments(String text, List<TagError> errors) {
		Matcher m = HTML_COMMENT_MARKER.matcher(text);
		int open = 0;
		int lastStart = -1;

		while (m.find()) {
			String match = m.group();

			if (match.equals(MARKER_MD_COMMENT_OPEN)) {
				if (open == 0)
					lastStart = m.start();
				open++;
			}
			// HEILUNG: Aktive Nutzung des schließenden Markers
			else if (match.equals(MARKER_MD_COMMENT_CLOSE)) {
				if (open == 0) {
					errors.add(new TagError(m.start(), m.end(), ErrorID.REDUNDANT_COMMENT_CLOSING, CTX_HTML));
				} else {
					open--;
				}
			}
		}

		if (open > 0) {
			// HEILUNG: Nutze die Länge der Konstante statt der magischen Zahl 4
			errors.add(new TagError(lastStart, lastStart + MARKER_MD_COMMENT_OPEN.length(), ErrorID.UNCLOSED_COMMENT, CTX_HTML));
		}
	}

	/**
	 * Überprüft die Parität von CSS-Kommentaren (/* ... * /). Arbeitet innerhalb von Style-Blöcken und nutzt bStart für korrekte Dokumenten-Offsets.
	 */
	private static void lintCssComments(String css, int bStart, List<TagError> errors) {
		if (css == null || css.isEmpty())
			return;

		Matcher m = CSS_COMMENT_MARKER.matcher(css);
		int openCount = 0;
		int lastStartPos = -1;

		while (m.find()) {
			String marker = m.group();

			// 1. Öffnender Marker (/*)
			if (marker.equals(MARKER_CSS_COMMENT_OPEN)) {
				if (openCount == 0) {
					lastStartPos = m.start();
				}
				openCount++;
			}
			// 2. Schließender Marker (*/) - AKTIV GENUTZT
			else if (marker.equals(MARKER_CSS_COMMENT_CLOSE)) {
				if (openCount == 0) {
					// Fehler: Schließender Marker ohne startendes Gegenstück
					errors.add(new TagError(bStart + m.start(), bStart + m.end(), ErrorID.REDUNDANT_COMMENT_CLOSING, CTX_CSS));
				} else {
					openCount--;
				}
			}
		}

		// 3. Abschlussprüfung: Ungeschlossener Kommentar am Ende des CSS-Blocks
		if (openCount > 0) {
			// Fehlerbreite wird dynamisch aus der Länge der Konstante berechnet
			int errorWidth = MARKER_CSS_COMMENT_OPEN.length();
			errors.add(new TagError(bStart + lastStartPos, bStart + lastStartPos + errorWidth, ErrorID.UNCLOSED_COMMENT, CTX_CSS));
		}
	}

	/**
	 * Validiert CSS-Eigenschaften innerhalb eines Blocks oder style-Attributs. Prüft auf fehlende Doppelpunkte, fehlende Semikolons und korrupte Anführungszeichen.
	 */
	private static void lintCssProperties(String blockContent, int blockOffset, String contextName, List<TagError> errors) {
		if (blockContent == null || blockContent.isEmpty())
			return;

		// A: DOPPELPUNKT-CHECK (Prüft die strukturelle Integrität jedes Statements)
		Matcher sm = CSS_STATEMENT_PATTERN.matcher(blockContent);
		while (sm.find()) {
			String segment = sm.group(1);
			String trimmed = segment.trim();

			// Überspringe Kommentare (falls unmaskiert) und Leerzeichen
			if (trimmed.isEmpty() || trimmed.startsWith(MARKER_COMMENT_ALT) || trimmed.startsWith(MARKER_STAR)) {
				continue;
			}

			// Falls kein Doppelpunkt vorhanden ist, liegt ein Syntaxfehler vor
			if (!trimmed.contains(String.valueOf(CHAR_COLON))) {
				String before = blockContent.substring(0, sm.start()).trim();
				// Wir melden den Fehler nur, wenn es ein neues Statement nach einem Semikolon ist
				if (before.isEmpty() || before.endsWith(String.valueOf(CHAR_SEMICOLON))) {
					int errP = blockOffset + sm.start() + segment.indexOf(trimmed);
					errors.add(new TagError(errP, errP + trimmed.length(), ErrorID.MISSING_COLON, contextName));
				}
			}
		}

		// B: SEMIKOLON & QUOTE CHECK (Prüft die Werte und deren Abschluss)
		Matcher pm = CSS_PROP_VALUE_PATTERN.matcher(blockContent);
		while (pm.find()) {
			int vStartDoc = blockOffset + pm.start(2);
			String val = pm.group(2);
			String lookAhead = blockContent.substring(pm.end()).trim();

			// Prüfe, ob das Statement sauber mit einem Semikolon abgeschlossen wurde
			if (!lookAhead.startsWith(String.valueOf(CHAR_SEMICOLON))) {
				// Wir markieren das Ende des Wertes als Fehlerposition
				errors.add(new TagError(blockOffset + pm.end() - 1, blockOffset + pm.end(), ErrorID.MISSING_SEMICOLON, contextName));
			}

			// Quote-Parität innerhalb des Wertes (Streaming-Check)
			Stack<Integer> quoteStack = new Stack<>();
			for (int j = 0; j < val.length(); j++) {
				char c = val.charAt(j);
				if (c == CHAR_QUOTE_DOUBLE) {
					if (quoteStack.isEmpty())
						quoteStack.push(j);
					else
						quoteStack.pop();
				}
				// Ein Komma innerhalb offener Quotes deutet oft auf eine kaputte Aufzählung hin
				else if (c == CHAR_COMMA && !quoteStack.isEmpty()) {
					int errP = vStartDoc + quoteStack.pop();
					errors.add(new TagError(errP, errP + 1, ErrorID.MALFORMED_ATTRIBUTE, CTX_CSS_VAL));
				}
			}

			// Alle übrig gebliebenen Quotes im Stack sind ungeschlossen
			for (int p : quoteStack) {
				errors.add(new TagError(vStartDoc + p, vStartDoc + p + 1, ErrorID.MALFORMED_ATTRIBUTE, CTX_CSS_VAL));
			}
		}
	}

	/**
	 * Extrahiert alle definierten Klassen-Namen aus einem CSS-String. Hochperformant durch O(n) Regex-Scan.
	 */
	public static java.util.Set<String> extractDefinedClasses(String css) {
		java.util.Set<String> classes = new java.util.HashSet<>();
		if (css == null)
			return classes;
		Matcher m = CSS_CLASS_DEF_PATTERN.matcher(css);
		while (m.find()) {
			classes.add(m.group(1));
		}
		return classes;
	}

	/**
	 * Extrahiert alle definierten Klassen-Namen aus einem CSS-String. Robust gegen Kommentare und gruppierte Selektoren (Kommas).
	 */
//	public static java.util.Set<String> extractDefinedClasses(String css) {
//	    java.util.Set<String> classes = new java.util.HashSet<>();
//	    if (css == null || css.isEmpty()) return classes;
//	    
//	    // 1. KOMMENTARE ENTFERNEN (Bereinigung)
//	    // Wir löschen alles zwischen /* und */, damit der Parser nicht stolpert.
//	    String cleanCss = css.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
//	    
//	    // 2. ROBUSTER SCAN
//	    // Das Pattern sucht nach einem Punkt, gefolgt vom Namen.
//	    // Der Lookahead (?=...) prüft, ob danach irgendwann eine öffnende Klammer kommt,
//	    // ohne dass dazwischen eine schließende Klammer oder ein Semikolon steht.
//	    // Das erlaubt Konstrukte wie: .klasse1, .klasse2 {
//	    
//	    // Einfachere Variante für Performance: Wir suchen .name gefolgt von Space/Komma/{
//	    Pattern p = Pattern.compile("\\.([a-zA-Z0-9_-]+)(?=[\\s,]*[\\{,])");
//	    
//	    Matcher m = p.matcher(cleanCss);
//	    while (m.find()) {
//	        classes.add(m.group(1));
//	    }
//	    return classes;
//	}

	/**
	 * Scannt das HTML nach Klassen-Attributen und prüft diese gegen die Inventur.
	 */
	public static void lintMissingClasses(String text, java.util.Set<String> definedClasses, List<TagError> errors) {
		if (text == null)
			return;
		Matcher m = HTML_CLASS_ATTR_PATTERN.matcher(text);
		while (m.find()) {
			String[] classesInAttr = m.group(1).split("\\s+");
			for (String className : classesInAttr) {
				if (!className.isEmpty() && !definedClasses.contains(className)) {
					// Wir markieren exakt den Klassen-Namen im Editor
					int start = m.start(1) + m.group(1).indexOf(className);
					errors.add(new TagError(start, start + className.length(), ErrorID.MISSING_CSS_CLASS, className));
				}
			}
		}
	}

	/**
	 * Hauptmethode für die HTML-Strukturanalyse. 1. Schützt vor ungeschlossenen Tags (Runaway-Schutz). 2. Validiert die Verschachtelungs-Hierarchie via Stack. 3. Initiiert das
	 * Attribut-Streaming für Inline-Validierung.
	 */
	private static void lintHtmlTagsAndHierarchy(String text, List<TagError> errors) {
		if (text == null || text.isEmpty())
			return;

		// --- 1. VORPRÜFUNG: UNGESCHLOSSENE / DEFEKTE TAGS (Runaway-Schutz) ---
		Matcher probeMatcher = TAG_START_PROBE.matcher(text);
		while (probeMatcher.find()) {
			String probeContent = text.substring(probeMatcher.start());

			// Ignoriere Markdown-Autolinks (Sonderfall)
			if (probeContent.startsWith(MARKER_HTTP) || probeContent.startsWith(MARKER_HTTPS)) {
				continue;
			}

			// Suche nach einem vollständigen Match an dieser Position
			Matcher fullMatcher = TAG_PATTERN.matcher(text);
			boolean foundValidTag = fullMatcher.find(probeMatcher.start()) && fullMatcher.start() == probeMatcher.start();

			// Validierung: Ein Tag darf kein zweites '<' enthalten (außer am Start)
			if (foundValidTag && fullMatcher.group(0).substring(1).indexOf(CHAR_TAG_OPEN) != -1) {
				foundValidTag = false;
			}

			if (!foundValidTag) {
				String rawName = probeMatcher.group(1);
				// Bereinige den Namen für die Fehlermeldung (entferne '/')
				String cleanName = rawName.startsWith(String.valueOf(CHAR_SLASH)) ? rawName.substring(1) : rawName;

				errors.add(new TagError(probeMatcher.start(), probeMatcher.end(), ErrorID.MALFORMED_TAG, cleanName));
			}
		}

		// --- 2. HIERARCHIE- & ATTRIBUT-LOGIK (Die logische Maschine) ---
		Stack<TagAnchor> stack = new Stack<>();
		Matcher m = TAG_PATTERN.matcher(text);

		while (m.find()) {
			String fullTag = m.group(0);

			// Sicherheits-Check: Falls der Matcher durch einen Fehler übergesprungen ist
			if (fullTag.substring(1).indexOf(CHAR_TAG_OPEN) != -1)
				continue;

			String tag = m.group(1).toLowerCase();
			String attrs = m.group(2); // Attribut-Block aus RegEx-Gruppe 2

			// A: HIERARCHIE-MANAGEMENT
			// Ignoriere Kommentare, self-closing tags (/>) und inhaltsleere Tags (area, br, etc.)
			if (!(tag.startsWith(MARKER_HTML_COMMENT) || fullTag.endsWith(MARKER_TAG_SELF_CLOSE) || isSelfClosing(tag))) {

				// HEILUNG: Unnötige Berechnungen entfernt
				// int lineStart = text.lastIndexOf(CHAR_LINE_BREAK, m.start()) + 1;
				// boolean isSolitary = text.substring(lineStart, m.start()).trim().isEmpty();
				// int indent = m.start() - lineStart;

				if (tag.startsWith(String.valueOf(CHAR_SLASH))) {
					// Schließendes Tag: Validierung gegen den Stack (ohne Indent/Solitary)
					lintClosingTag(tag.substring(1).trim(), m.start(), m.end(), stack, errors);
				} else {
					// Öffnendes Tag: Auf den Stack legen (mit schlankerem Konstruktor)
					stack.push(new TagAnchor(tag, m.start(), m.end()));
				}
			}

			// B: ATTRIBUT-STREAMING (Validierung von Quotes und Inline-CSS)
			if (attrs != null && !attrs.trim().isEmpty()) {
				// Wir übergeben den exakten Offset des Attribut-Blocks im Gesamtdoum den
				int attrStartInDoc = m.start(2);
				lintHtmlAttributesStreaming(tag, attrs, attrStartInDoc, errors);
			}
		}

		// --- 3. ABSCHLUSSPRÜFUNG: Offene Fragmente am Dokumentende ---
		while (!stack.isEmpty()) {
			TagAnchor u = stack.pop();
			errors.add(new TagError(u.start, u.end, ErrorID.UNCLOSED_OPENING, u.name));
		}
	}

	private static void lintClosingTag(String name, int s, int e, Stack<TagAnchor> stack, List<TagError> errors) {
		int mIdx = -1;
		for (int i = stack.size() - 1; i >= 0; i--) {
			TagAnchor a = stack.get(i);
			if (a.name.equals(name)) {
				mIdx = i;
				break;
			}
		}
		if (mIdx != -1) {
			while (stack.size() > mIdx + 1) {
				TagAnchor f = stack.pop();
				errors.add(new TagError(f.start, f.end, ErrorID.UNCLOSED_OPENING, f.name));
			}
			stack.pop();
		} else {
			errors.add(new TagError(s, e, ErrorID.REDUNDANT_CLOSING, name));
		}
	}

	/**
	 * Hybride Streaming-Analyse (v7.7.3 Master - STABLE). Validiert Quote-Parität, Inline-CSS und erlaubt Gleichheitszeichen innerhalb von Werten (z.B. in URLs).
	 */
	private static void lintHtmlAttributesStreaming(String tag, String attrs, int aStart, List<TagError> errors) {
		if (attrs == null || attrs.isEmpty())
			return;

		Stack<Integer> quoteStack = new Stack<>();
		int styleValueStart = -1;
		int styleValueEnd = -1;

		for (int j = 0; j < attrs.length(); j++) {
			char c = attrs.charAt(j);

			if (c == CHAR_QUOTE_DOUBLE) {
				if (quoteStack.isEmpty()) {
					// ÖFFNENDES QUOTE -> JETZT PRÜFEN WIR DEN NACHBARN
					quoteStack.push(j);

					// Wir suchen rückwärts nach dem ersten Nicht-Leerzeichen
					int prevCharIndex = j - 1;
					while (prevCharIndex >= 0 && Character.isWhitespace(attrs.charAt(prevCharIndex))) {
						prevCharIndex--;
					}

					// FEHLER-CHECK: Wenn vor dem Quote kein '=' steht -> Alarm!
					// Dies bleibt aktiv, da es echte HTML-Fehler findet.
					if (prevCharIndex < 0 || attrs.charAt(prevCharIndex) != CHAR_EQUALS) {
						errors.add(new TagError(aStart + j, aStart + j + 1, ErrorID.MISSING_EQUALS, tag));
					}

					// --- Bestehende Style-Erkennung bleibt ---
					int styleMarkerLen = MARKER_STYLE_ATTR_EQ.length();
					if (j >= styleMarkerLen && attrs.substring(j - styleMarkerLen, j).equalsIgnoreCase(MARKER_STYLE_ATTR_EQ)) {
						styleValueStart = j + 1;
					}
				} else {
					// SCHLIESSENDES QUOTE
					int startIdx = quoteStack.pop();
					if (startIdx == styleValueStart - 1) {
						styleValueEnd = j;
					}
				}
			}
			// DER FEHLERHAFTE BLOCK WURDE HIER SOUVERÄN ENTFERNT.
			// Gleichheitszeichen innerhalb von Quotes (z.B. in URLs) werden nun ignoriert.
		}

		// --- Prüfung auf offene Quotes am Ende bleibt ---
		for (int p : quoteStack) {
			errors.add(new TagError(aStart + p, aStart + p + 1, ErrorID.MALFORMED_ATTRIBUTE, tag));
		}

		// --- Bestehende CSS-Prüfung bleibt ---
		if (styleValueStart != -1 && styleValueEnd != -1) {
			String styleContent = attrs.substring(styleValueStart, styleValueEnd);
			lintCssProperties(styleContent, aStart + styleValueStart, tag, errors);
		}
	}

	private static boolean isSelfClosing(String tag) {
		return HTML_SELF_CLOSING_TAGS.contains(tag);
	}

	// NEU: Die Ausschlussliste als private, statische und finale Konstante.
	// Sie wird nur einmal beim Laden der Klasse erstellt und ist optimal für die Performance.
	private static final Set<String> IGNORED_TAGS_FOR_BLANK_LINE_LINT = Set.of("pre", "code", "style", "script"
	// Hier können bei Bedarf weitere Tags hinzugefügt werden.
	);

	private static void lintMissingBlankLineAfterHtml(String text, List<TagError> errors) {
		if (text == null || text.isEmpty())
			return;

		Matcher lineMatcher = LINE_ITERATOR_PATTERN.matcher(text);
		Matcher tagNameMatcher = HTML_TAG_NAME_EXTRACTOR.matcher("");
		String lastLineFull = null;

		while (lineMatcher.find()) {
			String currentLineFull = lineMatcher.group();
			int currentLineStartOffset = lineMatcher.start();

			if (lastLineFull != null && !currentLineFull.isBlank() && !lastLineFull.isBlank()) {
				String currentTrimmed = currentLineFull.trim();
				String lastTrimmed = lastLineFull.trim();

				if (!currentTrimmed.isEmpty() && !lastTrimmed.isEmpty()) {
					char firstChar = currentTrimmed.charAt(0);

					// 1. Prüfe, ob die aktuelle Zeile Markdown ist
					boolean isMarkdown = MARKDOWN_START_CHARS.indexOf(firstChar) != -1 || Character.isDigit(firstChar);

					if (isMarkdown) {
						// 2. HEILUNG: Nutze char-Konstanten für die Block-Erkennung
						// Wir prüfen das erste und letzte Zeichen des getrimmten vorherigen Strings
						if (lastTrimmed.charAt(0) == CHAR_TAG_OPEN && lastTrimmed.charAt(lastTrimmed.length() - 1) == CHAR_TAG_CLOSE) {

							tagNameMatcher.reset(lastTrimmed);
							// Nutze CTX_HTML als Fallback, wie von dir vorgeschlagen
							String htmlTagName = tagNameMatcher.find() ? tagNameMatcher.group(1).toLowerCase() : CTX_HTML;

							if (!IGNORED_TAGS_FOR_BLANK_LINE_LINT.contains(htmlTagName)) {
								errors.add(
								        new TagError(currentLineStartOffset, currentLineStartOffset + currentTrimmed.length(), ErrorID.MISSING_BLANK_LINE_AFTER_HTML, htmlTagName));
							}
						}
					}
				}
			}
			lastLineFull = currentLineFull;
		}
	}

}