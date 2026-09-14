package com.yourserver.webbridge.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatManager {
    private static final List<ChatMessage> chatHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY = 50;

    public static class ChatMessage {
        public String player;
        public String message;

        public ChatMessage(String player, String message) {
            this.player = player;
            this.message = message;
        }
    }

    public static void addMessage(String player, String message) {
        synchronized (chatHistory) {
            if (chatHistory.size() >= MAX_HISTORY) {
                chatHistory.remove(0);
            }
            chatHistory.add(new ChatMessage(player, message));
        }
    }

    public static String getChatJson() {
        synchronized (chatHistory) {
            List<String> jsonMessages = new ArrayList<>();
            for (ChatMessage msg : chatHistory) {
                // Escape quotes to prevent invalid JSON
                String cleanPlayer = msg.player.replace("\"", "\\\"");
                String cleanMsg = msg.message.replace("\"", "\\\"");
                jsonMessages.add(String.format("{\"player\":\"%s\",\"message\":\"%s\"}", cleanPlayer, cleanMsg));
            }
            return "[" + String.join(",", jsonMessages) + "]";
        }
    }
}
