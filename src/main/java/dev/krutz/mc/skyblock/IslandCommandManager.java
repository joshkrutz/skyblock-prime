package dev.krutz.mc.skyblock;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            "Turn island warp on or off.",
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

        islandCommands.addSubcommand(new CommandInfo(
            "help",
            "Get information on island commands.",
            (sender, args) -> { sendHelpMenu(sender, args, islandCommands); })
            .setAliases(List.of("?"))
            .setRequiredArgs(List.of("page"))
        );

        commandManager.registerCommand(islandCommands);

    }

    private void sendHelpMenu(CommandSender sender, String[] args, SubcommandGroup islandCommands) {

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
               page = 1;
            }
        }

        Component message = Component.text("\n-====== Island Help Menu ======-\n")
            .color(TextColor.color(NamedTextColor.GREEN))
            .decoration(TextDecoration.BOLD, true) // Bold the title
            .append(
                Component.text("Click to execute a command or read more...\n")
                    .color(TextColor.color(NamedTextColor.YELLOW))
                    .decoration(TextDecoration.BOLD, false) // Remove bold for this part
            );

        Set<CommandInfo> commandSet = new LinkedHashSet<>(islandCommands.getSubcommands().values()); 
        List<CommandInfo> sortedCommands = new ArrayList<>(commandSet); 
        Collections.sort(sortedCommands, new Comparator<CommandInfo>() { 
            @Override public int compare(CommandInfo c1, CommandInfo c2) { 
                return c1.getBaseCommand().compareTo(c2.getBaseCommand()); 
            }
        });

        // Calculate the maximum number of pages
        int totalCommands = sortedCommands.size();
        int totalPages = (int) Math.ceil(totalCommands / 5.0);
        page = Math.max(1, Math.min(page, totalPages));
        int startIndex = (page - 1) * 5;
        int endIndex = Math.min(startIndex + 5, totalCommands);

        for (int i = startIndex; i < endIndex; i++) {
            CommandInfo command = sortedCommands.get(i);
            if(command.getPermission() != null && !sender.hasPermission(command.getPermission())) continue;

            message = message.append(getCommandComponent(command));
        }

        message = message.append(
            Component.text("\n")
            .append(Component.text("\n<<             ")
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/island help " + Math.max(1, page - 1)))
                .hoverEvent(HoverEvent.showText(Component.text("Click for previous page.").color(TextColor.color(NamedTextColor.WHITE)))))
            .append(Component.text("Page ")
                .decoration(TextDecoration.BOLD, false))
            .append(Component.text(page)
                .decoration(TextDecoration.BOLD, false))
            .append(Component.text("/")
                .decoration(TextDecoration.BOLD, false))
            .append(Component.text(totalPages)
                .decoration(TextDecoration.BOLD, false))
            .append(Component.text("             >>")
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/island help " + Math.min(totalPages, page + 1)))
                .hoverEvent(HoverEvent.showText(Component.text("Click for next page").color(TextColor.color(NamedTextColor.WHITE)))))
                .color(NamedTextColor.YELLOW)
        );

        sender.sendMessage(message);
    }

    private Component getAliasesStyled(List<String> aliases){
        if (aliases.isEmpty()) return Component.empty();

        Component builder = Component.text("\nAliases: ")
            .color(TextColor.color(NamedTextColor.GOLD));
        
        for(String alias : aliases){
            Component aliasComponent = Component.text(alias)
                .color(TextColor.color(NamedTextColor.YELLOW));

            if(aliases.indexOf(alias) != aliases.size() - 1){
                aliasComponent = aliasComponent.append(Component.text(", ").color(NamedTextColor.GOLD));
            }

            builder = builder.append(aliasComponent);
        }

        return builder;
    }

    private Component getArgsComponents(List<String> args){
        Component builder = Component.empty();

        for(String arg : args){
            Component argComponent = Component.text(" <" + arg + ">")
                .color(TextColor.color(NamedTextColor.YELLOW))
                .decoration(TextDecoration.BOLD, false);

            builder = builder.append(argComponent);
        }

        return builder;
    }

    private Component getCommandComponent(CommandInfo command) {
        Component commandComponent = Component.text("\n  /island " + command.getBaseCommand())
            .color(TextColor.color(NamedTextColor.BLUE)) // Command color
            .clickEvent(ClickEvent.suggestCommand("/island " + command.getBaseCommand())) 
            .hoverEvent(
                HoverEvent.showText(
                    Component.text("Click to fast type command.\n\n")
                        .color(TextColor.color(NamedTextColor.WHITE))

                    // Template Usage
                    .append(Component.text("Usage: ")
                        .color(TextColor.color(NamedTextColor.GOLD))
                        .append(Component.text("/island " + command.getBaseCommand()).color(TextColor.color(NamedTextColor.YELLOW)))
                        .append(getArgsComponents(command.getRequiredArgs())))
                    
                    // Accepted Aliases (if any)
                    .append(getAliasesStyled(command.getAliases()))
                )
            )
            .decoration(TextDecoration.BOLD, false);
        
        Component descriptionComponent = Component.text(" - " + command.getDescription())
            .color(TextColor.color(NamedTextColor.AQUA)) // Description color
            .decoration(TextDecoration.BOLD, false);

        return commandComponent.append(descriptionComponent);
    
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