package dev.krutz.mc.skyblock;

import org.bukkit.entity.Player;

import java.util.List;

public class IslandCommandManager {

    public void registerCommands(CommandManager commandManager) {
        SubcommandGroup islandCommands = new SubcommandGroup("island", "Manage your island.");
        islandCommands.setAliases(List.of("is"));

        islandCommands.addSubcommand(new CommandInfo(
            "accept", 
            "Accept an island invite", 
            (sender, args) -> { IslandManager.acceptIslandInvite((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "auto", 
            "Teleports you to your island (or create one)",
            (sender, args) -> {
                Player player = (Player) sender;
                if(IslandManager.getIslandByPlayerUUID(player.getUniqueId().toString()) == null) 
                    IslandManager.createIsland(player);
                else
                    IslandManager.teleportToIsland(player);
            })
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "ban",
             "Ban a player from your island", 
             null));

        islandCommands.addSubcommand(new CommandInfo(
            "create",
            "Create a new island.",
            (sender, args) -> { IslandManager.createIsland((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "home",
            "Teleport to your island home.",
            (sender, args) -> { IslandManager.teleportToIsland((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "info",
            "Check your or another player's island info.",
            null)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "invite",
            "Invite a player to your island.",
            (sender, args) -> {IslandManager.invitePlayerToIsland((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "kick",
            "Kick a player from your island.",
            (sender, args) -> {IslandManager.kickPlayerFromIsland((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
            .setAliases(List.of("remove"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "leave",
            "Leave your party.",
            (sender, args) -> {IslandManager.leaveIsland((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "level", 
            "Check your or another player's island level", 
            null));

        islandCommands.addSubcommand(new CommandInfo(
            "lock",
            "Lock your island to non-party members.",
            null)
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "makeleader",
            "Make a party member the party leader.",
            (sender, args) -> { IslandManager.makeIslandLeader((Player) sender, args);})
            .setPlayerOnly(true)
            .setAliases(List.of("promote"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "open",
            "Open your island to visitors.",
            null)
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "party",
            "Show party information",
            (sender, args) -> { IslandManager.showIslandParty((Player) sender);})
            .setPlayerOnly(true)
            .setAliases(List.of("p"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "permissions",
            "Change a member's island permissions.",
            null)
            .setPlayerOnly(true)
            .setAliases(List.of("perms"))
            .setRequiredArgs(List.of("player"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "reject",
            "Reject an island invite.",
            (sender, args) -> { IslandManager.rejectIslandInvite((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "restart",
            "Restart your island.",
            (sender, args) -> { IslandManager.restartIsland((Player) sender);})
            .setPlayerOnly(true)
            .setAliases(List.of("reset"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setbiome",
            "Set your island biome.",
            null)
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setfarewell",
            "Set your island farewell.",
            (sender, args) -> { IslandManager.setFarewell((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("farewell"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setgreeting",
            "Set your island greeting.",
            (sender, args) -> { IslandManager.setGreeting((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("greeting"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "sethome",
            "Set your island home.",
            (sender, args) -> { IslandManager.setIslandSpawn((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "settings",
            "Change your island settings.",
            null)
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setwarp",
            "Set your island's warp location.",
            null)
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
                "teleport",
                "Teleport to a party member.",
                (sender, args) -> {IslandManager.teleportToIslandFriend((Player) sender, args);})
                .setPlayerOnly(true)
                .setAliases(List.of("tp"))
                .setRequiredArgs(List.of("friend"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "togglewarp",
            "Activate or deactivate your island's warp.",
            null)
            .setPlayerOnly(true)
            .setAliases(List.of("tw"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "top",
            "Check the top islands.",
            null)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "unban",
            "Unban a player from your island.",
            null)
        );

        commandManager.registerCommand(islandCommands);

    }

    // @Override
    // public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    //     if (args.length == 0) {
    //         if (sender instanceof Player player) {
    //             new IslandListener().openIslandMenu(player);
    //         } else {
    //             sender.sendMessage("This command can only be executed by players.");
    //         }
    //         return true;
    //     }

    //     String subcommandName = args[0].toLowerCase();
    //     CommandInfo subcommand = islandCommands.getSubcommand(subcommandName);

    //     // Check aliases
    //     if (subcommand == null) {
    //         for (CommandInfo cmd : islandCommands.getSubcommands().values()) {
    //             if (cmd.getAliases().contains(subcommandName)) {
    //                 subcommand = cmd;
    //                 break;
    //             }
    //         }
    //     }

    //     if (subcommand == null) {
    //         sender.sendMessage(Component.text("Unknown " + command.getName() + " subcommand: " + args[0]).color(NamedTextColor.RED));
    //         return true;
    //     }

    //     if (subcommand.isPlayerOnly() && !(sender instanceof Player)) {
    //         sender.sendMessage(Component.text("This command can only be executed by players.").color(NamedTextColor.RED));
    //         return true;
    //     }

    //     if (subcommand.getPermission() != null && !sender.hasPermission(subcommand.getPermission())) {
    //         sender.sendMessage(Component.text("You don't have permission to execute this command.").color(NamedTextColor.RED));
    //         return true;
    //     }

    //     // Validate required arguments
    //     if (args.length - 1 < subcommand.getRequiredArgs().size()) {
    //         sender.sendMessage(
    //             Component.text("Invalid syntax! Try: ").color(NamedTextColor.WHITE).append(
    //                 Component.text("/" + islandCommands.getBaseCommand() + " " + subcommand.getBaseCommand()).color(NamedTextColor.GOLD).append(
    //                     Component.text(" " + String.join(" ", 
    //                         subcommand.getRequiredArgs().stream()
    //                             .map(arg -> "<" + arg + ">")  // Wrap each argument in angle brackets
    //                             .toArray(String[]::new))).color(NamedTextColor.RED)).append(Component.text(".").color(NamedTextColor.WHITE))));
    //         return true;
    //     }

    //     // Execute the subcommand action
    //     subcommand.getAction().accept(sender, args);
    //     return true;
    // }
}