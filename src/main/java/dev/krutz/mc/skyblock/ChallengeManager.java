package dev.krutz.mc.skyblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.krutz.mc.skyblock.Challenge.ChallengeComplexity;
import io.papermc.paper.text.PaperComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeManager {

    private static final Map<String, Challenge> challenges = new HashMap<>();
    private static JavaPlugin plugin;
    
    public static void initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        loadChallenges();
    }

    // Load challenges from the JSON file
    public static void loadChallenges() {
        try {
            File configFile = new File(plugin.getDataFolder(), "challenges.json");
            if (!configFile.exists()) {
                plugin.saveResource("challenges.json", false); // Save default if it doesn't exist
            }
            String jsonContent = Files.readString(configFile.toPath());
            JSONObject jsonObject = new JSONObject(jsonContent);
            JSONArray challengeArray = jsonObject.getJSONArray("challenges");

            for (int i = 0; i < challengeArray.length(); i++) {
                JSONObject challengeObject = challengeArray.getJSONObject(i);
                String command = challengeObject.optString("command", "unknown");
                String name = challengeObject.optString("name", "Unnamed Challenge");
                String description = challengeObject.optString("description", "No description available.");
                String complexity = challengeObject.optString("complexity", "0");

                // Parse requirements and rewards
                List<Requirement> requirements = parseRequirements(challengeObject.optJSONArray("requirements"));
                List<Reward> rewards = parseRewards(challengeObject.optJSONArray("rewards"));
                List<Reward> repeatableRewards = parseRewards(challengeObject.optJSONArray("repeat_rewards"));

                Challenge challenge = new Challenge(name, description, complexity, requirements, rewards, repeatableRewards);
                challenges.put(command.toLowerCase(), challenge);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Could not read challenges.json: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().severe("Error parsing challenges.json: " + e.getMessage());
        }
    }

    private static List<Challenge> getChallengeNamesByComplexity(String complexityStr){
        return getChallengeNamesByComplexity(ChallengeComplexity.valueOf(complexityStr.toUpperCase()));
    }

    private static List<Challenge> getChallengeNamesByComplexity(ChallengeComplexity complexity){
        List<Challenge> challengesByComplexity = new ArrayList<>();
        for (Challenge challenge : challenges.values()) {
            if (challenge.getComplexity() == complexity) {
                challengesByComplexity.add(challenge);
            }
        }
        return challengesByComplexity;
    }

    private static List<Requirement> parseRequirements(JSONArray array) {
        List<Requirement> requirements = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    String type = obj.optString("type", "item");
                    String name = obj.optString("name", "unknown");
                    int amount = obj.optInt("amount", 0);
                    requirements.add(new Requirement(type, name, amount));
                }
            }
        }
        return requirements;
    }

    private static List<Reward> parseRewards(JSONArray array) {
        List<Reward> rewards = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    String type = obj.optString("type", "item");
                    String name = obj.optString("name", "unknown");
                    int amount = obj.optInt("amount", 0);
                    rewards.add(new Reward(type, name, amount));
                }
            }
        }
        return rewards;
    }

    public List<String> getChallengeNames() {
        List<String> challengeNames = new ArrayList<>();
        for (Challenge challenge : challenges.values()) {
            challengeNames.add(challenge.getName());
        }
        return challengeNames;
    }

    public static Challenge getChallenge(String command) {
        return challenges.get(command.toLowerCase());
    }

    public static boolean isValidCompletion(Player player, Challenge challenge) {
        for (Requirement req : challenge.getRequirements()) {
            if ("item".equalsIgnoreCase(req.getType()) && !hasRequiredItem(player, req)) {
                return false;
            }
            else if("experience".equalsIgnoreCase(req.getType()) && player.getTotalExperience() < req.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasRequiredItem(Player player, Requirement req) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.getMaterial(req.getName().toUpperCase())) {
                total += item.getAmount();
                if (total >= req.getAmount())
                    return true;
            }
        }
        return false;
    }

    private static void payRequirements(Player player, Challenge challenge)
    {
        // Remove requirements as necessary
        for (Requirement req : challenge.getRequirements()) {
            if ("item".equalsIgnoreCase(req.getType())) {
                ItemStack item = new ItemStack(Material.getMaterial(req.getName().toUpperCase()), req.getAmount());
                player.getInventory().removeItem(item);
            } else if ("experience".equalsIgnoreCase(req.getType())) {
                player.setTotalExperience(player.getTotalExperience() - req.getAmount());
            }
        }
    }

    private static void giveRewardAndUpdateLog(Player player, Challenge challenge, boolean isRepeatChallenge)
    {
        List<Reward> rewardsToIssue = isRepeatChallenge ? challenge.getRepeatableRewards() : challenge.getRewards();

        for (Reward reward : rewardsToIssue) {
            if ("item".equalsIgnoreCase(reward.getType())) {
                player.getInventory().addItem(new ItemStack(Material.getMaterial(reward.getName().toUpperCase()), reward.getAmount()));
            } else if ("experience".equalsIgnoreCase(reward.getType())) {
                player.giveExp(reward.getAmount());
            }
        }

        updatePlayerCompletionLog(player, challenge.getName());

    }

    public static void completeChallenge(Player player, Challenge challenge) {
        if (isValidCompletion(player, challenge)) 
        {
            if( !hasPlayerCompletedChallengeBefore(player.getUniqueId().toString(), challenge.getName()) )
            {
                payRequirements(player, challenge);
                giveRewardAndUpdateLog(player, challenge, false);
                
                // Announce completion
                ChallengeAnnouncer.announceChallengeCompletion(player.getName(), challenge.getName(), challenge.getDifficultyRating());
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            else {
                if( challenge.isRepeatable() )
                {
                    payRequirements(player, challenge);
                    giveRewardAndUpdateLog(player, challenge, true);

                    player.sendMessage(ChallengeAnnouncer.craftMessageForRepeatChallengeCompletion(player.getName(), challenge.getName(), challenge.getDifficultyRating()));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
                }
                else{
                    player.sendMessage(Component.text("This challenge is not repeatable."));
                }
            }
        }
        else 
        {
            player.sendMessage("You haven't completed the requirements for the challenge yet.");
        }
        
    }

    
    public static void listChallenges(Player player) {
        for(String complexity : Challenge.ChallengeComplexity.getComplexityNames()){

            String titleCaseComplexity = complexity.substring(0, 1).toUpperCase() + complexity.substring(1).toLowerCase();
            // Get the hue for the complexity
            SkyblockUtil.ComplexityHue hues = SkyblockUtil.getHueStartAndStopFromComplexity(Challenge.ChallengeComplexity.valueOf(complexity));
            float hueStart = hues.getStartHue();
            float hueStop = hues.getStopHue();
            float saturation = 0.5f;
            float brightness = 0.9f;

            Color incomplete = Color.getHSBColor(hueStart, saturation, brightness);
            Color complete = Color.getHSBColor(hueStop, saturation, brightness);

            List<Challenge> challengesPerComplexity = getChallengeNamesByComplexity(complexity);
            if (challengesPerComplexity.isEmpty()) {
                continue;
            }
            player.sendMessage(Component.text(titleCaseComplexity + " Challenges:"));

            Component message = Component.empty();
            for (Challenge challenge : challengesPerComplexity) {
                Color color = incomplete;
                String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                if (hasPlayerCompletedChallengeBefore(player.getUniqueId().toString(), challenge.getName())) {
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

    private static void updatePlayerCompletionLog(Player player, String challengeName){
        String uuid = player.getUniqueId().toString();

        File playerDataFile = new File(plugin.getDataFolder(), "player-data.json");
        if (!playerDataFile.exists())
            createPlayerDataFile();

        try {
            String jsonContent = Files.readString(playerDataFile.toPath());
            JSONObject jsonObject = new JSONObject(jsonContent);
            JSONArray playerArray = jsonObject.getJSONArray("players");

            boolean playerFound = false;
            for (int i = 0; i < playerArray.length(); i++) {
                JSONObject playerObject = playerArray.getJSONObject(i);
                if (playerObject.getString("uuid").equals(uuid)) {
                    playerFound = true;
                    JSONArray completedChallenges = playerObject.getJSONArray("completed_challenges");
                    // See if entry already exists
                    boolean challengeFound = false;
                    for (int j = 0; j < completedChallenges.length(); j++) {
                        JSONObject completedChallenge = completedChallenges.getJSONObject(j);
                        if (completedChallenge.getString("name").equals(challengeName)) {
                            completedChallenge.put("last_completion", System.currentTimeMillis());
                            completedChallenge.put("total_completions", completedChallenge.optInt("total_completions", 0) + 1);
                            challengeFound = true;
                            break;
                        }
                    }
                    if (!challengeFound) {
                        JSONObject completedChallenge = new JSONObject();
                        completedChallenge.put("last_completion", System.currentTimeMillis());
                        completedChallenge.put("first_completion", System.currentTimeMillis());
                        completedChallenge.put("total_completions", 1);
                        completedChallenge.put("name", challengeName);
                        completedChallenges.put(completedChallenge);
                    }   
                    break;
                }
            }

            if(!playerFound){
                JSONObject playerObject = new JSONObject();
                JSONArray completedChallenges = new JSONArray();
                JSONObject completedChallenge = new JSONObject();
                completedChallenge.put("first_completion", System.currentTimeMillis());
                completedChallenge.put("last_completion", System.currentTimeMillis());
                completedChallenge.put("total_completions", 1);
                completedChallenge.put("name", challengeName);
                completedChallenges.put(completedChallenge);
                playerObject.put("completed_challenges", completedChallenges);
                playerObject.put("uuid", uuid);
                playerObject.put("name", player.getName());
                playerArray.put(playerObject);
            }

            Files.writeString(playerDataFile.toPath(), jsonObject.toString(4));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not read player-data.json: " + e.getMessage());
        }
    }

    public static boolean hasPlayerCompletedChallengeBefore(String uuid, String challengeName) {
        File playerDataFile = new File(plugin.getDataFolder(), "player-data.json");
        if (!playerDataFile.exists())
            createPlayerDataFile();

        try {
            String jsonContent = Files.readString(playerDataFile.toPath());
            JSONObject jsonObject = new JSONObject(jsonContent);
            JSONArray playerArray = jsonObject.getJSONArray("players");

            for (int i = 0; i < playerArray.length(); i++) {
                JSONObject playerObject = playerArray.getJSONObject(i);
                if (playerObject.getString("uuid").equals(uuid)) {
                    JSONArray completedChallenges = playerObject.getJSONArray("completed_challenges");
                    for (int j = 0; j < completedChallenges.length(); j++) {
                        JSONObject completedChallenge = completedChallenges.getJSONObject(j);
                        if (completedChallenge.getString("name").equals(challengeName)) {
                            return true;
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not read player-data.json: " + e.getMessage());
        }
        return false;
    }

    private static void createPlayerDataFile(){
        File playerDataFile = new File(plugin.getDataFolder(), "player-data.json");
        try {
            JSONObject jsonObject = new JSONObject();
            JSONArray playerArray = new JSONArray();
            jsonObject.put("players", playerArray);
            Files.writeString(playerDataFile.toPath(), jsonObject.toString(4));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write player-data.json: " + e.getMessage());
        }
    }

    public static void createChallenge(Player player, String challengeName){
        try {
            File configFile = new File(plugin.getDataFolder(), "challenges.json");
            
            JSONObject challengesLog = new JSONObject(Files.readString(configFile.toPath()));
            JSONArray challengesArray = challengesLog.getJSONArray("challenges");

            for (int i = 0; i < challengesArray.length(); i++) {
                JSONObject challengeObject = challengesArray.getJSONObject(i);
                if (challengeObject.getString("name").equalsIgnoreCase(challengeName)) {
                    player.sendMessage("Challenge already exists: " + challengeName);
                    return;
                }
            }

            JSONObject challengeObject = new JSONObject();
            challengeObject.put("name", challengeName);
            challengeObject.put("description", "TODO");
            challengeObject.put("complexity", "TODO");
            challengeObject.put("command", challengeName.toLowerCase().replace(" ", ""));
            
            JSONArray requirements = new JSONArray();

            // Iterate over inventory to build out required items
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null)
                    continue;

                // Ignore any shulker boxes (these are used for rewards)
                if (item.getType() == Material.SHULKER_BOX)
                    continue;
                

                // If name is already in requirements, increment amount
                boolean found = false;
                for (int i = 0; i < requirements.length(); i++) {
                    JSONObject req = requirements.getJSONObject(i);
                    if (req.getString("name").equals(item.getType().name())) {
                        req.put("amount", req.getInt("amount") + item.getAmount());
                        found = true;
                        break;
                    }
                }
                if (found)
                    continue;


                // Item must be new, add it to requirements
                JSONObject requirement = new JSONObject();
                requirement.put("type", "item");
                requirement.put("name", item.getType().name());
                requirement.put("amount", item.getAmount());
                requirements.put(requirement);
            }
            challengeObject.put("requirements", requirements);
            
            JSONArray rewards = new JSONArray();
            
            // Iterate over shulker box contents for rewards
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType() != Material.SHULKER_BOX || !item.hasItemMeta())
                    continue;
                
                BlockStateMeta itemMeta = (BlockStateMeta) item.getItemMeta();
                if (itemMeta == null || itemMeta.getBlockState() == null || (itemMeta.getBlockState() instanceof ShulkerBox == false))
                    continue;

                ShulkerBox shulkerBox = (ShulkerBox) itemMeta.getBlockState();
                for (ItemStack rewardItem : shulkerBox.getInventory().getContents()) {
                    if (rewardItem == null)
                        continue;

                    // If name is already in rewards, increment amount
                    boolean found = false;
                    for (int i = 0; i < rewards.length(); i++) {
                        JSONObject req = rewards.getJSONObject(i);
                        if (req.getString("name").equals(rewardItem.getType().name())) {
                            req.put("amount", req.getInt("amount") + rewardItem.getAmount());
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        continue;
                    
                    // Reward must be new, add it to rewards
                    JSONObject reward = new JSONObject();
                    reward.put("type", "item");
                    reward.put("name", rewardItem.getType().name());
                    reward.put("amount", rewardItem.getAmount());
                    rewards.put(reward);
                }
            }
            challengeObject.put("rewards", rewards);
            challengeObject.put("repeat_rewards", rewards);

            // Add challenge to challenges map and challenges.json
            Challenge challenge = new Challenge(challengeName, "TODO", "TODO", parseRequirements(requirements), parseRewards(rewards), parseRewards(rewards));
            challenges.put(challengeName.toLowerCase().replace(" ", ""), challenge);

            challengesArray.put(challengeObject);
            challengesLog.put("challenges", challengesArray);

            Files.writeString(configFile.toPath(), challengesLog.toString(4));
            player.sendMessage("Challenge created: " + challengeName);

        } catch (IOException e) {
            plugin.getLogger().severe("Could not write challenges.json: " + e.getMessage());
        }
    }

    public static void resetPlayerChallenges(Player player) {
        File playerDataFile = new File(plugin.getDataFolder(), "player-data.json");
        if (!playerDataFile.exists())
            return;

        try {
            String jsonContent = Files.readString(playerDataFile.toPath());
            JSONObject jsonObject = new JSONObject(jsonContent);
            JSONArray playerArray = jsonObject.getJSONArray("players");

            for (int i = 0; i < playerArray.length(); i++) {
                JSONObject playerObject = playerArray.getJSONObject(i);
                if (playerObject.getString("uuid").equals(player.getUniqueId().toString())) {
                    playerObject.put("completed_challenges", new JSONArray());
                    break;
                }
            }

            Files.writeString(playerDataFile.toPath(), jsonObject.toString(4));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not read player-data.json: " + e.getMessage());
        }
    }
}