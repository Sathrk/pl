package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetMapHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        List<String> playerList = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            playerList.add(String.format(
                "{\"name\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"yaw\":%.2f,\"world\":\"%s\"}",
                p.getName(),
                p.getLocation().getX(),
                p.getLocation().getY(),
                p.getLocation().getZ(),
                p.getLocation().getYaw(),
                p.getWorld().getName()
            ));
        }

        String response = String.format(
            "{\"onlinePlayers\":%d,\"maxPlayers\":%d,\"players\":[%s]}",
            Bukkit.getOnlinePlayers().size(),
            Bukkit.getMaxPlayers(),
            String.join(",", playerList)
        );

        CorsHelper.sendJson(exchange, response);
    }
}
