package com.ericdebouwer.petdragon;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;

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
		
		if (args.length == 0){
			manager.sendMessage(player, Message.COMMAND_USAGE, null);
			return true;
		}
		
		
		if (args[0].equalsIgnoreCase("spawn")){
			if (!player.hasPermission("petdragon.command.spawn")){
				manager.sendMessage(player, Message.NO_PERMISSION_COMMAND, null);
				return true;
			}
			if (!player.hasPermission("petdragon.bypass.dragonlimit")){
				int dragonCount = plugin.getFactory().getDragons(player).size();
				if (dragonCount >= plugin.getConfigManager().maxDragons){
					manager.sendMessage(player, Message.DRAGON_LIMIT, ImmutableMap.of("amount", "" + dragonCount));
					return true;
				}
			}
			PetEnderDragon dragon = plugin.getFactory().create(player.getLocation().add(0, 2, 0), player.getUniqueId());
			dragon.spawn();
			manager.sendMessage(player, Message.DRAGON_SPAWNED, null);
			return true;
		}
		else if (args[0].equalsIgnoreCase("remove")){
			if (!player.hasPermission("petdragon.command.remove")){
				manager.sendMessage(player, Message.NO_PERMISSION_COMMAND, null);
				return true;
			}
			
			boolean found = false;
			
			if (args.length >= 2){
				try {
					Entity potentialDragon = Bukkit.getEntity(UUID.fromString(args[1]));
					if (plugin.getFactory().isPetDragon(potentialDragon)){
						plugin.getFactory().removeDragon((EnderDragon) potentialDragon);
						found = true;
					}
				} catch(IllegalArgumentException ignore){
				}
			}
			if (!found) {
				for (Entity ent: player.getNearbyEntities(3, 3, 3)){
					if (!plugin.getFactory().isPetDragon(ent)) continue;
					
					plugin.getFactory().removeDragon((EnderDragon) ent);
					found = true;
					break;
				}
			}
			if (found) manager.sendMessage(player, Message.DRAGON_REMOVED, null);
			else plugin.getConfigManager().sendMessage(player, Message.DRAGON_NOT_FOUND, null);
			return true;
		}
		else if (args[0].equalsIgnoreCase("locate")){
			if (!player.hasPermission("petdragon.command.locate")){
				manager.sendMessage(player, Message.NO_PERMISSION_COMMAND, null);
				return true;
			}
			Set<EnderDragon> dragons = plugin.getFactory().getDragons(player);
			if (dragons.isEmpty()) plugin.getConfigManager().sendMessage(player, Message.NO_LOCATE, null);
			
			else {
				manager.sendMessage(player, Message.LOCATED_DRAGONS, ImmutableMap.of("amount", "" + dragons.size()));
				for (EnderDragon dragon: dragons){
					Location loc = dragon.getLocation();
					String text = manager.parseMessage(Message.LOCATE_ONE, ImmutableMap.of("x", "" +loc.getBlockX(), "y", "" + loc.getBlockY(), "z", "" + loc.getBlockZ(), "world", loc.getWorld().getName()));
					if (manager.clickToRemove) {
						TextComponent message = new TextComponent(text);
						message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/dragon remove "+ dragon.getUniqueId()));
						message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(manager.parseMessage(Message.LOCATED_HOVER, null))));
						player.spigot().sendMessage(message);
					}else {
						player.sendMessage(text);
					}
				}
			}
			return true;
		}
		
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
