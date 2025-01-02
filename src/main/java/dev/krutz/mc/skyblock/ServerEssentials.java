package dev.krutz.mc.skyblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class ServerEssentials {

    //private static JavaPlugin plugin;

    public static void initialize(JavaPlugin pluginInstance) {
        //plugin = pluginInstance;
        loadServerData();
    }

    public static void loadServerData() {
        
    }

    private static Component getCommandComponent(String command, String description) {
        return Component.text("\n  * ")
            .color(TextColor.color(NamedTextColor.DARK_PURPLE)) // Bullet color
            .append(
                Component.text(command)
                    .color(TextColor.color(NamedTextColor.LIGHT_PURPLE)) // Command color
                    .clickEvent(ClickEvent.runCommand(command)) // Click to execute
                    .hoverEvent(
                        HoverEvent.showText(
                            Component.text("Click to execute: ")
                                .color(TextColor.color(NamedTextColor.GOLD))
                                .append(Component.text(command).color(TextColor.color(NamedTextColor.RED)))
                        )
                    )
            )
            .append(
                Component.text(" - " + description)
                    .color(TextColor.color(NamedTextColor.DARK_PURPLE)) // Description color
            ).decoration(TextDecoration.BOLD, false);
    }
    
    public static void sendHelpMenu(CommandSender sender) {
        Component message = Component.text("\n-======  Help Menu  ======-\n")
            .color(TextColor.color(NamedTextColor.YELLOW))
            .decoration(TextDecoration.BOLD, true) // Bold the title
            .append(
                Component.text("Click to execute a command or read more...\n")
                    .color(TextColor.color(NamedTextColor.YELLOW))
                    .decoration(TextDecoration.BOLD, false) // Remove bold for this part
            )
            .append(getCommandComponent("/spawn", "Teleport to the lobby."))
            .append(getCommandComponent("/list", "List all online players."))
            .append(getCommandComponent("/seen", "Check when a player was last online."))
            .append(getCommandComponent("/home", "Teleport to your home location."))
            .append(getCommandComponent("/island help", "Get help with island commands."))
            .append(getCommandComponent("/challenge list", "List all challenges."))
            .append(Component.text("\n"));

        sender.sendMessage(message);
    }

    public static void teleportToSpawn(Player player) {
        World spawnWorld = Bukkit.getServer().getWorld(Main.spawnWorldName);
        if (spawnWorld != null) {
            player.teleport(spawnWorld.getSpawnLocation());
        } else {
            player.sendMessage("The spawn world is not loaded or does not exist.");
        }
    }

    public static void setSpawn(Player player) {
        World spawnWorld = Bukkit.getServer().getWorld(Main.spawnWorldName);
        if (spawnWorld != null) {
            spawnWorld.setSpawnLocation(player.getLocation());
            player.sendMessage("You have set the spawn location for the spawn world.");
        } else {
            player.sendMessage("The spawn world is not loaded or does not exist.");
        }
    }

    public static void setHome(Player player) {
        player.setRespawnLocation(player.getLocation(), true);
        player.sendMessage("You have set your home location.");
    }

    public static void teleportToHome(Player player) {

        Location loc = player.getRespawnLocation();
        if (loc == null) {
            loc = player.getWorld().getSpawnLocation();
        }
        player.teleport(loc);
        player.sendMessage("You have been teleported to your home location.");
    }

    

}