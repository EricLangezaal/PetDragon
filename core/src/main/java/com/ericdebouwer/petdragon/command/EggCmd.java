package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EggCmd extends SubCommand {

    public EggCmd(PetDragon plugin) {
        super(plugin, "egg");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        ItemStack egg = plugin.getCustomItems().getEgg();
        player.getInventory().addItem(egg);
        configManager.sendMessage(player, Message.EGG_RECEIVED, null);
        return true;
    }
}
