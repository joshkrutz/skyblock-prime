package dev.krutz.mc.skyblock;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * This singleton controller enables all island management tasks, such as creating, resetting, and teleporting to islands.
 */
public class IslandManager {
    private static IslandManager instance;

    // Configurable values
    private static final String ISLANDS_FILE = "island-data.json"; // Path to your islands data file
    private static final long SCORE_TASK_INTERVAL = 100L; // Interval in ticks to calculate island scores
    private static final long SAVE_TASK_INTERVAL = 20 * 60 * 5; // Interval in ticks to save island data to file
    private static final long INVITATION_EXPIRATION_TIME = 20 * 30; // Time in ticks before an invitation expires
    private static final int NUM_ISLANDS_IN_TOP_ISLANDS_MESSAGE = 10; // Number of islands to display in the top islands message

    // Instance variables
    private final JavaPlugin plugin;
    private Map<Integer, Island> islands = new ConcurrentHashMap<>();
    private Map<String, Island> islandInvites = new ConcurrentHashMap<>();
    private Map<Integer, Double> islandScores = new ConcurrentHashMap<>();

    private BukkitTask taskScore;
    private BukkitTask taskAutosave;

    /**
     * Get the instance of the IslandManager singleton.
     * @param plugin - JavaPlugin instance (usually the main class of your plugin)
     * @return The IslandManager instance
     */
    public static synchronized IslandManager getInstance(JavaPlugin plugin) {
        // Create a new instance if it doesn't exist
        if (instance == null) {
            instance = new IslandManager(plugin);
        }
        return instance;
    }

    /**
     * Cancel all tasks
     */
    public static synchronized void cancelTasks() {
        if (instance != null) {
            instance.plugin.getServer().getScheduler().cancelTasks(instance.plugin);

            if (instance.taskScore != null) {
                instance.taskScore.cancel();
            }

            if (instance.taskAutosave != null) {
                instance.taskAutosave.cancel();
            }
        }
    }

    /**
     * Private constructor to prevent instantiation from outside the class.
     * Establishes island synchronization tasks, loads island data from file,
     * and initializes relevant island data structures.
     * @param plugin - JavaPlugin instance (usually the main class of your plugin)
     */
    private IslandManager(JavaPlugin plugin) {
        this.plugin = plugin;

        startAsyncTasks();
        loadData();
    }

    /**
     * Start all necessary asynchronous tasks. Including
     * tasks to calculate island scores.
     */
    private void startAsyncTasks(){
        if(plugin.isEnabled())
        {
            calculateIslandScoresAsync();
            Bukkit.getLogger().info("Island score calculation task started.");
            saveDataToFileAsync();
            Bukkit.getLogger().info("Island data autosave task started.");
        }
    }

    /**
     * Calculate the scores of all islands asynchronously.
     * This task runs every SCORE_TASK_INTERVAL ticks. 
     * This value can be changed in the config.
     */
    public void calculateIslandScoresAsync() {
        taskScore = new BukkitRunnable() {
            @Override
            public void run() {
                for (Island island : islands.values()) {
                    island.calculateScore();
                    islandScores.put(island.getIndex(), island.getScore());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, SCORE_TASK_INTERVAL);
    }

    /**
     * Automatically save island data to file every SAVE_TASK_INTERVAL minutes.
     */
    public void saveDataToFileAsync() {
        taskAutosave = new BukkitRunnable() {
            @Override
            public void run() {
                saveData();
            }
        }.runTaskTimerAsynchronously(plugin, 0, SAVE_TASK_INTERVAL);
    }

    /**
     * Get the file where island data is stored. ISLANDS_FILE is configured in the config.
     * @return File object representing the island data file
     */
    private File getIslandFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            plugin.getLogger().info("Creating plugin data folder...");
            dataFolder.mkdir();
        }
        return new File(dataFolder, ISLANDS_FILE);
    }

    /**
     * Load island data from file and populate the islands map.
     */
    private void loadData() {
        try (FileReader reader = new FileReader(getIslandFile())) 
        { 
            Gson gson = new Gson(); 
            ArrayList<Island> loadedIslands = gson.fromJson(reader, new TypeToken<List<Island>>() {}.getType());
            if (loadedIslands != null) {
                for (Island island : loadedIslands) {
                    islands.put(island.getIndex(), island);
                }
                // Get last island index
                Island.setLastIslandIndex(loadedIslands.size());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Save island data to file.
     */
    public void saveData(){
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create(); 
        String json = gson.toJson(islands.values()); 
        
        try (FileWriter writer = new FileWriter(getIslandFile())) 
        { 
            writer.write(json);

            // Mark islands as not modified after saving 
            islands.values().forEach(island -> island.setModified(false));

            Bukkit.getLogger().info("Island data autosaved to file.");
        } catch (IOException e) { 
            e.printStackTrace();
        }
    } 

    /**
     * Reset the player's state and clear their island.
     * This will reset inventory, ender chest, health, food level, experience, and challenges.
     * @param player
     */
    private void resetPlayerData(Player player){
        // Reset player's state
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setExp(0);
        player.setHealth(20);
        player.setFoodLevel(20);

        // Reset player's challenge progress
        ChallengeManager.resetPlayerChallenges(player);
   }

    /**
     * Get island at the specified index.
     * @param index
     * @return Island object or null if not found
     */
    public Island getIslandByIndex(int index){
        return islands.get(index);
    }

    /**
     * Get island that a player owns.
     * @param uuid - Player UUID
     * @return Island object or null if not found
     */
    public Island getIslandByOwnerUUID(UUID uuid){
        for (Island island : islands.values()) {
            if(island == null){
                continue;
            }

            if(island.hasOwner(uuid)){
                return island;
            }
        }

        return null;
    }

    /**
     * Get island that a player belongs to.
     * @param uuid - Player UUID
     * @return Island object or null if not found
     */
    public Island getIslandByPlayerUUID(UUID uuid){
        for (Island island : islands.values()) {
            if(island == null){
                continue;
            }

            if(island.hasOwner(uuid)){
                return island;
            }

            if(island.hasFriend(uuid)){
                return island;
            }
        }

        return null;
    }

    /**
     * Get all skyblock islands.
     * @return List of all islands
     */
    public ArrayList<Island> getAllIslands() {
        return islands.values().stream().collect(Collectors.toCollection(ArrayList::new));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// Command methods
    /// ============================================================================================
    /// These methods are called by the command manager to handle island-related commands.
    /// They are specifically responsible for creating, resetting, and managing islands.
    /// They also handle inviting, kicking, and promoting players within an island.
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Teleport a player to their island. If the player does not have an island, nothing happens.
     * @param player
     */
    public void teleportToIsland(Player player){
        // Check if the player has an island
        Island island = getIslandByPlayerUUID(player.getUniqueId());

        if( island == null){
            player.sendMessage("You do not have an island to teleport to.");
            return;
        }

        player.teleport(island.getIslandSpawn());
        return;
    }

    /**
     * Restart the player's island. This will clear the island and reset it to its default state. 
     * The player will also have their state reset and be teleported to the new island spawn point.
     * If the player does not have an island, nothing happens.
     * @param player
     */
    public void restartIsland(Player player){
        Island island = getIslandByPlayerUUID(player.getUniqueId());

        // Check if the player has an island
        if(island == null){
            player.sendMessage("You do not have an island to restart.");
            return;
        }

        boolean playerOwnsIsland = island.hasOwner(player.getUniqueId());

        // Check if the player owns the island
        if (!playerOwnsIsland) {
            player.sendMessage("You cannot restart an island that you do not own. Use /island leave to leave this island and /island create to make a new one.");
            return;
        }

        ArrayList<Player> islandPlayers = new ArrayList<>();
        islandPlayers.add(player);
        for(IslandFriend friend : island.getFriends()){
            Player playerFriend = Bukkit.getPlayer(friend.getUUID());
            if(playerFriend != null){
                islandPlayers.add(playerFriend);
            }
        }

        for(Player p : islandPlayers){
            // If they are in the skyblock world, move player to spawn
            if( p.getLocation().getWorld().getName().equals(Main.skyblockWorldName))
            p.teleport(Bukkit.getWorld(Main.spawnWorldName).getSpawnLocation());
            
            // Reset player's state
            resetPlayerData(p);

            // Notify the player
            if (p.equals(player))
                p.sendMessage("Your island has been reset. Use /island home to teleport to your new island.");
            else
                p.sendMessage("The island has been reset. Please wait for the owner to invite you back or create your own island.");
            
        }

         // Clear the island
         island.clearIslandBlocks();

         // Reset the island data to default and rebuild the island
         island.resetIslandData();
         island.buildIsland();

         player.teleport(island.getIslandSpawn());
    }

    /**
     * Set the player's island spawn point to their current location.
     * If the player does not have an island, nothing happens.
     * If the player is not on their island, nothing happens.
     * @param player
     */
    public void setIslandSpawn(Player player){
        // Check that player has an island
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to set the spawn for.");
            return;
        }

        // Check that player is on their island
        if(!IslandListener.isPlayerOnTheirIsland(player, island)){
            player.sendMessage("You must be on your island to set the spawn point.");
            return;
        }

        // Set the island spawn point
        island.setIslandSpawn(player.getLocation());

        // Notify the player
        player.sendMessage("Island spawn point set to your current location.");
    }

    /**
     * Set the player's island warp point to their current location.
     * If the player does not have an island, nothing happens.
     * If the player is not on their island, nothing happens.
     * @param player
     */
    public void setIslandWarp(Player player){
        // Check that player has an island
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to set the waro for.");
            return;
        }

        // Check that player is on their island
        if(!IslandListener.isPlayerOnTheirIsland(player, island)){
            player.sendMessage("You must be on your island to set the warp point.");
            return;
        }

        // Set the island spawn point
        island.setIslandWarp(player.getLocation());

        // Notify the player
        player.sendMessage("Island warp point set to your current location.");
    }

    /**
     * Create a new island for the player. If the player belongs to a party, they will be prompted to leave it first.
     * If the player owns an island, they will be prompted to restart it instead.
     * @param player
     */
    public void createIsland(Player player) {
        Island oldIsland = getIslandByPlayerUUID(player.getUniqueId());
        boolean playerOwnsIsland = oldIsland != null && oldIsland.hasOwner(player.getUniqueId());
        boolean playerBelongsToIsland = oldIsland != null;

        if (playerBelongsToIsland && !playerOwnsIsland) {
            player.sendMessage("You must leave this island before creating a new one. Use /island leave to leave this island.");
            return;
        }

        if (playerOwnsIsland) {
            player.sendMessage("You already have an island. Use /island restart to reset your island.");
            return;
        }

        Island newIsland = new Island(player);
        newIsland.buildIsland();

        islands.put(newIsland.getIndex(), newIsland);

        // Teleport the player to the new island spawn point
        player.teleport(newIsland.getIslandSpawn());
    }

    /**
     * Teleport the player to a party member.
     * If the player does not have an island, nothing happens.
     * @param player
     * @param args - Player name to teleport to
     */
    public void teleportToIslandFriend(Player player, String[] args) {
        if(args.length < 1){
            player.sendMessage("Usage: /island teleport <friend>");
            return;
        }

        String friendName = args[1];

        // Check if friend is in friend list
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        Player friend = Bukkit.getPlayer(friendName);

        // Check if player is online
        if(friend == null){
            player.sendMessage("Player " + friendName + " is not online.");
            return;
        }

        // Check if player is in friend list
        if(!island.hasFriend(friend.getUniqueId())){
            player.sendMessage("Player " + friendName + " does not have access to this island.");
            return;
        }

        // Check if friend is in same world
        if(!friend.getLocation().getWorld().getName().equals(island.getIslandSpawn().getWorld().getName())){
            player.sendMessage("Player " + friendName + " is not in the same world as your island.");
            return;
        }
        Location eyeLevelLocation = island.getIslandSpawn();
        eyeLevelLocation.setY(friend.getLocation().getY());
        eyeLevelLocation.setWorld(friend.getLocation().getWorld());

        // If friend is on island
        if(friend.getLocation().toVector().distance(eyeLevelLocation.toVector()) >= ((int) Island.CHUNK_ISLAND_RADIUS * 16)){
            player.sendMessage("Player " + friendName + " is not on the island.");
            return;
        }

        // Teleport the player to the friend's location
        player.teleport(friend.getLocation());
    }

     /**
     * Teleport the player to a the requested island warp location
     * @param player
     * @param args - Optional player name to teleport to
     */
    public void warpTeleport(Player player, String[] args) {
        OfflinePlayer targetPlayer = player;
        if(args.length >= 1){
            targetPlayer = Bukkit.getOfflinePlayer(args[0]);
        }

        // Check if target player has an island
        Island island = getIslandByPlayerUUID(targetPlayer.getUniqueId());
        if(island == null){
            player.sendMessage(targetPlayer.getName() + " does not have an island to warp to.");
            return;
        }

        // Check if target player has a warp location set
        if(island.getIslandWarp() == null){
            player.sendMessage(targetPlayer.getName() + " does not have a warp location set.");
            return;
        }

        // Check if island warp is open
        if(island.isLocked() && !island.hasFriend(player.getUniqueId()) && !island.hasOwner(player.getUniqueId())){
            player.sendMessage(targetPlayer.getName() + "'s island warp is not open.");
            return;
        }

        // Teleport the player to the island warp location
        player.teleport(island.getIslandWarp());
    }


    /**
     * Set the greeting message for the player's island.
     * If the player does not have an island, nothing happens.
     * @param player
     * @param args - Greeting message
     */
    public void setGreeting(Player player, String[] args) {
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        if(args.length < 1){
            player.sendMessage("Invalid syntax. Usage: /island setgreeting <message>");
            return;
        }

        String greeting = String.join(" ", args);
        island.setGreetingMessage(greeting);
    }

    /**
     * Set the farewell message for the player's island.
     * If the player does not have an island, nothing happens.
     * @param player
     * @param args - Farewell message
     */
    public void setFarewell(Player player, String[] args) {
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        if(args.length < 1){
            player.sendMessage("Invalid syntax. Usage: /island setfarewell <message>");
            return;
        }

        String farewell = String.join(" ", args);
        island.setFarewellMessage(farewell);
    }

    /**
     * Toggle the island warp lock status.
     * @param player
     */
    public void toggleWarpLock(Player player){
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        if(island.isLocked()){
            island.unlockIsland();
        }
        else{
            island.lockIsland();
        }

        player.sendMessage("Island warp is now " + (island.isLocked() ? "locked" : "unlocked") + ".");
    }

    /**
     * Unlock the island warp
     * @param player
     */
    public void unlockWarp(Player player){
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        if(island.isLocked()){
            player.sendMessage("Island warp is already unlocked.");
            return;
        }

        island.unlockIsland();
        player.sendMessage("Island warp is now unlocked.");
    }

    /**
     * Lock the island warp
     * @param player
     */
    public void lockWarp(Player player){
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if(island == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        if(island.isLocked()){
            player.sendMessage("Island warp is already locked.");
            return;
        }

        island.lockIsland();
        player.sendMessage("Island warp is now locked.");
    }

    /**
     * Invite a friend to the player's island.
     * If the player does not have an island, nothing happens.
     * @param player
     * @param args - Friend name to invite
     */
    public void invitePlayerToIsland(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("Invalid syntax. Usage: /island invite <player>");
            return;
        }

        String friendName = args[0];
        Player invitedPlayer = Bukkit.getPlayer(friendName);

        if (invitedPlayer == null) {
            player.sendMessage("Player " + friendName + " is not online.");
            return;
        }

        Island island = getIslandByOwnerUUID(player.getUniqueId());
        if (island == null) {
            player.sendMessage("You do not have an island to invite players to.");
            return;
        }

        if(player.getUniqueId().toString().equals(invitedPlayer.getUniqueId().toString())){
            player.sendMessage("You cannot invite yourself to your own island.");
            return;
        }

        if (island.hasFriend(invitedPlayer.getUniqueId())) {
            player.sendMessage("Player " + friendName + " is already a party member.");
            return;
        }

        if(island.hasBanned(invitedPlayer.getUniqueId())){
            unbanPlayerFromIsland(player, args);
        }

        sendInvitation(island, invitedPlayer);
        player.sendMessage("Invitation sent to " + friendName + ".");
    }

    /**
     * Send an invitation with a INVITATION_EXPIRATION_TIME timeout. This value can be changed in the config.
     * Invitations will expire after this time and a new invitation will need to be sent.
     * Invitations sent to the same player will overwrite the previous invitations (this includes invites sent from different islands).
     * @param islandToJoin
     * @param invitedPlayer
     */
    private void sendInvitation(Island islandToJoin, Player invitedPlayer) {
        invitedPlayer.sendMessage("You have been invited to join " + islandToJoin.getName() + ". Use /is accept to join or /is reject to ignore.");
        invitedPlayer.sendMessage("This invitation will expire in 30 seconds.");
        islandInvites.put(invitedPlayer.getUniqueId().toString(), islandToJoin);

        // Create a task to remove the invitation after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if(islandInvites.containsKey(invitedPlayer.getUniqueId().toString()) && islandInvites.get(invitedPlayer.getUniqueId().toString()).equals(islandToJoin))
            {
                invitedPlayer.sendMessage("The invitation to join " + islandToJoin.getName() + " has expired.");
                islandInvites.remove(invitedPlayer.getUniqueId().toString());

                Player owner = Bukkit.getPlayer(islandToJoin.getOwnerUUID());
                if (owner != null) {
                    owner.sendMessage("Your invitation to " + invitedPlayer.getName() + " has expired.");
                }
            } 
        }, INVITATION_EXPIRATION_TIME);
    }

    /**
     * If an invitation exists then the player can accept it and join the invitor's island.
     * If multiple invitations were sent, the player can only accept the most recent one.
     * If the player has an island, it will be deleted and the player's state will be reset.
     * The island isn't truly cleared but the player can no longer access it (creates a ghost island).
     * @param player
     */
    public void acceptIslandInvite(Player player) {
        if (!islandInvites.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("You do not have any pending island invitations.");
            return;
        }

        // If player has an island, remove them from it and delete it
        Island currentIsland = getIslandByPlayerUUID(player.getUniqueId());
        if (currentIsland != null) {
            // If player is the owner, delete the island
            if (currentIsland.getOwnerUUID().equals(player.getUniqueId())) {
                islands.remove(currentIsland.getIndex());
                islandScores.remove(currentIsland.getIndex());
            } else {
                // Remove player from friends list
                currentIsland.removeFriend(player.getUniqueId());
            }
        }

        // Add to new island
        Island islandToJoin = islandInvites.get(player.getUniqueId().toString());
        islandToJoin.addFriend(player.getUniqueId());

        // Reset player's state
        resetPlayerData(player);

        player.sendMessage("You have joined " + islandToJoin.getName() + ".");

        Player owner = Bukkit.getPlayer(islandToJoin.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage(player.getName() + " has joined your island.");
        }

        islandInvites.remove(player.getUniqueId().toString());
        player.teleport(islandToJoin.getIslandSpawn());
    }

    /**
     * If an invitation exists then the player can reject it and ignore the invitor's island.
     * @param player
     */
    public void rejectIslandInvite(Player player) {
        if (!islandInvites.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("You do not have any pending island invitations.");
            return;
        }

        Island islandToReject = islandInvites.get(player.getUniqueId().toString());
        islandInvites.remove(player.getUniqueId().toString());

        player.sendMessage("You have rejected the invitation to join " + islandToReject.getName() + ".");
        Player owner = Bukkit.getPlayer(islandToReject.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage(player.getName() + " has rejected the invitation to join your island.");
        }
    }

    /**
     * Ban a player from entering the island. If the banned player is a party member, they will also be removed from the party.
     * @param player
     * @param args
     */
    public void banPlayerFromIsland(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("Invalid syntax. Usage: /island ban <player>");
            return;
        }

        Island island = getIslandByOwnerUUID(player.getUniqueId());
        if (island == null) {
            player.sendMessage("You do not own an island to ban players from.");
            return;
        }

        String bannedPlayerName = args[0];
        OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(bannedPlayerName);

        if (bannedPlayer == null) {
            player.sendMessage(bannedPlayerName + " could not be found.");
            return;
        }

        // Prevent banning self
        if (bannedPlayer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("You cannot ban yourself from the island.");
            return;
        }

        if (island.hasFriend(bannedPlayer.getUniqueId())) {
            kickPlayerFromIsland(player, args);
        }

        // If player is already banned, do nothing
        if (island.hasBanned(bannedPlayer.getUniqueId())) {
            player.sendMessage(bannedPlayerName + " is already banned from the island.");
            return;
        }

        island.banPlayer(bannedPlayer.getUniqueId());
        player.sendMessage(bannedPlayerName + " has been banned from the island.");

        if (bannedPlayer.isOnline()) {
            bannedPlayer.getPlayer().sendMessage("You have been banned from the island by " + player.getName() + ".");
        }
    }

    /**
     * Unban a player from the island. If the player is not banned, nothing happens.
     * @param player
     * @param args - Player name to unban
     */
    public void unbanPlayerFromIsland(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("Invalid syntax. Usage: /island unban <player>");
            return;
        }

        Island island = getIslandByOwnerUUID(player.getUniqueId());
        if (island == null) {
            player.sendMessage("You do not own an island to pardon players from.");
            return;
        }

        String bannedPlayerName = args[0];
        OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(bannedPlayerName);

        if (bannedPlayer == null) {
            player.sendMessage(bannedPlayerName + " could not be found.");
            return;
        }

        if (!island.hasBanned(bannedPlayer.getUniqueId())) {
            player.sendMessage(bannedPlayerName + " is not banned from the island.");
            return;
        }

        island.unbanPlayer(bannedPlayer.getUniqueId());
        player.sendMessage(bannedPlayerName + " has been pardoned and may re-enter the island.");
        
        if (bannedPlayer.isOnline()) {
            bannedPlayer.getPlayer().sendMessage("You have been pardoned and may re-enter " + island.getName() + ".");
        }
    }

    /**
     * Kick a player from the island party/friends list. The kicked player can no longer build or access the island.
     * @param player
     * @param args - Player name to kick
     */
    public void kickPlayerFromIsland(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("Invalid syntax. Usage: /island kick <player>");
            return;
        }

        String friendName = args[0];
        Player friend = Bukkit.getPlayer(friendName);

        if (friend == null) {
            player.sendMessage("Player " + friendName + " is not online.");
            return;
        }

        Island island = getIslandByOwnerUUID(player.getUniqueId());
        if (island == null) {
            player.sendMessage("You do not have an island to kick players from.");
            return;
        }

        if (!island.hasFriend(friend.getUniqueId())) {
            player.sendMessage("Player " + friendName + " is not an island party member.");
            return;
        }

        island.removeFriend(friend.getUniqueId());
        resetPlayerData(friend);

        player.sendMessage("Player " + friendName + " has been kicked from the island.");
        friend.sendMessage("You have been kicked from the island by " + player.getName() + ".");
        friend.teleport(Bukkit.getWorld(Main.spawnWorldName).getSpawnLocation());
    }

    /**
     * Leave the friend's island. If the player is the owner, they must promote another player to leader or restart the island.
     * @param player
     */
    public void leaveIsland(Player player) {
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if (island == null) {
            player.sendMessage("You do not have an island to leave.");
            return;
        }

        if (island.hasOwner(player.getUniqueId())){
            player.sendMessage("You cannot leave an island that you own. Use /is promote to transfer leadership. Or use /is restart to restart.");
            return;
        }

        island.removeFriend(player.getUniqueId());

        // Reset player's state
        resetPlayerData(player);

        player.sendMessage("You have left " + island.getName() + ".");
        player.teleport(Bukkit.getWorld(Main.spawnWorldName).getSpawnLocation());
    }

    /**
     * Promote a player to island leader. The previous owner will be demoted to a party member.
     * @param sender
     * @param args - Player name to promote
     */
    public void makeIslandLeader(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Invalid syntax. Usage: /island promote <player>");
            return;
        }

        String newLeaderName = args[0];
        UUID newLeaderUUID = Bukkit.getOfflinePlayer(newLeaderName).getUniqueId();

        Island island = getIslandByOwnerUUID(sender.getUniqueId());
        if (island == null) {
            sender.sendMessage("You do not have an island to promote players in.");
            return;
        }

        if (!island.hasFriend(newLeaderUUID)) {
            sender.sendMessage("Player " + newLeaderName + " is not an island party member.");
            return;
        }

        island.setOwnerUUID(newLeaderUUID);

        // Add the old owner to the friends list
        island.addFriend(sender.getUniqueId());

        sender.sendMessage("You have promoted " + newLeaderName + " to island leader.");

        Player newLeader = Bukkit.getPlayer(newLeaderUUID);
        if(newLeader != null)
            newLeader.sendMessage("You have been promoted to island leader by " + sender.getName() + ".");
    }

    /**
     * Show the player's island party members.
     * @param player
     */
    public void showIslandParty(Player player) {
        Island island = getIslandByPlayerUUID(player.getUniqueId());
        if (island == null) {
            player.sendMessage("You do not have an island to view party information for.");
            return;
        }

        player.sendMessage("Island: " + island.getName());
        player.sendMessage("Owner: " + Bukkit.getOfflinePlayer(island.getOwnerUUID()).getName());
        player.sendMessage("Friends: ");
        for (IslandFriend friend : island.getFriends()) {
            player.sendMessage("* " + Bukkit.getOfflinePlayer(friend.getUUID()).getName());
        }
    }

    /**
     * Abstracted function that calls the helper function to show island info for a player.
     * @param sender
     * @param args - Player name to show island info for
     */
    public void showIslandInfo(CommandSender sender, String[] args) {
        if (args.length < 1 && !(sender instanceof Player)) {
            sender.sendMessage("Invalid syntax. Usage: /island info <player>");
            return;
        }

        if (args.length < 1 && sender instanceof Player player) {
            showIslandInfoHelper(sender, player.getUniqueId());
            return;
        }

        String playerName = args[0];
        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();

        showIslandInfoHelper(sender, playerUUID);
    }

    /**
     * Helper function to show island info, including rank, score, and top 10 contributing blocks.
     * @param sender
     * @param playerUUID
     */
    private void showIslandInfoHelper(CommandSender sender, UUID playerUUID){
        Island island = getIslandByPlayerUUID(playerUUID);
        if (island == null) {
            sender.sendMessage(Bukkit.getOfflinePlayer(playerUUID).getName() + " does not have an island.");
            return;
        }

        // Calculate score
        double score = 0;
        island.calculateScore();
        score = island.getScore();
        Map<String, Double> scoreBreakdown = island.getScoreBreakdown();

        //sort from highest to lowest
        scoreBreakdown = scoreBreakdown.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        sender.sendMessage("Island: " + island.getName());
        sender.sendMessage("Rank: " + getRank(island) + "/" + islands.size());
        sender.sendMessage("Score: " + String.format("%.2f", score));
        
        // Display top 10 blocks
        int i = 0;
        for (Map.Entry<String, Double> entry : scoreBreakdown.entrySet()) {
            if(i >= 10)
                break;
            String key = entry.getKey();
            String block_key = key.replace("_", " ");
            String[] words = block_key.split(" ");
            words = Arrays.stream(words).map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()).toArray(String[]::new);
            block_key = String.join(" ", words);

            sender.sendMessage("* " + block_key + ": " + String.format("%.2f", entry.getValue()));
            i++;
        }
    }

    /**
     * Show the top 10 islands based on score.
     */
    public void sendTopIslandsMsg(CommandSender sender){
        if(islandScores.isEmpty()){
            sender.sendMessage("No islands have been created yet.");
            return;
        }

        islandScores = islandScores.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int numTop = Math.min(NUM_ISLANDS_IN_TOP_ISLANDS_MESSAGE, islands.size());

        sender.sendMessage("Top " + numTop + " Islands:");
        int i = 0;
        for (Map.Entry<Integer, Double> entry : islandScores.entrySet()) {
            if(i >= 10)
                break;
            Island island = getIslandByIndex(entry.getKey());
            if(island == null){
                continue;
            }
            sender.sendMessage(i + 1 + ". " + island.getName() + " - " + String.format("%.2f", entry.getValue()));
            i++;
        }
    }

    /**
     * Get the rank of an island based on its score compared to all other islands.
     * @param island
     * @return Rank of the island
     */
    private int getRank(Island island) {

        if (island == null) {
            return -1;
        }

        if (islandScores.isEmpty()) {
            return -1;
        }

        islandScores = islandScores.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        
        return new ArrayList<>(islandScores.keySet()).indexOf(island.getIndex()) + 1;
    }

    /**
     * Set the biome of the player's island to the specified biome.
     * If the player does not have an island, nothing happens.
     * @param player
     * @param args - Biome name
     */
    public void setIslandBiome(Player player, String[] args) {
        if(args.length < 1){
            player.sendMessage("Invalid syntax. Usage: /island setbiome <biome>");
            return;
        }

        Island island = getIslandByPlayerUUID(player.getUniqueId());

        if(island == null){
            player.sendMessage("You do not have an island to set the biome for.");
            return;
        }

        // List of supported biomes
        Map<String, Biome> availableBiomes = Map.ofEntries(
            Map.entry("PLAINS", Biome.PLAINS),
            Map.entry("DESERT", Biome.DESERT),
            Map.entry("FOREST", Biome.FOREST),
            Map.entry("SWAMP", Biome.SWAMP),
            Map.entry("TAIGA", Biome.TAIGA),
            Map.entry("SNOWY_TAIGA", Biome.SNOWY_TAIGA),
            Map.entry("SAVANNA", Biome.SAVANNA),
            Map.entry("JUNGLE", Biome.JUNGLE),
            Map.entry("BADLANDS", Biome.BADLANDS),
            Map.entry("OCEAN", Biome.OCEAN),
            Map.entry("RIVER", Biome.RIVER),
            Map.entry("MUSHROOM_FIELDS", Biome.MUSHROOM_FIELDS),
            Map.entry("DRIPSTONE_CAVES", Biome.DRIPSTONE_CAVES),
            Map.entry("LUSH_CAVES", Biome.LUSH_CAVES),
            Map.entry("DEEP_DARK", Biome.DEEP_DARK),
            //Map.entry("PALE_GARDEN", Biome.PALE_GARDEN), // Not available in 1.21.3
            Map.entry("MEADOW", Biome.MEADOW)
        );

        // Sort the biomes alphabetically
        availableBiomes = availableBiomes.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        String biomeName = args[0].toUpperCase();

        if(!availableBiomes.containsKey(biomeName)){
            player.sendMessage("Invalid biome. Available biomes: " + String.join(", ", availableBiomes.keySet()));
            return;
        }

        Biome biome = availableBiomes.get(biomeName);

        if (biome == null) {
            player.sendMessage("Invalid biome. Available biomes: " + String.join(", ", availableBiomes.keySet()));
            return;
        }

        // Set the biome on each block in the island radius
        Location islandCenter = island.getIslandCenter();
        int radius = island.getRadius();
        World world = island.getIslandSpawn().getWorld();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = world.getMinHeight(); y <= world.getMaxHeight(); y++) {
                    Location blockLocation = islandCenter.clone().add(x, y, z);
                    if (blockLocation.getBlock().getBiome() != biome) {
                        blockLocation.getBlock().setBiome(biome);
                    }
                }
            }
        }

        // Trigger player chunk refresh to see changes (otherwise requires player to relog)
        for (Chunk chunk :  island.getChunks()){
            // Update the biome for all players near the chunk
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Player) {
                    Player nearbyPlayer = (Player) entity;
                    nearbyPlayer.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
                }
            }
        }

        player.sendMessage("Your island biome has been set to " + biomeName.toLowerCase().replace("_", " ") + ".");
    }
}