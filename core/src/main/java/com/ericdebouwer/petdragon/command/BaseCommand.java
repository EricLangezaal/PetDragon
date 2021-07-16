package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BaseCommand implements CommandExecutor, TabCompleter {

    public final static String NAME = "dragon";

    private List<SubCommand> subCommands = new ArrayList<>();
    private PetDragon plugin;

    public BaseCommand(PetDragon plugin){
        this.plugin = plugin;
        plugin.getCommand(NAME).setExecutor(this);
        plugin.getCommand(NAME).setTabCompleter(this);

        subCommands.add(new EggCmd(plugin));
        subCommands.add(new LocateCmd(plugin));
        subCommands.add(new ReloadCmd(plugin));
        subCommands.add(new RemoveCmd(plugin));
        subCommands.add(new SpawnCmd(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender,Command command, String label,  String[] args) {
        if (args.length == 0){
            plugin.getConfigManager().sendMessage(sender, Message.COMMAND_USAGE, null);
            return true;
        }
        SubCommand subCommand = subCommands.stream().filter(cmd -> cmd.name.equalsIgnoreCase(args[0])).findFirst().orElse(null);
        if (subCommand == null) {
            plugin.getConfigManager().sendMessage(sender, Message.COMMAND_USAGE, null);
            return true;
        }

        if (subCommand.isPlayerOnly() && !(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + plugin.logPrefix + "This command can only be used by a player!");
            return true;
        }

        if (!subCommand.hasPermission(sender)){
            plugin.getConfigManager().sendMessage(sender, Message.NO_PERMISSION_COMMAND, null);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.perform(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete( CommandSender sender, Command command,  String alias, String[] args) {
        if (args.length == 0) return Collections.emptyList();

        if (args.length == 1){
            return subCommands.stream().
                    filter(cmd -> cmd.hasPermission(sender) && cmd.name.startsWith(args[0].toLowerCase()))
                    .map(cmd -> cmd.name).collect(Collectors.toList());
        }
        SubCommand subCommand = subCommands.stream().
                filter(cmd -> cmd.name.equalsIgnoreCase(args[0]) && cmd.hasPermission(sender)).findFirst().orElse(null);

        if (subCommand == null) return Collections.emptyList();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return  subCommand.tabComplete(subArgs).stream()
                .filter(s -> s.startsWith(args[args.length - 1])).collect(Collectors.toList());
    }
}
