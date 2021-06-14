package com.ericdebouwer.petdragon;

import java.util.logging.Level;

import com.ericdebouwer.petdragon.command.BaseCommand;
import com.ericdebouwer.petdragon.config.ConfigManager;
import com.ericdebouwer.petdragon.database.DragonRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;


public class PetDragon extends JavaPlugin  {
	
	//SUPPORTED:
	// 1.16.1, 1.16.2, 1.16.3, 1.16.4, 1.16.5 (tested)
	// 1.15, 1.15.1 (not tested), 1.15.2 (1.15-R1)
	// 1.14.4, 1.14.x(not tested)

	//TODO: database upsert fixen
	//TODO: MySQL testen
	//TODO: all NMS classes updaten
	//TODO: testen (veel)
	//DONE: double/int ook goed rekenen bij config validatie
	//DONE: Database voor dragon registry. Zowel MySQL als SQLite
	//DONE: rewritten NMS from scratch. faster & better
	//DONE: Possible to stop dragons from launching entities without cancelling damage. Even API event for it now
	//DONE: configurable head and wing damage amount. Improved config validator to allow all numbers
	
	public String logPrefix;
	private ConfigManager configManager;
	private DragonFactory dragonFactory;
	private EggManager eggManager;
	private DragonRegistry dragonRegistry;
	
	@Override
	public void onEnable(){
		this.logPrefix = "[" + this.getName() + "] ";

		this.dragonFactory = new DragonFactory(this);
		
		if (!dragonFactory.isCorrectVersion()){
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" +ChatColor.RED + logPrefix + "Unsupported minecraft version! Check the download page for supported versions!");
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" +ChatColor.RED + logPrefix +"Plugin will disable to prevent crashing!");
			return;
		}
		
		this.configManager = new ConfigManager(this);
		
		if (!this.configManager.isValid()){
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" +ChatColor.RED + logPrefix +"Invalid config.yml, plugin will disable to prevent crashing!");
 			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" + ChatColor.RED + logPrefix + "See the header of the config.yml about fixing the problem.");
			return;
		}
		getLogger().log(Level.INFO, "Configuration has been successfully loaded!");

		dragonRegistry = new DragonRegistry(this);

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () ->
			getLogger().info("If you really love this project, you could consider donating to help me keep this project alive! https://paypal.me/3ricL"));

		new BaseCommand(this);
		//new DragonCommand(this);
		eggManager = new EggManager(this);
		DragonEvents dragonEvents = new DragonEvents(this);
		getServer().getPluginManager().registerEvents(dragonEvents, this);
		
		if (configManager.checkUpdates) {
			new UpdateChecker(this)
				.onStart(() -> getLogger().info("Checking for updates..."))
				.onError(() -> getLogger().warning("Failed to check for updates!"))
				.onOldVersion((oldVersion, newVersion) -> {
					getLogger().info( "Update detected! You are using version " + oldVersion + ", but version " + newVersion + " is available!");
					getLogger().info("You can download the new version here -> https://www.spigotmc.org/resources/" +  UpdateChecker.RESOURCE_ID + "/updates");
				})
				.onNoUpdate(() -> getLogger().info("You are running the latest version."))
			.run();
		}
	}

	@Override
	public void onDisable(){
		if (dragonRegistry != null) dragonRegistry.close();
	}
	
	public ConfigManager getConfigManager(){
		return this.configManager;
	}
	
	public DragonFactory getFactory(){
		return this.dragonFactory;
	}
	
	public EggManager getEggManager(){
		return this.eggManager;
	}
	
	public DragonRegistry getDragonRegistry() {return this.dragonRegistry;}
	
}
