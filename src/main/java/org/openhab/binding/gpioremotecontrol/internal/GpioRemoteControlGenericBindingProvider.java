/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpioremotecontrol.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.TreeMap;

import home.control.model.HostAndTempAndPinConfiguration;
import home.control.model.HostAndTempAndPinConfiguration.ConfigMode;
import home.control.model.Temperature;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.openhab.binding.gpioremotecontrol.GpioRemoteControlBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.PercentType;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.internal.ws.assembler.jaxws.MustUnderstandTubeFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author MichaelP
 * @since 1.0
 */
public class GpioRemoteControlGenericBindingProvider extends AbstractGenericBindingProvider implements GpioRemoteControlBindingProvider {

	private static final Logger logger = 
			LoggerFactory.getLogger(GpioRemoteControlGenericBindingProvider.class);
	
	private TreeMap<URI, Client> clientMap = new TreeMap<>();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBindingType() {
		return "gpioremotecontrol";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem || item instanceof DimmerItem || item instanceof StringItem || item instanceof PercentType || item instanceof NumberItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Switch- String- Percent- Number- and DimmerItems are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		HostAndTempAndPinConfiguration config = new HostAndTempAndPinConfiguration();		
		
//		State state = item.getState();
		
		//Format in .items: GpioRemoteControl="HostWithPort;PinNumber|deviceId;direction|temperature"   ... "123.123.123.31:1234;1"
		/*
		 * 123.123.123.31:1234;1;out
		 * 123.123.123.31:1234;2;in
		 * 123.123.123.31:1234;28-00044a7273ff;temperature
		 */
		String[] properties = bindingConfig.split(";");
		config.setHostWithPort(properties[0]);
		
		if(properties[2].toLowerCase().equals("temperature")){ //Is temperature config
			config.configMode = ConfigMode.TEMPERATURE;
			config.temperature.setDeviceId(properties[1]);
		} else {			
			if (properties[2].toLowerCase().equals("out")) {
				config.configMode = ConfigMode.OUTPUT;
				config.pinConfiguration.setNumber(Integer.parseInt(properties[1])); //Configure pinConfiguration Number. It's not -1 anymore, so it must be output.
			} else {
				config.configMode = ConfigMode.INPUT;
				config.pinInput.setNumber(Integer.parseInt(properties[1])); //Same here, must be "PinInput"
			}
		}
		
		
		//parse bindingconfig here ...
		logger.debug("GpioRemoteControl: processBindingConfiguration({},{}) is called!", config.pinConfiguration.getNumber(), config.getHostWithPort());		
		addBindingConfig(item, config);		
		handleWebsocketConnections(config);
	}
	
	/* ================================= SELF WRITTEN METHODS - BEGIN ===============================*/
	
	private void handleWebsocketConnections(HostAndTempAndPinConfiguration config) {		
		logger.debug("GpioRemoteControl: handleWebsocketConnections ({},{}) is called!", config.pinConfiguration.getNumber(), config.getHostWithPort());
		try {			
			URI uri = new URI("ws://" + config.getHostWithPort());
			if (!clientMap.containsKey(uri)) { //If the connection is not already set up, start one...
				clientMap.put(uri, null);
			}						
		} catch (URISyntaxException e) {
			logger.debug("GpioRemoteControl: handleWebsocketConnections URISyntaxException ({},{})", config.getHostWithPort(), config.pinConfiguration.getNumber());
			e.printStackTrace();
		} catch (Exception e) {
			logger.debug("GpioRemoteControl: handleWebsocketConnections Exception ({},{})", config.getHostWithPort(), config.pinConfiguration.getNumber());
			e.printStackTrace();
		}
		
		
	}

	@Override
	public HostAndTempAndPinConfiguration getConfig(String itemName) {
		HostAndTempAndPinConfiguration config = (HostAndTempAndPinConfiguration) bindingConfigs.get(itemName);
		checkConfigOfNull(config, itemName);
		return config;
	}
		
	@Override
	public TreeMap<URI, Client> getClientMap() {
		return clientMap;
	}
	
	/**
	 * Simply throws an {@link IllegalArgumentException}, if the given config is null. 
	 * 
	 * @param config the config to check. Type: GpioRemoteControlBindingConfig
	 * @param itemName The name of the Item
	 * 
	 * @throws IllegalArgumentException
	 */
	private void checkConfigOfNull(HostAndTempAndPinConfiguration config, String itemName){
		if (config == null) {
			throw new IllegalArgumentException("The item name '" + itemName + "'is invalid or the item isn't configured");
		}
	}	
	
	/* ================================= SELF WRITTEN METHODS - END ===============================*/
	
}
