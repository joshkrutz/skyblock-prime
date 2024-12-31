package dev.krutz.mc.skyblock;

import java.awt.Component;

import net.md_5.bungee.api.chat.ComponentBuilder;

public class SkyblockUtil {

    public static class ComplexityHue {
        private final float startHue;
        private final float stopHue;

        public ComplexityHue(float startHue, float stopHue) {
            this.startHue = startHue;
            this.stopHue = stopHue;
        }

        public float getStartHue() {
            return startHue;
        }

        public float getStopHue() {
            return stopHue;
        }
    }

    // make static method that turns Challenge Complexity into a hue
    public static ComplexityHue getHueStartAndStopFromComplexity(Challenge.ChallengeComplexity complexity) {
        final int MAX_DIFFICULTY = Challenge.ChallengeComplexity.values().length;
        float hueStart = (float) (complexity.getValue() - 1.5) / MAX_DIFFICULTY;
        float hueEnd = ((float) complexity.getValue()) / ((float) MAX_DIFFICULTY);  

        return new ComplexityHue(hueStart, hueEnd);
    }

    // public Component generateIncorrectSyntaxMessage(String command, String feedback, String usage) {
    //     ComponentBuilder builder = new ComponentBuilder();
    //     builder.append(feedback);
    // }
}
