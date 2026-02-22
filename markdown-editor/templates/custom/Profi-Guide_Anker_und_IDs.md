---
author: "FlowShift Architect"
title: "Profi-Guide: Navigation, Anker & IDs"
format: WEB_READING
paginate: false
version: 1.3
status: FINAL
mTop: 15
mBot: 15
mLeft: 15
mRight: 15
---

# Profi-Guide: Navigation & ID-Logik

Dieses Dokument ist eine technische Referenz für die Erstellung von Inhaltsverzeichnissen (TOC) und internen Verknüpfungen in der **FlowShift Sovereign Document Engine**.

---

## 1. Das Hybrid-System
Die Engine nutzt ein intelligentes System zur Identifizierung von Abschnitten. Sie haben die Wahl zwischen der komfortablen **Vollautomatik** und der präzisen **manuellen Kontrolle**.

---

## 2. Die Automatik-Regeln (Slugification)
Wenn Sie eine Überschrift im Markdown-Stil verfassen (z. B. `## Meine Überschrift`), generiert die Engine im Hintergrund automatisch eine ID (Anker), damit Links dieses Ziel finden.  
<br>

### Die Umwandlungsformel:
1.  **Kleinschreibung:** Alle Großbuchstaben werden in Kleinbuchstaben umgewandelt.
2.  **Punkte-Filter:** Satzzeichen wie Punkte (`.`), Kommas (`,`) oder Semikolons (`;`) werden **gelöscht**.
3.  **Bindestrich-Ersatz:** Leerzeichen und Sonderzeichen (wie `:`, `&`, `/`, `(`, `)`) werden durch einen Bindestrich (`-`) ersetzt.
4.  **Sovereign Merge:** Mehrere aufeinanderfolgende Bindestriche werden zu einem **einzigen** Bindestrich verschmolzen (verhindert unschöne `---`).

### Beispiele der Automatik:
| Header-Text (Markdown) | Generierte ID / Link-Ziel |
| :--- | :--- |
| `## 1. Einleitung` | `#1-einleitung` |
| `## Hilfe & Support` | `#hilfe-support` |
| `## 4.3 Linter: Details` | `#43-linter-details` |
| `## (Neu) Funktionen!` | `#neu-funktionen` |

---

## 3. Manueller Override (Power-Feature)
Manchmal erzeugt die Automatik zu lange IDs oder Sie möchten einen Link stabil halten, selbst wenn Sie den Text der Überschrift später ändern. Hier nutzen Sie den **Sovereign Override**.

**Syntax:** Fügen Sie am Ende der Überschrift `{#deine-id}` hinzu.

**Beispiel:**
`## 4.3 Linter & Diagnostics (Qualitätssicherung) {#linter}`

*   **Resultat:** Die ID lautet schlicht `linter`.
*   **Verlinkung:** `[Zum Linter springen](#linter)`

> **Profi-Tipp:** Nutzen Sie kurze, einprägsame manuelle IDs für technische Dokumentationen, um das Inhaltsverzeichnis übersichtlich zu halten.

---

## 4. HTML-Header (Souveräner Code)
Die Engine respektiert Ihren HTML-Code und verändert ihn nicht. Wenn Sie Überschriften direkt als HTML-Tags schreiben, erfolgt **keine automatische ID-Generierung**.

*   **Inaktiv:** `<h2>Überschrift</h2>` (Kein Ziel für Links vorhanden)
*   **Aktiv:** `<h2 id="mein-ziel">Überschrift</h2>` (Ziel ist manuell definiert)

---

## 5. Zusammenfassung der Möglichkeiten

| Methode | Beispiel-Eingabe | Erzeugte ID |
| :--- | :--- | :--- |
| **Markdown Automatik** | `## Kontakt & Impressum` | `kontakt-impressum` |
| **Markdown Manuell** | `## Kontakt & Impressum {#info}` | `info` |
| **HTML Manuell** | `<h2 id="info">Kontakt</h2>` | `info` |

---
**Kassandras Fazit:** Vertrauen Sie bei einfachen Dokumenten der Automatik. Nutzen Sie für komplexe Projekte und stabile Verlinkungen den manuellen Override `{#id}`.

<style>
/* --- 1. SOVEREIGN LAW: Universal Box Model --- */
* { box-sizing: border-box; }

/* --- 2. ARCHITECTURAL RESET --- */
/* HEALING: We removed 'ul' and 'ol' from the zero-padding rule
   to allow room for markers. */
h1, h2, h3, h4, h5, h6, p, li, blockquote, table, pre {
    margin: 0;
    margin-bottom: 0.4em;
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
</style>
