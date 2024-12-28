package dev.krutz.mc.skyblock;

import java.util.ArrayList;
import java.util.List;

public class Challenge {

    public enum ChallengeComplexity {
        NOVICE(0),
        APPRENTICE(1),
        JOURNEYMAN(2),
        ADEPT(3),
        EXPERT(4),
        MASTER(5),
        SKYLORD(6);

        private final int value;
        private ChallengeComplexity(int value) {
            this.value = value;
        }

        public String toString(){
            return name().toUpperCase();
        }

        public int getValue(){
            return value;
        }

        public static String[] getComplexityNames(){
            ChallengeComplexity[] complexities = ChallengeComplexity.values();
            String[] names = new String[complexities.length];
            for (int i = 0; i < complexities.length; i++) {
                names[i] = complexities[i].toString();
            }
            return names;
        }
    }

    private String name;
    private String description;
    private ChallengeComplexity complexity;
    private String command;
    private List<Requirement> requirements = new ArrayList<>();
    private List<Reward> rewards = new ArrayList<>();
    private List<Reward> repeatableRewards = new ArrayList<>();

    public Challenge(String name, String description, String complexity, List<Requirement> requirements, List<Reward> rewards) {
        this.name = name;
        this.description = description;
        this.complexity = getComplexityFromString(complexity);
        this.command = name.toLowerCase().replace(" ", "");
        this.requirements = requirements;
        this.rewards = rewards;
    }

    private ChallengeComplexity getComplexityFromString(String complexity) {
        switch (complexity.toLowerCase()) {
            case "novice":
                return ChallengeComplexity.NOVICE;
            case "apprentice":
                return ChallengeComplexity.APPRENTICE;
            case "journeyman":
                return ChallengeComplexity.JOURNEYMAN;
            case "adept":
                return ChallengeComplexity.ADEPT;
            case "expert":
                return ChallengeComplexity.EXPERT;
            case "master":
                return ChallengeComplexity.MASTER;
            case "skylord":
                return ChallengeComplexity.SKYLORD;
            default:
                return ChallengeComplexity.NOVICE;
        }
    }

    public Challenge(String name, String description, String complexity, List<Requirement> requirements, List<Reward> rewards, List<Reward> repeatableRewards) {
        this.name = name;
        this.description = description;
        this.complexity = getComplexityFromString(complexity);
        this.command = name.toLowerCase().replace(" ", "");
        this.requirements = requirements;
        this.rewards = rewards;
        this.repeatableRewards = repeatableRewards;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public List<Reward> getRepeatableRewards() {
        return repeatableRewards;
    }

    public boolean isRepeatable(){
        return repeatableRewards.size() > 0;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ChallengeComplexity getComplexity() {
        return complexity;
    }

    public int getDifficultyRating(){
        return complexity.getValue();
    }

    public String getCommand() {
        return command;
    }
}