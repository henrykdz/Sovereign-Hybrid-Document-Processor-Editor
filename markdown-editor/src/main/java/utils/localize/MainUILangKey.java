package utils.localize;


/**
 * Encapsulates all localizable string keys for the main UI of the WebmarksApp, including core dialogs like the "unsaved changes" prompt.
 * 
 * Convention: Use escaped double quotes (e.g., \"MyFile.txt\") for filenames and other placeholders within strings to ensure visual consistency.
 */
public enum MainUILangKey implements LocalizableKey {

    // =========================================================================
    // Unsaved Changes Dialog (from WebmarksApp.promptToSaveAndCloseIfModified)
    // =========================================================================
	LK_DIALOG_UNSAVED_CHANGES_TITLE("dialog.unsaved_changes.title", "Unsaved Changes"),
	LK_DIALOG_UNSAVED_CHANGES_MESSAGE("dialog.unsaved_changes.message", "Your changes will be lost if you don't save them."),

    // --- Context-specific Headers ---
	LK_F_HEADER_UNSAVED_CHANGES_DEFAULT("header.unsaved_changes.default", "Do you want to save the changes to \"%s\"?"),
	LK_F_HEADER_UNSAVED_CHANGES_OPENING("header.unsaved_changes.opening", "Save changes to \"%s\" before opening another archive?"),
	LK_F_HEADER_UNSAVED_CHANGES_CREATING("header.unsaved_changes.creating", "Save changes to \"%s\" before creating a new archive?"),

	/** A generic checkbox text for all save dialogs to remember the 'autosave' choice. */
	LK_DIALOG_CHECKBOX_REMEMBER_AUTOSAVE("dialog.checkbox.remember_autosave", "Always save changes automatically in the future.\n(can be changed in Preferences)"),

    // --- Context-specific Button Texts ---
	LK_BUTTON_SAVE("button.save", "Save"),
	LK_BUTTON_DONT_SAVE("button.dont_save", "Don't Save"),
	LK_BUTTON_CANCEL("button.cancel", "Cancel"),

    // =========================================================================
    // Exit Application Dialog (Specific texts)
    // =========================================================================
	LK_EXIT_DIALOG_TITLE("exit_dialog.title", "Exit Application"),
	LK_EXIT_DIALOG_HEADER("exit_dialog.header", "Unsaved changes detected."),
	LK_EXIT_DIALOG_MESSAGE("exit_dialog.message", "Do you want to save your changes before exiting?"),
	LK_BUTTON_SAVE_AND_EXIT("exit_dialog.button.save_and_exit", "Save and Exit"),
	LK_BUTTON_EXIT_WITHOUT_SAVING("exit_dialog.button.exit_without_saving", "Exit Without Saving"),

    // =========================================================================
    // "Save As" Toast Notification
    // =========================================================================
	LK_TOAST_SAVED_AS_TITLE("toast.title.savedAs", "Saved As"),
	LK_F_TOAST_SAVED_AS_MESSAGE("toast.message.savedAsActiveFile", "The newly saved file %s is now the active archive."),

    // =========================================================================
    // Autosave Toast Notification (NEU)
    // =========================================================================
	LK_TOAST_AUTOSAVE_TITLE("toast.title.autosave", "Autosave"),
	LK_F_TOAST_AUTOSAVE_ARCHIVE("toast.message.autosave", "Archive '%s' was saved automatically."),

    // =========================================================================
    // Export Toast Notifications
    // =========================================================================
	LK_TOAST_EXPORT_SUCCESS_TITLE("toast.title.exportSuccess", "Export Successful"),
	LK_TOAST_EXPORT_SUCCESS_MESSAGE("toast.message.exportSuccess", "Archive exported to HTML."),
	LK_TOAST_EXPORT_FAILED_TITLE("toast.title.exportFailed", "Export Failed"),
	LK_TOAST_EXPORT_FAILED_MESSAGE("toast.message.exportFailed", "Could not write HTML file."),
	LK_BUTTON_OPEN_FOLDER("button.openFolder", "Open Folder"),
	LK_BUTTON_OPEN_FILE("button.openFile", "Open File"),

    // --- Lizenz-Registrierungs-Dialog ---
	LK_DLG_LICENSE_TITLE("dlg.license.title", "Product Registration"),
	LK_DLG_LICENSE_HEADER("dlg.license.header", "Webmarks License"),
	LK_DLG_LICENSE_CONTENT("dlg.license.content", "Your version is currently unlicensed. Please choose an option:"),
	LK_BTN_LICENSE_BUY("btn.license.buy", "Buy License"),
	LK_BTN_LICENSE_ENTER_KEY("btn.license.enterKey", "Enter Key"),
	LK_BTN_LICENSE_SAVE("btn.license.save", "Save Key"),
	
	LK_DLG_LICENSE_INPUT_HEADER("dlg.license.input_header", "Please enter your purchased license key:"),
	LK_DLG_LICENSE_INPUT_PROMPT("dlg.license.input_prompt", "License Key:"),
	LK_TOAST_LICENSE_SAVED_RESTART("toast.license_saved_restart", "Key saved. Please restart application to activate.");

	private final String key;
	private final String defaultText;

	MainUILangKey(String key, String defaultText) {
		this.key = key;
		this.defaultText = defaultText;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getDefaultText() {
		return defaultText;
	}
}