package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import com.ericdebouwer.petdragon.database.DatabaseDragon;
import com.google.common.collect.ImmutableMap;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LocateCmd extends SubCommand {

    public LocateCmd(PetDragon plugin) {
        super(plugin, "locate");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        plugin.getDragonRegistry().fetchDragons(player.getUniqueId()).thenAccept((dragons) -> {
            if (dragons.isEmpty()){
                plugin.getConfigManager().sendMessage(player, Message.NO_LOCATE, null);
                return;
            }
            configManager.sendMessage(player, Message.LOCATED_DRAGONS, ImmutableMap.of("amount", "" + dragons.size()));
            for (DatabaseDragon dragon: dragons){
                Location loc = dragon.getLocation();
                String text = configManager.parseMessage(Message.LOCATE_ONE, ImmutableMap.of("x", "" +loc.getBlockX(),
                        "y", "" + loc.getBlockY(), "z", "" + loc.getBlockZ(), "world", dragon.getWorldName()));

                if (configManager.clickToRemove) {
                    TextComponent message = new TextComponent(text);
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + BaseCommand.NAME + " remove " + dragon.getUniqueId()));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(configManager.parseMessage(Message.LOCATED_HOVER, null))));
                    player.spigot().sendMessage(message);
                }else {
                    player.sendMessage(text);
                }
            }
        });
        return true;
    }
}
