package dev.krutz.mc.skyblock;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Generate the auto-complete for a command
     * @param sender
     * @param command
     * @param alias
     * @param args
     * @return
     */
    public List<String> getTabComplete(CommandSender sender, Command command, String label, String[] args) {

        // Auto-fill command
        if(args.length == 0){
            List<String> options = new ArrayList<String>(commands.keySet());
            return filterSuggestions(label, options);
        }

        CommandInfo cmd = getCommand(label);

        // Auto fill subcommand
        if (cmd != null && cmd instanceof SubcommandGroup cmd_group) {
            // Auto fill subcommands if applicable
            if(args.length == 1){
                List<String> subcmds = new ArrayList<String>(cmd_group.getSubcommands().keySet());
                return filterSuggestions(args[0], subcmds);
            }

            // Auto fill subcommand args if applicable
            if(args.length > 1){
                String subcommand_string = args[0];
                CommandInfo subcommand = cmd_group.getSubcommand(subcommand_string);
                if(subcommand != null){
                    List<String> templated_args = subcommand.getRequiredArgs();
                    List<String> typed_args = new ArrayList<String>(List.of(args));

                    // Remove index 0, this is the subcommand
                    typed_args.remove(0);
                    
                    if(typed_args.size() > templated_args.size()){
                        return Collections.emptyList();
                    }

                    

                    // If there are templated args, continue processing
                    if(templated_args.size() >= 1){
                        String currentArgumentTemplate = templated_args.get(typed_args.size() - 1);
                        // If arg is all players, sub in all offline/online players
                        if(currentArgumentTemplate.equalsIgnoreCase("All Players")){
                            ArrayList<String> allPlayers = new ArrayList<>();
                            for(OfflinePlayer p : Bukkit.getOfflinePlayers()){
                                allPlayers.add(p.getName());
                            }
                            return filterSuggestions(typed_args.get(typed_args.size() - 1), allPlayers);
                        }
                        // If arg is online players, sub in all online players
                        else if(currentArgumentTemplate.equalsIgnoreCase("Online Players")){
                            ArrayList<String> allPlayers = new ArrayList<>();
                            for(Player p : Bukkit.getOnlinePlayers()){
                                allPlayers.add(p.getName());
                            }
                            return filterSuggestions(typed_args.get(typed_args.size() - 1), allPlayers);
                        }
                        // If arg is Biome, sub in all available biomes
                        else if(currentArgumentTemplate.equalsIgnoreCase("Biomes")){
                            List<String> biomes = List.of(
                                "plains", "desert", "forest", "swamp", "taiga",
                                "snowy_taiga", "savanna", "jungle", "badlands",
                                "ocean", "river", "mushroom_fields", "dripstone_caves",
                                "lush_caves", "deep_dark", "pale_garden", "meadow"
                            );
                            return filterSuggestions(typed_args.get(typed_args.size() - 1), biomes);
                        }
                        // Return the template variable name
                        return filterSuggestions(typed_args.get(typed_args.size() - 1),  List.of("<" + currentArgumentTemplate + ">"));
                    }
                }
            }
        }

        // Fill in args
        if(cmd != null && args.length >= 1){
            // Suggest first arg if not subcommand type
            List<String> cmdargs = cmd.getRequiredArgs();

            if(cmdargs.size() >= 1){
                return filterSuggestions(args[args.length - 1], List.of(cmdargs.get(args.length - 1)));
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterSuggestions(String current, List<String> options) {
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(current.toLowerCase())) {
                filtered.add(option);
            }
        }
        return filtered;
    }

}