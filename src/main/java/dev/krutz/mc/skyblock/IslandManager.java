package dev.krutz.mc.skyblock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import org.json.JSONArray;

public class IslandManager {
    private static ArrayList<Island> islands = new ArrayList<>();
    private static Map<String, Island> islandInvites = new HashMap<>();

    private static final String ISLANDS_FILE = "island-data.json"; // Path to your islands data file

    private static JavaPlugin plugin;

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public static void initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;

        //populate islands from file
        try{
            JSONArray islandsData = loadIslandData();

            if (islandsData == null) {
                return;
            }

            for(int i = 0; i < islandsData.length(); i++){
                JSONObject islandData = islandsData.getJSONObject(i);
                int index = islandData.getInt("index");

                // Fill in any gaps in the islands list to enable O(1) lookup
                while(index >= islands.size() - 1){
                    islands.add(null);
                }

                int x = islandData.getInt("x");
                int z = islandData.getInt("z");
                
                String name = islandData.getString("name");
                String owner = islandData.getString("owner");   
                List<String> friends = new ArrayList<>();
                for (Object friend : islandData.getJSONArray("friends")) {
                    friends.add((String) friend);
                }
                String enterMessage = islandData.getString("greeting_message");
                String exitMessage = islandData.getString("farewell_message");
                JSONObject islandLocationData = islandData.getJSONObject("island_spawn");
                float spawnX = (float) islandLocationData.getDouble("spawn_x");
                float spawnY = (float) islandLocationData.getDouble("spawn_y");
                float spawnZ = (float) islandLocationData.getDouble("spawn_z");
                float spawnYaw = (float) islandLocationData.getDouble("spawn_yaw");
                float spawnPitch = (float) islandLocationData.getDouble("spawn_pitch");
                World w = Bukkit.getWorld(islandLocationData.optString("spawn_world", Main.skyblockOverworldName));
                Location islandSpawn = new Location(w, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);

                islands.add(new Island(x, z, name, index, owner, friends, enterMessage, exitMessage, islandSpawn));
            }
            
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void saveDataToFile(){
        try{
            JSONArray islandsData = loadIslandData();

            Bukkit.getLogger().info("Saving islands. There are " + islands.size() + " islands total to iterate over");

            if (islandsData == null) {
                Bukkit.getLogger().info("islandsData returned null... ending here");
                return;
            }

            for (Island curIsland : islands) {
                if(curIsland == null || !curIsland.changed)
                    continue;

                

                JSONObject island = new JSONObject();
                int json_index = -1;
                for(int i = 0; i < islandsData.length(); i++){
                    JSONObject curIslandData = islandsData.getJSONObject(i);
                    if(curIslandData.getInt("index") == curIsland.getIndex()){
                        island = curIslandData;
                        json_index = i;
                        break;
                    }
                }

                island.put("owner", curIsland.getOwnerUUID());
                island.put("farewell_message", curIsland.getFarewellMessage());
                island.put("greeting_message", curIsland.getGreetingMessage());
                island.put("x", curIsland.getX());
                island.put("name", curIsland.getName());
                island.put("index", curIsland.getIndex());

                JSONObject islandLocation = new JSONObject();
                islandLocation.put("spawn_x", curIsland.getIslandSpawn().getX());
                islandLocation.put("spawn_y", curIsland.getIslandSpawn().getY());
                islandLocation.put("spawn_z", curIsland.getIslandSpawn().getZ());
                islandLocation.put("spawn_yaw", curIsland.getIslandSpawn().getYaw());
                islandLocation.put("spawn_pitch", curIsland.getIslandSpawn().getPitch());
                islandLocation.put("spawn_world", curIsland.getIslandSpawn().getWorld().getName());

                island.put("island_spawn", islandLocation);
                island.put("z", curIsland.getZ());

                JSONArray friendsArray = new JSONArray();
                for (String friend : curIsland.getFriends()) {
                    friendsArray.put(friend);
                }
                island.put("friends", friendsArray);

                if(json_index > -1)
                    islandsData.put(json_index, island);
                else
                    islandsData.put(island);

                // Write json to file
                File file = getIslandFile();
                if (!file.exists()) {
                    plugin.getLogger().info("Creating island file...");
                    file.createNewFile();
                }
                Files.write(file.toPath(), islandsData.toString(4).getBytes());
                curIsland.changed = false;

                Bukkit.getLogger().info("Island updates saved to file.");
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }   

    public static void updateIsland(Island updatedIsland){
        updatedIsland.changed = true;
        islands.set(updatedIsland.getIndex(), updatedIsland);
        saveDataToFile();
    }

    public static Island getIslandByIndex(int index){
        return islands.get(index);
    }

    public static Island getIslandByOwnerUUID(String uuid){
        for (Island island : islands) {
            if(island == null){
                continue;
            }

            if(island.getOwnerUUID().equals(uuid)){
                return island;
            }
        }

        return null;
    }

    public static Island getIslandByPlayerUUID(String uuid){
        for (Island island : islands) {
            if(island == null){
                continue;
            }

            if(island.getOwnerUUID().equals(uuid)){
                return island;
            }

            if(island.isFriend(uuid)){
                return island;
            }
        }

        return null;
    }

    public static void teleportToIsland(Player player){
        // Check if the player has an island
        Island island = getIslandByPlayerUUID(player.getUniqueId().toString());

        if( island == null){
            return;
        }

        player.teleport(island.getIslandSpawn());
        return;
    
    }

    public static void restartIsland(Player player){
        Island island = getIslandByPlayerUUID(player.getUniqueId().toString());

        // Check if the player has an island
        if(island == null){
            player.sendMessage("You do not have an island to restart.");
            return;
        }

        boolean playerOwnsIsland = island.getOwnerUUID().equals(player.getUniqueId().toString());

        // Check if the player owns the island
        if (!playerOwnsIsland) {
            player.sendMessage("You cannot restart an island that you do not own. Use /island leave to leave this island and /island create to make a new one.");
            return;
        }

        // Move player to spawn, if they are in the skyblock world
        if( player.getLocation().getWorld().getName().equals(Main.skyblockOverworldName)){
            player.teleport(Bukkit.getWorld(Main.spawnWorldName).getSpawnLocation());
        }

        // Reset player's state
        resetPlayerData(player);

        // Clear the island
        island.clearIslandBlocks();

        // Reset the island data to default and rebuild the island
        island.resetIslandData();
        island.buildIsland();

        island.setIslandSpawn();

        // Notify the player
        player.sendMessage("Your island has been reset. Use /island home to teleport to your new island.");

        player.teleport(island.getIslandSpawn());
    }

    public static void setIslandSpawn(Player player){
        // Check that player has an island
        if(getIslandByPlayerUUID(player.getUniqueId().toString()) == null){
            player.sendMessage("You do not have an island to set the spawn for.");
            return;
        }

        // Check that player is on their island
        Island island = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(IslandListener.isPlayerOnTheirIsland(player, island)){
            player.sendMessage("You must be on your island to set the spawn point.");
            return;
        }

        // Set the island spawn point
        island.setIslandSpawn(player.getLocation());

        // Save the island data
        island.changed = true;
        saveDataToFile();

        // Notify the player
        player.sendMessage("Island spawn point set to your current location.");
    }

    public static void createIsland(Player player) {
        boolean playerBelongsToIsland = getIslandByPlayerUUID(player.getUniqueId().toString()) != null;
        boolean playerOwnsIsland = getIslandByOwnerUUID(player.getUniqueId().toString()) != null;

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
        newIsland.setIslandSpawn();
        newIsland.changed = true;

        islands.add(newIsland);

        saveDataToFile();

        // Teleport the player to the new island spawn point
        player.teleport(newIsland.getIslandSpawn());
    }

    // Load the island data from the file
    public static JSONArray loadIslandData() throws IOException {
        File file = getIslandFile();
        if (!file.exists()) {
            plugin.getLogger().info("Island file does not exist. Creating a new one...");
            file.createNewFile();
            Files.write(file.toPath(), new JSONArray().toString(4).getBytes());
            return new JSONArray();
        }

        // Read the file content
        String content = new String(Files.readAllBytes(file.toPath()));
        return content.isEmpty() ? new JSONArray() : new JSONArray(content);
    }

    private static File getIslandFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            plugin.getLogger().info("Creating plugin data folder...");
            dataFolder.mkdir();
        }
        return new File(dataFolder, ISLANDS_FILE);
    }

    public static void teleportToIslandFriend(Player player, String[] args) {
        if(args.length < 2){
            player.sendMessage("Usage: /island teleport <friend>");
            return;
        }

        String friendName = args[1];

        // Check if friend is in friend list
        Island islandData = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(islandData == null){
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
        if(!islandData.getFriends().contains(friend.getUniqueId().toString())){
            player.sendMessage("Player " + friendName + " does not have access to this island.");
            return;
        }

        // Check if friend is in same world
        if(!friend.getLocation().getWorld().getName().equals(islandData.getIslandSpawn().getWorld().getName())){
            player.sendMessage("Player " + friendName + " is not in the same world as your island.");
            return;
        }
        Location eyeLevelLocation = islandData.getIslandSpawn();
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

    public static void setGreeting(Player player, String[] args) {
        Island islandData = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(islandData == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        String greeting = String.join(" ", args);
        islandData.setGreetingMessage(greeting);
        updateIsland(islandData);
    }

    public static void setFarewell(Player player, String[] args) {
        Island islandData = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(islandData == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        String farewell = String.join(" ", args);
        islandData.setFarewellMessage(farewell);
        updateIsland(islandData);
    }

    public static ArrayList<Island> getIslands() {
        return islands;
    }

    public static void invitePlayerToIsland(Player player, String[] args) {
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

        Island island = getIslandByOwnerUUID(player.getUniqueId().toString());
        if (island == null) {
            player.sendMessage("You do not have an island to invite players to.");
            return;
        }

        if(player.getUniqueId().toString().equals(invitedPlayer.getUniqueId().toString())){
            player.sendMessage("You cannot invite yourself to your own island.");
            return;
        }

        if (island.isFriend(invitedPlayer.getUniqueId().toString())) {
            player.sendMessage("Player " + friendName + " is already a party member.");
            return;
        }

        sendInvitation(island, invitedPlayer);

        return;
    }

    public static void sendInvitation(Island islandToJoin, Player invitedPlayer) {
        invitedPlayer.sendMessage("You have been invited to join " + islandToJoin.getName() + ". Use /is accept to join or /is reject to ignore.");
        invitedPlayer.sendMessage("This invitation will expire in 30 seconds.");
        islandInvites.put(invitedPlayer.getUniqueId().toString(), islandToJoin);

        // Create a task to remove the invitation after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if(islandInvites.containsKey(invitedPlayer.getUniqueId().toString()) && islandInvites.get(invitedPlayer.getUniqueId().toString()).equals(islandToJoin))
            {
                invitedPlayer.sendMessage("The invitation to join " + islandToJoin.getName() + " has expired.");
                islandInvites.remove(invitedPlayer.getUniqueId().toString());
            }
        }, 20 * 30);
    }

    public static void acceptIslandInvite(Player player) {
        if (!islandInvites.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("You do not have any pending island invitations.");
            return;
        }

        // If player has an island, remove them from it and delete it
        Island currentIsland = getIslandByPlayerUUID(player.getUniqueId().toString());
        if (currentIsland != null) {
            // If player is the owner, delete the island
            if (currentIsland.getOwnerUUID().equals(player.getUniqueId().toString())) {
                islands.remove(currentIsland);
                saveDataToFile();
            } else {
                // Remove player from friends list
                currentIsland.removeFriend(player.getUniqueId().toString());
                updateIsland(currentIsland);
            }
        }

        // Add to new island
        Island islandToJoin = islandInvites.get(player.getUniqueId().toString());
        islandToJoin.addFriend(player.getUniqueId().toString());
        updateIsland(islandToJoin);

        // Reset player's state
        resetPlayerData(player);

        player.sendMessage("You have joined " + islandToJoin.getName() + ".");

        Player owner = Bukkit.getPlayer(UUID.fromString(islandToJoin.getOwnerUUID()));
        if (owner != null) {
            owner.sendMessage(player.getName() + " has joined your island.");
        }

        islandInvites.remove(player.getUniqueId().toString());
        player.teleport(islandToJoin.getIslandSpawn());
    }

    private static void resetPlayerData(Player player){
         // Reset player's state
         player.getInventory().clear();
         player.getEnderChest().clear();
         player.setExp(0);
         player.setHealth(20);
         player.setFoodLevel(20);
 
         // Reset player's challenge progress
         ChallengeManager.resetPlayerChallenges(player);
    }

    public static void rejectIslandInvite(Player player) {
        if (!islandInvites.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("You do not have any pending island invitations.");
            return;
        }

        Island islandToReject = islandInvites.get(player.getUniqueId().toString());
        islandInvites.remove(player.getUniqueId().toString());

        player.sendMessage("You have rejected the invitation to join " + islandToReject.getName() + ".");
        Player owner = Bukkit.getPlayer(UUID.fromString(islandToReject.getOwnerUUID()));
        if (owner != null) {
            owner.sendMessage(player.getName() + " has rejected the invitation to join your island.");
        }
    }

    public static void kickPlayerFromIsland(Player player, String[] args) {
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

        Island island = getIslandByOwnerUUID(player.getUniqueId().toString());
        if (island == null) {
            player.sendMessage("You do not have an island to kick players from.");
            return;
        }

        if (!island.isFriend(friend.getUniqueId().toString())) {
            player.sendMessage("Player " + friendName + " is not an island party member.");
            return;
        }

        island.removeFriend(friend.getUniqueId().toString());
        updateIsland(island);

        player.sendMessage("Player " + friendName + " has been kicked from the island.");
        friend.sendMessage("You have been kicked from the island by " + player.getName() + ".");
        friend.teleport(Bukkit.getWorld(Main.spawnWorldName).getSpawnLocation());
    }

    public static void leaveIsland(Player player) {
        Island island = getIslandByPlayerUUID(player.getUniqueId().toString());
        if (island == null) {
            player.sendMessage("You do not have an island to leave.");
            return;
        }

        if (island.getOwnerUUID().equals(player.getUniqueId().toString())) {
            player.sendMessage("You cannot leave an island that you own. Use /is promote to transfer leadership. Or use /is restart to restart.");
            return;
        }

        island.removeFriend(player.getUniqueId().toString());
        updateIsland(island);

        player.sendMessage("You have left " + island.getName() + ".");
        player.teleport(Bukkit.getWorld(Main.spawnWorldName).getSpawnLocation());
    }

    public static void makeIslandLeader(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Invalid syntax. Usage: /island promote <player>");
            return;
        }

        String newLeaderName = args[0];
        String newLeaderUUID = Bukkit.getOfflinePlayer(newLeaderName).getUniqueId().toString();

        Island island = getIslandByOwnerUUID(sender.getUniqueId().toString());
        if (island == null) {
            sender.sendMessage("You do not have an island to promote players in.");
            return;
        }

        if (!island.isFriend(newLeaderUUID)) {
            sender.sendMessage("Player " + newLeaderName + " is not an island party member.");
            return;
        }

        island.setOwnerUUID(newLeaderUUID);

        // Add the old owner to the friends list
        island.addFriend(sender.getUniqueId().toString());

        updateIsland(island);

        sender.sendMessage("You have promoted " + newLeaderName + " to island leader.");

        Player newLeader = Bukkit.getPlayer(newLeaderUUID);
        if(newLeader != null)
            newLeader.sendMessage("You have been promoted to island leader by " + sender.getName() + ".");
    }

    public static void showIslandParty(Player player) {
        Island island = getIslandByPlayerUUID(player.getUniqueId().toString());
        if (island == null) {
            player.sendMessage("You do not have an island to view party information for.");
            return;
        }

        player.sendMessage("Island: " + island.getName());
        player.sendMessage("Owner: " + Bukkit.getOfflinePlayer(UUID.fromString(island.getOwnerUUID())).getName());
        player.sendMessage("Friends: ");
        for (String friendUUID : island.getFriends()) {
            player.sendMessage("* " + Bukkit.getOfflinePlayer(UUID.fromString(friendUUID)).getName());
        }
    }

    public static void showIslandInfo(CommandSender sender, String[] args) {
        if (args.length < 1 && !(sender instanceof Player)) {
            sender.sendMessage("Invalid syntax. Usage: /island info <player>");
            return;
        }

        if (args.length < 1 && sender instanceof Player player) {
            showIslandInfoHelper(sender, player.getUniqueId().toString());
            return;
        }

        String playerName = args[0];
        String playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();

        showIslandInfoHelper(sender, playerUUID);
    }

    private static void showIslandInfoHelper(CommandSender sender, String playerUUID){
        Island island = getIslandByPlayerUUID(playerUUID);
        if (island == null) {
            sender.sendMessage(Bukkit.getOfflinePlayer(UUID.fromString(playerUUID)).getName() + " does not have an island.");
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
            sender.sendMessage("Score: " + score);
            
            // Display top 10 blocks
            int i = 0;
            for (Map.Entry<String, Double> entry : scoreBreakdown.entrySet()) {
                if(i >= 10)
                    break;
                sender.sendMessage("* " + entry.getKey() + ": " + entry.getValue());
                i++;
            }
    
    }
        
}
        
class Island {
    // TODO Make this pull from a config file
    public static final float CHUNK_ISLAND_RADIUS = 3.5f;
    public static final int CHUNK_BUFFER = 1; // Buffer space between islands
    private static final int MAX_ISLANDS_PER_ROW = 10;
    private static final int Y_HEIGHT = 100; // Offset for island schematic
    private static final World SKYBLOCK_WORLD = Bukkit.getWorld(Main.skyblockOverworldName);
    private static final  ItemStack[] STARTER_CHEST_CONTENTS = {
        new ItemStack(Material.PUMPKIN_SEEDS),
        new ItemStack(Material.LAVA_BUCKET),
        new ItemStack(Material.CACTUS),
        new ItemStack(Material.MELON_SEEDS),
        new ItemStack(Material.ICE),
        new ItemStack(Material.BEETROOT_SEEDS),
        new ItemStack(Material.SWEET_BERRIES),
        new ItemStack(Material.RED_MUSHROOM),
        new ItemStack(Material.SUGAR_CANE),
        new ItemStack(Material.BROWN_MUSHROOM)
    };
    private static final double SPAWN_OFFSET_X = 2.5;
    private static final double SPAWN_OFFSET_Y = 5;
    private static final double SPAWN_OFFSET_Z = 0.5;
    private static final float SPAWN_OFFSET_YAW = 90;
    private static final float SPAWN_OFFSET_PITCH = 0;

    // CONSTANTS NOT TO BE CONFIGURABLE
    public static final int BLOCKS_PER_CHUNK = 16;
    public static final int MINIMUM_Y = -64;
    public static final int MAXIMUM_Y = 320;

    // Island data
    private final int x;
    private final int z;
    private String name;
    private final int index;
    private String ownerUUID;
    private List<String> friends;
    private String enterMessage;
    private String exitMessage;
    private Location islandSpawn;
    public boolean changed = false;
    private double score = 0;
    private Map<String, Double> scoreBreakdown = new HashMap<>();

    private static int lastIslandIndex = 0;

    // Islands instantiated with this constructor are assumed to be loaded from file
    public Island(int x, int z, String name, int index, String ownerUUID, List<String> friends, String enterMessage, String exitMessage, Location islandSpawn){
        this.x = x;
        this.z = z;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.friends = friends;
        this.enterMessage = enterMessage;
        this.exitMessage = exitMessage;
        this.index = index;
        this.islandSpawn = islandSpawn;
    }

    // Islands instantiated with this constructor are assumed to be new requests from players
    public Island(Player player){
        index = lastIslandIndex++;
        float chunksX = ((((index % Island.MAX_ISLANDS_PER_ROW) + 1) * CHUNK_BUFFER) + ((index % Island.MAX_ISLANDS_PER_ROW) * 2 + 1) * CHUNK_ISLAND_RADIUS);
        float chunksZ = (((index / Island.MAX_ISLANDS_PER_ROW) + 1) * CHUNK_BUFFER) + ((index / Island.MAX_ISLANDS_PER_ROW) * 2 + 1) * CHUNK_ISLAND_RADIUS;
        x = ChunkToBlock(chunksX) + BLOCKS_PER_CHUNK / 2;
        z = ChunkToBlock(chunksZ) + BLOCKS_PER_CHUNK / 2;
        name = player.getName() + "\'s Island";
        enterMessage = "Welcome to " + name;
        exitMessage = "Now leaving " + name;
        ownerUUID = player.getUniqueId().toString();
        friends = new ArrayList<>();
        islandSpawn = new Location(player.getWorld(), x + 2, Y_HEIGHT, z);
    }

    public int ChunkToBlock(int chunk) {
        return chunk * 16;
    }
            
    public void calculateScore() {

        score = 0;
        scoreBreakdown = new HashMap<>();

        // Get the center of the island
        Location center = getIslandCenter();

        // Calculate the chunk-aligned starting X and Z
        int startX = (center.getBlockX() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);
        int startZ = (center.getBlockZ() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);

        // Calculate score
        // Loop through the chunk area (7x7 chunks = ISLAND_RADIUS*2)
        for (int x = startX; x < startX + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; x++) {
            for (int z = startZ; z < startZ + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; z++) {
                for (int y = MINIMUM_Y; y < MAXIMUM_Y; y++) {
                    Material blockType = SKYBLOCK_WORLD.getBlockAt(x, y, z).getType();
                    if( blockType != Material.AIR){
                        score += 0.01;
                        if(scoreBreakdown.containsKey(blockType.toString()))
                            scoreBreakdown.put(blockType.toString(), scoreBreakdown.get(blockType.toString()) + 0.01);
                        else
                            scoreBreakdown.put(blockType.toString(), 0.01);
                    }
                }
            }
        }
    }

    public double getScore() {
        return score;
    }

    public Map<String, Double> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public int ChunkToBlock(float chunk) {
        return (int) chunk * 16;
    }

    public void clearIslandBlocks() {
        // Get the center of the island
        Location center = getIslandCenter();

        // Calculate the chunk-aligned starting X and Z
        int startX = (center.getBlockX() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);
        int startZ = (center.getBlockZ() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);
    
        // Loop through the chunk area (7x7 chunks = ISLAND_RADIUS*2)
        for (int x = startX; x < startX + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; x++) {
            for (int z = startZ; z < startZ + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; z++) {
                for (int y = MINIMUM_Y; y < MAXIMUM_Y; y++) {
                    // Clear block at each coordinate within the island boundaries
                    setBlock(SKYBLOCK_WORLD, x, y, z, Material.AIR);
                }
            }
        }

        // Clear any entities within the island boundaries
        for (Entity entity : SKYBLOCK_WORLD.getEntities()) {
            Location centerAxis = center.clone();
            centerAxis.setY(entity.getLocation().getY());
            if (entity.getLocation().distance(centerAxis) <= CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK) {
                // Remove all entities that are not players
                if (!(entity instanceof Player)) {
                    entity.remove();
                }
                else{
                    Player player = (Player) entity;
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.sendMessage("This island has been reset. You have been teleported to your spawn point.");
                }
            }
        }
    }

    public void resetIslandData(){
        Player player = Bukkit.getPlayer(UUID.fromString(ownerUUID));
        name = player.getName() + "\'s Island";
        enterMessage = "Welcome to " + name;
        exitMessage = "Now leaving " + name;
        ownerUUID = player.getUniqueId().toString();
        friends = new ArrayList<>();
        islandSpawn = new Location(player.getWorld(), x + 2, Y_HEIGHT, z);
    }

    private void populateStarterChest(Location chestLocation) {
        // Populate the chest with starter items
        Block chestBlock = SKYBLOCK_WORLD.getBlockAt(chestLocation);
        // Set the chest's facing direction
        if (chestBlock != null && chestBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) chestBlock.getBlockData();
            directional.setFacing(BlockFace.EAST); // Rotate to face east
            chestBlock.setBlockData(directional);
        }

        if(chestBlock != null && chestBlock.getState() instanceof Chest){
            Chest chest = (Chest) chestBlock.getState();
            Inventory chestInventory = chest.getInventory();

            // Add starter items to the chest
            ItemStack[] items = new ItemStack[27]; // A chest inventory has 27 slots

            // Randomly place the items into the chest
            for (int i = 0; i < STARTER_CHEST_CONTENTS.length; i++) {
                int slot = (int) (Math.random() * items.length);
                while (items[slot] != null) {
                    slot = (int) (Math.random() * items.length);
                }
                items[slot] = STARTER_CHEST_CONTENTS[i];
            }

            // Set the items into the chest
            chestInventory.setContents(items);
        }
    }

    public void setIslandSpawn(){
        // Set the island spawn point
        Location spawnLocation = new Location(SKYBLOCK_WORLD, x + SPAWN_OFFSET_X, Y_HEIGHT + SPAWN_OFFSET_Y, z + SPAWN_OFFSET_Z);
        spawnLocation.setYaw(SPAWN_OFFSET_YAW);
        spawnLocation.setPitch(SPAWN_OFFSET_PITCH);
        setIslandSpawn(spawnLocation);
        this.changed = true;    
    }

    public void buildIsland(){
        // Bedrock base
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT, z, Material.BEDROCK);

        // Grass block top layer
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x + 3, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 3, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 3, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 3, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 3, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x - 3, Y_HEIGHT + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 3, Y_HEIGHT + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 3, Y_HEIGHT + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 3, Y_HEIGHT + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 3, Y_HEIGHT + 4, z - 2, Material.GRASS_BLOCK);

        // Dirt blocks, 2nd layer
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 3, z - 2, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z, Material.SAND);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z - 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z + 3, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 3, z - 3, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 3, z - 2, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 3, z - 2, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 3, z - 2, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 3, Y_HEIGHT + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 3, Y_HEIGHT + 3, z, Material.DIRT);
        
        // Dirt blocks 3rd layer
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 2, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 2, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 2, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 2, z, Material.SAND);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 2, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 2, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 2, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 2, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 2, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 2, z - 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 2, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, Y_HEIGHT + 2, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, Y_HEIGHT + 2, z, Material.DIRT);

        // Dirt blocks 4th layer
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 1, z, Material.SAND);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 1, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, Y_HEIGHT + 1, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 1, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, Y_HEIGHT + 1, z, Material.DIRT);

        // Grow the tree
        SKYBLOCK_WORLD.generateTree(new Location(SKYBLOCK_WORLD, x, Y_HEIGHT + 5, z), TreeType.TREE);

        // Chest
        Location chestLocation = new Location(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 5, z);
        setBlock(SKYBLOCK_WORLD, x + 1, Y_HEIGHT + 5, z, Material.CHEST);
        populateStarterChest(chestLocation);
    }

    private static void setBlock(World world, int x, int y, int z, Material material) {
        Location location = new Location(world, x, y, z);
        Block block = world.getBlockAt(location);
        block.setType(material);
    }

    public void setIslandSpawn(Location location){
        islandSpawn = location;
    }

    public Location getIslandSpawn(){
        return islandSpawn;
    }

    public Location getIslandCenter(){
        return new Location(islandSpawn.getWorld(), x, Y_HEIGHT, z);
    }

    public void addFriend(String friendUUID){
        friends.add(friendUUID);
    }

    public void removeFriend(String friendUUID){
        friends.remove(friendUUID);
    }

    public boolean isFriend(String playerUUID){
        return friends.contains(playerUUID);
    }

    public List<String> getFriends(){
        return friends;
    }

    public boolean isOwner(String playerUUID){
        return playerUUID.equals(ownerUUID);
    }

    public String getGreetingMessage(){
        return enterMessage;
    }

    public void setGreetingMessage(String message){
        enterMessage = message;
    }

    public String getFarewellMessage(){
        return exitMessage;
    }

    public void setFarewellMessage(String message){
        exitMessage = message;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getIndex() {
        return index;
    }

    public String getName(){
        return name;
    }

    public int getRadius(){
        return (int) (Island.CHUNK_ISLAND_RADIUS * 16);
    }

    public void setName(String name){
        this.name = name;
    }

    public String getOwnerUUID(){
        return ownerUUID;
    }

    public void setOwnerUUID(String newOwnerUUID){
        this.ownerUUID = newOwnerUUID;
        IslandManager.updateIsland(this);
        return;
    }

}