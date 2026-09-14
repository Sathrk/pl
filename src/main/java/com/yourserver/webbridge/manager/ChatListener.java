package com.yourpackage.webserver.listener;

import com.yourpackage.webserver.manager.ChatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        ChatManager.addMessage(event.getPlayer().getName(), event.getMessage());
    }
}
