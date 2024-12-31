package dev.krutz.mc.skyblock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;

import java.awt.Color;

public class ChallengeAnnouncer {

    public static void announceChallengeCompletion(String playerName, String challengeName, int difficulty) {
        // Create the rainbow parts for player name and challenge name
        Component difficultyGradientMessage = generateDifficultyGradientMessage(playerName, challengeName, difficulty);

        // Broadcast the message
        Bukkit.getServer().sendMessage(difficultyGradientMessage);
    }

    public static Component craftMessageForRepeatChallengeCompletion(String playerName, String challengeName, int difficulty) {
        String combined = playerName + challengeName;
        Component result = Component.empty();
        int length = combined.length();

        final int MAX_DIFFICULTY = Challenge.ChallengeComplexity.values().length;

        // Determine hue range for difficulty
        float hueStart = (float) (difficulty - 1.5) / MAX_DIFFICULTY; // Start of hue range
        float hueEnd = ((float) difficulty) / ((float) MAX_DIFFICULTY);        // End of hue range
        
        for (int i = 0; i < length; i++) {
            char c = combined.charAt(i);

            // Interpolate hue based on character position
            float hue = hueStart + (hueEnd - hueStart) * ((float) i / (length - 1));
            float saturation = 0.5f;
            float brightness = 0.9f;
            Color color = Color.getHSBColor(hue, saturation, brightness);
            String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

            // Append each character with its corresponding color
            result = result.append(
                Component.text(String.valueOf(c))
                    .color(TextColor.fromHexString(hexColor))
            );

            if( i == playerName.length() - 1){
                result = result.append(
                    Component.text(", congratulations on completing the ")
                        .color(TextColor.color(255, 255, 255))
                );
            }
        }

        result = result.append(
            Component.text(" challenge again!")
                .color(TextColor.color(255, 255, 255))
        );
        return result;
    }

    private static Component generateDifficultyGradientMessage(String playerName, String challengeName, int difficulty) {
        String combined = playerName + challengeName;
        Component result = Component.empty();
        int length = combined.length();

        final int MAX_DIFFICULTY = Challenge.ChallengeComplexity.values().length;

        // Determine hue range for difficulty
        float hueStart = (float) (difficulty - 1.5) / MAX_DIFFICULTY; // Start of hue range
        float hueEnd = ((float) difficulty) / ((float) MAX_DIFFICULTY);        // End of hue range
        
        for (int i = 0; i < length; i++) {
            char c = combined.charAt(i);

            // Interpolate hue based on character position
            float hue = hueStart + (hueEnd - hueStart) * ((float) i / (length - 1));
            float saturation = 0.5f;
            float brightness = 0.9f;
            Color color = Color.getHSBColor(hue, saturation, brightness);
            String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

            // Append each character with its corresponding color
            result = result.append(
                Component.text(String.valueOf(c))
                    .color(TextColor.fromHexString(hexColor))
            );

            if( i == playerName.length() - 1){
                result = result.append(
                    Component.text(" has completed the ")
                        .color(TextColor.color(255, 255, 255))
                );
            }
        }

        result = result.append(
            Component.text(" challenge!")
                .color(TextColor.color(255, 255, 255))
        );

        return result;
    }

}