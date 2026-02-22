package utils.resources;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.function.Function;

import javafx.scene.image.Image;

/**
 * This class represents a resource entry in the ResourceManager.<br> A resource entry consists of a
 * ResKey and a ResObject.
 * @param <T> the type of resource object contained in the ResObject
 */
public class ResEntry<T> {

	// The ResKey object that uniquely identifies this resource entry
	private final ResKey<T> key;
	private final URI uri;
	// The ResObject that contains the resource object of this entry
	private T value;
	// TODO could be received via URL
	private final ResType type; // should be equal to MIME-Type which is generated in ResObject



	/**
	 * Constructs a new ResEntry object with the specified ResKey.
	 * @param key the ResKey object that uniquely identifies this resource entry
	 */
	public ResEntry(ResKey<T> key, URI uri, T resource) {
		this.key = key;
		this.uri = uri;
		this.type = key.getType();
		this.value = resource;
	}

	/**
	 * Returns the ResKey object that uniquely identifies this resource entry.
	 * @return the ResKey object that uniquely identifies this resource entry
	 */
	public ResKey<T> getKey() { return key; }

	public URI getURI() { return uri; }

	public ResType getType() { return type; }

	public String getMIMEType() {
		if (value == null) { return null; }
		return type.getDirectoryName();
	}

	/**
	 * Returns the ResObject that contains the resource object of this entry. If the ResObject has not yet
	 * been loaded, it will be loaded from the file specified by the ResKey.
	 * @return the ResObject that contains the resource object of this entry
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Loads the resource object from the file specified by the ResKey and returns it as a ResObject.
	 * @return a ResObject containing the resource object loaded from the file specified by the ResKey
	 */
//	private ResObject<T> loadValue() {
//		try (InputStream inputStream = url.openStream()) {
//			return loadInputStream(inputStream);
//		} catch (IOException e) {
//			// If an IOException occurs while loading the resource object, log the error and return null
//			Log.severe(ResEntry.class, "loadValue: " + key.getFileName(), e, true);
//			return null;
//		}
//	}

	/**
	 * Loads an Image from the specified InputStream and returns it as a ResObject.
	 * @param inputStream the InputStream containing the Image data
	 * @return a ResObject containing the loaded Image
	 * @throws IOException if an IOException occurs while reading from the InputStream
	 */
	public static ResObject<Image> loadImage(InputStream inputStream) throws IOException {
		byte[] data = inputStream.readAllBytes();
		Image image = new Image(inputStream);
		String mimeType = URLConnection.guessContentTypeFromStream(inputStream);
		return new ResObject<>(image, mimeType, data);
	}

	/**
	 * Loads a resource object of type T from the specified InputStream using the specified loader
	 * Function and returns it as a ResObject.
	 * @param inputStream the InputStream containing the resource object data
	 * @param loader      the Function used to load the resource object from the InputStream
	 * @param <T>         the type of resource object to load
	 * @return a ResObject containing the loaded resource object
	 * @throws IOException if an IOException occurs while reading from the InputStream
	 */
	public static <T> ResObject<T> load(InputStream inputStream, Function<InputStream, T> loader) throws IOException {
		byte[] data = inputStream.readAllBytes();
		T object = loader.apply(inputStream);
		String mimeType = URLConnection.guessContentTypeFromStream(inputStream);
		return new ResObject<>(object, mimeType, data);
	}


	/**
	 * A wrapper class that contains the byte array data of a resource, the resource object itself (of
	 * generic type T), and the mime type of the resource. Instances of this class are created by the
	 * load* methods in the ResEntry class.
	 * @param <T> the type of the resource object
	 */
	public static class ResObject<T> {
		private final byte[] data;
		private final T object;
		private final String mimeType;

		/**
		 * Creates a new ResObject with the given resource object, mime type, and byte array data.
		 * @param object   the resource object
		 * @param mimeType the mime type of the resource
		 * @param data     the byte array data of the resource
		 */
		public ResObject(T object, String mimeType, byte[] data) {
			this.object = object;
			this.mimeType = mimeType;
			this.data = data;
		}

		/**
		 * Returns the byte array data of the resource.
		 * @return the byte array data of the resource
		 */
		public byte[] getData() { return data; }

		/**
		 * Returns the resource object.
		 * @return the resource object
		 */
		public T getResource() { return object; }

		/**
		 * Returns the mime type of the resource.
		 * @return the mime type of the resource
		 */
		public String getMimeType() { return mimeType; }
	}
}