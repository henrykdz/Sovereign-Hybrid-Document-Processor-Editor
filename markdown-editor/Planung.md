# FlowShift - Roadmap to Sovereign Publishing

**Status:** Das Fundament (v7.7.2) ist gegossen. Die Kern-Engine (Linter, Formatter, Highlighter) und die UI-Architektur (Controller, Dialogs) sind stabil und kohärent. Die folgenden Schritte dienen der Perfektionierung des Produkts und der Vorbereitung des Markteintritts.

---

### PHASE 1: Finaler Feinschliff (Pre-Launch Polish)

*   `[ ]` **Final UI Audit:** Eine letzte visuelle Prüfung aller Dialoge und Komponenten auf Konsistenz (Abstände, Höhen, Schriftgrößen). Sicherstellen, dass das "28px-Gesetz" überall greift.
*   `[ ]` **Performance-Stresstest:** Laden und Formatieren einer extrem großen Markdown-Datei (> 5 MB), um sicherzustellen, dass keine UI-Freezes auftreten.
*   `[ ]` **Javadoc & Code-Hygiene:** Alle neuen, zentralen Klassen (`LinterLineNumberFactory`, `ProjectSettingsDialog`) mit präziser Javadoc versehen, um die architektonische Absicht für die Zukunft zu dokumentieren.

---

### PHASE 2: Die Content-Offensive (The Value Proposition)

*   `[ ]` **Implementierung des "Wizard"-Frameworks:**
    *   `[ ]` **Der Table-Wizard:** Ein einfacher Dialog, der sauberen Markdown/HTML-Code für Tabellen generiert.
    *   `[ ]` **Der Badge-Wizard:** Eine UI zur visuellen Erstellung von Shields.io-Badges für professionelle `README.md`-Dateien.
    *   `[ ]` **Der Placeholder-Inserter:** Ein Menü oder Button in der Sidebar, um verfügbare Platzhalter (`{{date}}`, `{{documentTitle}}`) am Cursor einzufügen.

*   `[ ]` **Erstellung der "Genesis Collection" (Premium-Templates):**
    *   `[ ]` **"The Sovereign Offer":** Das perfekte Template für Angebote und Rechnungen.
    *   `[ ]` **"The Authority Protocol":** Das Template für offizielle Protokolle und Berichte.
    *   `[ ]` **"The Crimson Architect":** Ein Template im Stil des "Suno Architect" für technische Dokumentationen und `READMEs`.
    *   `[ ]` **"The Nexus CV":** Ein Template für einen erstklassigen, gebrandeten Lebenslauf.

---

### PHASE 3: Die GitHub-Offensive (Der Markteintritt)

*   `[ ]` **GitHub Repository aufsetzen:** `FlowShift` als Open-Source-Projekt (oder zumindest die Demo) mit einer sauberen Struktur veröffentlichen.
*   `[ ]` **Das Master-`README.md` schmieden:**
    *   `[ ]` Die Vision und das Wertversprechen klar kommunizieren (Souveränität, Professionalität, Heilung von Frust).
    *   `[ ]` GIFs oder kurze Videos erstellen, die den Linter, den Formatter und die Design Forge in Aktion zeigen.
    *   `[ ]` Die Demo- vs. Premium-Version klar abgrenzen.
*   `[ ]` **Die Brücke zu IzonLink bauen:**
    *   `[ ]` Im Menü `Hilfe` einen Punkt `Über den Architekten` oder `FlowShift & IzonLink` einfügen.
    *   `[ ]` Dieser Menüpunkt öffnet eine Webseite, die dich, deine Vision und den Weg zur kommerziellen Version (Premium-Templates, IzonLink) vorstellt.
*   `[ ]` **Lizenzmodell definieren:** Festlegen, welche Features (z.B. Bundle-Export, Premium-Templates) der Vollversion vorbehalten sind.

---

### PHASE 4: Der nächste Horizont (Die Evolution)

*   `[ ]` **"Visual Assembly" (Das harte Projekt):**
    *   `[ ]` **Stufe 1 (Mapping):** Flexmark-Renderer erweitern, um unsichtbare `fs-block`-Wrapper mit Zeilen-Daten zu injizieren.
    *   `[x]` **Stufe 2 (Die Brücke):** Die JS-Java-Bridge für das Abfangen von Klicks und Drag-Events in der WebView implementieren.
    *   `[ ]` **Stufe 3 (Staging):** Das "Transactional Draft System" entwickeln, das visuelle Änderungen speichert, ohne den Quellcode direkt zu manipulieren.

---

### PHASE 5: Die Befreiung (The Sovereign Document Standard `*.fsb`)

**Mission:** Die Etablierung eines neuen, offenen Standards für interaktive, souveräne Dokumente, der die Rigidität von PDF überwindet. `FlowShift` wird die Referenz-Implementierung.

*   `[ ]` **Spezifikation des `*.fsb`-Formats:**
    *   `[ ]` Definieren der exakten Paketstruktur (Verzeichnis-Layout für `content.md`, `style.css`, `header.html`, `viewer.js`).
    *   `[ ]` Entwurf der `manifest.json`, die Metadaten und Versionierungsinformationen enthält.

*   `[ ]` **Entwicklung des `viewer.js` (The Sovereign Sandbox):**
    *   `[ ]` Implementierung des „Walled Garden“-Prinzips: Blockieren aller externen Skript- und Ressourcen-Anfragen.
    *   `[ ]` Design der „Curated API“ (`FSB.calculate`, `FSB.toggleVisibility` etc.), die nur sichere, dokumenteninterne Operationen erlaubt.
    *   `[ ]` Implementierung der **Browser-Härtung** durch dynamische Injektion von `Content-Security-Policy`-Meta-Tags.
    *   `[ ]` Entwicklung des „Stil-Wechslers“ und der `Druckansicht <-> Ausfüll-Modus`-Logik.

*   `[ ]` **Integration des `*.fsb`-Exports in `FlowShift`:**
    *   `[ ]` Erweiterung der „Export Bundle“-Funktion, um den `viewer.js` und die korrekte Paketstruktur automatisch zu erzeugen.
    *   `[ ]` Signatur-Mechanismus implementieren, um die Integrität des Bundles zu garantieren (optional, aber wichtig für amtliche Nutzung).

---
### PHASE 6: Das Imperium (Eroberung des Marktes)

**Mission:** Die Etablierung von `FlowShift` als das unersetzliche Werkzeug für professionelle, interaktive Dokumentation und die Verdrängung von PDF in Schlüsselmärkten.

*   `[ ]` **Entwicklung von „Sovereign Forms“:**
    *   `[ ]` Bau eines Wizards in `FlowShift` für interaktive Formular-Elemente (Eingabefelder, Checkboxen, Berechnungsfelder).
    *   `[ ]` Erweiterung des `CssThemeManager`, um perfekte `@media print`-Stylesheets für Formulare zu generieren (ausgefüllte Felder werden zu statischem Text).

*   `[ ]` **Erschließung strategischer Nischen:**
    *   `[ ]` Erstellung von Premium-Bundles, die spezifische Schmerzpunkte lösen: „Das interaktive Angebot“, „Der wissenschaftliche Bericht mit dynamischen Formeln“, „Das NASA-Inspektionsprotokoll“.
    *   `[ ]` Kooperation mit Open-Source-Projekten: Biete ihnen `FlowShift` an, um ihre `READMEs` und Dokumentationen auf ein neues Level zu heben.

*   `[ ]` **Schaffung der `fsb.dev`-Plattform:**
    *   `[ ]` Eine Webseite, die den `*.fsb`-Standard erklärt.
    *   `[ ]` Ein Online-Showcase, in dem Nutzer ihre `*.fsb`-Dokumente per Drag-and-Drop ansehen können (der `viewer.js` in Aktion).
    *   `[ ]` Ein Marktplatz für `FlowShift` Premium-Templates und -Bundles.

---