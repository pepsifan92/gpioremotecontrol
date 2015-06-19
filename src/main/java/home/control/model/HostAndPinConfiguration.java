package home.control.model;

import org.openhab.core.binding.BindingConfig;

public class HostAndPinConfiguration extends PinConfiguration implements BindingConfig{
	private String HostWithPort;

	public String getHostWithPort() {
		return HostWithPort;
	}

	public void setHostWithPort(String hostWithPort) {
		HostWithPort = hostWithPort;
	}
}
