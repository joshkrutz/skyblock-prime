package dev.krutz.mc.skyblock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;

public class Main extends JavaPlugin {

    static final String spawnWorldName = "world";
    static final String skyblockOverworldName = "skyblock_world";
    private final CommandManager commandManager = new CommandManager();

    @Override
    public void onEnable() {

        getLogger().info("Skyblock plugin has been enabled!");

         // Create the skyblock world if it doesn't exist
         if (getServer().getWorld(skyblockOverworldName) == null) {
            WorldCreator worldCreator = new WorldCreator(skyblockOverworldName);
            worldCreator.environment(World.Environment.NORMAL);
            worldCreator.type(WorldType.NORMAL);
            worldCreator.generator(new SkyblockWorldGenerator());
            worldCreator.createWorld();
        }

        // Load all necessary configs and data
        ensureChallengesFileExists();

        ServerEssentials.initialize(this);
        IslandManager.initialize(this);
        ChallengeManager.initialize(this);

        // Register commands
        new EssentialsCommandManager().registerCommands(commandManager);
        new IslandCommandManager().registerCommands(commandManager);
        new ChallengeCommandManager().registerCommands(commandManager);

        getServer().getPluginManager().registerEvents(new IslandListener(), this);
       
    }

    @Override
    public void onDisable() {
        IslandManager.saveDataToFile();
        
        getLogger().info("Skyblock plugin has been disabled!");
        IslandManager.saveDataToFile();
        // Cleanup resources or save data here.
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandManager.executeCommand(sender, label, args);
    }

    private void ensureChallengesFileExists() {
        File configFile = new File(getDataFolder(), "challenges.json");

        if (!configFile.exists()) {
            try {
                if (!getDataFolder().exists()) {
                    getDataFolder().mkdirs();
                }

                // Copy the file from resources
                InputStream in = getResource("challenges.json");
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("Default challenges.json created.");
            } catch (IOException e) {
                getLogger().severe("Failed to create default challenges.json: " + e.getMessage());
            }
        }
    }
}