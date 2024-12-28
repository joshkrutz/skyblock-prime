package dev.krutz.mc.skyblock;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.command.Command;

public class Main extends JavaPlugin {

    static final String spawnWorldName = "world";
    static final String skyblockOverworldName = "skyblock_world";
    private ChallengeManager challengeManager;
    private ServerEssentials serverEssentials;

    @Override
    public void onEnable() {
        getLogger().info("Skyblock plugin has been enabled!");

         // Create the skyblock world if it doesn't exist
         if (getServer().getWorld(skyblockOverworldName) == null) {
            WorldCreator worldCreator = new WorldCreator(skyblockOverworldName);
            worldCreator.environment(World.Environment.NORMAL);
            worldCreator.type(WorldType.FLAT);
            worldCreator.generator(new WorldGenerator());
            worldCreator.createWorld();
        }

        // Load all necessary configs and data
        ensureChallengesFileExists();

        // Initialize the Challenge Manager
        challengeManager = new ChallengeManager(this);

        // Initialize the Server Essentials
        serverEssentials = new ServerEssentials(this);

        IslandManager.initialize(this);

        getServer().getPluginManager().registerEvents(new IslandListener(), this);
       
    }

    @Override
    public void onDisable() {
        IslandManager.saveDataToFile();
        
        getLogger().info("Skyblock plugin has been disabled!");
        
        // Cleanup resources or save data here.
    }

    // Command handler
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;

        // Handle /island command
        if (command.getName().equalsIgnoreCase("island")) {
            return handleIslandCommand(player, args);
        }

        // Handle /challenge command
        if (command.getName().equalsIgnoreCase("challenge") || command.getName().equalsIgnoreCase("c")) {
            return handleChallengesCommand(player, args);
        }

        else {
            return serverEssentials.handleEssentialsCommand(sender, command, label, args);
        }

    }

    private boolean handleIslandCommand(Player player, String[] args){
        switch (args[0].toLowerCase()) {
            case "create":
                IslandManager.createIsland(player);
                break;
            case "restart":
                break;
            case "help":
                break;
            case "spawn":
                IslandManager.teleportToIsland(player);
                break;
            case "home":
                IslandManager.teleportToIsland(player);
                break;
            case "setgreeting":
                IslandManager.setGreeting(player, args);
                break;
            case "setfarewell":
                IslandManager.setFarewell(player, args);
                break;
            case "teleport":
                IslandManager.teleportToIslandFriend(player, args);
                break;
            case "tp":
                IslandManager.teleportToIslandFriend(player, args);
                break;
            case "warp":
                break;
            case "invite":
                break;
            case "accept":
                break;
            case "reject":
                break;
            case "level":
                break;
            case "rank":
                break;
            default:
                player.sendMessage("Unknown island subcommand: " + args[0]);
                break;
        }
        return true;
    }

    // Handle /challenges command
    private boolean handleChallengesCommand(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("complete") || subCommand.equals("c")) {
            String challengeCmdName = args[1].toLowerCase();
            Challenge challenge = challengeManager.getChallenge(challengeCmdName);
            if (challenge == null) {
                player.sendMessage("Challenge not found: " + challengeCmdName);
                return true;
            }

            challengeManager.completeChallenge(player, challenge);
            return true;
        }
        else if (subCommand.equals("create")) {
            // challenge name is args 2 to end
            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (challengeName.isEmpty()) {
                player.sendMessage("Usage: /challenge create <challengeName>");
                return true;
            }
            challengeManager.createChallenge(player, challengeName);
            return true;
        }
        else if (subCommand.equals("list")) {
            listChallenges(player);
            return true;
        }

        player.sendMessage("Unknown subcommand: " + subCommand);
        return true;
    }

    private void listChallenges(Player player) {
        for(String complexity : Challenge.ChallengeComplexity.getComplexityNames()){

            String titleCaseComplexity = complexity.substring(0, 1).toUpperCase() + complexity.substring(1).toLowerCase();
            // Get the hue for the complexity
            ChallengeUtil.ComplexityHue hues = ChallengeUtil.getHueStartAndStopFromComplexity(Challenge.ChallengeComplexity.valueOf(complexity));
            float hueStart = hues.getStartHue();
            float hueStop = hues.getStopHue();
            float saturation = 0.5f;
            float brightness = 0.9f;

            Color incomplete = Color.getHSBColor(hueStart, saturation, brightness);
            Color complete = Color.getHSBColor(hueStop, saturation, brightness);

            List<Challenge> challengesPerComplexity = challengeManager.getChallengeNamesByComplexity(complexity);
            if (challengesPerComplexity.isEmpty()) {
                continue;
            }
            player.sendMessage(Component.text(titleCaseComplexity + " Challenges:"));

            Component message = Component.empty();
            for (Challenge challenge : challengesPerComplexity) {
                Color color = incomplete;
                String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                if (challengeManager.hasPlayerCompletedChallengeBefore(player.getUniqueId().toString(), challenge.getName())) {
                    color = complete;
                    hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                    if (!challenge.isRepeatable()) {
                        // Cross out the text
                        message = message.append(
                            Component.text(challenge.getName()).color(TextColor.fromHexString(hexColor)).decoration(TextDecoration.STRIKETHROUGH, true)
                        ).append(Component.text(", "));
                        continue;
                    }                    
                }
                message = message.append(
                    Component.text(challenge.getName()).color(TextColor.fromHexString(hexColor)));

                // if not last challenge, add a comma
                if (challengesPerComplexity.indexOf(challenge) != challengesPerComplexity.size() - 1) {
                    message = message.append(Component.text(", "));
                }
            }
            player.sendMessage(message);
        }
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