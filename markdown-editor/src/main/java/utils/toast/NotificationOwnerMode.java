package utils.toast;

public enum NotificationOwnerMode {
    // Display options for OwnerMode ChoiceBox

	DESKTOP("Desktop (Global)"), // Verweist auf den Sprachschlüssel
	MAIN_WINDOW("Main Window Relative"); // Verweist auf den Sprachschlüssel

	private final String displayKey;

	NotificationOwnerMode(String displayKey) {
		this.displayKey = displayKey;
	}

	public String getDisplayString() {
		return displayKey; // Holt den lokalisierten String
	}

	public static NotificationOwnerMode fromString(String internalValue) {
		if ("MAIN_WINDOW".equalsIgnoreCase(internalValue)) {
			return MAIN_WINDOW;
		}
		return DESKTOP; // Default
	}

	public String getInternalValue() {
		return this.name(); // Gibt "DESKTOP" oder "MAIN_WINDOW" zurück
	}
}