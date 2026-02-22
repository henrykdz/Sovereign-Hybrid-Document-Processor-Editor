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
import java.util.List;
//import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFormatter {

	private static final int INDENT_SIZE = 2;

	private static final List<String> CONTAINER_TAGS = Arrays.asList("div", "table", "thead", "tbody", "ul", "ol", "section", "header", "tr", "blockquote");

//	private static final Set<String> SELF_CLOSING_TAGS = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr");

//	private static final Pattern FULL_TAG_EXTRACTOR = Pattern.compile("<[^>]+>");
	private static final Pattern TAG_NAME_PATTERN   = Pattern.compile("^<(/?[a-zA-Z0-9]+)");
	private static final Pattern STYLE_ATTR_PATTERN = Pattern.compile("style=\"([^\"]*)\"");
	private static final Pattern CSS_REPAIR         = Pattern.compile("\\s*([:;])\\s*");

	private static final Pattern CSS_COLON_REPAIR     = Pattern.compile("\\s*:\\s*");
	private static final Pattern CSS_SEMICOLON_REPAIR = Pattern.compile("\\s*;\\s*");
	private static final Pattern CSS_IMPORTANT_REPAIR = Pattern.compile("\\s*!\\s*");

	public String format(String text) {
		if (text == null || text.isEmpty())
			return text;

		String[] lines = text.split("\\n");
		StringBuilder formatted = new StringBuilder();

		int htmlDepth = 0;
//		boolean inYaml = false; // Bleibt zur Sicherheit deaktiviert
		boolean inStyleBlock = false;
		List<String> tableBuffer = new ArrayList<>();

		for (String line : lines) {
			String trimmed = line.trim();

			// 1. Tabellen-Erkennung
			if (trimmed.startsWith("|") && trimmed.contains("|")) {
				tableBuffer.add(trimmed);
				continue;
			} else if (!tableBuffer.isEmpty()) {
				formatted.append(processTable(tableBuffer));
				tableBuffer.clear();
			}

			// 2. Leerzeilen-Erhalt
			if (trimmed.isEmpty()) {
				formatted.append("\n");
				continue;
			}

			/*
			 * YAML-Sektion (Auf Wunsch auskommentiert) if (trimmed.startsWith("---")) { inYaml = !inYaml; formatted.append(trimmed).append("\n"); continue; } if (inYaml) {
			 * formatted.append(line).append("\n"); continue; }
			 */

			// 3. HTML & Style-Block Logik
			String tagName = getTagName(trimmed);
			boolean isClosing = tagName != null && tagName.startsWith("/");
			String pureName = isClosing ? tagName.substring(1).toLowerCase() : (tagName != null ? tagName.toLowerCase() : null);

			// Tiefen-Korrektur vor dem Druck (bei schließenden Tags)
			if (isClosing && CONTAINER_TAGS.contains(pureName)) {
				htmlDepth = Math.max(0, htmlDepth - 1);
			}

			if (pureName != null && pureName.equals("style") && isClosing) {
				inStyleBlock = false;
			}

			// Einrückung bestimmen
			int currentIndent = (trimmed.startsWith("<")) ? htmlDepth : 0;

			// Verarbeitung
			String processed;
			if (inStyleBlock && !trimmed.startsWith("<")) {
				processed = repairCssLine(line); // CSS-Regeln reparieren
			} else if (trimmed.startsWith("<")) {
				processed = repairHtmlTag(trimmed); // HTML-Tags säubern
			} else {
				processed = trimmed;
			}

			formatted.append(" ".repeat(currentIndent * INDENT_SIZE)).append(processed).append("\n");

			// Tiefen-Korrektur nach dem Druck (bei öffnenden Tags)
			if (!isClosing && pureName != null) {
				if (pureName.equals("style"))
					inStyleBlock = true;
				if (CONTAINER_TAGS.contains(pureName) && !trimmed.contains("</") && !trimmed.endsWith("/>")) {
					htmlDepth++;
				}
			}
		}

		// Finaler Flush für Tabellen am Ende
		if (!tableBuffer.isEmpty()) {
			formatted.append(processTable(tableBuffer));
		}

		return formatted.toString().stripTrailing();
	}

	private String processTable(List<String> rows) {
		if (rows.isEmpty())
			return "";

		int maxCols = 0;
		for (String row : rows)
			maxCols = Math.max(maxCols, row.split("\\|").length);

		int[] widths = new int[maxCols];
		String[] alignments = new String[maxCols]; // "left", "center", "right"
		Arrays.fill(alignments, "left"); // Standardmäßig linksbündig

		// 1. Analyse-Phase
		for (String row : rows) {
			String[] cells = row.split("\\|");

			// HEILUNG: Prüfung auf Bindestriche hinzugefügt
			if (row.matches("^[\\s|:\\-]+$") && row.contains("-")) {
				for (int i = 0; i < cells.length && i < maxCols; i++) {
					String c = cells[i].trim();
					if (c.startsWith(":") && c.endsWith(":"))
						alignments[i] = "center";
					else if (c.endsWith(":"))
						alignments[i] = "right";
					else if (c.startsWith(":"))
						alignments[i] = "left";
				}
				continue;
			}

			for (int i = 0; i < cells.length; i++) {
				widths[i] = Math.max(widths[i], cells[i].trim().length());
			}
		}

		// 2. Bau-Phase
		StringBuilder sb = new StringBuilder();
		for (String row : rows) {
			String[] cells = row.split("\\|");
//			boolean isSep = row.matches("^[\\s|:\\-]+$");
			// Ein Separator MUSS Bindestriche enthalten, um nicht mit leeren Zeilen verwechselt zu werden.
			boolean isSep = row.matches("^[\\s|:\\-]+$") && row.contains("-");
			sb.append("|");

			for (int i = 1; i < maxCols; i++) {
				int w = widths[i];
				String align = alignments[i];

				if (isSep) {
					// Trenner mit Ausrichtungs-Logik bauen
					if ("center".equals(align))
						sb.append(":").append("-".repeat(w)).append(":|");
					else if ("right".equals(align))
						sb.append("-").append("-".repeat(w)).append(":|");
					else
						sb.append(":").append("-".repeat(w)).append("-|"); // left/default
				} else {
					String content = (i < cells.length) ? cells[i].trim() : "";

					// Textausrichtung anwenden
					if ("center".equals(align)) {
						sb.append(" ").append(centerText(content, w)).append(" |");
					} else if ("right".equals(align)) {
						sb.append(" ").append(String.format("%" + w + "s", content)).append(" |");
					} else {
						sb.append(" ").append(String.format("%-" + w + "s", content)).append(" |");
					}
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	// Hilfsmethode für Zentrierung
	private String centerText(String text, int width) {
		int pad = width - text.length();
		int left = pad / 2;
		int right = pad - left;
		return " ".repeat(left) + text + " ".repeat(right);
	}

	private String getTagName(String line) {
		Matcher m = TAG_NAME_PATTERN.matcher(line);
		return m.find() ? m.group(1) : null;
	}

	private String repairHtmlTag(String tag) {
		Matcher m = STYLE_ATTR_PATTERN.matcher(tag);
		if (m.find()) {
			String styleContent = m.group(1);
			String cleanStyle = CSS_COLON_REPAIR.matcher(styleContent).replaceAll(": ");
			cleanStyle = CSS_SEMICOLON_REPAIR.matcher(cleanStyle).replaceAll("; ");
			cleanStyle = CSS_IMPORTANT_REPAIR.matcher(cleanStyle).replaceAll(" !").trim();
			if (cleanStyle.endsWith("; "))
				cleanStyle = cleanStyle.substring(0, cleanStyle.length() - 1);
			tag = tag.replace(m.group(1), cleanStyle);
		}
		return tag.replaceAll(" ?< ?", "<").replaceAll(" ?/ ?", "/").replaceAll(" ?= ?", "=").replaceAll(" ?>", ">").replaceAll(" />", "/>");
	}

	private String repairCssLine(String line) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.contains("{") || trimmed.contains("}"))
			return trimmed;
		return CSS_REPAIR.matcher(trimmed).replaceAll("$1 ");
	}

	/**
	 * Formats raw CSS code with proper hierarchical indentation and property cleaning.
	 */
	public String formatPureCss(String css) {
		if (css == null || css.isEmpty())
			return css;

		String[] lines = css.split("\\n");
		StringBuilder sb = new StringBuilder();
		int depth = 0;

		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				sb.append("\n");
				continue;
			}

			// Closing brace reduces indent BEFORE processing the line
			if (trimmed.contains("}"))
				depth = Math.max(0, depth - 1);

			// Apply indentation (4 spaces for CSS clarity)
			sb.append("    ".repeat(depth)).append(repairCssLine(trimmed)).append("\n");

			// Opening brace increases indent AFTER processing the line
			if (trimmed.contains("{"))
				depth++;
		}
		return sb.toString().trim();
	}


}
