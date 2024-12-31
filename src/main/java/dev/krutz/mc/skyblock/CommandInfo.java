package dev.krutz.mc.skyblock;

import java.util.List;
import java.util.function.BiConsumer;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class CommandInfo {
    private final String baseCommand;
    private final String description;
    private BiConsumer<CommandSender, String[]> action;
    private List<String> aliases = new ArrayList<>();
    private List<String> requiredArgs = new ArrayList<>();
    private String permission = null;
    private boolean playerOnly = false;

    public CommandInfo(String baseCommand, String description, BiConsumer<CommandSender, String[]> action) {
        this.baseCommand = baseCommand;
        this.description = description;
        this.action = action;
    }

    public String getBaseCommand() {
        return baseCommand;
    }

    public String getDescription() {
        return description;
    }

    public BiConsumer<CommandSender, String[]> getAction() {
        return action;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getRequiredArgs() {
        return requiredArgs;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public CommandInfo setAliases(List<String> aliases) {
        this.aliases = aliases;
        return this;
    }

    public CommandInfo setRequiredArgs(List<String> requiredArgs) {
        this.requiredArgs = requiredArgs;
        return this;
    }

    public CommandInfo setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public CommandInfo setPlayerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
        return this;
    }

    public void setAction(BiConsumer<CommandSender, String[]> action) {
        this.action = action;
    }
}
