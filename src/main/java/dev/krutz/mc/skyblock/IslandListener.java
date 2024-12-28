package dev.krutz.mc.skyblock;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;


public class IslandListener implements Listener {

    private final Map<Player, Island> lastIslandMap = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

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

    //TODO if not on friends or the owner, prevent damaging, set to adventure mode
}