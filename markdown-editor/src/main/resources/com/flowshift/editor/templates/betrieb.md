---
format: A4_PORTRAIT
documentTitle: "Inspection Document"
headerStyle: "Professional Clean"
showHeader: true
authorName: Administrator
version: 1.0
status: DRAFT
companyName: "Generic Company"
department: Operations
company_name: "Sample Company"
document_title: "Inspection Document"
legalNote: "Confidential Property"
logoUrl: ""
---
<style>
  /* CHECKBOX SYSTEM */
  .checkbox-printed {
  font-family: "Segoe UI Symbol", "Apple Symbols", "DejaVu Sans",
  "Arial Unicode MS", sans-serif !important;
  font-size: 16px !important;
  display: inline-block !important;
  width: 1.2em !important;
  height: 1.2em !important;
  text-align: center !important;
  vertical-align: middle !important;
  margin-right: 8px !important;
  line-height: 1.2 !important;
  color: #111111 !important; /* Always dark for preview on white */
  }
  /* Print Mode */
  @media print {
  .checkbox-printed {
  font-size: 14pt !important;
  color: #000000 !important; /* Always black when printing */
  -webkit-print-color-adjust: exact !important;
  print-color-adjust: exact !important;
  }
  }
</style>

<div class="print-container">
  <div style="text-align: center; margin-bottom: 10mm;">
    <div style="font-size: 14pt; margin-top: 2mm;">
      Daily Safety and Machine Inspection
    </div>
    <div style="font-size: 11pt; margin-top: 1mm; color: #666;">
      Document No.: SF-2024-001 | Rev.: 1.2 | Valid from: 2024-01-01
    </div>
  </div>
  <table style="width: 100%; border-collapse: collapse; margin-bottom: 8mm; font-size: 10pt;">
    <tr>
      <td style="border: 1pt solid #000; padding: 2mm; width: 33%;">
        <strong>Location:</strong> _______________________
      </td>
      <td style="border: 1pt solid #000; padding: 2mm; width: 33%;">
        <strong>Shift:</strong> □ Early □ Late □ Night
      </td>
      <td style="border: 1pt solid #000; padding: 2mm; width: 34%;">
        <strong>Date:</strong> {{date}}
      </td>
    </tr>
    <tr>
      <td style="border: 1pt solid #000; padding: 2mm;">
        <strong>Responsible Person:</strong> _______________________
      </td>
      <td style="border: 1pt solid #000; padding: 2mm;">
        <strong>Department:</strong> _______________________
      </td>
      <td style="border: 1pt solid #000; padding: 2mm;">
        <strong>Time:</strong> __________ hrs
      </td>
    </tr>
  </table>
  <div class="warning-box">
    <strong>⚠️ SAFETY NOTICE:</strong> All inspection points must be checked before starting work.
    <br>In case of defects, inform supervisor immediately and shut down the machine!
  </div>
  <h2 style="border-bottom: 1.5pt solid #000; padding-bottom: 2mm; margin-top: 8mm;">
    1. General Safety Inspections
  </h2>
  <div class="check-grid">
    <div>
      <h3>A. Work Environment</h3>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Escape routes clear and marked
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Emergency exits fully accessible
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Floors clean and slip-resistant
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Lighting sufficient (> 300 Lux)
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Emergency shower/eye wash functional
      </div>
      <h3 style="margin-top: 6mm;">C. Fire Protection</h3>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Fire extinguishers accessible and inspected
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Fire doors close automatically
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Smoke detectors operational (test)
      </div>
    </div>
    <div>
      <h3>B. Personal Protective Equipment (PPE)</h3>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Safety goggles available and intact
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Hearing protection available
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Safety shoes worn
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Gloves (correct type) available
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Respiratory protection (if required) ready
      </div>
      <br><br><br>
      <h3 style="margin-top: 6mm;">D. First Aid</h3>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> First aid kit complete and inspected
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> Emergency numbers up-to-date and visible
      </div>
      <div style="margin-bottom: 3mm;">
        <span class="checkbox-printed">□</span> First aid logbook available and accessible
      </div>
    </div>
  </div>
  <h2 style="border-bottom: 1.5pt solid #000; padding-bottom: 2mm; margin-top: 8mm;">
    2. Machine-Specific Inspections
  </h2>
  <table style="width: 100%; border-collapse: collapse; margin-top: 4mm;">
    <thead>
      <tr style="background-color: #f0f0f0;">
        <th style="border: 1pt solid #000; padding: 3mm; text-align: left; width: 50%;">
          Machine / Equipment
        </th>
        <th style="border: 1pt solid #000; padding: 3mm; text-align: left; width: 20%;">
          Status
        </th>
        <th style="border: 1pt solid #000; padding: 3mm; text-align: left; width: 30%;">
          Comments / Defects
        </th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td style="border: 1pt solid #000; padding: 2mm;">
          CNC Milling Machine #1
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          □ OK □ Fault □ Out of Service
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          _______________________
        </td>
      </tr>
      <tr>
        <td style="border: 1pt solid #000; padding: 2mm;">
          Press System HS-300
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          □ OK □ Fault □ Out of Service
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          _______________________
        </td>
      </tr>
      <tr>
        <td style="border: 1pt solid #000; padding: 2mm;">
          Conveyor Belt System B
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          □ OK □ Fault □ Out of Service
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          _______________________
        </td>
      </tr>
      <tr>
        <td style="border: 1pt solid #000; padding: 2mm;">
          <em>Other: _______________________</em>
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          □ OK □ Fault □ Out of Service
        </td>
        <td style="border: 1pt solid #000; padding: 2mm;">
          _______________________
        </td>
      </tr>
    </tbody>
  </table>
  <div style="margin-top: 8mm;">
    <h3>Special Occurrences / Defects</h3>
    <div style="border: 1pt solid #000; min-height: 30mm; padding: 3mm; margin-top: 2mm;">
      <em>Enter special occurrences, malfunctions, or safety defects here:</em>
      <br><br>
      ________________________________________________________
      <br><br>
      ________________________________________________________
      <br><br>
      ________________________________________________________
    </div>
  </div>
  <div style="margin-top: 8mm; page-break-inside: avoid;">
    <h3>Confirmation and Signatures</h3>
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10mm; margin-top: 8mm;">
      <div>
        <strong>Performed by (Employee):</strong>
        <div class="signature-field"></div>
        <div style="text-align: center; font-size: 9pt; margin-top: 1mm;">
          Name, Date, Signature
        </div>
      </div>
      <div>
        <strong>Checked by (Supervisor/Foreman):</strong>
        <div class="signature-field"></div>
        <div style="text-align: center; font-size: 9pt; margin-top: 1mm;">
          Name, Date, Signature
        </div>
      </div>
    </div>
  </div>
  <div style="text-align: center; margin-top: 8mm; font-size: 9pt; color: #666;">
    <div style="margin-bottom: 2mm;">
      <strong>Digital Log:</strong> Scan this code for archiving
    </div>
    <div style="border: 1pt dashed #ccc; width: 25mm; height: 25mm; margin: 0 auto; display: flex; align-items: center; justify-content: center;">
      [QR-CODE]
    </div>
    <div style="margin-top: 2mm;">
      Log ID: SF-{{date}}-{{random}}
    </div>
  </div>
</div>