package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;

public class ReloadCmd extends SubCommand {

    public ReloadCmd(PetDragon plugin) {
        super(plugin, "reload");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {
        configManager.reloadConfig();
        for (World world: Bukkit.getWorlds()){
            for (EnderDragon dragon: world.getEntitiesByClass(EnderDragon.class)){
                plugin.getFactory().resetDragon(dragon);
            }
        }
        if (configManager.isValid()) configManager.sendMessage(sender, Message.RELOAD_SUCCESS, null);
        else configManager.sendMessage(sender, Message.RELOAD_FAIL, null);
        return true;
    }

    @Override
    public boolean isPlayerOnly(){
        return false;
    }
}
