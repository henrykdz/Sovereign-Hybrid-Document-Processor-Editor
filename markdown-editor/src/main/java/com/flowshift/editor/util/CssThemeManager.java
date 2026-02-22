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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.flowshift.editor.DocumentFormat;
import com.flowshift.editor.model.DocumentSettings;

import utils.logging.Log;

/**
 * CssThemeManager v7.7.1 - Professional Grade Verwaltet die visuelle Identität der JanusApp (Editor, Preview, Print).
 */
public class CssThemeManager {
	// --- 1. PRESET NAMEN (Für die UI) ---
	public static final String NAME_MODERN         = "Modern Clean";
	public static final String NAME_PAPER_STANDARD = "Paper Standard";

	// --- 2. STATISCHE RESSOURCEN ---
	private static final String SVG_BULB = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'><path fill='%23FFD600' d='M12,2A7,7 0 0,0 5,9C5,11.38 6.19,13.47 8,14.74V17A1,1 0 0,0 9,18H15A1,1 0 0,0 16,17V14.74C17.81,13.47 19,11.38 19,9A7,7 0 0,0 12,2Z'/><path fill='%23FFA000' d='M9,21A1,1 0 0,0 10,22H14A1,1 0 0,0 15,21V20H9V21Z'/><path fill='%23546E7A' d='M9,19H15V20H9V19M9,17H15V18H9V17Z'/></svg>";

	/**
	 * The Sovereign CSS Blueprint for the "Paper Standard" theme. Version: 10.4 - Structural Symmetry & Block Integrity.
	 */
	private static final String BLUEPRINT_PAPER_STANDARD = """
	        /* --- 1. SOVEREIGN LAW: Universal Box Model --- */
	        * { box-sizing: border-box; }

	        /* --- 2. ARCHITECTURAL RESET --- */
	        /* HEALING: We removed 'ul' and 'ol' from the zero-padding rule
	           to allow room for markers. */
	        h1, h2, h3, h4, h5, h6, p, li, blockquote, table, pre {
	            margin: 0;
	            padding: 0;
	            border: none;
	        }

	        /* --- 3. LIST GEOMETRY --- */
	        /* Explicitly define the indentation for lists to ensure that
	           bullets and numbers remain within the document's margins. */
	        ul, ol {
	            margin: 0;
	            padding-left: 24px; /* Sufficient space for bullets/double-digit numbers */
	            margin-bottom: 0.8em;
	        }

	        li {
	            margin-bottom: 0.2em; /* Subtle spacing between list items */
	            display: list-item;
	        }

	        /* Prevents top-margin collapse for the first element, ensuring alignment with guides. */
	        .document-content > :first-child {
	            margin-top: 0 !important;
	        }

	        /* --- 3. CORE TYPOGRAPHY --- */
	        body {
	            font-family: 'Segoe UI', Arial, sans-serif;
	            line-height: 1.3;
	            font-size: 11pt;
	            color: #111111;
	        }

	        /* --- 4. STRUCTURAL ELEMENTS --- */
	        h1, h2, h3 { color: #264F78; }

	        .page-break {
	            display: block;
	            height: 0;
	            visibility: hidden;
	        }

	        /* --- 5. FORM ELEMENTS --- */
	        input[type="text"], textarea, .printable-field, .printable-textarea {
	            background-color: #ffffff !important;
	            color: #111111 !important;
	            border: 1px solid #ccc !important;
	            -webkit-appearance: none;
	        }

	        /* --- 6. CODE BLOCKS & SYNTAX --- */
	        pre {
	            background-color: #f5f5f5;
	            border: 1px solid #ddd;
	            padding: 12px;
	            padding-top: 15px !important; /* Space for the Copy button overlay */
	            border-radius: 4px;
	            overflow: auto;    /* Enable horizontal scroll for long lines */
	            position: relative; /* Coordinate anchor for the absolute-positioned copy button */
	        }

	        code {
	            font-family: Consolas, monospace;
	            color: #c7254e !important; /* Sovereign highlight color */
	            background-color: #f5f5f5;
	            padding: 2px 4px;
	            border-radius: 3px;
	        }

	        /* Symmetry Fix: Neutralize padding for code blocks nested within pre tags */
	        pre code {
	            display: block; /* Prevents indentation on the first line */
	            padding: 0 !important;
	            background-color: transparent !important;
	        }

	        /* --- 7. TABLE LOGIC (v7.7.3 Master) --- */
	        table {
	            border-collapse: collapse;
	            width: 100%;
	            margin-top: 15px;
	            border: 1px solid #ddd;
	            table-layout: auto;
	        }

	        th, td {
	            border: 1px solid #ddd;
	            padding: 10px;
	            white-space: normal;
	        }

	        th { background-color: #f8f9fa; color: #264F78; font-weight: bold; }

	        /* --- 8. BLOCKQUOTES --- */
	        blockquote {
	            border-left: 4px solid #264F78;
	            background-color: #f9f9f9;
	            margin: 10px 0;
	            padding: 10px 20px;
	            color: #555;
	            font-style: italic;
	        }
	        """;

	/**
	 * The definitive CSS blueprint for printing raw Markdown/HTML source code. Optimized for readability, ink-saving, and technical code reviews.
	 */
	private static final String CSS_SOURCE_CODE_PRINT = """
	        /* --- 1. PHYSICAL PAGE (Driver Settings) --- */
	        @media print {
	            @page {
	                /* Standard margins for code review printouts */
	                margin: 15mm 15mm 20mm 15mm; /* Top, Right, Bottom, Left */
	                size: auto;
	            }
	            body {
	                /* No color-adjust: Force black/white for ink efficiency */
	                margin: 0;
	                padding: 0;
	            }
	        }

	        /* --- 2. GLOBAL TYPOGRAPHY (Content Frame) --- */
	        body {
	            font-family: 'Consolas', 'Monaco', monospace;
	            font-size: 9pt;
	            line-height: 1.3;
	            color: #000;      /* Force black for max contrast */
	            background: #fff; /* Force white for paper */
	        }

	        /* --- 3. PERSISTENT HEADER & FOOTER (Branding & Context) --- */
	        .header, .footer {
	            position: fixed;
	            left: 0;
	            right: 0;
	            width: 100%;
	            font-family: sans-serif;
	            background-color: white;
	            padding-left: 15mm;
	            padding-right: 15mm;
	        }

	        .header {
	            top: 0;
	            font-size: 8pt;
	            color: #333;
	            font-weight: 600;
	            display: flex;
	            justify-content: space-between;
	            align-items: center;
	            border-bottom: 1px solid #ddd;
	            padding-top: 5mm;
	            padding-bottom: 4px;
	        }

	        .footer {
	            bottom: 0;
	            height: 15mm;
	            text-align: center;
	            font-size: 7pt;
	            color: #999;
	            border-top: 1px solid #ddd;
	            padding-top: 5px;
	        }
	        """;

	// --- CSS MODULE ---

	private static final String CSS_WORKSPACE_BASE = """
	        @media screen {
	            html { -webkit-text-size-adjust: none; text-size-adjust: none; }

	            body {
	                background-color: #909090 !important; /* Etwas dunkler für edlen Kontrast */
	                margin: 0 !important;

	                /* Exakt 5px Abstand nach oben, 10px nach unten */
	                padding: 5px 0 10px 0 !important;

	                display: flex;
	                flex-direction: column;
	                align-items: center;
	                min-width: fit-content;
	                -webkit-font-smoothing: antialiased;
	            }

	            body.dark-mode { background-color: #1a1c24 !important; }

	            /* BASIS STILE FÜR HINTS */
	            .workspace-info, .workspace-tips {
	                width: 100% !important;
	                max-width: 800px !important;

	                /* Erzwingt Zentrierung in jedem Modus */
	                margin-left: auto !important;
	                margin-right: auto !important;

	                background: #e3f2fd !important;
	                padding: 8px 12px 8px 35px;
	                border-radius: 4px;
	                border-left: 5px solid #2196f3;
	                font-family: 'Segoe UI', sans-serif;
	                font-size: 13px;
	                box-sizing: border-box;
	                position: relative;
	            }

	             .workspace-info {
	                margin-top: 0 !important;
	                margin-bottom: 10px !important; /* 10px Lücke zum Dokument */
	            }

	            .workspace-tips {
	                /* HEILUNG: Schließt bündig an den 10px-Margin des Dokuments an */
	                margin-top: 0 !important;
	                margin-bottom: 0 !important;
	            }

	            .workspace-tips h4 {
	                margin-top: 0;
	                margin-bottom: 5px;
	                color: #0d47a1;
	                font-weight: bold;
	            }

	            .workspace-tips::before {
	                content: "";
	                display: block;
	                width: 24px;
	                height: 24px;
	                position: absolute;
	                left: 10px;
	                top: 10px;
	                background-image: url('{{SVG_BULB}}');
	                background-size: contain;
	                background-repeat: no-repeat;
	            }

	            /* PAGE BREAK (wie gehabt, Edge-to-Edge) */
	            .workspace-mode .page-break {
	                visibility: visible;
	                display: block;
	                position: relative;
	                left: -{{L}}mm !important;
	                width: {{WIDTH}} !important;
	                height: 4px;
	                margin-top: 15px;
	                margin-bottom: 0;
	                background-color: rgba(255, 82, 82, 0.1);
	                border-top: 2px dashed #FF5252;
	                clear: both;
	            }

	            .workspace-mode .page-break::after {
	                content: "✂ MANUAL PAGE BREAK";
	                position: absolute;
	                right: 20px;
	                top: -14px;
	                font-size: 10px;
	                color: #FF5252;
	                font-weight: bold;
	                background: white;
	                padding: 2px 8px;
	                border: 1px solid #FF5252;
	                border-radius: 10px;
	            }
	        }
	        """;

	private static final String CSS_BRANDING_HEADER = """
	        .document-header h1 { font-size: 16pt; font-weight: bold; color: #003366; margin: 0; }
	        .document-header .company-logo { height: 40px; float: left; margin-right: 15px; }

	        /* HEILUNG: Wir entfernen margin/padding 0 !important.
	           Der Header soll atmen können und Overrides erlauben. */
	        .document-header {
	            display: block;
	            width: 100%;
	        }
	        """;

	private static final String CSS_VIRTUAL_PAPER = """
	        .document-page {
	            position: relative;
	            width: {{WIDTH}} !important;
	            min-height: {{HEIGHT}} !important;
	            background-color: white !important;
	            color: #111111 !important;
	            box-sizing: border-box;
	            font-family: 'Segoe UI', Arial, sans-serif;
	            font-size: 11pt !important;
	            line-height: 1.3 !important;

	            /* Fallback Padding */
	            padding-top: {{T}}mm !important;
	            padding-bottom: {{B}}mm !important;
	            padding-left: {{L}}mm !important;
	            padding-right: {{R}}mm !important;

	            overflow: visible !important;

	            /* HEILUNG: 10px Abstand nach unten erzwingen */
	        	margin: 0 auto 10px auto !important;
	        }

	        .document-header + .document-content { margin-top: 0 !important; }
	        .workspace-mode .document-page { box-shadow: 0 0 15px rgba(0, 0, 0, 0.15); }
	        .document-page img { max-width: 100%% !important; height: auto !important; display: block; margin: 10px auto; }

	        /* Checkboxen */
	        input[type="checkbox"] {
	            -webkit-appearance: none; appearance: none; width: 10.5pt !important; height: 10.5pt !important;
	            border: 1px solid #444; background-color: #f0f0f0; vertical-align: middle; position: relative; top: -1.2pt; margin: 0 8px 0 0 !important; cursor: pointer;
	        }
	        input[type="checkbox"]:checked { background-color: #20BFDF; border-color: #20BFDF; }
	        input[type="checkbox"]:checked::after { content: '✔'; color: white; font-size: 8.5pt; position: absolute; top: 50%%; left: 50%%; transform: translate(-50%%,-55%%); }

	        {{GUIDES}}
	        """;

	/* TYP A: Die Web-Rolle (Web-Formate) */
	private static final String GUIDES_ROLLE = """
	        .workspace-mode .document-page::after {
	            content: ""; position: absolute; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; z-index: 9999;

	            background-image:
	                /* TOP MARGIN (Blau) */
	                linear-gradient(to bottom, transparent calc(%1$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%1$dmm - 1px), rgba(32, 191, 223, 0.3) %1$dmm, transparent %1$dmm),

	                /* BOTTOM MARGIN (Blau) - Am Ende des Dokuments */
	                linear-gradient(to top, transparent calc(%2$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%2$dmm - 1px), rgba(32, 191, 223, 0.3) %2$dmm, transparent %2$dmm),

	                /* LEFT MARGIN (Blau) */
	                linear-gradient(to right, transparent calc(%3$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%3$dmm - 1px), rgba(32, 191, 223, 0.3) %3$dmm, transparent %3$dmm),

	                /* RIGHT MARGIN (Blau) */
	                linear-gradient(to left, transparent calc(%4$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%4$dmm - 1px), rgba(32, 191, 223, 0.3) %4$dmm, transparent %4$dmm)
	            !important;

	            background-size: 100%% 100%% !important;
	            background-repeat: no-repeat !important;
	        }
	        """;

	private static final String GUIDES_PAGINATED = """
	        .workspace-mode .virtual-page::after {
	            content: ""; position: absolute; top: 0; left: 0; right: 0; bottom: 0;
	            pointer-events: none; z-index: 9999;

	            background-image:
	                /* ROTE LINIE (unten) - Indikator für Seitenende */
	                linear-gradient(to top, rgba(255, 0, 0, 0.3) 0, rgba(255, 0, 0, 0.3) 1px, transparent 1px),

	                /* TOP MARGIN (Blau) */
	                linear-gradient(to bottom, transparent calc(%1$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%1$dmm - 1px), rgba(32, 191, 223, 0.3) %1$dmm, transparent %1$dmm),

	                /* BOTTOM MARGIN (Blau) */
	                linear-gradient(to top, transparent calc(%2$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%2$dmm - 1px), rgba(32, 191, 223, 0.3) %2$dmm, transparent %2$dmm),

	                /* LEFT MARGIN (Blau) */
	                linear-gradient(to right, transparent calc(%3$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%3$dmm - 1px), rgba(32, 191, 223, 0.3) %3$dmm, transparent %3$dmm),

	                /* RIGHT MARGIN (Blau) */
	                linear-gradient(to left, transparent calc(%4$dmm - 1px), rgba(32, 191, 223, 0.3) calc(%4$dmm - 1px), rgba(32, 191, 223, 0.3) %4$dmm, transparent %4$dmm)
	            !important;

	            background-repeat: no-repeat !important;
	            background-size: 100%% 100%% !important;
	        }
	        """;

	private static final String CSS_PRINT_LOGIC = """
	        @media print {
	            @page {
	                size: {{PAGE_SIZE}} !important;
	                margin: 0 !important;
	            }

	            * {
	                -webkit-box-shadow: none !important;
	                box-shadow: none !important;
	                text-shadow: none !important;
	            }

	            body {
	                background: white !important;
	                padding: 0 !important;
	                margin: 0 !important;
	                -webkit-print-color-adjust: exact !important;
	                print-color-adjust: exact !important;
	            }

	            .document-page {
	                width: {{WIDTH}} !important;
	                height: auto !important;
	                padding-top: {{T}}mm !important;
	                padding-bottom: {{B}}mm !important;
	                padding-left: {{L}}mm !important;
	                padding-right: {{R}}mm !important;
	                margin: 0 !important;
	                box-sizing: border-box !important;
	                border: none !important;
	                overflow: visible !important;
	            }

	            /* HEILUNG: Alle Workspace-Hilfen und Marker in einem Block entfernen */
	            .no-print,
	            .workspace-info,
	            .workspace-tips,
	            .page-break {
	                display: none !important;
	            }

	            /* Umbrüche innerhalb wichtiger Elemente verhindern */
	            table, h1, h2, h3, img { page-break-inside: avoid; }
	        }
	        """;

	private static final String CSS_PAGE_CONTAINER = """
	        .virtual-page {
	            width: {{WIDTH}} !important; height: {{HEIGHT}} !important;
	            padding-top: {{T}}mm !important; padding-bottom: {{B}}mm !important;
	            padding-left: {{L}}mm !important; padding-right: {{R}}mm !important;
	            background: white !important; margin: 10px auto !important;
	            box-shadow: 0 0 15px rgba(0, 0, 0, 0.15) !important;
	            position: relative; box-sizing: border-box; overflow: hidden; display: flow-root;

	              /* Bündiger Seiteabstand ohne eigenen Zusatz-Abstand */
	            margin: 0 auto 10px auto !important;

	            /* FONT LOCK */
	            font-family: 'Segoe UI', Arial, sans-serif !important;
	            font-size: 11pt !important;
	            line-height: 1.3 !important;
	        }

	        .slicer-wrap { display: inline; }

	        /* --- SOUVERÄNE GEOMETRIE-SICHERUNG --- */

	        /* Wir verzichten auf den :first-child Wächter, da der Blueprint margin-top: 0 erzwingt. */

	        /* Der :last-child Wächter ist jedoch ESSENZIELL:
	           Er verhindert, dass der Slicer durch rhythmische Abstände (margin-bottom)
	           zu früh umbricht. Das sorgt für eine maximale Ausnutzung der Seite. */
	        .virtual-page > :last-child {
	            margin-bottom: 0;
	        }

	        /* --- PRINT-OPTIMIERUNG --- */
	        .virtual-page, .virtual-page * {
	            -webkit-box-shadow: none !important;
	            text-shadow: none !important;
	            color-adjust: exact !important;
	            print-color-adjust: exact !important;
	        }

	        .workspace-mode .virtual-page {
	            box-shadow: 0 0 15px rgba(0, 0, 0, 0.15) !important;
	        }

	        @media print {
	            .virtual-page {
	                margin: 0 !important; box-shadow: none !important;
	                border: none !important;
	                width: {{WIDTH}} !important;
	                height: calc({{HEIGHT}} - 0.5mm) !important;
	                padding-top: {{T}}mm !important; padding-bottom: {{B}}mm !important;
	                padding-left: {{L}}mm !important; padding-right: {{R}}mm !important;
	                box-sizing: border-box !important;
	                page-break-after: always !important;
	                overflow: hidden;
	            }
	            .virtual-page:last-child {
	                page-break-after: avoid !important;
	            }
	        }
	        """;

	public String getSlicerScript(double pageHeightMm, double marginTopMm, double marginBottomMm) {
		return """
		        (function() {
		            const PAGE_HEIGHT_LIMIT = %s;

		            window.reSlice = function() {
		                const content = document.querySelector('.document-content');
		                // ARCHITECTURAL GUARD: Do not slice if in Roll-Mode
		                if (!content || document.querySelector('.document-page')) return;

		                // 1. FAST CLEANUP (Unwrap existing pages)
		                const pages = content.querySelectorAll('.virtual-page');
		                if (pages.length > 0) {
		                    const frag = document.createDocumentFragment();
		                    pages.forEach(p => { while(p.firstChild) frag.appendChild(p.firstChild); });
		                    content.innerHTML = "";
		                    content.appendChild(frag);
		                }

		                // 2. INTELLIGENT WRAPPING (Preserve existing wrappers)
		                Array.from(content.childNodes).forEach(node => {
		                    if (node.nodeType === 3 && node.nodeValue.trim() && !node.parentElement.classList.contains('slicer-wrap')) {
		                        const span = document.createElement('span');
		                        span.className = 'slicer-wrap';
		                        node.parentNode.insertBefore(span, node);
		                        span.appendChild(node);
		                    }
		                });

		                const elements = Array.from(content.children);
		                content.innerHTML = "";

		                const createPage = () => {
		                    const div = document.createElement('div');
		                    div.className = 'virtual-page';
		                    content.appendChild(div);
		                    return div;
		                };

		                // 3. PERFORMANCE-LOOP WITH MANUAL BREAK SUPPORT
		                let currentPage = createPage();
		                const containerHeight = currentPage.clientHeight; // Cache container height

		                elements.forEach(el => {
		          // --- SOVEREIGN COMMAND: MANUAL PAGE BREAK (Heilung) ---
		          if (el.classList.contains('page-break')) {
		              // 1. Wir hängen den Marker an das ENDE der aktuellen Seite an.
		              // Da er height: 0 hat, verursacht er dort keinen Overflow.
		              currentPage.appendChild(el);

		              // 2. Wir erzwingen SOFORT eine neue Seite für den nächsten Inhalt.
		              currentPage = createPage();

		              // 3. Wir brechen die Iteration für dieses Element ab.
		              return;
		          }

		                    // --- AUTOMATIC OVERFLOW CHECK ---
		                    currentPage.appendChild(el);
		                    if (currentPage.scrollHeight > containerHeight + 1) {
		                        if (currentPage.children.length > 1) {
		                            currentPage.removeChild(el);
		                            currentPage = createPage();
		                            currentPage.appendChild(el);
		                        }
		                    }
		                });
		            };

		            // Initial execution
		            if (document.readyState === 'complete') window.reSlice();
		            else window.addEventListener('load', window.reSlice);
		        })();
		        """.formatted(String.valueOf(pageHeightMm));
	}

	public String getSlicerScriptOld(double pageHeightMm, double marginTopMm, double marginBottomMm) {
		return """
		        (function() {
		            const PAGE_HEIGHT_LIMIT = %s;

		            window.reSlice = function() {
		                // 1. SUCHE DEN CONTAINER
		                const content = document.querySelector('.document-content');
		                if (!content) return;

		                // 2. RADIKALE REINIGUNG (Unwrap)
		                // Wir holen den Inhalt aus allen existierenden virtuellen Seiten heraus
		                const existingPages = content.querySelectorAll('.virtual-page');
		                if (existingPages.length > 0) {
		                    let fragment = document.createDocumentFragment();
		                    existingPages.forEach(page => {
		                        while(page.firstChild) fragment.appendChild(page.firstChild);
		                    });
		                    content.innerHTML = "";
		                    content.appendChild(fragment);
		                }

		                // --- HEILUNG: DER MODUS-WÄCHTER ---
		                // Wenn wir im "Rollen-Modus" sind, existiert ein Element mit der Klasse '.document-page'.
		                // In diesem Fall dürfen wir KEINE neuen virtuellen Seiten erstellen.
		                if (document.querySelector('.document-page')) {
		                    console.log("Slicer: Roll-Mode detected. Stopping after cleanup.");
		                    return;
		                }

		                // 3. TEXT-WRAPPING FIX
		                const allNodes = Array.from(content.childNodes);
		                allNodes.forEach(node => {
		                    if (node.nodeType === 3 && node.nodeValue.trim().length > 0) {
		                        const wrapper = document.createElement('span');
		                        node.parentNode.insertBefore(wrapper, node);
		                        wrapper.appendChild(node);
		                    }
		                });

		                // 4. SLICING-LOGIK (Nur wenn wir nicht im Rollen-Modus gestoppt haben)
		                const originalElements = Array.from(content.children);
		                content.innerHTML = "";

		                function createNewPage() {
		                    const div = document.createElement('div');
		                    div.className = 'virtual-page';
		                    content.appendChild(div);
		                    return div;
		                }

		                function hasOverflow(element) {
		                    if (PAGE_HEIGHT_LIMIT > 9000) return false;
		                    return element.scrollHeight > element.clientHeight + 1;
		                }

		                let currentPage = createNewPage();
		                originalElements.forEach(el => {
		                    currentPage.appendChild(el);
		                    if (hasOverflow(currentPage)) {
		                        currentPage.removeChild(el);
		                        currentPage = createNewPage();
		                        currentPage.appendChild(el);
		                    }
		                });
		            };

		            // Initialer Aufruf bei Systemstart
		            if (document.readyState === 'complete') window.reSlice();
		            else window.addEventListener('load', window.reSlice);
		        })();
		        """.formatted(String.valueOf(pageHeightMm));
	}

	private final Map<String, String> themes = new HashMap<>();

	/**
	 * Liefert eine Menge aller geschützten System-Klassen, die von FlowShift zur Layout-Steuerung und für Branding-Elemente genutzt werden.
	 */
	public Set<String> getSystemClasses() {
		return Set.of("document-page", "document-header", "document-content", "company-logo", "workspace-mode", "workspace-info", "workspace-tips", "dark-mode", "page-break",
		        "no-print", "printable-field", "printable-textarea", "copy-btn", "fs-hover" // fs-hover ist für die Bridge-Interaktion
		);
	}

	public CssThemeManager() {
		loadBuiltInThemes();
	}

	private void loadBuiltInThemes() { // TODO hier ist der fucking fehler, sobald man diese lädt geht das page forma kaputt
//		themes.put(NAME_PAPER_STANDARD, BLUEPRINT_PAPER_STANDARD);
//		themes.put(NAME_MODERN, "font-family: 'Helvetica', sans-serif; color: #2c3e50;");
	}

	/**
	 * Liefert die Namen aller verfügbaren Design-Blueprints.
	 */
	public List<String> getThemeNames() {
		return new ArrayList<>(themes.keySet());
	}

	/**
	 * Generates the complete geometric and aesthetic CSS for the document view. Synchronizes document settings (margins, format, pagination) with CSS templates to ensure visual
	 * consistency between the editor and exported formats.
	 * 
	 * @param ds The active document settings.
	 * @return A consolidated CSS string ready for WebView injection.
	 */
	public String getPageLayoutCss(DocumentSettings ds) {
		try {
			DocumentFormat format = ds.getFormat();

			// 1. Context Acquisition: Convert numeric margins to specific types for formatting
			int mt = (int) ds.getMarginTop();
			int mb = (int) ds.getMarginBottom();
			int ml = (int) ds.getMarginLeft();
			int mr = (int) ds.getMarginRight();

			String sTop = String.valueOf(mt);
			String sBot = String.valueOf(mb);
			String sLeft = String.valueOf(ml);
			String sRight = String.valueOf(mr);

			// 2. Process Layout Overrides for Web-Native Formats
			StringBuilder layoutOverride = new StringBuilder();

			if (!format.isPaper) {
				// WEB-FLUID GEOMETRY: Ensures edge-to-edge rendering and preserves workspace hints
				layoutOverride.append("""
				        	body {
				        		align-items: center !important;
				        		padding: 5px 0 10px 0 !important;
				        	}
				        	#sovereign-payload { width: 100% !important; }
				        	.document-page {
				        		margin: 0 auto 10px auto !important;
				        		width: {{WIDTH}} !important;
				        		max-width: none !important;
				        		box-shadow: none !important;
				        		border: none !important;

				        		/* Map YAML margins to CSS padding for fluid containers */
				        		padding-top: {{T}}mm !important;
				        		padding-bottom: {{B}}mm !important;
				        		padding-left: {{L}}mm !important;
				        		padding-right: {{R}}mm !important;
				        	}
				        	.workspace-mode .document-page { box-shadow: none !important; }
				        """);
			}

			// 3. Geometry Visualization (Margin Guides)
			String guides;
			boolean marginsAreSet = (mt > 0 || mb > 0 || ml > 0 || mr > 0);

			if (ds.isPaginated() && format.isPaper) {
				// Mode A: Physical Pagination (Slicer active)
				guides = String.format(Locale.ROOT, GUIDES_PAGINATED, mt, mb, ml, mr);
			} else if (marginsAreSet) {
				// Mode B: Continuous Roll (Display guides only if margins are non-zero)
				guides = String.format(Locale.ROOT, GUIDES_ROLLE, mt, mb, ml, mr);
			} else {
				// Mode C: Borderless Digital Flow
				guides = "";
			}

			// 4. Master Assembly: Consolidate CSS modules
			StringBuilder css = new StringBuilder();
			css.append(CSS_WORKSPACE_BASE);

			// Logic: Prevent CSS pollution by only injecting branding styles when active
			if (!"NONE".equalsIgnoreCase(ds.getActiveHeaderStyle())) {
				css.append(CSS_BRANDING_HEADER);
			}

			css.append(CSS_VIRTUAL_PAPER);
			css.append(CSS_PRINT_LOGIC);
			css.append(CSS_PAGE_CONTAINER);
			css.append(layoutOverride);

			// 5. Final Token Injection: Replace placeholders with live data
			String result = css.toString().replace("{{SVG_BULB}}", SVG_BULB).replace("{{PAGE_SIZE}}", format.pageSize).replace("{{WIDTH}}", format.width)
			        .replace("{{HEIGHT}}", format.height).replace("{{T}}", sTop).replace("{{B}}", sBot).replace("{{L}}", sLeft).replace("{{R}}", sRight)
			        .replace("{{GUIDES}}", guides);

			// Percent-escape for JavaFX WebView compatibility
			return result.replace("%%", "%");

		} catch (Exception e) {
			Log.error(e, "Sovereign Engine: CSS generation critical failure.");
			return "";
		}
	}

	/**
	 * Retrieves the CSS content for a specific theme name. Falls back to the 'Paper Standard' theme if the requested name is not found.
	 */
	public String getThemeContent(String name) {
		// HEILUNG: Fällt auf den neuen, sauberen Standard zurück
		return themes.getOrDefault(name, themes.get(NAME_PAPER_STANDARD));
	}

	/**
	 * Returns the default aesthetic styles (typography, colors) wrapped in a <style> tag.
	 * 
	 * This method provides the foundational visual appearance for the content within the live preview, before any user-defined styles from the Style Sanctuary are applied. It
	 * complements the geometric layout defined in CSS_VIRTUAL_PAPER.
	 *
	 * @return A complete <style> block containing the default theme's aesthetic rules.
	 */
	public String getBaseAestheticStyleBlock() {
		// HEILUNG: Nutzt die einzige Quelle der Wahrheit
		return "<style>\n" + BLUEPRINT_PAPER_STANDARD + "\n</style>";
	}

	/**
	 * Returns the default styling blueprint to be used as a starting point in the Style Sanctuary (CSS Editor). This is typically invoked when a user wants to start customizing
	 * from the application's standard theme.
	 *
	 * @return A clean, editable CSS string representing the default theme.
	 */
	public String getDefaultThemeBlueprint() {
		return BLUEPRINT_PAPER_STANDARD;
	}

	/**
	 * Returns the CSS blueprint required for printing raw source code. This method's purpose is to provide a standardized, readable format for technical reviews or archival of the
	 * Markdown source itself.
	 *
	 * @return The complete CSS string for source code printing.
	 */
	public String getCssForSourceCodePrint() {
		return CSS_SOURCE_CODE_PRINT;
	}

	/**
	 * Registriert ein benutzerdefiniertes Theme in der Bibliothek. Ermöglicht das Laden externer CSS-Dateien aus dem Nutzerverzeichnis.
	 * 
	 * @param name       Der Anzeigename des Themes.
	 * @param cssContent Der reine CSS-Code.
	 */
	public void addCustomTheme(String name, String cssContent) {
		if (name != null && cssContent != null) {
//			themes.put(name, cssContent);
			Log.info("Custom theme registered: " + name);
		}
	}

	public void loadExternalThemes() {
		File themeDir = new File(System.getProperty("user.home"), ".flowshift/themes");
		if (themeDir.exists()) {
			for (File f : themeDir.listFiles((dir, name) -> name.endsWith(".css"))) {
				try {
					String css = Files.readString(f.toPath());
					// Der Dateiname (ohne .css) wird zum Theme-Namen in der UI
					String themeName = f.getName().replace(".css", "");
					// Theme zur Map hinzufügen
					addCustomTheme(themeName, css);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// --- WEBVIEW UI COMPONENTS ---

	/**
	 * Styles the scrollbars to be extremely bright and clean. Optimized for high visibility within the neutral code blocks.
	 */
	private static final String CSS_SCROLLBARS = """
	        <style>
	            /* --- 1. THE SCROLLBAR TRACK (The Path) --- */
	            ::-webkit-scrollbar {
	                width: 10px;
	                height: 10px;
	            }
	            ::-webkit-scrollbar-track {
	                /* HEILUNG: Sehr helles, fast weißes Grau für maximale Sichtbarkeit */
	                background: #f0f0f0 !important;
	                border-radius: 5px;
	                border: 1px solid #d1d1d1; /* Subtle border for definition */
	            }

	            /* --- 2. THE SCROLLBAR THUMB (The Draggable Part) --- */
	            ::-webkit-scrollbar-thumb {
	                /* Ein sauberes Hellgrau, das sich vom Track abhebt */
	                background: #cccccc !important;
	                border: 1px solid #f0f0f0; /* Creates visual padding/air */
	                border-radius: 5px;
	            }

	            /* Interaction Feedback */
	            ::-webkit-scrollbar-thumb:hover {
	                background: #aaaaaa !important;
	            }

	            ::-webkit-scrollbar-corner {
	                background: #f0f0f0;
	            }
	        </style>
	        """;

	/** Defines the aesthetics of the Code Block. Pure Neutral Gray Edition. */
	private static final String CSS_COPY_BUTTON = """
	        <style>
	            .code-block-wrapper {
	                position: relative;
	                margin: 20px 0;
	                border-radius: 6px;
	                border: 1px solid #d1d1d1;
	                /* HEILUNG: Absolut neutrales, helles Grau ohne Blaustich */
	                background-color: #f5f5f5;
	                overflow: hidden;
	            }

	            pre {
	                background-color: transparent !important;
	                padding: 15px;
	                padding-top: 42px !important;
	                margin: 0 !important;
	                border: none !important;
	                overflow: auto;
	                max-width: 100%;
	                /* Sicherstellen, dass der Text im Code-Block neutral bleibt */
	                color: #333333;
	            }

	            .copy-btn {
	                position: absolute;
	                top: 10px;
	                right: 10px;

	                /* --- IDLE STATE: Pure Medium Gray --- */
	                background-color: #888888;
	                color: #ffffff;
	                border: none;

	                border-radius: 4px;
	                padding: 4px 12px;
	                font-family: 'Segoe UI', sans-serif;
	                font-size: 10px;
	                font-weight: 600;
	                text-transform: uppercase;
	                letter-spacing: 0.5px;
	                cursor: pointer;
	                opacity: 0.9;
	                transition: background-color 0.2s ease, opacity 0.2s ease;
	                z-index: 100;
	            }

	            /* --- HOVER STATE: Sovereign Teal --- */
	            .copy-btn:hover {
	                opacity: 1;
	                background-color: #209FAF;
	                color: white;
	            }

	            .copy-btn.copied {
	                background-color: #28a745; /* Ein saubereres Erfolgs-Grün */
	                opacity: 1;
	            }
	        </style>
	        """;

	/** The JavaScript logic now handles the dynamic wrapping of code blocks. */
	private static final String JS_COPY_LOGIC = """
	        <script>
	            window.initializeCopyButtons = function() {
	                document.querySelectorAll('pre').forEach(pre => {
	                    // 1. Idempotency Check: Avoid double wrapping
	                    if (pre.parentElement.classList.contains('code-block-wrapper')) return;

	                    // 2. Create the Wrapper and Button
	                    const wrapper = document.createElement('div');
	                    wrapper.className = 'code-block-wrapper';

	                    const btn = document.createElement('button');
	                    btn.className = 'copy-btn';
	                    btn.textContent = 'Copy';

	                    // 3. Structural Re-arrangement
	                    // Insert wrapper before pre, then move pre inside wrapper
	                    pre.parentNode.insertBefore(wrapper, pre);
	                    wrapper.appendChild(pre);
	                    wrapper.appendChild(btn);

	                    // 4. Clipboard Action
	                    btn.onclick = function() {
	                        const code = pre.querySelector('code') || pre;
	                        const text = code.innerText || code.textContent;
	                        const el = document.createElement('textarea');
	                        el.value = text;
	                        document.body.appendChild(el);
	                        el.select();
	                        document.execCommand('copy');
	                        document.body.removeChild(el);
	                        btn.textContent = 'Copied!';
	                        btn.classList.add('copied');
	                        setTimeout(() => {
	                            btn.textContent = 'Copy';
	                            btn.classList.remove('copied');
	                        }, 2000);
	                    };
	                });
	            };
	            if (document.readyState === 'complete') window.initializeCopyButtons();
	            else document.addEventListener('DOMContentLoaded', window.initializeCopyButtons);
	        </script>
	        """;

	/**
	 * Returns the consolidated interaction and aesthetic package for the WebView. Combines scrollbar styling, button design, and clipboard logic.
	 */
	public String getInteractionScriptForExport() {
		StringBuilder sb = new StringBuilder();
		sb.append(CSS_SCROLLBARS); // Fixes the black scrollbar track
		sb.append(CSS_COPY_BUTTON); // Restores the Crimson/Grey design
		sb.append(JS_COPY_LOGIC); // Injects the clipboard engine
		return sb.toString();
	}


}
