package dev.krutz.mc.skyblock;

import java.util.HashMap;
import java.util.Map;

public class SubcommandGroup extends CommandInfo {
    private final Map<String, CommandInfo> subcommands = new HashMap<>();

    public SubcommandGroup(String baseCommand, String description) {
        super(baseCommand, description, null);
    }

    public void addSubcommand(CommandInfo subcommand) {
        subcommands.put(subcommand.getBaseCommand(), subcommand);
        // Include aliases if any
        for (String alias : subcommand.getAliases()) {
            subcommands.put(alias, subcommand);
        }
    }

    public CommandInfo getSubcommand(String name) {
        return subcommands.get(name);
    }

    public Map<String, CommandInfo> getSubcommands() {
        return subcommands;
    }
}