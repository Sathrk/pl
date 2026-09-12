package com.yourserver.webbridge;

import fi.iki.elonen.NanoHTTPD;
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
        String uri = session.getUri();
        Method method = session.getMethod();

        if (Method.OPTIONS.equals(method)) {
            Response response = newFixedLengthResponse(Response.Status.OK, "text/plain", "");
            addCorsHeaders(response);
            return response;
        }

        if ("/api/status".equalsIgnoreCase(uri)) {
            Response res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"online\"}");
            addCorsHeaders(res);
            return res;
        }

        if ("/api/verify".equalsIgnoreCase(uri) && Method.POST.equals(method)) {
            try {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String postData = files.get("postData");

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
                Response res = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Server error\"}");
                addCorsHeaders(res);
                return res;
            }
        }

        Response res = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        addCorsHeaders(res);
        return res;
    }

    private void addCorsHeaders(Response res) {
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
