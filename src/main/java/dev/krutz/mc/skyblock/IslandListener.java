package dev.krutz.mc.skyblock;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;


public class IslandListener implements Listener {

    private final Map<Player, Island> lastIslandMap = new HashMap<>();
    private static final Map<UUID, Inventory> openMenus = new HashMap<>();


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null) return;

        if(!to.getWorld().getName().equals(Main.skyblockOverworldName)) return;
            
        Island currentIsland = getIslandAtLocation(IslandManager.getIslands(), to);
        Island lastIsland = lastIslandMap.get(player);

        if (currentIsland != lastIsland) {
            if (lastIsland != null) {
                player.sendMessage(lastIsland.getFarewellMessage());
            }
            if (currentIsland != null) {
                player.sendMessage(currentIsland.getGreetingMessage());
            }
            lastIslandMap.put(player, currentIsland);
        }
    }

    private Island getIslandAtLocation(ArrayList<Island> islands, Location from) {
        for (Island island : islands) {
            if(island == null)
                continue;
            Location islandCenter = island.getIslandCenter();
            islandCenter.setY(from.getY());
            double radiusSquared = island.getRadius() * island.getRadius();  

            if(islandCenter.distanceSquared(from) < radiusSquared) {
                return island;
            }
        }
        return null;
    }

    public void openIslandMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, Component.text("Island Menu")
            .color(NamedTextColor.DARK_GREEN)
            .decorate(TextDecoration.BOLD));

        // Add "Friends" option with player head
        ItemStack friends = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta friendsMeta = (SkullMeta) friends.getItemMeta();
        if (friendsMeta != null) {
            friendsMeta.displayName(Component.text("Friends").color(NamedTextColor.GOLD));
            friends.setItemMeta(friendsMeta);
        }
        menu.setItem(3, friends); // Position 3 in the menu

        // Add "Home" option with a bed
        ItemStack home = new ItemStack(Material.RED_BED);
        ItemMeta homeMeta = home.getItemMeta();
        if (homeMeta != null) {
            homeMeta.displayName(Component.text("Home").color(NamedTextColor.GOLD));
            home.setItemMeta(homeMeta);
        }
        menu.setItem(5, home); // Position 5 in the menu

        // Open the menu for the player
        player.openInventory(menu);
        openMenus.put(player.getUniqueId(), menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        UUID playerId = player.getUniqueId();
        if (!openMenus.containsKey(playerId)) return; // Not our menu

        if (event.getClickedInventory() == null || !event.getView().title().equals(Component.text("Island Menu").color(NamedTextColor.DARK_GREEN))) {
            return;
        }

        event.setCancelled(true); // Prevent moving items

        int slot = event.getSlot();
        switch (slot) {
            case 3:
                player.sendMessage(Component.text("Opening Friends menu...").color(NamedTextColor.GREEN));
                break;
            case 5:
                player.sendMessage(Component.text("Teleporting to home...").color(NamedTextColor.GREEN));
                break;
            default:
                player.sendMessage(Component.text("Invalid option!").color(NamedTextColor.RED));
                break;
        }

        player.closeInventory(); // Optional: Close after an action
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        openMenus.remove(player.getUniqueId()); // Remove menu tracking
    }

    public static boolean isPlayerOnTheirIsland(Player player, Island playersIsland) {
        if (playersIsland == null) return false;

        Location playerLocation = player.getLocation();
        Location islandCenter = playersIsland.getIslandCenter();
        islandCenter.setY(playerLocation.getY());

        if (!playerLocation.getWorld().getName().equals(islandCenter.getWorld().getName())) return false;

        double radiusSquared = playersIsland.getRadius() * playersIsland.getRadius();
        return islandCenter.distanceSquared(playerLocation) < radiusSquared;
    }

    //TODO if not on friends or the owner, prevent damaging, set to adventure mode
}