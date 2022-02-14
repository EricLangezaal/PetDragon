package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon;
import com.google.common.collect.ImmutableMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCmd extends SubCommand {

    public SpawnCmd(PetDragon plugin) {
        super(plugin, "spawn");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("petdragon.bypass.dragonlimit")) {
            int dragonCount = plugin.getFactory().getDragons(player).size();
            if (dragonCount >= plugin.getConfigManager().getMaxDragons()){
                configManager.sendMessage(player, Message.DRAGON_LIMIT, ImmutableMap.of("amount", "" + dragonCount));
                return true;
            }
        }

        PetEnderDragon dragon = plugin.getFactory().create(player.getLocation().add(0, 2, 0), player.getUniqueId());
        dragon.spawn();
        configManager.sendMessage(player, Message.DRAGON_SPAWNED, null);

        return true;
    }
}
