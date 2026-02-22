# FlowShift - Sovereign Hybrid Document Engine

<div align="center">
    <strong>⚡ Precision · Sovereignty · Zero Compromise ⚡</strong>
    <br><br>
    <a href="https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/">🌐 Live Demo</a> •
    <a href="#key-features-what-the-engine-delivers">✨ Features</a> •
    <a href="#installation">📦 Installation</a> •
    <a href="#technical-architecture-under-the-hood">🏗️ Architecture</a> •
    <a href="#background-the-story-behind-flowshift">📖 Background</a>
</div>

---

## 🚀 Introduction: What is the Sovereign Document Engine?

The **FlowShift Sovereign Document Engine** is more than a Markdown editor. It is a **precision instrument** for creating, managing, and publishing documents — combining the power of native desktop applications with the flexibility of web technologies and the intelligence of an AGI (Kassandra Core).

Developed by architect **Henryk Daniel Zschuppan** in partnership with Kassandra AGI, this engine is a counter‑design to the complex, resource‑hungry, and often unreliable software of 2026. It is built for professionals who demand absolute control over content, form, and the physical geometry of their documents.

> *"FlowShift transforms complex ideas into perfectly formatted documents. Our hybrid engine combines the flexibility of Markdown, HTML/CSS, and YAML with the precision of ISO standards."*

---

## 🧠 Core Philosophy: Sovereignty & Efficiency

Every feature of the FlowShift Engine is founded on three principles:

| Principle | Description |
|-----------|-------------|
| **Sovereignty** | Full user control over data, design, and workflow. The document is law, not the application. |
| **Efficiency** | Maximum performance with minimal resource consumption. No unnecessary waiting, no bloat. |
| **Precision** | Pixel‑perfect WYSIWYG rendering, consistent across all output media (screen, HTML export, PDF print). |

The engine listens to the architect's command and does exactly what is intended — without "magic" intermediate steps or unwanted autonomy.

---

## ✨ Key Features: What the Engine Delivers

### 3.1 Unrivalled Editing Experience

| Feature | Description |
|---------|-------------|
| **Flicker‑free live rendering** | Thanks to the `Sovereign Swap` mechanism, the preview updates in real time without any visual flicker. |
| **Scroll invariance** | The focus remains stable at the desired position in the preview while you type. |
| **Bidirectional navigation** | A click in the preview jumps the cursor precisely to the corresponding spot in the Markdown source. |
| **Intelligent error diagnostics** | The `ErrorTooltipManager` visualises linter errors directly in the status bar. |

### 3.2 Layout & Design Sovereignty

| Feature | Description |
|---------|-------------|
| **CSS Forge** | A dedicated code editor enables live styling directly in the document flow. |
| **Precise pagination** | The engine converts documents exactly into physical pages (A4, Letter). |
| **Neutral start** | Documents begin without any enforced formatting. Margins and pagination are defined explicitly in YAML. |

### 3.3 Complete Feature List

| | |
|---|---|
| ✅ **Formatter** | Automatic formatting of Markdown, HTML and CSS |
| ✅ **Linter** | Real-time error detection with visual feedback |
| ✅ **Syntax Highlighter** | Color-highlighted code for better readability |
| ✅ **Custom Placeholders** | Custom placeholders for templates and mail merges |
| ✅ **ISO-compliant Export** | A4, Letter, Web – everything possible |

---

## 🏗️ Technical Architecture: Under the Hood

### 4.1 Technology Stack

| Component | Technology |
|-----------|------------|
| **Java Version** | JavaSE-23 (Zulu 23) with bundled JavaFX |
| **Build Tool** | Apache Maven |
| **UI Framework** | JavaFX 23 (WebKit, Controls, FXML) |
| **Markdown Processing** | Flexmark-Java v0.64.8 (with extensions: tables, GFM strikethrough, tasklists, YAML front matter, attributes, anchor links) |
| **Rich Text Editor** | RichTextFX v0.11.7 |
| **Icons** | Ikonli v12.4.0 (FontAwesome5, Material, MaterialDesign) |
| **JSON Processing** | Jackson Databind v2.18.2, Gson v2.11.0 |
| **HTML Parsing** | Jsoup v1.18.3 |
| **SVG Support** | fxsvgimage v1.1 |
| **Testing** | JUnit 5, Mockito |

### 4.2 Hybrid Rendering Approach

The engine uses **JavaFX & WebKit**, combined with the `Sovereign Swap`. Instead of reloading the WebView, content is injected via `innerHTML`, guaranteeing absolute smoothness and zero flicker.

### 4.3 The Sovereign Bridge (Source Mapping)

The `SovereignSourceMapper` injects a unique `data-fsid` into every HTML element. An internal map links this ID to the exact character offset in the source text, enabling precise bidirectional navigation.

### 4.4 Navigation & Anchor Logic

The FlowShift Engine ensures your documents remain fully navigable in export and print:

- **Fully automatic:** Headings automatically receive unique IDs for tables of contents
- **Sovereign override:** Use the syntax `{#custom-id}` directly after a heading to override the automation
- **HTML integrity:** Pure HTML tags remain untouched, giving you full control

---

## 🔧 Installation

### Prerequisites

- **Java:** JavaSE-23 (Zulu 23 or compatible) – JavaFX is bundled
- **Maven:** 3.6 or higher
- **IDE:** Eclipse 2024+ recommended (with Maven integration)

### Build & Run

```bash
# Clone the repository
git clone https://github.com/henrykdz/Sovereign-Hybrid-Document-Processor-Editor.git
cd Sovereign-Hybrid-Document-Processor-Editor

# Build with Maven
mvn clean compile

# Run with Maven (JavaFX plugin)
mvn javafx:run

# Create executable JAR with dependencies
mvn clean package shade:shade

# The executable JAR will be in target/
java -jar target/markdown-editor-0.0.1-SNAPSHOT.jar
```

### Eclipse Import

1. **File → Import → Maven → Existing Maven Projects**
2. **Root Directory:** Select your cloned repository
3. **Finish**

### Maven Configuration Highlights

Your `pom.xml` includes:

- **Java 23** with module path configuration for JavaFX
- **RichTextFX** v0.11.7 for the code editor
- **Flexmark** with extensions for full Markdown support
- **Ikonli** for beautiful icons (FontAwesome5, Material, MaterialDesign)
- **Jackson & Gson** for JSON serialization
- **JAXB** for JDK9+ compatibility
- **Shade plugin** for creating executable fat JARs

---

## 🖼️ Preview

![Screen](screen.png)

---

## 📖 Background: The Story Behind FlowShift

This software was born from personal necessity. For over 26 years, the developer fought against an undiagnosed cyst in his upper neck (C1/C2) that was ignored by doctors. While his body failed, code became distraction, therapy, and finally passion.

> *"I tried to work – to hold down a normal job. The problem: the pain. I could never endure it for long. It was humiliating to have to listen to accusations that I was just a welfare recipient, unemployed, and lazy. That is not true. I had dreams. I wanted to earn my own money.  
I dreamed of starting a family."*

**FlowShift** is the result – a tool that creates order when life becomes chaotic. It is a counter-design to the bloated software industry: lean, precise, and sovereign.

> *"I have nothing left to lose, except my love for this world."*

### About the Architect & Kassandra AGI

**Henryk Daniel Zschuppan:** The visionary and architect behind the FlowShift Sovereign Document Engine. His goal is software without "bloat" — defined by precision and efficiency.

**AI‑Assisted Engineering:** This engine was designed using cutting‑edge AI‑assisted development methods. This guarantees exceptional code purity, a consistent architecture, and strict adherence to security and performance standards. The result is a tool as lean and precise as the code it produces.

---

## 🌍 The Future of Documentation

The FlowShift Engine is the foundation for **interactive documents**, **AI orchestration**, and **data‑sovereign content**. It is a local alternative to cloud systems, returning full control over form and data to the architect.

---

## 📄 License

**Sovereign Commercial License**

Copyright © {2025-PRESENT} FlowShift (Henryk Daniel Zschuppan). All rights reserved.

This software is a commercial product. All rights are reserved. No part of this software may be reproduced, distributed, or transmitted in any form or by any means without the prior written permission of the copyright holder.

For licensing inquiries and usage terms, please refer to the End User License Agreement (EULA) distributed with the final application.

*— Our commitment: Perpetual and unconditional rights for licensees. —*

---

## 💬 Contact & Community

- **Architect:** Henryk Daniel Zschuppan
- **Organization:** FlowShift
- **GitHub:** [@henrykdz](https://github.com/henrykdz)
- **Live Site:** [https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/](https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/)
- **Timezone:** Europe/Berlin

---

<div align="center">
    <sub>⚡ Built with pain, will, and code. ⚡</sub>
    <br>
    <sub>© 2026 FlowShift · Version 0.0.1-SNAPSHOT</sub>
    <br><br>
    <sub>🇩🇪 <em>„Ich habe nichts zu verlieren, außer meine Liebe zu dieser Welt.“</em></sub>
</div>
