---
company: "Generic Company "
logo: "https://cdn-icons-png.flaticon.com/512/2163/2163350.png"
author: Administrator
dept: Operations
title: "Daily Safety and Machine Inspection"
format: A4_PORTRAIT
paginate: true
version: 1.0
status: DRAFT
header: "Professional Sleek"
mTop: 12
mBot: 15
mLeft: 15
mRight: 15
---

<div class="document-header" style="margin-top: 0.0em;">
  <div class="document-title">
{{title}}
  </div>
  <div class="document-subtitle">
Document No.: SF-2024-001 | Rev.: 1.2 | Valid from: 2024-01-01
  </div>
</div>


<table class="solid-table">
  <tr>
    <td style="width: 33%;"><strong>Location:</strong></td>
    <td style="width: 35%;">
    <strong>Shift:</strong><br>
    <span class="checkbox-printed" style="margin-right: 0px !important;">□</span> Early
    <span class="checkbox-printed" style="margin-right: 0px !important;">□</span> Late
    <span class="checkbox-printed" style="margin-right: 0px !important;">□</span> Night
    </td>
    <td style="width: 30%; vertical-align: middle;"><strong>Date:</strong> {{date}}</td>
  </tr>
  <tr>
    <td><strong>Responsible Person:</strong></td>
    <td><strong>Department:</strong></td>
    <td style="vertical-align: middle;">
    <strong style="font-size: 12pt;">Time:</strong>
    <span style="font-size: 10pt;">________________ hrs</span>
    </td>
  </tr>
</table>

<div class="warning-box">
  <strong>⚠ SAFETY NOTICE:</strong> All inspection points must be checked before starting work.<br>
In case of defects, inform supervisor immediately and shut down the machine!
</div>

<h2 class="section-header">1. General Safety Inspections</h2>

<div class="check-grid">
  <h3>A. Work Environment</h3>
  <div class="check-item"><span class="checkbox-printed">□</span> Escape routes clear and marked</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Emergency exits fully accessible</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Floors clean and slip-resistant</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Lighting sufficient (> 300 Lux)</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Emergency shower/eye wash functional</div>
</div>

<div class="check-grid">
  <h3>B. Personal Protective Equipment (PPE)</h3>
  <div class="check-item"><span class="checkbox-printed">□</span> Safety goggles available and intact</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Hearing protection available</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Safety shoes worn</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Gloves (correct type) available</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Respiratory protection (if required) ready</div>
</div>

<div class="check-grid">
  <h3>C. Fire Protection</h3>
  <div class="check-item"><span class="checkbox-printed">□</span> Fire extinguishers accessible and inspected</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Fire doors close automatically</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Smoke detectors operational (test)</div>
</div>

<div class="check-grid">
  <h3>D. First Aid</h3>
  <div class="check-item"><span class="checkbox-printed">□</span> First aid kit complete and inspected</div>
  <div class="check-item"><span class="checkbox-printed">□</span> Emergency numbers up-to-date and visible</div>
  <div class="check-item"><span class="checkbox-printed">□</span> First aid logbook available and accessible</div>
</div>


<h2 class="section-header">2. Machine-Specific Inspections</h2>
<table class="solid-table">
  <thead>
    <tr>
      <th style="width: 35%;">Machine/Equipment</th>
      <th style="width: 25%;">Status</th>
      <th style="width: 40%;">Comments/Defects</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>CNC Milling Machine #1</td>
      <td>
      <span class="checkbox-printed">□</span> OK
      <span class="checkbox-printed">□</span> Fault<br>
      <span class="checkbox-printed">□</span> Out of Service
      </td>
      <td></td>
    </tr>
    <tr>
      <td>Press System HS-300</td>
      <td>
      <span class="checkbox-printed">□</span> OK
      <span class="checkbox-printed">□</span> Fault<br>
      <span class="checkbox-printed">□</span> Out of Service
      </td>
      <td></td>
    </tr>
    <tr>
      <td>Conveyor Belt System B</td>
      <td>
      <span class="checkbox-printed">□</span> OK
      <span class="checkbox-printed">□</span> Fault<br>
      <span class="checkbox-printed">□</span> Out of Service
      </td>
      <td></td>
    </tr>
    <tr>
      <td><em>Other:</em></td>
      <td>
      <span class="checkbox-printed">□</span> OK
      <span class="checkbox-printed">□</span> Fault<br>
      <span class="checkbox-printed">□</span> Out of Service
      </td>
      <td></td>
    </tr>
  </tbody>
</table>

<div class="no-print-break">
  <h3 class="section-subheader">Special Occurrences/Defects</h3>
  <div style="border: 1pt solid #000; min-height: 30mm; padding: 2mm; margin-top: 0mm;">
    <em>Enter special occurrences, malfunctions, or safety defects here:</em>
    <br><br>____________________________________________________________________________________________________________
    <br><br>____________________________________________________________________________________________________________
    <br><br>____________________________________________________________________________________________________________
  </div>
</div>

<div class="signature-area">
  <h3 class="section-subheader">Confirmation and Signatures</h3>
  <div class="signature-grid">
    <div>
      <strong>Performed by (Employee):</strong>
      <div class="signature-field"></div>
      <div class="signature-label">Name, Date, Signature</div>
    </div>
    <div>
      <strong>Checked by (Supervisor/Foreman):</strong>
      <div class="signature-field"></div>
      <div class="signature-label">Name, Date, Signature</div>
    </div>
  </div>
</div>

<div class="qr-container">
  <div style="margin-bottom: 2mm;"><strong>Digital Log:</strong> Scan this code for archiving</div>
  <div class="qr-box">[QR-CODE]</div>
  <div style="margin-top: 2mm;">Log ID: SF-{{date}}-{{random}}</div>
</div>

<style>
/* GENERAL STYLES */
body {
    font-family: "Segoe UI", Arial, sans-serif; 
    font-size: 11pt; 
    line-height: 1.4; 
    margin: 0; 
    padding: 0; 
}

/* TABLE STYLES */
.solid-table {
    width: 100%; 
    border-collapse: collapse; 
    margin-top: -2mm; 
    margin-bottom: 2mm; 
    font-size: 12pt; 
    table-layout: fixed; 
}

.solid-table th,
.solid-table td {
    border: 1pt solid #000; 
    padding: 3mm; 
}

.solid-table th {
    background-color: #f2f2f4; 
    padding: 3mm; 

}

.solid-table tr {
    height: 16mm; /* Angemessene Höhe für Formularzeilen */
}

/* Oder noch besser: */
.solid-table td {
    padding-top: 1mm;
    padding-bottom: 3mm;
    vertical-align: top; /* Text oben ausrichten */
}

/* In dein CSS einfügen */
.solid-table .checkbox-printed {
    margin-right: 0px !important;
}

/* CSS hinzufügen */
.fill-space {
    min-width: 50mm; 
    display: inline-block; 
}

/* WARNING BOX */
.warning-box {
    background-color: #fff8e1; 
    border: 1pt solid #ffd54f; 
    border-radius: 2mm; 
    padding: 1mm; 
    margin: 1mm 0 5mm 0; 
    font-size: 10.5pt; 
}

/* HEADER STYLES */
.document-header {
    text-align: center; 
    margin-bottom: 6mm; 
}

.document-title {
    font-size: 16pt; 
    margin-top: 0mm; 
    margin-bottom: 1mm; 
}

.document-subtitle {
    font-size: 11pt; 
    margin-top: 1mm; 
    color: #666; 
}

/* SECTION STYLES */
.section-header {
    border-bottom: 1.4pt solid #000; 
    padding-bottom: 0mm; 
    margin-bottom: 4mm; 
}

.section-subheader {
    margin-top: 10mm; 
    margin-bottom: 3mm; 
}

/* CHECKBOX SYSTEM - UNVERÄNDERT LASSEN */
.checkbox-printed {
    font-family: "Segoe UI Symbol", "Apple Symbols", "DejaVu Sans",
    "Arial Unicode MS", sans-serif !important; 
    font-size: 14pt !important; 
    display: inline-block !important; 
    width: 1.5em !important; 
    height: 1.1em !important; 
    text-align: center !important; 
    vertical-align: middle !important; 
    margin-right: 16px !important; 
    line-height: 1.0 !important; 
    color: #111111 !important; /* Always dark for preview on white */
}

/* CHECKBOX GRID */
.check-grid {
    margin-bottom: 4mm; 
}

.check-item {
    margin-bottom: 2.5mm; 
    display: block; 
    line-height: 1.4; 
}

/* SIGNATURE AREA */
.signature-area {
    margin-top: 8mm; 
    page-break-inside: avoid; 
}

.signature-grid {
    display: grid; 
    grid-template-columns: 1fr 1fr; 
    gap: 10mm; 
    margin-top: 10mm; 
}

.signature-field {
    border-bottom: 1pt solid #000; 
    height: 12mm; 
    margin-top: 2mm; 
}

.signature-label {
    text-align: center; 
    font-size: 9pt; 
    margin-top: 1mm; 
    color: #666; 
}

/* QR CODE */
.qr-container {
    text-align: center; 
    margin-top: 8mm; 
    font-size: 9pt; 
    color: #666; 
}

.qr-box {
    border: 1pt dashed #ccc; 
    width: 25mm; 
    height: 25mm; 
    margin: 0 auto; 
    display: flex; 
    align-items: center; 
    justify-content: center; 
}

/* SOUVERÄNER TYPOGRAFIE-RESET */
h1, h2, h3, h4, h5, h6, p {
    margin-top: 0; /* Nicht "1", sondern "0" */
    margin-bottom: 0.5em;
}

/* Print Mode */
@media print {
    .checkbox-printed {
        font-size: 14pt !important; 
        color: #000000 !important; /* Always black when printing */
        -webkit-print-color-adjust: exact !important; 
        print-color-adjust: exact !important; 
    }

    .no-print-break {
        page-break-inside: avoid; 
    }

    .print-only {
        display: block; 
    }
}
</style>
