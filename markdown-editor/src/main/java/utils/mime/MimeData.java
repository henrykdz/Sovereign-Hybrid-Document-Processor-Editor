package utils.mime;

public class MimeData {
	private static final String CLIPBOARD_ACCESS_TYPE = "clipboard";

	private String  mimeType;
	private String  accessType;
	private int     index;
	private String  name;
	private boolean inMemoryFile; // Flag for in-memory files, e.g., clipboard
	private boolean rendered;     // Flag, ob der Taint gesetzt wurde

	public MimeData(String mimeType) {
		this.mimeType = mimeType;
		this.rendered = false; // Standardmäßig nicht gerendert
	}

	// Getter und Setter Methoden
	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
		// Setze inMemoryFile, wenn der accessType auf "clipboard" steht
		if (CLIPBOARD_ACCESS_TYPE.equalsIgnoreCase(accessType)) {
			this.inMemoryFile = true;
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isUrlName() {
		return name != null && name.toLowerCase().endsWith(".url");
	}

	public boolean isInMemoryFile() {
		return inMemoryFile;
	}

	// Getter für 'rendered' Flag
	public boolean isRendered() {
		return rendered;
	}

	// Setter für 'rendered' Flag
	public void setRendered(boolean rendered) {
		this.rendered = rendered;
	}

	@Override
	public String toString() {
		return "MimeData{" + "rawMime='" + mimeType + '\'' + ", accessType='" + accessType + '\'' + ", index=" + index + ", name='" + name + '\'' + '}';
	}
}