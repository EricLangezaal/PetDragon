package com.ericdebouwer.petdragon.command;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class RemoveCmd extends SubCommand {

    public RemoveCmd(PetDragon plugin) {
        super(plugin, "remove");
    }

    @Override
    boolean perform(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        int range = 3;
        boolean found = false;

        if (args.length >= 2){
            try {
                Entity potentialDragon = Bukkit.getEntity(UUID.fromString(args[1]));
                if (plugin.getFactory().isPetDragon(potentialDragon)){
                    potentialDragon.remove();
                    found = true;
                }

            } catch(IllegalArgumentException ila){
                try {
                    int argRange = Integer.parseInt(args[0]);
                    if (argRange < 0 || argRange > 20) throw new NumberFormatException();
                    range = argRange;
                } catch (NumberFormatException e){
                    configManager.sendMessage(player, Message.RANGE_INVALID, null);
                    return true;
                }
            }
        }
        if (!found) {

            List<Entity> nearbyEnts = (List<Entity>) player.getWorld().getNearbyEntities(
                    player.getLocation(), range, range, range, (e) -> plugin.getFactory().isPetDragon(e));
            nearbyEnts.sort(Comparator.comparingDouble((e) ->
                    e.getLocation().distanceSquared(player.getLocation())));

            if (!nearbyEnts.isEmpty()){
                EnderDragon dragon = (EnderDragon) nearbyEnts.get(0);
                UUID owner = plugin.getFactory().getOwner(dragon);
                if (!player.hasPermission("petdragon.bypass.remove") && !player.getUniqueId().equals(owner)){
                    String ownerName = Bukkit.getOfflinePlayer(owner).getName();
                    configManager.sendMessage(player, Message.NOT_YOURS_TO_REMOVE,
                            ImmutableMap.of("owner", ownerName == null ? "unknown" : ownerName));
                    return true;
                }
                dragon.remove();
                found = true;
            }
        }

        if (found) configManager.sendMessage(player, Message.DRAGON_REMOVED, null);
        else configManager.sendMessage(player, Message.DRAGON_NOT_FOUND, null);

        return true;
    }

    @Override
    public List<String> tabComplete(String[] args){
        if (args.length == 1)
            return this.filter(Arrays.asList("3", "5", "10"), args[0].toLowerCase());

        return super.tabComplete(args);
    }
}
