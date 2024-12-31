package dev.krutz.mc.skyblock;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
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
import org.bukkit.block.Block;

// KNOWN ISSUES: 
// - Pressure plate detection is not implemented, so players can still use them outside their island
// - Potions can be thrown outside the island
// - Animals/entites can be killed, bred, sheared, ridden, (take armor and saddles) leashed outside their island
// - Player can interact with donkeys/mules (and access chests) outside their island
// - Player can interact with minecarts with hoppers and chests outside their island
// - Player can pick up items and throw items on other islands
// - Player can fire projectiles from their island at nearby islands
// - Player can use pistons to exceed island radius
// 

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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (player.isOp()) return;

        // Need to update to block non-block interactions (e.g. snowballs, ender pearls, eggs)
        if (block == null) return;

        Location blockLocation = block.getLocation();
        Material blockType = block.getType();

        // Check if the block is within their island boundaries
        boolean isWithinIsland = isLocationInIsland(blockLocation, IslandManager.getIslandByPlayerUUID(player.getUniqueId().toString()));

        // Handle block breaking (left-clicking)
        if (action == Action.LEFT_CLICK_BLOCK) {
            if (!isWithinIsland) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You can't break blocks outside your island!").color(NamedTextColor.RED));
            }
            return;
        }

        // Handle interacting with blocks (right-clicking interactable blocks)
        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (!isWithinIsland) {
                switch (blockType) {
                    case ENDER_CHEST:
                    case TRAPPED_CHEST:
                    case BARREL:
                    case SHULKER_BOX:
                    case CHEST:
                    case HOPPER:
                    case HOPPER_MINECART:
                    case CHEST_MINECART:
                    case DISPENSER:
                    case DROPPER:
                        player.sendMessage(Component.text("You can't open containers outside your island!").color(NamedTextColor.RED));
                        event.setCancelled(true);
                        return;
                    case ANVIL:
                    case FURNACE_MINECART:
                    case CRAFTING_TABLE:
                    case BREWING_STAND:
                    case FLETCHING_TABLE:
                    case ENCHANTING_TABLE:
                    case GRINDSTONE:
                    case SMITHING_TABLE:
                    case CARTOGRAPHY_TABLE:
                    case LOOM:
                    case STONECUTTER:
                        player.sendMessage(Component.text("You can't use workstations outside your island!").color(NamedTextColor.RED));
                        event.setCancelled(true);
                        event.setCancelled(true);
                        return;
                    case SMOKER:
                    case BLAST_FURNACE:
                    case FURNACE:
                        player.sendMessage(Component.text("You can't use furnaces outside your island!").color(NamedTextColor.RED));
                        event.setCancelled(true);
                        return;
                    case LEVER:
                        player.sendMessage(Component.text("You can't use levers outside your island!").color(NamedTextColor.RED));
                        event.setCancelled(true);
                        return;
                    default:
                        if (blockType.name().contains("DOOR") || blockType.name().contains("GATE")) {
                            player.sendMessage(Component.text("You can't interact with doors or gates outside your island!").color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        }
                        else if (blockType.name().contains("BUTTON")) {
                            player.sendMessage(Component.text("You can't interact with buttons outside your island!").color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        }
                        else if (blockType.name().contains("SIGN")) {
                            player.sendMessage(Component.text("You can't interact with signs outside your island!").color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        }
                        else if (blockType.name().contains("BED")) {
                            player.sendMessage(Component.text("You can't interact with beds outside your island!").color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        } 
                }
            }
        }

        // Get hand that the player is using
        EquipmentSlot hand = event.getHand();

        // Get the item in the player's hand
        ItemStack itemInHand = hand == EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();

        boolean playerHasItemInHand = itemInHand != null && itemInHand.getType() != Material.AIR;

        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        boolean playerIsHoldingBlock = playerHasItemInHand && itemInHand.getType().isBlock();

        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (!isWithinIsland && playerIsHoldingBlock) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You can't place blocks outside your island!").color(NamedTextColor.RED));
            }
            else if(!isWithinIsland && playerHasItemInHand){
                boolean isItemFood = itemInHand.getType().isEdible();
                if(isItemFood)
                    return;
                
                boolean isItemArmor = itemInHand.getType().name().contains("HELMET") || itemInHand.getType().name().contains("CHESTPLATE") || itemInHand.getType().name().contains("TUNIC") || itemInHand.getType().name().contains("PANTS") || itemInHand.getType().name().contains("CAP") || itemInHand.getType().name().contains("LEGGINGS") || itemInHand.getType().name().contains("BOOTS");
                if(isItemArmor)
                    return;

                event.setCancelled(true);
                player.sendMessage(Component.text("You can't use this outside your island!").color(NamedTextColor.RED));
            }
            return;
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
        return isLocationInIsland(playerLocation, playersIsland);
    }

    public static boolean isLocationInIsland(Location location, Island island) {
        if (island == null) return false;

        Location islandCenter = island.getIslandCenter();
        islandCenter.setY(location.getY());

        if (!location.getWorld().getName().equals(islandCenter.getWorld().getName())) return false;

        double radiusSquared = island.getRadius() * island.getRadius();
        return islandCenter.distanceSquared(location) < radiusSquared;
    }

    //TODO if not on friends or the owner, prevent damaging, set to adventure mode
}