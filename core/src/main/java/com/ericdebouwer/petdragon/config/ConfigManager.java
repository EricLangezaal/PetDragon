package com.ericdebouwer.petdragon.config;

import com.ericdebouwer.petdragon.PetDragon;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class ConfigManager {

	@Getter(AccessLevel.NONE)
	private final PetDragon plugin;
	@Getter(AccessLevel.NONE)
	private final String MESSAGES_PREFIX = "messages.";
	@Getter(AccessLevel.NONE)
	private final String CONFIG_NAME = "config.yml";

	private boolean isValid;
	
	private boolean checkUpdates;
	private boolean collectMetrics;

	private boolean rightClickRide;
	private boolean leftClickRide;
	private boolean deathAnimation;
	private boolean silent;
	private boolean doGriefing;
	private boolean flyThroughBlocks;
	private boolean damageEntities;
	private boolean interactEntities;
	private double speedMultiplier;
	private double shootCooldown;

	private float headDamage;
	private float wingDamage;

	private int maxDragons;
	private boolean clickToRemove;
	private final String pluginPrefix = "§r[§5§lPetDragon§r] ";
	private String dragonEggName;
	private boolean alwaysUseUpEgg;
	private boolean countEggsInMaxDragons;
	
	public ConfigManager(PetDragon plugin) {
		this.plugin = plugin;

		plugin.saveDefaultConfig();

		this.isValid = this.checkAndUpdateConfig();
		if (isValid) this.loadConfig();
		
	}
	
	public boolean checkAndUpdateConfig() {
		File currentFile = new File(plugin.getDataFolder(), CONFIG_NAME);
		InputStream templateStream = getClass().getClassLoader().getResourceAsStream(CONFIG_NAME);
		if (templateStream == null) return false;
		FileConfiguration templateConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(templateStream));

		if (this.validateSection(templateConfig, plugin.getConfig(), true, true)) return true;
		try {
			ConfigUpdater.update(plugin, CONFIG_NAME, currentFile, Collections.emptyList());
		} catch (IOException e){
			return false;
		}
		plugin.reloadConfig();
		if (this.validateSection(templateConfig, plugin.getConfig(), true, false)){
			plugin.getLogger().info("Automatically updated old/invalid configuration file!");
			return true;
		}
		plugin.getLogger().warning("Automatic configuration update failed! See the header and the comments of the config.yml about fixing it");
		return false;
	}
	
	public void loadConfig() {
		checkUpdates = plugin.getConfig().getBoolean("check-for-updates", true);
		collectMetrics = plugin.getConfig().getBoolean("collect-bstats-metrics", true);
		
		dragonEggName = ChatColor.translateAlternateColorCodes('§', plugin.getConfig().getString("dragon-egg-name", ""));
		alwaysUseUpEgg = plugin.getConfig().getBoolean("always-use-up-egg");

		rightClickRide = plugin.getConfig().getBoolean("right-click-to-ride", true);
		leftClickRide = plugin.getConfig().getBoolean("left-click-to-ride", true);
		deathAnimation = plugin.getConfig().getBoolean("do-death-animation");
		
		silent = plugin.getConfig().getBoolean("silent-dragons");
		
		doGriefing = plugin.getConfig().getBoolean("do-block-destruction", true);
		flyThroughBlocks = plugin.getConfig().getBoolean("fly-through-blocks", true);
		
		damageEntities = plugin.getConfig().getBoolean("do-entity-damage", true);
		interactEntities = plugin.getConfig().getBoolean("do-entity-interact", true);

		shootCooldown = plugin.getConfig().getDouble("shoot-cooldown-seconds");
		speedMultiplier = plugin.getConfig().getDouble("speed-multiplier", 1);

		headDamage = (float) plugin.getConfig().getDouble("dragon-head-damage", 10);
		wingDamage = (float) plugin.getConfig().getDouble("dragon-wing-damage", 5);

		maxDragons = plugin.getConfig().getInt("max-dragons-per-player", Integer.MAX_VALUE);
		countEggsInMaxDragons = plugin.getConfig().getBoolean("eggs-count-towards-max");

		clickToRemove = plugin.getConfig().getBoolean("click-to-remove");
	}
	
	public String parseMessage(Message message, ImmutableMap<String, String> replacements) {
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
	
	public void sendMessage(CommandSender p, Message message,ImmutableMap<String, String> replacements) {
		String msg = this.parseMessage(message, replacements);
		if (msg != null) p.sendMessage(msg);
	}

	private boolean validateSection(FileConfiguration template, FileConfiguration actual, boolean deep, boolean log) {
		if (template == null || actual == null) return false;

		boolean valid = true;

		for(String key: template.getKeys(deep)){
			if (!actual.getKeys(deep).contains(key) || !isSameType(template.get(key), actual.get(key))){
				if (log) plugin.getLogger().log(Level.WARNING, "Missing or invalid datatype key '" + key + "' while parsing config.yml");
				valid = false;
			}
		}
		return valid;
	}

	private boolean isSameType(Object template, Object real) {
		if (template instanceof Number && real instanceof Number){
			return true;
		}
		return template.getClass() == real.getClass();
	}
	
	public void reloadConfig() {
		plugin.reloadConfig();
		this.isValid = this.checkAndUpdateConfig();
		if (isValid) this.loadConfig();
	}
	

}
