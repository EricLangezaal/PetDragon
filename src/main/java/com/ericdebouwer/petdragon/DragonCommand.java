package com.ericdebouwer.petdragon;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class DragonCommand implements CommandExecutor, TabCompleter {
	
	PetDragon plugin;
	
	public DragonCommand(PetDragon plugin){
		this.plugin = plugin;
		plugin.getCommand("dragon").setExecutor(this);
		plugin.getCommand("dragon").setTabCompleter(this);
	}

	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
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
				PetEnderDragon dragon = plugin.createPetDragon(player.getLocation().add(0, 2, 0));
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
						ent.remove();
						found = true;
						plugin.getConfigManager().sendMessage(player, Message.DRAGON_REMOVED, null);
						break;
					}
				}
				if (!found) plugin.getConfigManager().sendMessage(player, Message.DRAGON_NOT_FOUND, null);
				return true;
			}
		}
		plugin.getConfigManager().sendMessage(player, Message.COMMAND_USAGE, null);
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1){
			List<String> commands = Arrays.asList("spawn", "remove");
			return commands.stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}



	
}
