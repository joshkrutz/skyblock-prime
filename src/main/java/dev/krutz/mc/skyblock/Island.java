package dev.krutz.mc.skyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.annotations.SerializedName;

import com.google.gson.annotations.Expose;

/**
 * This class is used to store the data of an island, and is used to serialize and deserialize the island to and from JSON.
 */
public class Island {
    // Configurable constants, can be changed in the config file
    public static final float CHUNK_ISLAND_RADIUS = 3.5f;
    public static final int CHUNK_BUFFER = 1; // Buffer space between islands
    private static final int MAX_ISLANDS_PER_ROW = 10;
    private static final int Y_HEIGHT = 100; // Offset for island schematic
    private static final double SPAWN_OFFSET_X = 2.5;
    private static final double SPAWN_OFFSET_Y = 5;
    private static final double SPAWN_OFFSET_Z = 0.5;
    private static final float SPAWN_OFFSET_YAW = 90;
    private static final float SPAWN_OFFSET_PITCH = 0;
    private static final Map<Material, Double> BLOCK_WEIGHTS = new HashMap<>();
    private static final double DEFAULT_BLOCK_WEIGHT = 0.01;

    // Constants that are not configurable
    private final int BLOCKS_PER_CHUNK = 16;
    private final int MINIMUM_Y = -64;
    private final int MAXIMUM_Y = 320;

    // Static variable to keep track of the last island index
    private static int lastIslandIndex = 0;
    
    // Island data that is serialized and deserialized
    @Expose @SerializedName("x") private final int x;
    @Expose @SerializedName("z") private final int z;
    @Expose @SerializedName("name") private String name;
    @Expose @SerializedName("index") private final int index;
    @Expose @SerializedName("owner") private String ownerUUID;
    @Expose @SerializedName("friends") private List<IslandFriend> friends;
    @Expose @SerializedName("ban_list") private List<UUID> banList;
    @Expose @SerializedName("greeting_message") private String enterMessage;
    @Expose @SerializedName("farewell_message") private String exitMessage;
    @Expose @SerializedName("island_spawn") private IslandLocation islandSpawn;
    @Expose @SerializedName("island_warp") private IslandLocation islandWarp;
    @Expose @SerializedName("isLocked") private boolean isLocked = false;

    // Volatile island data that is not serialized
    private boolean isModified = false;
    private double score = 0;
    private Map<String, Double> scoreBreakdown = new HashMap<>();

    /** 
    * Islands instantiated with this constructor are assumed to be loaded from file
    * The modified flag is set to false because the island is not modified after loading
    */
    public Island(int x, int z, String name, int index, String ownerUUID, List<IslandFriend> friends, List<UUID> banList, String enterMessage, String exitMessage, Location islandSpawn, Location islandWarp, boolean isLocked) {
        this.x = x;
        this.z = z;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.friends = friends;
        this.banList = banList;
        this.enterMessage = enterMessage;
        this.exitMessage = exitMessage;
        this.index = index;
        this.islandSpawn = new IslandLocation(islandSpawn);
        this.islandWarp = new IslandLocation(islandWarp);
        this.isLocked = isLocked;
    }

    /** 
    * Islands instantiated with this constructor are assumed to be new requests from players
    * The modified flag is set to true because the island is modified after creation.
    * All other data is set to default values.
    */
    public Island(Player player){
        index = lastIslandIndex++;
        float chunksX = ((((index % MAX_ISLANDS_PER_ROW) + 1) * CHUNK_BUFFER) + ((index % MAX_ISLANDS_PER_ROW) * 2 + 1) * CHUNK_ISLAND_RADIUS);
        float chunksZ = (((index / MAX_ISLANDS_PER_ROW) + 1) * CHUNK_BUFFER) + ((index / MAX_ISLANDS_PER_ROW) * 2 + 1) * CHUNK_ISLAND_RADIUS;
        x = ChunkToBlock(chunksX) + BLOCKS_PER_CHUNK / 2;
        z = ChunkToBlock(chunksZ) + BLOCKS_PER_CHUNK / 2;
        ownerUUID = player.getUniqueId().toString();
        clearIslandBlocks();
        resetIslandData();
    }

    /**
     * Set lastIslandIndex from the last island index from file.
     * @param newIndex
     */
    public static void setLastIslandIndex(int newIndex){
        lastIslandIndex = newIndex;
    }

    ////////////////////////////////////////////////////////////
    /// Utility functions
    ////////////////////////////////////////////////////////////

    /**
     * Convert chunk distance to distance in blocks
     * @param numChunks (int)
     * @return Number of blocks
     */
    public int ChunkToBlock(int numChunks) {
        return numChunks * 16;
    }

    /**
     * Convert chunk distance to distance in blocks
     * @param numChunks (float)
     * @return Number of blocks
     */
    public int ChunkToBlock(float numChunks) {
        return (int) numChunks * 16;
    }

    /**
     * Calculate the score of the island based on the blocks placed.
     * The score is calculated by counting the number of blocks placed on the island.
     * The weight of each block is 0.01. This is configurable in the config file.
     * This function populates the score and scoreBreakdown fields. Where the scoreBreakdown
     * is a map of block types to the number of blocks of that type on the island and score is the total score.
     */
    public void calculateScore() {
        double newScore = 0;
        Map<String, Double> newScoreBreakdown = new HashMap<>();

        // Get the center of the island
        Location center = getIslandCenter();

        // Calculate the chunk-aligned starting X and Z
        int startX = (center.getBlockX() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);
        int startZ = (center.getBlockZ() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);

        World SKYBLOCK_WORLD = center.getWorld();

        // Calculate score
        for (int x = startX; x < startX + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; x++) {
            for (int z = startZ; z < startZ + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; z++) {
                for (int y = MINIMUM_Y; y < MAXIMUM_Y; y++) {
                    Material blockType = SKYBLOCK_WORLD.getBlockAt(x, y, z).getType();
                    
                    if( blockType == Material.AIR){ continue; }
                    
                    double weight = DEFAULT_BLOCK_WEIGHT;
                    if(BLOCK_WEIGHTS.containsKey(blockType))
                    {
                        weight = BLOCK_WEIGHTS.get(blockType);
                    }

                    newScore += weight;
                    if(newScoreBreakdown.containsKey(blockType.toString()))
                    {
                        newScoreBreakdown.put(blockType.toString(), newScoreBreakdown.get(blockType.toString()) + weight);
                    }
                    else
                    {
                        newScoreBreakdown.put(blockType.toString(), weight);
                    }

                }
            }
        }

        score = newScore;
        scoreBreakdown = newScoreBreakdown;
    }

    /**
     * Clear all blocks and entities within the island boundaries
     */
    public void clearIslandBlocks() {
        // Get the center of the island
        Location center = getIslandCenter();

        // Calculate the chunk-aligned starting X and Z
        int startX = (center.getBlockX() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);
        int startZ = (center.getBlockZ() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock((int) CHUNK_ISLAND_RADIUS);

        World SKYBLOCK_WORLD = center.getWorld();

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

    /**
     * Build the island, including the bedrock base, dirt/sand, grass, tree, and starter chest.
     */
    public void buildIsland(){
        World SKYBLOCK_WORLD = islandSpawn.getWorld();

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

    /**
     * Fill the island's chest with starter items.
     * @param chestLocation
     */
    private void populateStarterChest(Location chestLocation) {
        World SKYBLOCK_WORLD = chestLocation.getWorld();
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
            ItemStack[] items = new ItemStack[27];
            
            ItemStack[] STARTER_CHEST_CONTENTS = { 
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

    /**
     * Set a block at a specific location
     * @param world
     * @param x
     * @param y
     * @param z
     * @param material
     */
    private void setBlock(World world, int x, int y, int z, Material material) {
        Location location = new Location(world, x, y, z);
        Block block = world.getBlockAt(location);
        block.setType(material);
    }

    /**
     * Check if a player is a friend of the island
     * @param playerUUID UUID of the player to check
     * @return True if the player is a friend, false otherwise
     */
    public boolean hasFriend(UUID playerUUID){
        return friends.contains(playerUUID.toString());
    }

    /**
     * Check if a player is the owner of the island
     * @param playerUUID UUID of the player to check
     * @return True if the player is the owner, false otherwise
     */
    public boolean hasOwner(UUID playerUUID){
        return ownerUUID.equals(playerUUID.toString());
    }

    /**
     * Check if a player is banned from the island
     * @param playerUUID UUID of the player to check
     * @return True if the player is banned, false otherwise
     */
    public boolean hasBanned(UUID uniqueId) {
        return banList.contains(uniqueId.toString());
    }

    ////////////////////////////////////////////////////////////
    /// Getters and Setters
    /// These functions are used to access and modify the island data
    /// They are used by the IslandManager class to interact with islands
    ////////////////////////////////////////////////////////////

    /**
     * Check if the island has been modified
     * @return True if the island has been modified, false otherwise
     */
    public boolean isModified(){
        return isModified;
    }

    /**
     * Set the modified flag
     * @param modified
     */
    public void setModified(boolean modified){
        isModified = modified;
    }

    /**
     * Get all chunks that the island occupies
     * @return List of chunks
     */
    public ArrayList<Chunk> getChunks(){
        ArrayList<Chunk> chunks = new ArrayList<>();

        // Get the center of the island
        Location center = getIslandCenter();
        
        World SKYBLOCK_WORLD = center.getWorld();

        // Calculate the chunk-aligned starting X and Z
        int startX = (center.getBlockX() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock(CHUNK_ISLAND_RADIUS);
        int startZ = (center.getBlockZ() - BLOCKS_PER_CHUNK / 2) - ChunkToBlock(CHUNK_ISLAND_RADIUS);

        for (int x = startX; x < startX + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; x += BLOCKS_PER_CHUNK) {
            for (int z = startZ; z < startZ + ((int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK)) * 2; z += BLOCKS_PER_CHUNK) {
                chunks.add(SKYBLOCK_WORLD.getChunkAt(x / BLOCKS_PER_CHUNK, z / BLOCKS_PER_CHUNK));
            }
        }

        return chunks;
    }
            
    /**
     * Get the island score
     * @return island score
     */
    public double getScore() {
        return score;
    }

    /**
     * Get the island score breakdown by block type
     * @return island score breakdown
     */
    public Map<String, Double> getScoreBreakdown() {
        return scoreBreakdown;
    }

    /**
     * Reset all island data to default values. This does not reset the island center or index as these are unique to each island.
     */
    public void resetIslandData(){
        
        Player player = Bukkit.getPlayer(UUID.fromString(ownerUUID));
        name = player.getName() + "\'s Island";
        enterMessage = "Welcome to " + name;
        exitMessage = "Now leaving " + name;
        ownerUUID = player.getUniqueId().toString();
        friends = new ArrayList<>();
        banList = new ArrayList<>();
        setIslandSpawn();
        islandWarp = new IslandLocation(getIslandSpawn());
        isLocked = false;
        isModified = true;
    }

    /**
     * Set the island spawn point to the default location. Default spawn offsets are configurable in the config file and are offset from the island center (Bedrock block).
     */
    private void setIslandSpawn(){
        World SKYBLOCK_WORLD = Bukkit.getWorld(Main.skyblockWorldName);
        // Set the island spawn point
        Location spawnLocation = new Location(SKYBLOCK_WORLD, x + SPAWN_OFFSET_X, Y_HEIGHT + SPAWN_OFFSET_Y, z + SPAWN_OFFSET_Z);
        spawnLocation.setYaw(SPAWN_OFFSET_YAW);
        spawnLocation.setPitch(SPAWN_OFFSET_PITCH);
        setIslandSpawn(spawnLocation);
        isModified = true;    
    }

    /**
     * Set the island spawn point to a specific location
     * @param location
     */
    public void setIslandSpawn(Location location){
        islandSpawn = new IslandLocation(location);
        isModified = true;
    }

    /**
     * Set the island warp point to a specific location
     * @param location
     */
    public void setIslandWarp(Location location){
        islandWarp = new IslandLocation(location);
        isModified = true;
    }

    /**
     * Get the island spawn point
     * @return island spawn point
     */
    public Location getIslandSpawn(){
        return islandSpawn.getLocation();
    }

    /**
     * Get the island warp point
     * @return island warp point
     */
    public Location getIslandWarp(){
        return islandWarp.getLocation();
    }

    /**
     * Get the location of the island's center (Bedrock block)
     * @return island center location
     */
    public Location getIslandCenter(){
        return new Location(Bukkit.getWorld(Main.skyblockWorldName), x, Y_HEIGHT, z);
    }

    /**
     * Lock the island. This prevents players from building on the island and using island warp.
     */
    public void lockIsland(){
        isLocked = true;
        isModified = true;
    }

    /**
     * Unlock the island. This allows players to build on the island and use island warp.
     */
    public void unlockIsland(){
        isLocked = false;
        isModified = true;
    }

    /**
     * Get the lock status of the island
     * @param isLocked
     */
    public boolean isLocked(){
        return isLocked;
    }


    /**
     * Add a friend to the island party
     * @param friendUUID String UUID of the friend to add
     */
    public void addFriend(UUID friendUUID){
        friends.add(new IslandFriend(friendUUID));
        isModified = true;
    }

    /**
     * Remove a friend from the island party
     * @param friendUUID String UUID of the friend to remove
     */
    public void removeFriend(UUID friendUUID){
        friends.remove(new IslandFriend(friendUUID));
        isModified = true;
    }

    /**
     * Get a list of all friends of the island
     * @return List of friends
     */
    public List<IslandFriend> getFriends(){
        return friends;
    }

    /**
     * Add a player to the island ban list
     * @return
     */
    public void banPlayer(UUID playerUUID){
        banList.add(playerUUID);
        isModified = true;
    }

    /**
     * Remove a player from the island ban list
     * @param playerUUID
     */
    public void unbanPlayer(UUID playerUUID){
        banList.remove(playerUUID);
        isModified = true;
    }

    /**
     * Get the greeting message for the island. This message is displayed when a player enters the island radius.
     * @return greeting message
     */
    public String getGreetingMessage(){
        return enterMessage;
    }

    /**
     * Set the greeting message for the island. This message is displayed when a player enters the island radius.
     * @param message
     */
    public void setGreetingMessage(String message){
        enterMessage = message;
        isModified = true;
    }

    /**
     * Get the farewell message for the island. This message is displayed when a player leaves the island radius.
     * @return farewell message
     */
    public String getFarewellMessage(){
        return exitMessage;
    }

    /**
     * Set the farewell message for the island. This message is displayed when a player leaves the island radius.
     * @param message
     */
    public void setFarewellMessage(String message){
        exitMessage = message;
        isModified = true;
    }

    /**
     * Get the island index
     * @return island index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the island name
     * @return name
     */
    public String getName(){
        return name;
    }

    /**
     * Set the island name
     * @param name
     */
    public void setName(String name){
        this.name = name;
        isModified = true;
    }

    /**
     * Get the island's radius in blocks
     * @return island radius
     */
    public int getRadius(){
        return (int) (CHUNK_ISLAND_RADIUS * BLOCKS_PER_CHUNK);
    }

    /**
     * Get the owner of the island
     * @return owner UUID
     */
    public UUID getOwnerUUID(){
        return UUID.fromString(ownerUUID);
    }

    /**
     * Set the owner of the island
     * @param newOwnerUUID String UUID of the new owner
     */
    public void setOwnerUUID(String newOwnerUUID){
        this.ownerUUID = newOwnerUUID;
        isModified = true;
    }

    /**
     * Set the owner of the island
     * @param newOwnerUUID
     */
    public void setOwnerUUID(UUID newOwnerUUID) {
        setOwnerUUID(newOwnerUUID.toString());
    }
}

