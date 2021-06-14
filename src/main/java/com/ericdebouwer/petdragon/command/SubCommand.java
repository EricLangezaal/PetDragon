package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.ConfigManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SubCommand {

    PetDragon plugin;
    ConfigManager configManager;
    String name;

    public SubCommand(PetDragon plugin, String name){
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.name = name;
    }

    public boolean isPlayerOnly(){
        return true;
    }

    abstract boolean perform(CommandSender sender, String[] args);

    public List<String> tabComplete(String[] args){
        return Collections.emptyList();
    }

    public boolean hasPermission(CommandSender sender){
        return sender.hasPermission("petdragon.command." + this.name);
    }

    public List<String> filter(List<String> original, String query){
        return original.stream().filter(s -> s.startsWith(query)).collect(Collectors.toList());
    }

}
