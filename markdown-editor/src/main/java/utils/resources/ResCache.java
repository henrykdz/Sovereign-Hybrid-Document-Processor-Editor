package utils.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResCache<T> {
	public static final Path                    FILE_SEPARATOR = Paths.get(System.getProperty("file.separator"));
	protected final Map<ResKey<T>, ResEntry<T>> map            = new ConcurrentHashMap<>();
	private final Path                          resourceDirectoryPath;

	public ResCache() {
		this.resourceDirectoryPath = null;
	}

	public ResEntry<T> get(ResKey<T> key) {
		return map.get(key);
	}

	public List<ResEntry<T>> get(List<ResKey<T>> keys) {
		return keys.stream().map(this::get).toList();
	}

	public void cache(ResEntry<T> entry) {
		if (map.containsKey(entry.getKey())) {
			return;
		}

		map.put(entry.getKey(), entry);
	}

	public void clear() {
		map.clear();
	}

	public boolean isCached(ResKey<T> key) {
		return map.containsKey(key);
	}

	public final Path getResourceDirectory() {
		return resourceDirectoryPath;
	}

	protected Path getResolvedResourceFilePath(ResKey<T> key) {
		return resourceDirectoryPath.resolve(key.getFileName());
	}

	protected byte[] readAllBytes(ResKey<T> key) throws IOException {
		return Files.readAllBytes(getResolvedResourceFilePath(key));
	}

	public boolean exists(ResKey<T> key) {
		return Files.exists(getResolvedResourceFilePath(key));
	}

	public List<ResKey<T>> listFileKeys() {
		List<ResKey<T>> keys = new ArrayList<>();
		File[] files = getResourceDirectory().toFile().listFiles();

		for (File file : files) {
			ResKey<T> key = new ResKey<>(file.getName());
			if (!file.isDirectory()) {
				keys.add(key);
			}
		}

		return keys;
	}

	public List<ResEntry<T>> getAll() {
		return new ArrayList<>(map.values());
	}

	public int size() {
		return map.size();
	}
}