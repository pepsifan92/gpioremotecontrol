/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpioremotecontrol.internal;

import home.control.model.*;
import home.control.model.HostAndTempAndPinConfiguration.ConfigMode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.java_websocket.WebSocket;
import com.google.gson.Gson;
import org.openhab.binding.gpioremotecontrol.GpioRemoteControlBindingProvider;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

	

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author MichaelP
 * @since 1.0
 */
public class GpioRemoteControlBinding extends AbstractActiveBinding<GpioRemoteControlBindingProvider> {
	
	private static final Logger logger = 
		LoggerFactory.getLogger(GpioRemoteControlBinding.class);	
	
	/**
	 * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
	 * method and must not be accessed anymore once the deactivate() method was called or before activate()
	 * was called.
	 */
	private BundleContext bundleContext;	
	Gson gson = new Gson();
	
	/** 
	 * the refresh interval which is used to poll values from the GpioRemoteControl
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 10000;
	
	
	public GpioRemoteControlBinding() {
		logger.debug("GpioRemoteControlBinding binding started");
	}
	
	/**
	 * Called by the SCR to activate the component with its configuration read from CAS
	 * 
	 * @param bundleContext BundleContext of the Bundle that defines this component
	 * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
		this.bundleContext = bundleContext;		
			
		// to override the default refresh interval one has to add a 
		// parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
		String refreshIntervalString = (String) configuration.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			refreshInterval = Long.parseLong(refreshIntervalString);
		}
		
		setProperlyConfigured(true);
	}
	
	/**
	 * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
	 * @param configuration Updated configuration properties
	 */
	public void modified(final Map<String, Object> configuration) {
		// update the internal configuration accordingly
	}
	
	/**
	 * Called by the SCR to deactivate the component when either the configuration is removed or
	 * mandatory references are no longer satisfied or the component has simply been stopped.
	 * @param reason Reason code for the deactivation:<br>
	 * <ul>
	 * <li> 0 – Unspecified
     * <li> 1 – The component was disabled
     * <li> 2 – A reference became unsatisfied
     * <li> 3 – A configuration was changed
     * <li> 4 – A configuration was deleted
     * <li> 5 – The component was disposed
     * <li> 6 – The bundle was stopped
     * </ul>
	 */
	public void deactivate(final int reason) {
		this.bundleContext = null;
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}

	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "GpioRemoteControl Refresh Service";
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling) goes here ...
		removeUnusedConnections();
		checkConnections();
	}

	private void checkConnections() {
		for(GpioRemoteControlBindingProvider provider : providers){
			for(URI keyUri : provider.getClientMap().keySet()){
				if (provider.getClientMap().get(keyUri) == null) {
					logger.debug("checkConnections: Client was NULL. Connect client: " + keyUri.toString());
					provider.getClientMap().put(keyUri, new Client(keyUri, this)); //Override old, confused WebSocket					
					provider.getClientMap().get(keyUri).connect(); //Connect it
				} else if(provider.getClientMap().get(keyUri).getReadyState() == WebSocket.READY_STATE_CONNECTING){ //If client is not connected yet
					logger.debug("checkConnections: Connect client: " + keyUri.toString());
					provider.getClientMap().put(keyUri, new Client(keyUri, this)); //Override old, confused WebSocket					
					provider.getClientMap().get(keyUri).connect(); //Connect it					
				} else if(provider.getClientMap().get(keyUri).getReadyState() == WebSocket.READY_STATE_OPEN) {
					logger.debug("checkConnections: running connection: " + keyUri.toString());
					//fine
				} else { //If else Client state, start and connect new Websocket  
					logger.debug("checkConnections: Create new client and connect: " + keyUri.toString());
					provider.getClientMap().put(keyUri, new Client(keyUri, this)); //Override old, confused WebSocket
					provider.getClientMap().get(keyUri).connect(); //Connect again
				}
			}
		}
	}

	private void removeUnusedConnections() {
		try{
			for(GpioRemoteControlBindingProvider provider : providers){
				keyLoop:
				for(URI keyUri : provider.getClientMap().keySet()){
					for(String itemName : provider.getItemNames()){						
						URI uri = new URI("ws://" + provider.getConfig(itemName).getHostWithPort());
						if(uri.equals(keyUri)){
							continue keyLoop;
						}						
						provider.getClientMap().get(keyUri).close(); //Close unused connection	
						provider.getClientMap().remove(keyUri); //If non Item uses the URI, remove the connection				
					}
				}			
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch(Exception e){
			logger.debug("Exception in removeUnusedConnections. Shit happens...");
		}				
	}


	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("GpioRemoteControl: internalReceiveCommand({},{}) is called!", itemName, command);
				
		for (GpioRemoteControlBindingProvider provider : providers) {
			if (provider.getConfig(itemName).configMode != ConfigMode.OUTPUT) {
				logger.warn("The Item '{}' wasn't configured as output item. It can't receive any commands!", itemName);
				return; //If it is not a output Item, stop here				
			}
			try {
				int pinNumber = provider.getConfig(itemName).pinConfiguration.getNumber();
				PinConfiguration pinConf = null;
				logger.debug("GpioRemoteControl: internalReceiveCommand: Event Auswahl folgt... " +
						"ItemName: {}, Command: {}", itemName, command);
				if(command == OnOffType.ON || command.toString().toLowerCase().equals("on")){
					pinConf = new PinConfiguration(Event.SET, pinNumber, true);
					provider.getConfig(itemName).pinConfiguration.setPwmValue(100);
					
				} else if (command == OnOffType.OFF || command.toString().toLowerCase().equals("off")){
					pinConf = new PinConfiguration(Event.SET, pinNumber, false);
					provider.getConfig(itemName).pinConfiguration.setPwmValue(0);
				
				} else if (command.toString().toLowerCase().equals("toggle")){
					pinConf = handleToggleCommand(provider, itemName, pinNumber, command);					
					
				} else if (command == IncreaseDecreaseType.INCREASE || command.toString().toLowerCase().equals("increase")){
					provider.getConfig(itemName).pinConfiguration.setPwmValue(provider.getConfig(itemName).pinConfiguration.getPwmValue()+1);
					pinConf = new PinConfiguration(Event.DIM, pinNumber, provider.getConfig(itemName).pinConfiguration.getPwmValue()+1);
					
				} else if (command == IncreaseDecreaseType.DECREASE || command.toString().toLowerCase().equals("decrease")){
					provider.getConfig(itemName).pinConfiguration.setPwmValue(provider.getConfig(itemName).pinConfiguration.getPwmValue()-1);
					pinConf = new PinConfiguration(Event.DIM, pinNumber, provider.getConfig(itemName).pinConfiguration.getPwmValue());	
					
				} else if (command.toString().toLowerCase().contains("dim_")){
					String[] split = command.toString().split("_");
					//Should be: 0:dim;1:pwmValue
					pinConf = new PinConfiguration(Event.DIM, pinNumber, Integer.parseInt(split[1]));					
					provider.getConfig(itemName).pinConfiguration.setPwmValue(Integer.parseInt(split[1])); //Save value of PWM
					
				} else if (command.toString().toLowerCase().contains("fade_")){
					pinConf = handleEvent(provider, itemName, pinNumber, command, Event.FADE);
					
				} else if (command.toString().toLowerCase().contains("fadeupdown_")){
					pinConf = handleEvent(provider, itemName, pinNumber, command, Event.FADE_UP_DOWN);
					
				} else if (command.toString().toLowerCase().contains("blink_")){
					pinConf = handleBlinkEvent(provider, itemName, pinNumber, command);	
					
				} else {					
					//parseable as Integer? 
					int tempPwmVal = Integer.parseUnsignedInt(command.toString());
					provider.getConfig(itemName).pinConfiguration.setPwmValue(tempPwmVal); //pinConfiguration.setPwmValue ensures the value is 0-100
					pinConf = new PinConfiguration(Event.DIM, pinNumber, provider.getConfig(itemName).pinConfiguration.getPwmValue());
				}
								
				URI uriOfPin = new URI("ws://" + provider.getConfig(itemName).getHostWithPort());
				
				provider.getClientMap().get(uriOfPin).send(gson.toJson(pinConf)); //Send Command to Remote GPIO Pin
//				provider.getClientMap().get(uriOfPin).send(gson.toJson("{\"event\":\"TEMP\",\"deviceId\":\"28-00044a72b1ff\"}"));
			} catch (IndexOutOfBoundsException e) {
				logger.warn("GpioRemoteControl: internalReceiveCommand: EventConfig not readable! Maybe wrong parameter in Sitemap? " +
						"ItemName: {}, Command: {}", itemName, command);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {		
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	

	private PinConfiguration handleToggleCommand(GpioRemoteControlBindingProvider provider, String itemName, int pinNumber, Command command) {
		if (provider.getConfig(itemName).pinConfiguration.getPwmValue() < 100) {
			provider.getConfig(itemName).pinConfiguration.setPwmValue(100);
			return new PinConfiguration(Event.SET, pinNumber, true);
		} else {
			provider.getConfig(itemName).pinConfiguration.setPwmValue(0);
			return new PinConfiguration(Event.SET, pinNumber, false);
		}		
	}

	private PinConfiguration handleBlinkEvent(GpioRemoteControlBindingProvider provider, String itemName, int pinNumber, Command command) {
		String[] split = command.toString().split("_");
		//Should be: 0:blink;1:uptime;2:downtime;3:pwmValue;4:repeat;5:cycles
		provider.getConfig(itemName).pinConfiguration.setPwmValue(0); //Save endVal of PWM
		if(split.length == 2) { //Short version: Only uptime - one flash to 100%.
			return new PinConfiguration(Event.BLINK, pinNumber, Integer.parseInt(split[1]), 1, 100, false, 0); //default values for no loop						
		} else {
			return new PinConfiguration(Event.BLINK, pinNumber, Integer.parseInt(split[1]), Integer.parseInt(split[2]), 
					Integer.parseInt(split[3]), Boolean.parseBoolean(split[4]), Integer.parseInt(split[5]));						
		}		
	}

	private PinConfiguration handleEvent(GpioRemoteControlBindingProvider provider, String itemName, int pinNumber, Command command, Event event) {
		String[] split = command.toString().split("_");
		//Should be: 0:fade;1:cycleDuration;2:startVal;3:endVal;4:repeat;5:cycles;6:cyclePause
		provider.getConfig(itemName).pinConfiguration.setPwmValue(Integer.parseInt(split[3])); //Save endVal of PWM
		if(split.length == 4) { //Short version: If only cycleDuration and start- and EndValue given 
			return new PinConfiguration(event, pinNumber, 
				Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), false, 0, 0); //default values for no loop
		} else {
			return new PinConfiguration(event, pinNumber, 
				Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), 
				Boolean.parseBoolean(split[4]), Integer.parseInt(split[5]), Integer.parseInt(split[6]));
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
//		logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);
	}	
	
	public void receiveServerMessage(String message){
//		logger.debug(">>>>> GpioRemoteControl: receiveServerMessage!");
		try {
			parseAndUpdatePinInput(message); // NOT TESTED YET, NO INPUT PIN AVAILABLE //	
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		try { 
			parseAndUpdateTemperature(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// METHOD NOT TESTED YET //
	private void parseAndUpdatePinInput(String message) {
		PinInput pinConfig = gson.fromJson(message, PinInput.class);
		if (pinConfig.getNumber() != -1) { //Was successfully parsed 
			if (providers.iterator().next().getConfig(getItemNameByPinNumber(pinConfig.getNumber())).configMode == ConfigMode.INPUT) {
				logger.debug("GpioRemoteControl: receiveServerMessage: PinConfig: {}, {}", pinConfig.getNumber(), pinConfig.getIsHigh());
				providers.iterator().next().getConfig(getItemNameByPinNumber(pinConfig.getNumber())).pinInput = pinConfig; //Replace pinConfiguration with received config
				if (pinConfig.getIsHigh()) {
					eventPublisher.postUpdate(getItemNameByPinNumber(pinConfig.getNumber()), OnOffType.ON);
				} else {
					eventPublisher.postUpdate(getItemNameByPinNumber(pinConfig.getNumber()), OnOffType.OFF);
				}
			}
		}
	}
	
	private void parseAndUpdateTemperature(String message) {
//		logger.debug("GpioRemoteControl: parseAndUpdateTemperature.");
		Temperature tempConfig = gson.fromJson(message, Temperature.class);
		if (tempConfig.getDeviceId() != "") { //Was successfully parsed		
//			logger.debug("GpioRemoteControl: receiveServerMessage: TemperatureConfig: {}, {}, {}", tempConfig.getDeviceId(), tempConfig.getTemperature(), getItemNameByTemperatureDeviceId(tempConfig.getDeviceId()));
			if (providers.iterator().next().getConfig(getItemNameByTemperatureDeviceId(tempConfig.getDeviceId())).configMode == ConfigMode.TEMPERATURE) {				
				providers.iterator().next().getConfig(getItemNameByTemperatureDeviceId(tempConfig.getDeviceId())).temperature = tempConfig;
				Float temperatureRaw = (float) (tempConfig.getTemperature());
				Float temperature = temperatureRaw/1000; //Received value is like 22500 for 22,5°C 
//				logger.debug("GpioRemoteControl: parseAndUpdateTemperature: {}", temperature.toString());
				eventPublisher.postUpdate(getItemNameByTemperatureDeviceId(tempConfig.getDeviceId()), DecimalType.valueOf(temperature.toString()));				
			}
		}
	}
	
	private String getItemNameByPinNumber(int pinNumber){
		for (GpioRemoteControlBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				if(provider.getConfig(itemName).pinConfiguration.getNumber() == pinNumber){
					return itemName;
				}
			}		
		}
		return "ItemNotFound in getItemName in gpioRemoteControl Pin";
	}
	
	private String getItemNameByTemperatureDeviceId(String deviceId){
		for (GpioRemoteControlBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				if(provider.getConfig(itemName).temperature.getDeviceId().equals(deviceId)){	
					return itemName;
				}
			}		
		}
		return "ItemNotFound in getItemName in gpioRemoteControl Temperature";
	}
}
