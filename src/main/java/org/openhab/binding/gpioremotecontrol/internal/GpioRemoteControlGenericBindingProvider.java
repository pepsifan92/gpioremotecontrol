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

import home.control.model.HostAndPinConfiguration;

import org.openhab.binding.gpioremotecontrol.GpioRemoteControlBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.PercentType;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
		if (!(item instanceof SwitchItem || item instanceof DimmerItem || item instanceof StringItem || item instanceof PercentType)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Switch- String- (for JSON PinConfig) Percent- and DimmerItems are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		HostAndPinConfiguration config = new HostAndPinConfiguration();
		
//		State state = item.getState();
		
		//Format in .items: GpioRemoteControl="HostWithPort;PinNumber"   ... "123.123.123.31:1234;1"
		String[] properties = bindingConfig.split(";");
		
		config.setHostWithPort(properties[0]);
		config.setNumber(Integer.parseInt(properties[1]));
		
		//parse bindingconfig here ...
		logger.debug("GpioRemoteControl: processBindingConfiguration({},{}) is called!", config.getNumber(), config.getHostWithPort());
		
		addBindingConfig(item, config);
		
		handleWebsocketConnections(config);
	}
	
	/* ================================= SELF WRITTEN METHODS - BEGIN ===============================*/
	
	private void handleWebsocketConnections(HostAndPinConfiguration config) {		
		logger.debug("GpioRemoteControl: handleWebsocketConnections ({},{}) is called!", config.getNumber(), config.getHostWithPort());
		try {			
			URI uri = new URI("ws://" + config.getHostWithPort());
			if (!clientMap.containsKey(uri)) { //If the connection is not already set up, start one...
				clientMap.put(uri, new Client(uri));
			}						
		} catch (URISyntaxException e) {
			logger.debug("GpioRemoteControl: handleWebsocketConnections URISyntaxException ({},{})", config.getHostWithPort(), config.getNumber());
			e.printStackTrace();
		} catch (Exception e) {
			logger.debug("GpioRemoteControl: handleWebsocketConnections Exception ({},{})", config.getHostWithPort(), config.getNumber());
			e.printStackTrace();
		}
		
		
	}

	@Override
	public HostAndPinConfiguration getConfig(String itemName) {
		HostAndPinConfiguration config = (HostAndPinConfiguration) bindingConfigs.get(itemName);
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
	private void checkConfigOfNull(HostAndPinConfiguration config, String itemName){
		if (config == null) {
			throw new IllegalArgumentException("The item name '" + itemName + "'is invalid or the item isn't configured");
		}
	}	
	
	/* ================================= SELF WRITTEN METHODS - END ===============================*/
	
}
