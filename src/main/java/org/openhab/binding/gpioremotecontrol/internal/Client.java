package org.openhab.binding.gpioremotecontrol.internal;

import java.net.URI;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class Client extends WebSocketClient {

	GpioRemoteControlBinding gpioRemoteControlBinding;
	
    public Client(URI serverURI, GpioRemoteControlBinding gpioRemoteControlBinding) {
        super(serverURI);
        this.gpioRemoteControlBinding = gpioRemoteControlBinding;
    }
    
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received message: " + message);  
        gpioRemoteControlBinding.receiveServerMessage(message);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occured:" + ex);
    }
}