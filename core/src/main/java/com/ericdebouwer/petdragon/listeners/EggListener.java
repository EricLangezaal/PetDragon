package com.ericdebouwer.petdragon.listeners;

import com.ericdebouwer.petdragon.PetDragon;
import com.ericdebouwer.petdragon.config.Message;
import com.ericdebouwer.petdragon.enderdragonNMS.PetEnderDragon;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

@RequiredArgsConstructor
public class EggListener implements Listener {

    private final PetDragon plugin;

    @EventHandler(priority= EventPriority.HIGH, ignoreCancelled=true)
    public void eggPlace(BlockPlaceEvent e){
        if (!e.getItemInHand().isSimilar(plugin.getCustomItems().getEgg())) return;
        if (!e.getPlayer().hasPermission("petdragon.spawnegg")) {
            plugin.getConfigManager().sendMessage(e.getPlayer(), Message.NO_EGG, null);
            e.setCancelled(true);
            return;
        }

        if (plugin.getConfigManager().isEggAbidesDragonMax() && !e.getPlayer().hasPermission("petdragon.bypass.dragonlimit")) {
            int dragonCount = plugin.getFactory().getDragons(e.getPlayer()).size();
            if (dragonCount >= plugin.getConfigManager().getMaxDragons()){
                plugin.getConfigManager().sendMessage(e.getPlayer(), Message.DRAGON_LIMIT, ImmutableMap.of("amount", "" + dragonCount));
                e.setCancelled(true);
                return;
            }
        }

        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && plugin.getConfigManager().isAlwaysUseUpEgg()){
            if (e.getItemInHand().getAmount() > 1) {
                e.getItemInHand().setAmount(e.getItemInHand().getAmount() - 1);
                e.getPlayer().getInventory().setItem(e.getHand(), e.getItemInHand());
            }
            else e.getPlayer().getInventory().setItem(e.getHand(), null);
        }

        PetEnderDragon dragon = plugin.getFactory().create(e.getBlock().getLocation().add(0, 3, 0), e.getPlayer().getUniqueId());
        dragon.spawn();

        plugin.getConfigManager().sendMessage(e.getPlayer(), Message.EGG_HATCHED, null);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> e.getBlock().setType(Material.AIR));
    }
}
