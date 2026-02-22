package utils.network;

public enum UserAgentMode {
	DEFAULT_JAVA("Default (Java Runtime)"), // String for display in ComboBox
	BROWSER_SIMULATION("Browser Simulation (Chrome/Opera)"),
	DISABLED("Disabled (Send No User-Agent)"),
	CUSTOM("Custom..."); // Display "Custom..." but internally uses the stored custom string

	private final String displayString;

	UserAgentMode(String display) {
		this.displayString = display;
	}

	@Override
	public String toString() {
		// This determines what's shown in the ComboBox
		return displayString;
	}
}
