package utils.network.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Objects;

import utils.general.StringUtils;
import utils.logging.Log;

/**
 * Represents an immutable proxy configuration.
 *
 * <p>
 * Instances are created using the nested {@link Builder}. This class holds configuration details like host, port, type, and optional authentication credentials. It is thread-safe.
 * </p>
 *
 * <p>
 * Example Usage:
 * 
 * <pre>{@code
 * ProxyConfig socksProxy = new ProxyConfig.Builder("proxy.example.com", 1080, ProxyConfig.ProxyType.SOCKS5).name("Secure SOCKS").requiresAuthentication(true, "user", "pass123")
 *         .build();
 *
 * Proxy javaNetProxy = socksProxy.getProxy(); // Get the underlying java.net.Proxy
 * InetSocketAddress address = socksProxy.getSocketAddress();
 * }</pre>
 *
 * @see ProxyTester For performing network tests using this configuration.
 */
public final class ProxyConfig { // final class - cannot be subclassed

	/**
	 * Enumeration of supported proxy types, mapping to java.net.Proxy.Type.
	 */
	public enum ProxyType {
		HTTP("HTTP", 80, Proxy.Type.HTTP),
		HTTPS("HTTPS", 443, Proxy.Type.HTTP), // Uses HTTP type in java.net.Proxy for CONNECT tunneling
		SOCKS4("SOCKS4", 1080, Proxy.Type.SOCKS),
		SOCKS5("SOCKS5", 1080, Proxy.Type.SOCKS);

		private final String     name;
		private final int        defaultPort;
		private final Proxy.Type javaNetProxyType;

		ProxyType(String name, int defaultPort, Proxy.Type proxyType) {
			this.name = name;
			this.defaultPort = defaultPort;
			this.javaNetProxyType = proxyType;
		}

		/** User-friendly name (e.g., "HTTP"). */
		public String getName() {
			return name;
		}

		/** Common default port for this type. */
		public int getDefaultPort() {
			return defaultPort;
		}

		/** The corresponding {@link java.net.Proxy.Type}. */
		public Proxy.Type toJavaNetProxyType() {
			return javaNetProxyType;
		}

		/** Converts from java.net.Proxy.Type back to this enum. */
		public static ProxyType fromJavaNetProxyType(Proxy.Type type) {
			for (ProxyType proxyType : values()) {
				// Handle HTTPS specifically if needed, though it maps to HTTP type
				if (proxyType.javaNetProxyType == type) {
					// Simple mapping might return HTTP for both HTTP/HTTPS, refine if distinction matters
					return proxyType;
				}
			}
			throw new IllegalArgumentException("Unsupported or unknown java.net.Proxy.Type: " + type);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	// --- Immutable Fields ---
	private final String            name;
	private final String            host;
	private final int               port;
	private final ProxyType         type;
	private final boolean           requiresAuth;
	private final String            username;      // Null if !requiresAuth
	private final String            password;      // Null if !requiresAuth
	private final InetSocketAddress socketAddress; // Eagerly created, InetSocketAddress is immutable

	// --- Lazy-initialized, thread-safe proxy instance ---
	// transient: Exclude from default serialization if needed
	// volatile: Required for correct double-checked locking visibility
	private volatile transient Proxy proxyInstance;

	/** Private constructor, use the Builder. */
	private ProxyConfig(Builder builder) {
		this.name = Objects.requireNonNull(builder.name, "Name cannot be null");
		this.host = Objects.requireNonNull(builder.host, "Host cannot be null");
		this.port = builder.port; // Validated in Builder
		this.type = Objects.requireNonNull(builder.type, "ProxyType cannot be null");
		this.requiresAuth = builder.requiresAuth;
		this.username = builder.username; // Null if auth not required by builder logic
		this.password = builder.password; // Null if auth not required by builder logic

		// Final validation of authentication consistency
		if (this.requiresAuth && (StringUtils.isBlank(this.username) || StringUtils.isBlank(this.password))) {
			throw new IllegalArgumentException("Username and Password are required when authentication is enabled.");
		}
		// Log warning if credentials provided but auth is off (builder might enforce this too)
		if (!this.requiresAuth && (!StringUtils.isBlank(this.username) || !StringUtils.isBlank(this.password))) {
			Log.warn("Username/Password provided for Proxy '" + this.name + "' but requiresAuth is false. Credentials will be ignored.");
			// Note: We keep the values as set by the builder, but they shouldn't be used.
		}

		// Eagerly create the immutable socket address
		this.socketAddress = new InetSocketAddress(this.host, this.port);
	}

	// --- Getters ---
	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public ProxyType getType() {
		return type;
	}

	public boolean isRequiresAuth() {
		return requiresAuth;
	}

	public String getUsername() {
		return username;
	} // Returns null if auth not required

	public String getPassword() {
		return password;
	} // Returns null if auth not required

	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}

	/**
	 * Gets the underlying {@link java.net.Proxy} instance, creating it lazily in a thread-safe manner if it hasn't been created yet.
	 *
	 * @return The configured {@link Proxy} instance.
	 */
	public Proxy getProxy() {
		Proxy result = proxyInstance; // Read volatile field once
		if (result == null) {
			synchronized (this) { // Synchronize on the ProxyConfig instance
				result = proxyInstance; // Double-check check
				if (result == null) {
					proxyInstance = result = new Proxy(type.toJavaNetProxyType(), socketAddress);
				}
			}
		}
		return result;
	}

	/**
	 * Returns a formatted string suitable for display in UI selection lists. Includes the proxy name if available, followed by connection details. Format: "Name (TYPE@host:port)"
	 * or "TYPE@host:port" if no name
	 * 
	 * @return user-friendly display string
	 */
	public String toDisplayString() {
		return hasName() ? String.format("%s [%s]", name, toStringSimple()) : toStringSimple();
	}

	/**
	 * Returns whether this proxy has a non-empty name
	 */
	private boolean hasName() {
		return name != null && !name.isEmpty();
	}

	/**
	 * Returns a minimal string representation containing only type, host and port. Suitable for use as a cache key as it excludes authentication details. Format:
	 * "PROXY_TYPE@host:port" (e.g. "SOCKS@proxy.example.com:1080")
	 * 
	 * @return compact string representation with essential connection details
	 */
	public String toStringSimple() {
		return type.name() + "@" + host + ":" + port;
	}

	/**
	 * Returns a detailed string representation including all configuration fields. Format: "ProxyConfig{name='...', type=..., host='...', port=..., requiresAuth=...,
	 * username='...'}"
	 * 
	 * @return complete string representation with all configuration details
	 */
	@Override
	public String toString() {
		return "ProxyConfig{" + (name != null && !name.isEmpty() ? "name='" + name + "', " : "") + "type=" + type + ", host='" + host + '\'' + ", port=" + port + ", requiresAuth="
		        + requiresAuth + (requiresAuth ? ", username='" + username + "'" : "") + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProxyConfig that = (ProxyConfig) o;
		// Compare core configuration fields
		return port == that.port && requiresAuth == that.requiresAuth && Objects.equals(name, that.name) && Objects.equals(host, that.host) && type == that.type
		        && Objects.equals(username, that.username) && // Handles nulls correctly
		        Objects.equals(password, that.password); // Handles nulls correctly
	}

	@Override
	public int hashCode() {
		// Hash based on the same core fields used in equals()
		return Objects.hash(name, host, port, type, requiresAuth, username, password);
	}

	// --- Builder Class ---

	/**
	 * Builder for creating immutable {@link ProxyConfig} instances. Provides a fluent API for setting configuration properties.
	 */
	public static class Builder {
		// Required fields
		private final String    host;
		private final int       port;
		private final ProxyType type;

		// Optional fields with defaults
		private String  name         = "Unnamed Proxy";
		private boolean requiresAuth = false;
		private String  username     = null;
		private String  password     = null;

		/**
		 * Creates a new Builder with the mandatory proxy details.
		 *
		 * @param host The proxy hostname or IP address. Cannot be blank.
		 * @param port The proxy port number (1-65535).
		 * @param type The type of the proxy (HTTP, SOCKS, etc.). Cannot be null.
		 * @throws NullPointerException     if host or type is null.
		 * @throws IllegalArgumentException if host is blank or port is out of range.
		 */
		public Builder(String host, int port, ProxyType type) {
			if (StringUtils.isBlank(host)) {
				throw new IllegalArgumentException("Host cannot be null or blank.");
			}
			if (port < 1 || port > 65535) {
				throw new IllegalArgumentException("Port must be between 1 and 65535, but was: " + port);
			}
			this.host = host;
			this.port = port;
			this.type = Objects.requireNonNull(type, "ProxyType cannot be null.");
		}

		/**
		 * Sets the optional descriptive name for the proxy configuration. If the provided name is blank, the default name ("Unnamed Proxy") is retained.
		 *
		 * @param name The desired name.
		 * @return This Builder instance for chaining.
		 */
		public Builder name(String name) {
			if (StringUtils.isNotBlank(name)) {
				this.name = name.trim(); // Trim whitespace
			}
			return this;
		}

		/**
		 * Configures proxy authentication.
		 *
		 * @param requiresAuth Set to true if authentication is required.
		 * @param username     The username for authentication (required if requiresAuth is true).
		 * @param password     The password for authentication (required if requiresAuth is true).
		 * @return This Builder instance for chaining.
		 * @throws IllegalArgumentException if requiresAuth is true but username or password is blank.
		 */
		public Builder authentication(boolean requiresAuth, String username, String password) {
			this.requiresAuth = requiresAuth;
			if (requiresAuth) {
				if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
					throw new IllegalArgumentException("Username and Password cannot be blank when authentication is required.");
				}
				this.username = username;
				this.password = password;
			} else {
				// Clear credentials if authentication is disabled
				this.username = null;
				this.password = null;
			}
			return this;
		}

		/**
		 * Builds the immutable {@link ProxyConfig} instance from the configured properties. Performs final validation checks.
		 *
		 * @return A new, immutable ProxyConfig instance.
		 * @throws IllegalArgumentException if the configured state is inconsistent (e.g., auth required but no credentials).
		 */
		public ProxyConfig build() {
			// The ProxyConfig constructor performs the final cross-field validation
			return new ProxyConfig(this);
		}
	}
}