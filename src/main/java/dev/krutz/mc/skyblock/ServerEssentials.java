package dev.krutz.mc.skyblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerEssentials {

    private final Main plugin;

    public ServerEssentials(Main plugin) {
        this.plugin = plugin;
        loadServerData();
    }

    public void loadServerData() {
        
    }

    public boolean handleEssentialsCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            teleportToSpawn(player);
            return true;
        }
        else if(command.getName().equalsIgnoreCase("setspawn")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if(player.hasPermission("skyblock.setspawn")) {
                setSpawn(player);
            } else {
                player.sendMessage("You do not have permission to use this command.");
            }
            return true;
        }
        else if(command.getName().equalsIgnoreCase("sethome")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if(player.hasPermission("skyblock.sethome")) {
                setHome(player);
            } else {
                player.sendMessage("You do not have permission to use this command.");
            }
            return true;
        }
        else if(command.getName().equalsIgnoreCase("home")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            teleportToHome(player);
            return true;
        }


        return false;
    }

    private void teleportToSpawn(Player player) {
        World spawnWorld = Bukkit.getServer().getWorld(Main.spawnWorldName);
        if (spawnWorld != null) {
            player.teleport(spawnWorld.getSpawnLocation());
            player.sendMessage("You have been teleported to the spawn world.");
        } else {
            player.sendMessage("The spawn world is not loaded or does not exist.");
        }
    }

    private void setSpawn(Player player) {
        World spawnWorld = Bukkit.getServer().getWorld(Main.spawnWorldName);
        if (spawnWorld != null) {
            spawnWorld.setSpawnLocation(player.getLocation());
            player.sendMessage("You have set the spawn location for the spawn world.");
        } else {
            player.sendMessage("The spawn world is not loaded or does not exist.");
        }
    }

    private void setHome(Player player) {
        player.setRespawnLocation(player.getLocation(), true);
        player.sendMessage("You have set your home location.");
    }

    private void teleportToHome(Player player) {
        player.teleport(player.getRespawnLocation());
        player.sendMessage("You have been teleported to your home location.");
    }

    

}