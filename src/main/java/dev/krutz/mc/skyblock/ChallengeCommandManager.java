package dev.krutz.mc.skyblock;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

public class ChallengeCommandManager {

    public void registerCommands(CommandManager commandManager) {
        SubcommandGroup challengeCommands = new SubcommandGroup("challenge", "See all challenge related commands.");
        challengeCommands.setAliases(List.of("c"));

        challengeCommands.addSubcommand(new CommandInfo(
            "complete", 
            "Complete a challenge.", 
            (sender, args) -> {
                String challengeCmdName = args[1].toLowerCase();
                Challenge challenge = ChallengeManager.getChallenge(challengeCmdName);
                if (challenge == null) {
                    sender.sendMessage("Challenge not found: " + challengeCmdName);
                }

                ChallengeManager.completeChallenge((Player) sender, challenge);
            })
            .setPlayerOnly(true)
            .setAliases(List.of("c"))
            .setRequiredArgs(List.of("challenge name"))
        );

        challengeCommands.addSubcommand(new CommandInfo(
            "list", 
            "List all challenges available to you.",
            (sender, args) -> { ChallengeManager.listChallenges((Player) sender);})
            .setPlayerOnly(true)
        );

        challengeCommands.addSubcommand(new CommandInfo(
            "create",
            "Create a new challenge.",
            (sender, args) -> { 
                // challenge name is args 2 to end
                String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (challengeName.isEmpty()) {
                    sender.sendMessage("Usage: /challenge create <challengeName>");
                    return;
                }
                ChallengeManager.createChallenge((Player) sender, challengeName);
            })
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("name"))
        );

        commandManager.registerCommand(challengeCommands);

    }
}