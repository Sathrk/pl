package com.yourserver.webbridge;

import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebHttpServer extends NanoHTTPD {

    public WebHttpServer(int port) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();

        // CORS Pre-flight handler
        if (Method.OPTIONS.equals(method)) {
            Response res = newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
            addCorsHeaders(res);
            return res;
        }

        // Endpoint: /api/status
        if ("/api/status".equalsIgnoreCase(uri)) {
            Response res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"online\"}");
            addCorsHeaders(res);
            return res;
        }

        // Endpoint: /api/verify
        if ("/api/verify".equalsIgnoreCase(uri) && Method.POST.equals(method)) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);

                String postData = files.get("postData");
                if (postData == null) {
                    postData = session.getQueryParameterString();
                }

                String code = postData != null ? postData.replaceAll("[^0-9]", "") : "";
                String playerName = PasscodeManager.verifyCode(code);

                Response res;
                if (playerName != null) {
                    res = newFixedLengthResponse(Response.Status.OK, "application/json", 
                        "{\"success\":true, \"username\":\"" + playerName + "\"}");
                } else {
                    res = newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", 
                        "{\"success\":false, \"message\":\"Invalid or expired passcode\"}");
                }
                addCorsHeaders(res);
                return res;

            } catch (Exception e) {
                Response res = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"success\":false, \"message\":\"Server error\"}");
                addCorsHeaders(res);
                return res;
            }
        }

        // Endpoint: /api/add-claim
        if ("/api/add-claim".equalsIgnoreCase(uri) && Method.POST.equals(method)) {
            try {
                Map<String, String> params = session.getParms();
                String targetUser = params.get("username");
                String itemMaterial = params.get("item");
                int count = Integer.parseInt(params.getOrDefault("amount", "1"));

                if (targetUser != null && itemMaterial != null) {
                    ClaimManager.addClaim(targetUser, itemMaterial, count);
                    Response res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
                    addCorsHeaders(res);
                    return res;
                }
            } catch (Exception e) {
                Response res = newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid Data\"}");
                addCorsHeaders(res);
                return res;
            }
        }

        // Endpoint: /api/inventory (Web Inventory Read-Only View)
        if ("/api/inventory".equalsIgnoreCase(uri) && Method.GET.equals(method)) {
            String username = session.getParms().get("username");
            if (username != null) {
                Player player = Bukkit.getPlayer(username);
                if (player != null && player.isOnline()) {
                    StringBuilder json = new StringBuilder("{\"online\":true, \"hotbar\":[");
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        String itemName = (item != null) ? item.getType().name() : "AIR";
                        int amount = (item != null) ? item.getAmount() : 0;
                        json.append("{\"slot\":").append(i).append(",\"item\":\"").append(itemName).append("\",\"amount\":").append(amount).append("}");
                        if (i < 8) json.append(",");
                    }
                    json.append("]}");
                    Response res = newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
                    addCorsHeaders(res);
                    return res;
                }
            }
            Response res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"online\":false}");
            addCorsHeaders(res);
            return res;
        }

        Response res = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        addCorsHeaders(res);
        return res;
    }

    private void addCorsHeaders(Response res) {
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    }
}
