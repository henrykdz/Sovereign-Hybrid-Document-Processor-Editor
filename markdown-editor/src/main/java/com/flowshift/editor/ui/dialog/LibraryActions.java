package com.flowshift.editor.ui.dialog;

public interface LibraryActions {
    boolean onTemplateLoad(String templateName);
    boolean onTemplateSave(String templateName);
    void onBundleImport();
    void onBundleExport();
    boolean onTemplateDelete(String templateName);
    void onVaultDocumentOpen(String fileName);   // Öffnen eines Dokuments aus dem Vault
    void onVaultDocumentDelete(String fileName); // Löschen eines Dokuments aus dem Vault
}