package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import org.bukkit.command.CommandSender;

public class ReloadCmd extends SubCommand {

    public ReloadCmd(PetDragon plugin) {
        super(plugin, "reload");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {
        configManager.reloadConfig();
        plugin.getFactory().reloadDragons();
        plugin.getDragonRegistry().setupConnection();
        if (configManager.isValid()) configManager.sendMessage(sender, Message.RELOAD_SUCCESS, null);
        else configManager.sendMessage(sender, Message.RELOAD_FAIL, null);
        return true;
    }

    @Override
    public boolean isPlayerOnly(){
        return false;
    }
}
