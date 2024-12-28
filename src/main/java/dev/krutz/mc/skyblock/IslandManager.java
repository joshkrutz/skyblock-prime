package dev.krutz.mc.skyblock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import org.json.JSONArray;

public class IslandManager {
    private static ArrayList<Island> islands = new ArrayList<>();

    private static final String ISLANDS_FILE = "island-data.json"; // Path to your islands data file

    private static JavaPlugin plugin;

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

    public static void createIsland(Player player) {
        Island newIsland = new Island(player);
        newIsland.buildIsland();

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
            player.sendMessage("Usage: /island tp <friend>");
            return;
        }

        String friendName = args[1];
        Player friend = Bukkit.getPlayer(friendName);

        // Check if friend is in friend list
        Island islandData = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(islandData == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        if(!islandData.getFriends().contains(friend.getUniqueId().toString())){
            player.sendMessage("Player " + friendName + " does not have access to this island.");
            return;
        }

        if(friend == null){
            player.sendMessage("Player " + friendName + " is not online.");
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
        if(friend.getLocation().toVector().distance(eyeLevelLocation.toVector()) >= Island.ISLAND_RADIUS){
            player.sendMessage("Player " + friendName + " is not on the island.");
            return;
        }

        // Teleport the player to the friend's location
        player.teleport(friend.getLocation());
    }

    public static void setGreeting(Player player, String[] args) {
        if(args.length < 2){
            player.sendMessage("Usage: /island setgreeting <message>");
            return;
        }

        Island islandData = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(islandData == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        // Get all but first arg
        ArrayList<String> greetingMsg = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            greetingMsg.add(args[i]);
        }

        String greeting = String.join(" ", greetingMsg);
        islandData.setGreetingMessage(greeting);
        updateIsland(islandData);
    }

    public static void setFarewell(Player player, String[] args) {
        if(args.length < 2){
            player.sendMessage("Usage: /island setfarewell <message>");
            return;
        }

        Island islandData = getIslandByPlayerUUID(player.getUniqueId().toString());
        if(islandData == null){
            player.sendMessage("You do not have an island to use this command on.");
            return;
        }

        // Get all but first arg
        ArrayList<String> farewellMsg = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            farewellMsg.add(args[i]);
        }

        String farewell = String.join(" ", farewellMsg);
        islandData.setFarewellMessage(farewell);
        updateIsland(islandData);
    }

    public static ArrayList<Island> getIslands() {
        return islands;
    }

}

class Island {
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

    private static int lastIslandIndex = 0;
    public static final int ISLAND_RADIUS = 5;
    public static final int BUFFER = 32; // Buffer space between islands
    private static final int MAX_ISLANDS_PER_ROW = 10;
    private static final World SKYBLOCK_WORLD = Bukkit.getWorld(Main.skyblockOverworldName);

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

    public Island(Player player){
        x = (lastIslandIndex % MAX_ISLANDS_PER_ROW) * (ISLAND_RADIUS * 2 + BUFFER);
        z = (lastIslandIndex / MAX_ISLANDS_PER_ROW) * (ISLAND_RADIUS * 2 + BUFFER);
        name = player.getName() + "\'s Island";
        enterMessage = "Welcome to " + name;
        exitMessage = "Now leaving " + name;
        index = lastIslandIndex++;
        ownerUUID = player.getUniqueId().toString();
        friends = new ArrayList<>();
        islandSpawn = new Location(player.getWorld(), x, 100, z);
    }

    public void buildIsland(){
        int y = 100;
        // Bedrock base
        setBlock(SKYBLOCK_WORLD, x, y, z, Material.BEDROCK);

        // Grass block top layer
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, y + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, y + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, y + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, y + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x, y + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z - 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z + 3, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 4, z - 3, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x + 2, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 4, z - 2, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x - 2, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 4, z - 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 4, z + 2, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 4, z - 2, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x + 3, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 3, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x + 3, y + 4, z - 1, Material.GRASS_BLOCK);

        setBlock(SKYBLOCK_WORLD, x - 3, y + 4, z, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 3, y + 4, z + 1, Material.GRASS_BLOCK);
        setBlock(SKYBLOCK_WORLD, x - 3, y + 4, z - 1, Material.GRASS_BLOCK);

        // Dirt blocks, 2nd layer
        setBlock(SKYBLOCK_WORLD, x - 1, y + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 3, z - 2, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x, y + 3, z, Material.SAND);
        setBlock(SKYBLOCK_WORLD, x, y + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 3, z - 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 3, z + 3, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 3, z - 3, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 1, y + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 3, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 3, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 3, z - 2, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 2, y + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 3, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x - 2, y + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 3, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 3, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 3, y + 3, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 3, y + 3, z, Material.DIRT);
        
        // Dirt blocks 3rd layer
        setBlock(SKYBLOCK_WORLD, x - 1, y + 2, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 2, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 2, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x, y + 2, z, Material.SAND);
        setBlock(SKYBLOCK_WORLD, x, y + 2, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 2, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x + 1, y + 2, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 2, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 2, z - 1, Material.DIRT);

        setBlock(SKYBLOCK_WORLD, x, y + 2, z - 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 2, z + 2, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 2, y + 2, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 2, y + 2, z, Material.DIRT);

        // Dirt blocks 4th layer
        setBlock(SKYBLOCK_WORLD, x, y + 1, z, Material.SAND);
        setBlock(SKYBLOCK_WORLD, x, y + 1, z + 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x, y + 1, z - 1, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x + 1, y + 1, z, Material.DIRT);
        setBlock(SKYBLOCK_WORLD, x - 1, y + 1, z, Material.DIRT);

        // Grow the tree
        SKYBLOCK_WORLD.generateTree(new Location(SKYBLOCK_WORLD, x, y + 5, z), TreeType.TREE);

        // Chest
        setBlock(SKYBLOCK_WORLD, x + 1, y + 5, z, Material.CHEST);

        // Populate the chest with starter items
        Block chestBlock = SKYBLOCK_WORLD.getBlockAt(x + 1, y + 5, z);
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
            chestInventory.addItem(new ItemStack(Material.DIAMOND, 1));
        }

        // Set the island spawn point
        Location spawnLocation = new Location(SKYBLOCK_WORLD, x + 2.5, y + 5, z + 0.5);
        spawnLocation.setYaw(90);
        spawnLocation.setPitch(0);
        setIslandSpawn(spawnLocation);
        this.changed = true;
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
        return new Location(SKYBLOCK_WORLD, x, 100, z);
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
        return ISLAND_RADIUS;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getOwnerUUID(){
        return ownerUUID;
    }

    public void setOwnerUUID(Player sender, String newOwnerUUID){
        if(!this.ownerUUID.equals(sender.getUniqueId().toString())){
            sender.sendMessage("You must be the owner of the island to update its ownership.");
            return;
        }

        if(!this.friends.contains(newOwnerUUID)){
            sender.sendMessage("The new owner must be a member of the island party.");
            return;
        }

        this.ownerUUID = newOwnerUUID;
        IslandManager.updateIsland(this);
        return;
    }

}