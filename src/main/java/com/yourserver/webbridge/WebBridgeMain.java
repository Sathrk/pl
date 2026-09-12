package com.survival.webdashboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WebBridgeMain extends JavaPlugin implements CommandExecutor {

    private PasscodeManager passcodeManager;
    private ClaimManager claimManager;
    private WebHttpServer httpServer;

    @Override
    public void onEnable() {
        this.passcodeManager = new PasscodeManager();
        this.claimManager = new ClaimManager();

        // Start HTTP API Server
        this.httpServer = new WebHttpServer(this);
        this.httpServer.start();

        // Register Commands
        if (this.getCommand("web") != null) this.getCommand("web").setExecutor(this);
        if (this.getCommand("claim") != null) this.getCommand("claim").setExecutor(this);

        getLogger().info("WebBridge Main Plugin successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop();
        }
        getLogger().info("WebBridge Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Sirf in-game players in commands ko execute kar sakte hain.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("web")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("login")) {
                String code = passcodeManager.generatePasscode(player.getName());
                player.sendMessage("§a[WebBridge] Your web login passcode is: §e§l" + code);
                player.sendMessage("§7Enter this 6-digit passcode on the web dashboard to authenticate.");
                return true;
            } else {
                player.sendMessage("§cUsage: /web login");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("claim")) {
            boolean success = claimManager.processClaims(player);
            if (success) {
                player.sendMessage("§a[WebBridge] Items claimed successfully!");
            } else {
                player.sendMessage("§e[WebBridge] You have no pending web store claims.");
            }
            return true;
        }

        return false;
    }

    public PasscodeManager getPasscodeManager() { return passcodeManager; }
    public ClaimManager getClaimManager() { return claimManager; }
}
