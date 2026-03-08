# FlowShift - Sovereign Hybrid Document Engine

<div align="center">
    <strong>⚡ Precision · Sovereignty · Zero Compromise ⚡</strong>
    <br><br>
    <a href="https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/">🌐 Live Demo</a> •
    <a href="#features">✨ Features</a> •
    <a href="#installation">📦 Installation</a> •
    <a href="#architecture">🏗️ Architecture</a> •
    <a href="#background">📖 Background</a>
</div>

<br>

<div align="center">
    
![Early Access](https://img.shields.io/badge/status-early%20access--private-blue)
![Preview](https://img.shields.io/badge/preview-Q3%202026-orange)
![Java](https://img.shields.io/badge/Java-23-blue?logo=java)
![License](https://img.shields.io/badge/license-Commercial-red)

</div>

---

## 🚀 Introduction: What is the Sovereign Document Engine?

The **FlowShift Sovereign Document Engine** is a high-performance **Hybrid Document Builder**. It transcends the limits of traditional editors by seamlessly unifying **Markdown**, **HTML5/CSS3**, and **YAML** into a single, cohesive workflow. 

It is a **precision instrument** for creating, managing, and publishing documents, combining the power of native desktop applications with the flexibility of web technologies and advanced, cutting-edge logic.

Developed by architect **Henryk Daniel Zschuppan**, this engine is a counter‑design to the complex, resource‑hungry, and often unreliable software of the current era. It is built for professionals who demand absolute control over content, form, and the physical geometry of their documents.

> *"FlowShift transforms complex ideas into perfectly formatted documents. Our hybrid engine combines the flexibility of Markdown, HTML/CSS, and YAML with the precision of ISO standards."*

---

## 🎯 Target Audience

<div align="left">

<table>
  <tr>
    <td width="200">📋 <strong>Protocol Officers</strong></td>
    <td width="200">🔬 <strong>Scientists & Researchers</strong></td>
  </tr>
  <tr>
    <td>🏭 <strong>Industry & Manufacturing</strong></td>
    <td>🏢 <strong>Enterprises & Corporations</strong></td>
  </tr>
  <tr>
    <td>⚖️ <strong>Legal Professionals</strong></td>
    <td>🏥 <strong>Medical & Healthcare</strong></td>
  </tr>
  <tr>
    <td>🏦 <strong>Finance & Insurance</strong></td>
    <td>📝 <strong>Technical Writers</strong></td>
  </tr>
</table>

</div>

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

<h2 id="features">✨ Key Features: What the Engine Delivers</h2>

### Unrivalled Editing Experience

| Feature | Description |
|---------|-------------|
| **Flicker‑free live rendering** | Thanks to the `Sovereign Swap` mechanism, the preview updates in real time without any visual flicker. |
| **Scroll invariance** | The focus remains stable at the desired position in the preview while you type. |
| **Bidirectional navigation** | A click in the preview jumps the cursor precisely to the corresponding spot in the Markdown source. |
| **Intelligent error diagnostics** | The `ErrorTooltipManager` visualises linter errors directly in the status bar. |

### Layout & Design Sovereignty

| Feature | Description |
|---------|-------------|
| **CSS Forge** | A dedicated code editor enables live styling directly in the document flow. |
| **Precise pagination** | The engine converts documents exactly into physical pages (A4, Letter). |
| **Neutral start** | Documents begin without any enforced formatting. Margins and pagination are defined explicitly in YAML. |

### Complete Feature List

| Feature | Description |
|---------|-------------|
| ✅ **Formatter** | Automatic formatting of Markdown, HTML and CSS |
| ✅ **Linter** | Real-time error detection with visual feedback |
| ✅ **Syntax Highlighter** | Color-highlighted code for better readability |
| ✅ **Custom Placeholders** | Custom placeholders for templates and mail merges |
| ✅ **ISO-compliant Export** | A4, Letter, Web – everything possible |

---

<h2 id="architecture">🏗️ Technical Architecture: Under the Hood</h2>

### High-Level Overview

- **JavaFX Desktop Application** – Native performance with modern UI
- **Hybrid Rendering Engine** – Markdown → HTML → WebView with zero flicker
- **Real-time Source Mapping** – Character-accurate bidirectional navigation
- **Modular Design** – Clean separation of concerns for maintainability

<details>
<summary><b>🔧 Complete Technology Stack</b> · <span style="color: #00d1ff; border-bottom: 1px dashed #00d1ff;">[ Developer Details ▼ ]</span></summary>
<br>

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

</details>

### Hybrid Rendering Approach

The engine uses **JavaFX & WebKit**, combined with the `Sovereign Swap`. Instead of reloading the WebView, content is injected via `innerHTML`, guaranteeing absolute smoothness and zero flicker.

### The Sovereign Bridge (Source Mapping)

The `SovereignSourceMapper` injects a unique `data-fsid` into every HTML element. An internal map links this ID to the exact character offset in the source text, enabling precise bidirectional navigation.

### Navigation & Anchor Logic

The FlowShift Engine ensures your documents remain fully navigable in export and print:

- **Fully automatic:** Headings automatically receive unique IDs for tables of contents
- **Sovereign override:** Use the syntax `{#custom-id}` directly after a heading to override the automation
- **HTML integrity:** Pure HTML tags remain untouched, giving you full control

---

<h2 id="installation">🔧 Installation</h2>

### Prerequisites

- **Java:** JavaSE-23 (Zulu 23 or compatible) – JavaFX is bundled
- **Maven:** 3.6 or higher
- **IDE:** Eclipse 2025+ 

## 🔧 Availability

**FlowShift is a commercial product.**  
The source code is private and not publicly available.

For licensing inquiries, demonstration requests, or partnership opportunities, please contact:

📧 **Email:** [coming soon]  
🌐 **Website:** [https://flowshift.dev](https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/)

---

*Enterprise licenses and evaluation versions are available upon request.*

---

## 📬 Early Access 2026

<div align="left">
    <br>
    <strong>🚀 FlowShift is in active development</strong>
    <br><br>
    
| | |
|---|---|
| **📊 Status:** | 🔧 Private Development · Preview Q3 2026 |
| **📧 Contact:** | [h.zschuppan@aol.com](mailto:h.zschuppan@aol.com?subject=Early%20Access%20FlowShift) |
| **👥 Program:** | By request · Limited spots |
    
</div>

<br>
<div align="center">
    <sub>💡 Interested in Early Access? Just send an email with subject "Early Access".</sub>
    <br><br>
</div>

---

## 🖼️ Preview

![Screen](assets/images/screen.png)

---

<h2 id="background">📖 The Story Behind FlowShift</h2>

This software was born from personal necessity. For over 26 years, the architect fought against an undiagnosed spinal condition (C1/C2) that was repeatedly dismissed by doctors. When his body couldn't, his mind did. Code became distraction, therapy, and finally passion

**FlowShift** is the result – a tool that creates **order when life becomes chaotic**. It is a conscious counter-design to the bloated software industry: **lean, precise, and sovereign**.

> *I've nothing left to prove – only something left to build.*

### About the Architect

The vision behind FlowShift comes from a place of deep personal experience. The architect's goal is software without "bloat", defined by **precision and efficiency**.

**AI‑Assisted Engineering:** FlowShift was designed using cutting-edge AI-assisted development methods, guaranteeing exceptional code purity, consistent architecture, and strict adherence to security standards. The result is a tool as **lean and precise** as the code it produces.

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
- **Email:** [h.zschuppan@aol.com](mailto:h.zschuppan@aol.com)
- **Organization:** FlowShift
- **GitHub:** [@henrykdz](https://github.com/henrykdz)
- **Live Site:** [https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/](https://henrykdz.github.io/Sovereign-Hybrid-Document-Processor-Editor/)
- **Timezone:** Europe/Berlin

---

<div align="center">
    <sub>⚡ Precision · Sovereignty · Zero Compromise ⚡</sub>
    <br>
    <sub>© 2026 FlowShift · Early Access Build</sub>
</div>
