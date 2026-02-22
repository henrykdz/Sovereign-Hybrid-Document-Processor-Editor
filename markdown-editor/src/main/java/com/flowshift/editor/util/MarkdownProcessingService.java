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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flowshift.editor.DocumentFormat;
import com.flowshift.editor.model.DocumentSettings;
import com.flowshift.editor.model.Placeholder;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import utils.logging.Log;

/** TODO Dies ist noch ungenutzte, vorbereitete Klasse um den Controller zu refactorisieren, da es mir jedoch nicht gut geht, verschiebe ich das auf einen anderen Zeitpunkt!
 * 
 * Ein spezialisierter Service zur Verarbeitung von Markdown.
 * Kapselt die gesamte Logik für das Parsen, Rendern und Vorbereiten von HTML-Output
 * für die Vorschau und den Export.
 * Dies entkoppelt den Controller von den Implementierungsdetails des Markdown-Engines.
 */
public class MarkdownProcessingService {

    private Parser markdownParser;
    private HtmlRenderer htmlRenderer;
    private CssThemeManager themeManager; // Brauchen wir hier, um CSS-Themen zu injizieren

    // Performance-Konstanten für Regex-Muster
    private static final Pattern STYLE_TAG_PATTERN = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);
//    private static final Pattern LEGACY_CSS_MARKER_PATTERN = Pattern.compile("/\\* --- (Start Extracted|End of) Block #\\d+ --- \\*/\\n?");
    private static final Pattern YAML_BLOCK_EXTRACT_PATTERN = Pattern.compile("\\A---\\s*\\n([\\s\\S]*?)\\n---");
    private static final Pattern YAML_FIELD_EXTRACT_PATTERN = Pattern.compile("^\\s*([\\w\\-]+):\\s*(.*)");

    /**
     * Konstruktor für den MarkdownProcessingService.
     * @param themeManager Die Instanz des CssThemeManagers, die zur Verfügung gestellt wird.
     */
    public MarkdownProcessingService(CssThemeManager themeManager) {
        this.themeManager = themeManager;
        setupFlexmark();
    }

    /**
     * Konfiguriert und initialisiert den Flexmark-Parser und HTML-Renderer.
     * Dies ist die Kern-Engine, die für die Umwandlung von Markdown in HTML zuständig ist.
     * Diese Methode wurde aus dem MarkdownEditorController verschoben.
     */
    private void setupFlexmark() {
        MutableDataSet options = new MutableDataSet();

        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                YamlFrontMatterExtension.create(),
                AttributesExtension.create()
        ));

        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(HtmlRenderer.RENDER_HEADER_ID, true);
        options.set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true);
        options.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true);
        options.set(HtmlRenderer.HEADER_ID_GENERATOR_NON_ASCII_TO_LOWERCASE, true);

        options.set(Parser.INDENTED_CODE_BLOCK_PARSER, false);
        // ACHTUNG: Source Position Attribute wird hier NICHT gesetzt, da es nur für die Live-Vorschau relevant ist
        // Es wird später bei Bedarf durch die aufrufende Methode (z.B. SourceMapper) hinzugefügt.
//		options.set(HtmlRenderer.SOURCE_POSITION_ATTRIBUTE, SovereignSourceMapper.DATA_LINE_ATTRIBUTE);
//		options.set(HtmlRenderer.SOURCE_POSITION_PARAGRAPH_LINES, true);

        markdownParser = Parser.builder(options).build();
        htmlRenderer = HtmlRenderer.builder(options).build();
        Log.fine("Flexmark parser and HTML renderer initialized in MarkdownProcessingService.");
    }

    /**
     * Parsen des Markdown-Textes in ein Flexmark-Dokument.
     * @param markdownText Der zu parsende Markdown-Text.
     * @return Ein Flexmark Node-Objekt, das die AST des Dokuments repräsentiert.
     */
    public Node parseMarkdown(String markdownText) {
        if (markdownText == null || markdownText.isEmpty()) {
            return markdownParser.parse(""); // Leeres Dokument parsen
        }
        return markdownParser.parse(markdownText);
    }

    /**
     * Rendert ein Flexmark-Dokument in HTML.
     * @param document Das zu rendernde Flexmark Node-Objekt.
     * @return Der generierte HTML-String.
     */
    public String renderHtml(Node document) {
        return htmlRenderer.render(document);
    }

    /**
     * Rendert einen Markdown-String direkt in HTML.
     * @param markdownText Der zu rendernde Markdown-Text.
     * @return Der generierte HTML-String.
     */
    public String renderHtml(String markdownText) {
        return renderHtml(parseMarkdown(markdownText));
    }
    
    // --- HILFSMETHODEN, DIE SPÄTER HIERHER VERSCHOBEN WERDEN ---
    // Diese Methoden werden vorerst leer oder als Dummies hinzugefügt,
    // um die Struktur vorzubereiten und Compiler-Fehler zu vermeiden,
    // wenn wir sie aus dem Controller entfernen.

    /**
     * Extrahiert CSS aus <style> tags und entfernt sie aus dem Markdown.
     * @param markdown Der Markdown-Text.
     * @return Ein String-Array: [0] Markdown ohne <style> tags, [1] Extrahiertes CSS.
     */
    public String[] extractAndRemoveCss(String markdown) {
        StringBuilder cssBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder(markdown);

        Matcher matcher = STYLE_TAG_PATTERN.matcher(markdown);

        int offset = 0;
        while (matcher.find()) {
            cssBuilder.append(matcher.group(1)).append("\n");
            int start = matcher.start() - offset;
            int end = matcher.end() - offset;
            contentBuilder.delete(start, end);
            offset += (end - start);
        }
        return new String[] { contentBuilder.toString(), cssBuilder.toString() };
    }

    /**
     * Bereinigt Markdown-Templates von unerwünschten Leerzeilen und Einrückungen.
     * @param markdown Der unbereinigte Markdown-Text.
     * @return Der bereinigte Markdown-Text.
     */
    public String preprocessTemplate(String markdown) {
        if (!isHtmlTemplate(markdown)) {
            return markdown;
        }
        String fixed = markdown;
        fixed = fixed.replaceAll("(?m)^[ \\t]*\\n[ \\t]*(</?[a-zA-Z])", "$1");
        fixed = fixed.replaceAll("(?m)^([ \\t]+)(<[a-zA-Z])", "$2");
        fixed = fixed.replaceAll("\\n{3,}", "\n\n");
        return fixed;
    }

    /**
     * Prüft, ob der Markdown-Text Anzeichen eines HTML-Templates enthält.
     * @param markdown Der zu prüfende Markdown-Text.
     * @return True, wenn es ein HTML-Template ist, sonst false.
     */
    private boolean isHtmlTemplate(String markdown) {
        int htmlTagCount = 0;
        String[] tags = { "<div", "<span", "<table", "<style", "<h1", "<h2", "<h3" };

        for (String tag : tags) {
            int count = 0;
            int index = 0;
            while ((index = markdown.indexOf(tag, index)) != -1) {
                count++;
                index += tag.length();
            }
            htmlTagCount += count;
        }
        return htmlTagCount >= 5;
    }

    /**
     * Extrahiert Metadaten aus dem YAML-Frontmatter-Block.
     * @param markdownText Der Markdown-Text.
     * @return Eine Map mit den extrahierten Metadaten.
     */
    public Map<String, String> readYamlMetadata(String markdownText) {
        Map<String, String> metadata = new HashMap<>();
        Matcher blockMatcher = YAML_BLOCK_EXTRACT_PATTERN.matcher(markdownText);

        if (blockMatcher.find()) {
            String yamlContent = blockMatcher.group(1);
            for (String line : yamlContent.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.startsWith("<!--") || trimmed.isEmpty()) {
                    continue;
                }
                Matcher fieldMatcher = YAML_FIELD_EXTRACT_PATTERN.matcher(line);
                if (fieldMatcher.find()) {
                    String key = fieldMatcher.group(1).trim();
                    String value = fieldMatcher.group(2).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    metadata.put(key, value);
                }
            }
        }
        return metadata;
    }

    /**
     * Sammelt alle verfügbaren Platzhalter (System-Defaults, Einstellungen, Dokument-Metadaten).
     * @param settings Die aktuellen Dokument-Einstellungen.
     * @param docMetadata Die Metadaten aus dem YAML-Frontmatter.
     * @param editorContent Der aktuelle Inhalt des Editors (für Wortzählungen).
     * @return Eine Map mit allen Platzhaltern und ihren Werten.
     */
    public Map<String, String> collectPlaceholders(DocumentSettings settings, Map<String, String> docMetadata, String editorContent) {
        Map<String, String> placeholders = new HashMap<>();

        placeholders.put(Placeholder.DATE.getKey(), new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
        placeholders.put(Placeholder.TIME.getKey(), new SimpleDateFormat("HH:mm").format(new Date()));

        long words = Arrays.stream(editorContent.split("\\s+")).filter(w -> !w.isEmpty()).count();
        placeholders.put(Placeholder.WORD_COUNT.getKey(), String.valueOf(words));
        placeholders.put(Placeholder.READING_TIME.getKey(), Math.max(1, Math.round(words / 200.0)) + " min");
        placeholders.put(Placeholder.RANDOM_ID.getKey(), "ID-" + (System.currentTimeMillis() % 10000));

        placeholders.putAll(settings.asMetaMap());
        if (docMetadata != null) {
            placeholders.putAll(docMetadata);
        }
        return placeholders;
    }

    /**
     * Wendet Platzhalter auf einen HTML- oder Markdown-String an.
     * @param content Der Inhalt, in den Platzhalter eingesetzt werden sollen.
     * @param placeholders Die Map der Platzhalter.
     * @return Der Inhalt mit ersetzten Platzhaltern.
     */
    public String applyPlaceholders(String content, Map<String, String> placeholders) {
        if (content == null || placeholders.isEmpty())
            return content;
        String result = content;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) continue;
            result = result.replace("{{" + key + "}}", value);
        }
        return result;
    }

    /**
     * Escapes alle nicht-ASCII-Zeichen (inkl. Emojis) in HTML.
     * @param input Der zu escapende String.
     * @return Der escapte String.
     */
    public String escapeUnicodeForHtml(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            int codePoint = input.codePointAt(i);
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }
            if (codePoint == 0x200D || codePoint == 0xFE0F) continue; // Remove ZWJ and VS16
            if (codePoint < 128) {
                sb.append((char) codePoint);
            } else {
                sb.append("&#x").append(Integer.toHexString(codePoint)).append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Baut das vollständige HTML-Dokument zusammen, inklusive Header, Body, CSS und Skripten.
     * @param css Der CSS-Inhalt.
     * @param body Der HTML-Body-Inhalt.
     * @param scripts JavaScript-Inhalte.
     * @param ds Die Dokument-Einstellungen.
     * @param meta Die Dokument-Metadaten.
     * @param isExport True, wenn das HTML für den Export generiert wird, sonst false.
     * @return Das vollständige HTML-Dokument als String.
     */
    public String buildCompleteHtml(String css, String body, String scripts, DocumentSettings ds, Map<String, String> meta, boolean isExport, boolean isDarkMode) {
        String header = generateHeaderSection(ds, meta);
        String payload = generateContentPayload(ds, header, body);

        if (!isExport) {
            payload = generateTopDocumentInfoHint(ds.getFormat()) + payload + generateBottomUsageHint();
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>__TITLE__</title>
                    <style id="layout-style">__LAYOUT__</style>
                    __BASE_CSS__
                    <style id="user-shadow-css">__USER_CSS__</style>
                </head>
                <body class="__CLASS__">
                    <div id="sovereign-payload">__PAYLOAD__</div>
                    <div id="script-block" class="no-print">__SCRIPTS__</div>
                </body>
                </html>""".replace("__TITLE__", ds.getCompanyName())
                .replace("__LAYOUT__", themeManager.getPageLayoutCss(ds))
                .replace("__BASE_CSS__", themeManager.getBaseAestheticStyleBlock())
                .replace("__USER_CSS__", (css != null ? css : ""))
                .replace("__CLASS__", (isDarkMode ? "dark-mode" : "light-mode") + (isExport ? "" : " workspace-mode"))
                .replace("__PAYLOAD__", payload)
                .replace("__SCRIPTS__", (scripts != null ? scripts : ""));
    }

    /**
     * Generiert den Header-Abschnitt des HTML-Dokuments.
     * @param settings Die Dokument-Einstellungen.
     * @param docMetadata Die Dokument-Metadaten.
     * @return Der HTML-String für den Header-Abschnitt.
     */
    public String generateHeaderSection(DocumentSettings settings, Map<String, String> docMetadata) {
        if ("NONE".equalsIgnoreCase(settings.getActiveHeaderStyle())) {
            return "";
        }
        String rawTemplate = settings.getHeaderHtml();
        if (rawTemplate == null || rawTemplate.trim().isEmpty()) {
            return "";
        }
        Map<String, String> placeholders = collectPlaceholders(settings, docMetadata, ""); // Editor Content is missing here, will be fixed later
        return "<header class='document-header'>" + applyPlaceholders(rawTemplate, placeholders).trim() + "</header>";
    }

    /**
     * Generiert den Inhalts-Payload des HTML-Dokuments.
     * @param settings Die Dokument-Einstellungen.
     * @param headerSection Der HTML-Header-Abschnitt.
     * @param bodyContent Der HTML-Body-Inhalt.
     * @return Der HTML-String für den Inhalts-Payload.
     */
    public String generateContentPayload(DocumentSettings settings, String headerSection, String bodyContent) {
        String cleanBody = bodyContent.trim();
        if (settings.isPaginated()) {
            return "<main class='document-content'>" + headerSection + cleanBody + "</main>";
        } else {
            return "<div class='document-page'>" + headerSection + "<main class='document-content'>" + cleanBody + "</main></div>";
        }
    }

    /**
     * Generiert den oberen Hinweis für den Editor-Modus.
     * @param format Das Dokumentformat.
     * @return Der HTML-String für den oberen Hinweis.
     */
    public String generateTopDocumentInfoHint(DocumentFormat format) {
        return String.format("""
                <div id="workspace-hint" class="workspace-info no-print">
                <strong>Editor Mode:</strong> Rendering %s (%s). Use 'Print' for PDF generation.
                </div>""", format.toString(), format.width).strip();
    }

    /**
     * Generiert den unteren Nutzungshinweis für den Editor-Modus.
     * @return Der HTML-String für den unteren Nutzungshinweis.
     */
    public String generateBottomUsageHint() {
        return """
                <div id="usage-tips" class="workspace-tips no-print">
                <h4> Document Intelligence</h4>
                <ul>
                    <li><strong>Dynamic Content:</strong> Use <code>{{documentTitle}}</code> or <code>{{date}}</code>.</li>
                    <li><strong>ID Generation:</strong> <code>{{randomId}}</code> creates a unique number.</li>
                    <li><strong>Auto-Fix:</strong> Press <code>Ctrl+Shift+F</code> to heal structure.</li>
                    <li><strong>Branding:</strong> Select a template in the Settings Nexus.</li>
                </ul>
                </div>""".strip();
    }

    /**
     * Analysiert den HTML-Stream und verpackt nur Text in <p>,
     * der sich auf der obersten Ebene befindet und nicht bereits von Block-Elementen umschlossen ist.
     */
    public String wrapTopLevelOrphanText(String html) {
        if (html == null || html.isEmpty())
            return html;

        StringBuilder result = new StringBuilder();
        Pattern tagPattern = Pattern.compile("<(/?[a-zA-Z0-9]+).*?>");
        Matcher matcher = tagPattern.matcher(html);

        int lastEnd = 0;
        int depth = 0;

        while (matcher.find()) {
            String textBetween = html.substring(lastEnd, matcher.start());
            if (depth == 0 && !textBetween.trim().isEmpty()) {
                result.append("<p>").append(textBetween.trim()).append("</p>");
            } else {
                result.append(textBetween);
            }

            String fullTag = matcher.group(0);
            String tagName = matcher.group(1).toLowerCase();

            if (fullTag.startsWith("</")) {
                depth = Math.max(0, depth - 1);
            } else if (!fullTag.endsWith("/>") && !isSelfClosingTag(tagName)) {
                depth++;
            }

            result.append(fullTag);
            lastEnd = matcher.end();
        }

        String tail = html.substring(lastEnd);
        if (depth == 0 && !tail.trim().isEmpty()) {
            result.append("<p>").append(tail).append("</p>");
        } else {
            result.append(tail);
        }

        return result.toString();
    }

    /**
     * Hilfsmethode für die Stack-Integrität.
     */
    private boolean isSelfClosingTag(String tagName) {
        return List.of("br", "hr", "img", "input", "link", "meta", "base", "area").contains(tagName);
    }

}
