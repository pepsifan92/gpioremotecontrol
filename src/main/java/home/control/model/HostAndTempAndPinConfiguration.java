package home.control.model;

import org.openhab.core.binding.BindingConfig;

public class HostAndTempAndPinConfiguration implements BindingConfig {
	
	private String HostWithPort;
	public Temperature temperature = new Temperature();
	public PinConfiguration pinConfiguration = new PinConfiguration();
	public PinInput pinInput = new PinInput();
	public ConfigMode configMode;
	
	static public enum ConfigMode {
		OUTPUT, INPUT, TEMPERATURE
	}

	public String getHostWithPort() {
		return HostWithPort;
	}

	public void setHostWithPort(String hostWithPort) {
		HostWithPort = hostWithPort;
	}
}
