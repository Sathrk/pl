package com.yourserver.webbridge.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetInventoryHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsHelper.addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            CorsHelper.send204(exchange);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String username = CorsHelper.getQueryParam(query, "username", "");

        Player player = Bukkit.getPlayer(username);
        if (player == null) {
            CorsHelper.sendJson(exchange, "{\"online\":false, \"items\":[]}");
            return;
        }

        List<String> itemList = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < 9; i++) { // Hotbar slots 0 to 8
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                itemList.add(String.format("{\"slot\":%d,\"type\":\"%s\",\"amount\":%d}", 
                    i, item.getType().name().toLowerCase(), item.getAmount()));
            } else {
                itemList.add(String.format("{\"slot\":%d,\"type\":\"air\",\"amount\":0}", i));
            }
        }

        String json = String.format("{\"online\":true,\"player\":\"%s\",\"items\":[%s]}", 
            player.getName(), String.join(",", itemList));

        CorsHelper.sendJson(exchange, json);
    }
}
