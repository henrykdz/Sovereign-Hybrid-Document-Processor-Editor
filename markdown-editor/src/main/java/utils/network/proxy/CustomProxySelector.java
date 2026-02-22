package utils.network.proxy;


import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import utils.logging.Log;

public class CustomProxySelector extends ProxySelector {
	private final Proxy proxy;


	public CustomProxySelector(Proxy proxy) { this.proxy = proxy; }


	@Override
	public List<Proxy> select(URI uri) { return Collections.singletonList(proxy); }


	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		// Optionally log or handle connection failures
		Log.warn("Proxy connection failed: " + ioe.getMessage());
	}
}