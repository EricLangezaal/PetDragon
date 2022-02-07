package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import com.google.common.collect.ImmutableMap;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;

import java.util.Set;

public class LocateCmd extends SubCommand {

    public LocateCmd(PetDragon plugin) {
        super(plugin, "locate");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        Set<EnderDragon> dragons = plugin.getFactory().getDragons(player);

        if (dragons.isEmpty()){
            plugin.getConfigManager().sendMessage(player, Message.NO_LOCATE, null);
            return true;
        }
        configManager.sendMessage(player, Message.LOCATED_DRAGONS, ImmutableMap.of("amount", "" + dragons.size()));
        for (EnderDragon dragon: dragons){
            Location loc = dragon.getLocation();
            String text = configManager.parseMessage(Message.LOCATE_ONE, ImmutableMap.of("x", "" +loc.getBlockX(),
                    "y", "" + loc.getBlockY(), "z", "" + loc.getBlockZ(), "world", loc.getWorld().getName()));

            if (configManager.clickToRemove) {
                TextComponent message = new TextComponent(text);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + BaseCommand.NAME + " remove " + dragon.getUniqueId()));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(configManager.parseMessage(Message.LOCATED_HOVER, null))));
                player.spigot().sendMessage(message);
            }else {
                player.sendMessage(text);
            }
        }
        return true;
    }
}
