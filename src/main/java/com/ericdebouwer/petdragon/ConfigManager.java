package com.ericdebouwer.petdragon;

import java.io.InputStream;
import java.io.InputStreamReader;
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
	private String pluginPrefix = "";
	
	
	public ConfigManager(PetDragon plugin){
		this.plugin = plugin;
		plugin.saveDefaultConfig();
		
		this.isValid = this.validateSection("", "", true);
		
		if (isValid){
			pluginPrefix = plugin.getConfig().getString("plugin-prefix");
			rightClickRide = plugin.getConfig().getBoolean("right-click-to-ride");
			leftClickRide = plugin.getConfig().getBoolean("left-click-to-ride");
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
	
	

}
