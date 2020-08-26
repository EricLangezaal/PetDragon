package com.ericdebouwer.petdragon;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class DragonCommand implements CommandExecutor, TabCompleter {
	
	PetDragon plugin;
	ConfigManager manager;
	
	public DragonCommand(PetDragon plugin){
		this.plugin = plugin;
		this.manager = plugin.getConfigManager();
		plugin.getCommand("dragon").setExecutor(this);
		plugin.getCommand("dragon").setTabCompleter(this);
	}

	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")){
			if (!sender.hasPermission("petdragon.command.reload")){
				manager.sendMessage(sender, Message.NO_PERMISSION_COMMAND, null);
				return true;
			}
			manager.reloadConfig();
			if (manager.isValid()) manager.sendMessage(sender, Message.RELOAD_SUCCESS, null);
			else manager.sendMessage(sender, Message.RELOAD_FAIL, null);
			return true;
		}
		
		if (!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "This command can only be used by a player!");
			return true;
		}
		Player player = (Player) sender;
		if (args.length > 0){
			if (args[0].equalsIgnoreCase("spawn")){
				if (!player.hasPermission("petdragon.command.spawn")){
					plugin.getConfigManager().sendMessage(player, Message.NO_PERMISSION_COMMAND, null);
					return true;
				}
				if (!player.hasPermission("petdragon.bypass.dragonlimit")){
					int dragonCount = plugin.getFactory().getDragons(player).size();
					if (dragonCount >= plugin.getConfigManager().maxDragons){
						plugin.getConfigManager().sendMessage(player, Message.DRAGON_LIMIT, ImmutableMap.of("amount", "" + dragonCount));
						return true;
					}
				}
				PetEnderDragon dragon = plugin.getFactory().create(player.getLocation().add(0, 2, 0), player.getUniqueId());
				dragon.spawn();
				plugin.getConfigManager().sendMessage(player, Message.DRAGON_SPAWNED, null);
				return true;
			}
			else if (args[0].equalsIgnoreCase("remove")){
				if (!player.hasPermission("petdragon.command.remove")){
					plugin.getConfigManager().sendMessage(player, Message.NO_PERMISSION_COMMAND, null);
					return true;
				}
				boolean found = false;
				for (Entity ent: player.getNearbyEntities(3, 3, 3)){
					if (ent instanceof EnderDragon && ent.getScoreboardTags().contains(PetEnderDragon.DRAGON_ID)){
						plugin.getFactory().removeDragon((EnderDragon) ent);
						found = true;
						plugin.getConfigManager().sendMessage(player, Message.DRAGON_REMOVED, null);
						break;
					}
				}
				if (!found) plugin.getConfigManager().sendMessage(player, Message.DRAGON_NOT_FOUND, null);
				return true;
			}
			else if (args[0].equalsIgnoreCase("locate")){
				if (!player.hasPermission("petdragon.command.locate")){
					plugin.getConfigManager().sendMessage(player, Message.NO_PERMISSION_COMMAND, null);
					return true;
				}
				Set<UUID> dragons = plugin.getFactory().getDragons(player);
				if (dragons.isEmpty()) plugin.getConfigManager().sendMessage(player, Message.NO_LOCATE, null);
				else {
					plugin.getConfigManager().sendMessage(player, Message.LOCATED_DRAGONS, ImmutableMap.of("amount", "" + dragons.size()));
					for (UUID dragon: dragons){
						Entity ent = Bukkit.getEntity(dragon);
						if (ent == null || !(ent instanceof EnderDragon)) continue;
						Location loc = ent.getLocation();
						plugin.getConfigManager().sendMessage(player, Message.LOCATE_ONE, ImmutableMap.of("x", "" +loc.getBlockX(), "y", "" + loc.getBlockY(), "z", "" + loc.getBlockZ(), "world", loc.getWorld().getName()));
					}
				}
				return true;
			}
		}
		plugin.getConfigManager().sendMessage(player, Message.COMMAND_USAGE, null);
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1){
			List<String> commands = Lists.newArrayList("spawn", "remove", "locate");
			if (sender.hasPermission("dragon.command.reload")){
				commands.add("reload");
			}
			return commands.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}



	
}
