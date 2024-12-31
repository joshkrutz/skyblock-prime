package dev.krutz.mc.skyblock;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

public class EssentialsCommandManager {

    public void registerCommands(CommandManager commandManager) {
        commandManager.registerCommand(new CommandInfo(
            "spawn", 
            "Teleport to the world spawn.", 
            (sender, args) -> {
                Player player = (Player) sender;
                ServerEssentials.teleportToSpawn(player);
            })
            .setPlayerOnly(true)
        );

        commandManager.registerCommand(new CommandInfo(
            "help", 
            "Get information on server and commands.", 
            (sender, args) -> { ServerEssentials.sendHelpMenu(sender); })
        );

        commandManager.registerCommand(new CommandInfo(
            "setspawn", 
            "Set the world spawn.", 
            (sender, args) -> {
                Player player = (Player) sender;
                ServerEssentials.setSpawn(player);
            })
            .setPlayerOnly(true)
            .setPermission("op")
        );

        commandManager.registerCommand(new CommandInfo(
            "sethome", 
            "Set your respawn location.", 
            (sender, args) -> {
                Player player = (Player) sender;
                ServerEssentials.setHome(player);
            })
            .setPlayerOnly(true)
        );

        commandManager.registerCommand(new CommandInfo(
            "home", 
            "Teleport to your respawn location.", 
            (sender, args) -> {
                Player player = (Player) sender;
                ServerEssentials.teleportToHome(player);
            })
            .setPlayerOnly(true)
        );
    }
}