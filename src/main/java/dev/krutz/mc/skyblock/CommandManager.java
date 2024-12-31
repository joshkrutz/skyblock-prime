package dev.krutz.mc.skyblock;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private final Map<String, CommandInfo> commands = new HashMap<>();

    public CommandManager() {
    }

    public void registerCommand(CommandInfo command) {
        commands.put(command.getBaseCommand(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }

    public CommandInfo getCommand(String name) {
        return commands.get(name);
    }

    public boolean executeCommand(CommandSender sender, String label, String[] args) {
        CommandInfo command = commands.get(label.toLowerCase());

        if (command == null) {
            sender.sendMessage(Component.text("Unknown command: " + label).color(NamedTextColor.RED));
            return false;
        }

        // Check if the command is player-only
        if (command.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be executed by players.").color(NamedTextColor.RED));
            return false;
        }

        // Check if the sender has the required permission
        if (command.getPermission() != null && (!sender.hasPermission(command.getPermission()) || command.getPermission().equals("op") && !sender.isOp())) {
            sender.sendMessage(Component.text("You don't have permission to execute this command.").color(NamedTextColor.RED));
            return false;
        }

        // Handle subcommands
        if (command instanceof SubcommandGroup group && args.length > 0) {
            CommandInfo subcommand = group.getSubcommand(args[0].toLowerCase());
            if (subcommand == null) {
                sender.sendMessage(Component.text("Unknown " + command.getBaseCommand() + " subcommand: " + args[0]).color(NamedTextColor.RED));
                return false;
            }

            String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            
            // Execute the subcommand action
            if(subcommand.getAction() != null) {
                subcommand.getAction().accept(sender, subArgs);
                return true;
            }

            sender.sendMessage("This subcommand is not yet implemented.");
            return false;
        }

        // Execute the action
        if (command.getAction() != null) {
            command.getAction().accept(sender, args);
            return true;
        }

        sender.sendMessage("This command is not yet implemented.");
        return false;
    }
}
