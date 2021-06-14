package com.ericdebouwer.petdragon.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.ericdebouwer.petdragon.PetDragon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.collect.ImmutableMap;

public class ConfigManager {
	
	private final PetDragon plugin;
	private final String MESSAGES_PREFIX = "messages.";
	private boolean isValid;
	
	public boolean checkUpdates = true;
	
	public boolean rightClickRide = true;
	public boolean leftClickRide = true;
	public boolean deathAnimation = true;
	public boolean silent = false;
	public boolean doGriefing = true;
	public boolean flyThroughBlocks = true;
	public boolean damageEntities = true;
	public boolean interactEntities = true;
	public double speedMultiplier = 1.0;
	public double shootCooldown = 2.0;

	public float headDamage = 10.0F;
	public float wingDamage = 5.0F;

	public int maxDragons = Integer.MAX_VALUE;
	public boolean clickToRemove = false;
	private String pluginPrefix = "";
	public String dragonEggName = "";
	public boolean alwaysUseUpEgg = true;
	
	
	public ConfigManager(PetDragon plugin){
		this.plugin = plugin;
		plugin.saveDefaultConfig();
		
		this.isValid = this.checkConfig();
		
		if (isValid) this.loadConfig();
		
	}
	
	public boolean checkConfig(){
		boolean valid = this.validateSection("", "", true, true);
		if (!valid){
			if (handleUpdate()){
				if (this.validateSection("", "", true, false)) {
					Bukkit.getLogger().log(Level.INFO, plugin.logPrefix + "================================================================");
		        	Bukkit.getLogger().log(Level.INFO, plugin.logPrefix + "Automatically updated old/invalid configuration file!");
		        	Bukkit.getLogger().log(Level.INFO, plugin.logPrefix + "================================================================");
		        	return true;
				}
			}
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED +  plugin.logPrefix + "Automatic configuration update failed! See the header and the comments of the config.yml about fixing it");
		}
		return valid;
	}
	
	public void loadConfig(){
		pluginPrefix = plugin.getConfig().getString("plugin-prefix");
		
		checkUpdates = plugin.getConfig().getBoolean("check-for-updates");
		
		dragonEggName = ChatColor.translateAlternateColorCodes('§', plugin.getConfig().getString("dragon-egg-name", ""));
		alwaysUseUpEgg = plugin.getConfig().getBoolean("always-use-up-egg");

		rightClickRide = plugin.getConfig().getBoolean("right-click-to-ride");
		leftClickRide = plugin.getConfig().getBoolean("left-click-to-ride");
		deathAnimation = plugin.getConfig().getBoolean("do-death-animation");
		
		silent = plugin.getConfig().getBoolean("silent-dragons");
		
		doGriefing = plugin.getConfig().getBoolean("do-block-destruction");
		flyThroughBlocks = plugin.getConfig().getBoolean("fly-through-blocks");
		
		damageEntities = plugin.getConfig().getBoolean("do-entity-damage");
		interactEntities = plugin.getConfig().getBoolean("do-entity-interact");

		shootCooldown = plugin.getConfig().getDouble("shoot-cooldown-seconds");
		speedMultiplier = plugin.getConfig().getDouble("speed-multiplier");

		headDamage = (float) plugin.getConfig().getDouble("dragon-head-damage");
		wingDamage = (float) plugin.getConfig().getDouble("dragon-wing-damage");

		maxDragons = plugin.getConfig().getInt("max-dragons-per-player");
		
		clickToRemove = plugin.getConfig().getBoolean("click-to-remove");
	}
	
	public boolean isValid(){
		return this.isValid;
	}
	
	
	public String parseMessage(Message message, ImmutableMap<String, String> replacements){
		String msg = plugin.getConfig().getString(MESSAGES_PREFIX + message.getKey());
		if (msg == null || msg.isEmpty()) return null;
		String colorMsg = ChatColor.translateAlternateColorCodes('§', this.pluginPrefix + msg);
		if (replacements != null){
			for (Map.Entry<String, String> entry: replacements.entrySet()){
				colorMsg = colorMsg.replace( "{" + entry.getKey() + "}", entry.getValue());
			}
		}
		return colorMsg;	
	}
	
	public void sendMessage(CommandSender p, Message message,ImmutableMap<String, String> replacements){
		String msg = this.parseMessage(message, replacements);
		if (msg != null) p.sendMessage(msg);
	}
	
	private boolean validateSection(String templatePath, String realPath, boolean deep, boolean log){
		InputStream templateFile = getClass().getClassLoader().getResourceAsStream("config.yml");
        FileConfiguration templateConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(templateFile));
        
        ConfigurationSection realSection = plugin.getConfig().getConfigurationSection(realPath);
        ConfigurationSection templateSection = templateConfig.getConfigurationSection(templatePath);

        if (realSection == null || templateSection == null) return false;
        
 		for(String key: templateSection.getKeys(deep)){
 			if (!realSection.getKeys(deep).contains(key) || !isSameType(templateSection.get(key), realSection.get(key))){
 				if (log) Bukkit.getLogger().log(Level.WARNING, plugin.logPrefix + "Missing or invalid datatype key '" + key + "' and possibly others in config.yml");
 				return false;
 			}
 		}
 		return true;
	}

	private boolean isSameType(Object template, Object real){
		if (template instanceof Number && real instanceof Number){
			return true;
		}
		return template.getClass() == real.getClass();
	}
	
	public boolean handleUpdate(){
		File oldConfig = new File(plugin.getDataFolder(), "config.yml");
		try {
			ConfigUpdater.update(plugin, "config.yml", oldConfig, Collections.emptyList());
			plugin.reloadConfig();
		} catch (IOException e){
			return false;
		}
		return true;
	}
	
	public void reloadConfig(){
		plugin.reloadConfig();
		this.isValid = this.checkConfig();
		if (isValid) this.loadConfig();
	}
	

}
