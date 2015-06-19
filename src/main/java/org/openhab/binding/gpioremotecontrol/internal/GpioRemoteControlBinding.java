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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import com.google.gson.Gson;

import org.openhab.binding.gpioremotecontrol.GpioRemoteControlBindingProvider;

import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.AbstractActiveBinding;
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

	
	/** 
	 * the refresh interval which is used to poll values from the GpioRemoteControl
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;
	
	
	public GpioRemoteControlBinding() {
		logger.debug("GpioRemoteControlBinding binding started");
	}
			
	
	/* ADDED THIS FOR GETTING ALL COMMANDS... ****************************/
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void receiveCommand(String itemName, Command command) {
		// does any provider contain a binding config?
		if (!providesBindingFor(itemName)) {
			return;
		}
		internalReceiveCommand(itemName, command);
	}
	/* ADDED THIS FOR GETTING ALL COMMANDS... ****************************/
	
	
	/**
	 * Called by the SCR to activate the component with its configuration read from CAS
	 * 
	 * @param bundleContext BundleContext of the Bundle that defines this component
	 * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
		this.bundleContext = bundleContext;

		// the configuration is guaranteed not to be null, because the component definition has the
		// configuration-policy set to require. If set to 'optional' then the configuration may be null
		
			
		// to override the default refresh interval one has to add a 
		// parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
		String refreshIntervalString = (String) configuration.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			refreshInterval = Long.parseLong(refreshIntervalString);
		}

		// read further config parameters here ...
		
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
		logger.debug("execute() method is called!");	
		logger.debug("execute() method: " + providers.iterator().next().getClientMap().keySet().toString());
		removeUnusedConnections();
		checkConnections();
	}

	private void checkConnections() {
		for(GpioRemoteControlBindingProvider provider : providers){
			for(URI keyUri : provider.getClientMap().keySet()){				
				if(provider.getClientMap().get(keyUri).getReadyState() == WebSocket.READY_STATE_CONNECTING){ //If client is not connected yet
					logger.debug("checkConnections: Connect client: " + keyUri.toString());
					provider.getClientMap().put(keyUri, new Client(keyUri)); //Override old, confused WebSocket
					provider.getClientMap().get(keyUri).connect(); //Connect it					
				} else if(provider.getClientMap().get(keyUri).getReadyState() == WebSocket.READY_STATE_OPEN) {
					logger.debug("checkConnections: running connection: " + keyUri.toString());
					//fine
				} else { //If else Client state, start and connect new Websocket  
					logger.debug("checkConnections: Create new client and connect: " + keyUri.toString());
					provider.getClientMap().put(keyUri, new Client(keyUri)); //Override old, confused WebSocket
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
		
		Gson gson = new Gson();
				
		for (GpioRemoteControlBindingProvider provider : providers) {
			try {
				int pinNumber = provider.getConfig(itemName).getNumber();
				PinConfiguration pinConf;
				
				if(command == OnOffType.ON){
					pinConf = new PinConfiguration(Event.FADE, pinNumber, 400, 0, 100, false, 1, 0);
					//pinConf = new PinConfiguration(Event.SET, pinNumber, true);
				} else {
					pinConf = new PinConfiguration(Event.FADE, pinNumber, 600, 100, 0, false, 1, 0);
					//pinConf = new PinConfiguration(Event.SET, pinNumber, false);
				}
				if(itemName.equals("NT")){
					if(command == OnOffType.ON){
						pinConf = new PinConfiguration(Event.SET, pinNumber, true);
					} else {
						pinConf = new PinConfiguration(Event.SET, pinNumber, false);
					}
				}
								
				URI uriOfPin = new URI("ws://" + provider.getConfig(itemName).getHostWithPort());
				
				provider.getClientMap().get(uriOfPin).send(gson.toJson(pinConf)); //Send Command to Remote GPIO Pin
				
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);
	}	
}
