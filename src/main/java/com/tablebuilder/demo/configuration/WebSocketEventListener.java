package com.tablebuilder.demo.configuration;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {

        var foo = event.getMessage().getHeaders();
        for (Object o : foo.keySet()) {
            if (o.equals("simpSessionId")) {
                System.out.println(foo.get(o));
            }
        }
        System.out.println("Received a new web socket connection.");
        System.out.println("Вошел в чат." + event.getMessage().getHeaders().get("sender"));
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        var atr = headerAccessor.getSessionAttributes();
        System.out.println("handleWebSocketDisconnectListener");

        String username = (String) atr.get("sender");

        if (username != null) {
            System.out.println("User Disconnected : " + username);

        }
    }
}