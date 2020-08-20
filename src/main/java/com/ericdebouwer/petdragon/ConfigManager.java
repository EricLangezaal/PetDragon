package com.ericdebouwer.petdragon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableMap;

public class ConfigManager {
	
	private PetDragon plugin;
	private final String MESSAGES_PREFIX = "messages.";
	private boolean isValid = true;
	public boolean rightClickRide = true;
	public boolean leftClickRide = true;
	public boolean doGriefing = true;
	public boolean damageEntities = true;
	private String pluginPrefix = "";
	
	
	public ConfigManager(PetDragon plugin){
		this.plugin = plugin;
		plugin.saveDefaultConfig();
		
		this.isValid = this.validateSection("", "", true);
		
		if (!isValid){
			if (!handleUpdate()){
				Bukkit.getLogger().log(Level.INFO,ChatColor.RED +  plugin.logPrefix + "Automatic configuration update failed! See the header of the config.yml about fixing it");
			}else {
				Bukkit.getLogger().log(Level.INFO, plugin.logPrefix + "================================================================");
	        	Bukkit.getLogger().log(Level.INFO, plugin.logPrefix + "Automatically updated old configuration file!");
	        	Bukkit.getLogger().log(Level.INFO, plugin.logPrefix + "================================================================");
	        	this.isValid = true;
			}
		}
		
		if (isValid){
			pluginPrefix = plugin.getConfig().getString("plugin-prefix");
			rightClickRide = plugin.getConfig().getBoolean("right-click-to-ride");
			leftClickRide = plugin.getConfig().getBoolean("left-click-to-ride");
			doGriefing = plugin.getConfig().getBoolean("do-block-destruction");
			damageEntities = plugin.getConfig().getBoolean("do-entity-damage");
		}
		
	}
	
	public boolean isValid(){
		return this.isValid;
	}
	
	
	public void sendMessage(Player p, Message message, ImmutableMap<String, String> replacements){
		String msg = plugin.getConfig().getString(MESSAGES_PREFIX + message.getKey());
		if (msg == null || msg.isEmpty()) return;
		String colorMsg = ChatColor.translateAlternateColorCodes('§', this.pluginPrefix + msg);
		if (replacements != null){
			for (Map.Entry<String, String> entry: replacements.entrySet()){
				colorMsg = colorMsg.replaceAll("\\{" + entry.getKey() + "\\}", entry.getValue());
			}
		}
		p.sendMessage(colorMsg);	
	}
	
	private boolean validateSection(String template_path, String real_path, boolean deep){
		InputStream templateFile = getClass().getClassLoader().getResourceAsStream("config.yml");
                FileConfiguration templateConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(templateFile));
        
                ConfigurationSection real_section = plugin.getConfig().getConfigurationSection(real_path);
                ConfigurationSection template_section = templateConfig.getConfigurationSection(template_path);
        
                if (real_section == null || template_section == null) return false;
        
 		for(String key: template_section.getKeys(deep)){
 			if (!real_section.getKeys(deep).contains(key) || template_section.get(key).getClass() != real_section.get(key).getClass()){
 				Bukkit.getLogger().log(Level.WARNING, plugin.logPrefix + "Missing or invalid datatype key '" + key + "' and possibly others in config.yml");
 				return false;
 			}
 		}
 		return true;
	}
	
	public boolean handleUpdate(){
		File oldConfig = new File(plugin.getDataFolder(), "config.yml");
		try {
			ConfigUpdater.update(plugin, "config.yml", oldConfig, Collections.emptyList());
		} catch (IOException e){
			return false;
		}
		return true;
	}
	

}
