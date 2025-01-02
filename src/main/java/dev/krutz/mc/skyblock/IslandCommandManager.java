package dev.krutz.mc.skyblock;

import org.bukkit.entity.Player;

import java.util.List;

public class IslandCommandManager {

    public void registerCommands(CommandManager commandManager, IslandManager islandManager) {
        SubcommandGroup islandCommands = new SubcommandGroup("island", "Manage your island.");
        islandCommands.setAliases(List.of("is"));

        islandCommands.addSubcommand(new CommandInfo(
            "accept", 
            "Accept an island invite", 
            (sender, args) -> { islandManager.acceptIslandInvite((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "auto", 
            "Teleports you to your island (or create one)",
            (sender, args) -> {
                Player player = (Player) sender;
                if(islandManager.getIslandByPlayerUUID(player.getUniqueId()) == null) 
                    islandManager.createIsland(player);
                else
                    islandManager.teleportToIsland(player);
            })
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "ban",
             "Ban a player from your island", 
             (sender, args) -> { islandManager.banPlayerFromIsland((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "create",
            "Create a new island.",
            (sender, args) -> { islandManager.createIsland((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "home",
            "Teleport to your island home.",
            (sender, args) -> { islandManager.teleportToIsland((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "info",
            "Check your or another player's island info.",
            (sender, args) -> { islandManager.showIslandInfo(sender, args);})
            .setRequiredArgs(List.of("player"))
            .setAliases(List.of("level"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "invite",
            "Invite a player to your island.",
            (sender, args) -> {islandManager.invitePlayerToIsland((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "kick",
            "Kick a player from your island.",
            (sender, args) -> {islandManager.kickPlayerFromIsland((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
            .setAliases(List.of("remove"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "leave",
            "Leave your party.",
            (sender, args) -> {islandManager.leaveIsland((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "lock",
            "Lock your island to non-party members.",
            (sender, args) -> { islandManager.lockWarp((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "makeleader",
            "Make a party member the party leader.",
            (sender, args) -> { islandManager.makeIslandLeader((Player) sender, args);})
            .setPlayerOnly(true)
            .setAliases(List.of("promote"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "open",
            "Open your island to visitors.",
            (sender, args) -> { islandManager.unlockWarp((Player) sender);})
            .setPlayerOnly(true)
            .setAliases(List.of("unlock"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "party",
            "Show party information",
            (sender, args) -> { islandManager.showIslandParty((Player) sender);})
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
            (sender, args) -> { islandManager.rejectIslandInvite((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
            "restart",
            "Restart your island.",
            (sender, args) -> { islandManager.restartIsland((Player) sender);})
            .setPlayerOnly(true)
            .setAliases(List.of("reset"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setbiome",
            "Set your island biome.",
            (sender, args) -> { islandManager.setIslandBiome((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("biome"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setfarewell",
            "Set your island farewell.",
            (sender, args) -> { islandManager.setFarewell((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("farewell"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "setgreeting",
            "Set your island greeting.",
            (sender, args) -> { islandManager.setGreeting((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("greeting"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "sethome",
            "Set your island home.",
            (sender, args) -> { islandManager.setIslandSpawn((Player) sender);})
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
            (sender, args) -> { islandManager.setIslandWarp((Player) sender);})
            .setPlayerOnly(true)
        );

        islandCommands.addSubcommand(new CommandInfo(
                "teleport",
                "Teleport to a party member.",
                (sender, args) -> {islandManager.teleportToIslandFriend((Player) sender, args);})
                .setPlayerOnly(true)
                .setAliases(List.of("tp"))
                .setRequiredArgs(List.of("friend"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "togglewarp",
            "Activate or deactivate your island's warp.",
            (sender, args) -> { islandManager.toggleWarpLock((Player) sender);})
            .setPlayerOnly(true)
            .setAliases(List.of("tw"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "top",
            "Check the top islands.",
            (sender, args) -> { islandManager.sendTopIslandsMsg(sender);})
        );

        islandCommands.addSubcommand(new CommandInfo(
            "unban",
            "Unban a player from your island.",
            (sender, args) -> { islandManager.unbanPlayerFromIsland((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
            .setAliases(List.of("pardon"))
        );

        islandCommands.addSubcommand(new CommandInfo(
            "warp",
            "Warp to your or another player's island.",
            (sender, args) -> { islandManager.warpTeleport((Player) sender, args);})
            .setPlayerOnly(true)
            .setRequiredArgs(List.of("player"))
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