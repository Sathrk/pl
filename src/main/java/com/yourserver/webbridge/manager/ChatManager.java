package com.yourserver.webbridge.manager;

import java.util.LinkedList;

public class ChatManager {
    private static final LinkedList<String> chatLogs = new LinkedList<>();
    private static final int MAX_LOGS = 50;

    public static synchronized void addMessage(String player, String message) {
        String jsonMsg = String.format("{\"player\":\"%s\",\"message\":\"%s\"}", 
            player.replace("\"", "\\\""), 
            message.replace("\"", "\\\"")
        );
        chatLogs.add(jsonMsg);
        if (chatLogs.size() > MAX_LOGS) {
            chatLogs.removeFirst();
        }
    }

    public static synchronized String getChatJson() {
        return "[" + String.join(",", chatLogs) + "]";
    }
}
