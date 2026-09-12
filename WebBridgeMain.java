package com.yourserver.webbridge;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WebBridgeMain extends JavaPlugin implements CommandExecutor {

    private WebHttpServer webServer;
    private final int PORT = 12935;

    @Override
    public void onEnable() {
        try {
            webServer = new WebHttpServer(PORT);
            getLogger().info("Web Bridge HTTP Server running on port: " + PORT);
        } catch (Exception e) {
            getLogger().severe("Could not start Web Bridge server on port " + PORT + ": " + e.getMessage());
        }

        if (getCommand("web") != null) {
            getCommand("web").setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Is command ko sirf in-game player use kar sakta hai!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("login")) {
            String code = PasscodeManager.generateCode(player.getName());
            player.sendMessage(ChatColor.GREEN + "[WebAuth] " + ChatColor.WHITE + "Aapka Web Dashboard Login Passcode: " 
                + ChatColor.YELLOW + ChatColor.BOLD + code);
            player.sendMessage(ChatColor.GRAY + "Yeh code agle 10 minutes tak valid hai.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /web login");
        return true;
    }
}
