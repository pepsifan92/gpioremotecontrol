/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.gpioremotecontrol;

import java.net.URI;
import java.util.TreeMap;

import org.openhab.binding.gpioremotecontrol.internal.Client;
import org.openhab.core.binding.BindingProvider;
import home.control.model.*;

/**
 * @author MichaelP
 * @since 1.0
 */
public interface GpioRemoteControlBindingProvider extends BindingProvider {
	
	public HostAndTempAndPinConfiguration getConfig(String itemName);
	public TreeMap<URI, Client> getClientMap();
	
}
