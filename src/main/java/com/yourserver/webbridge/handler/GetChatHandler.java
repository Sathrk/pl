package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.yourserver.webbridge.manager.ChatManager;

import java.io.IOException;

public class GetChatHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        CorsHelper.sendJson(exchange, ChatManager.getChatJson());
    }
}
