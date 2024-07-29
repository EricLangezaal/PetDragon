package com.ericdebouwer.petdragon;

import com.ericdebouwer.petdragon.command.BaseCommand;
import com.ericdebouwer.petdragon.config.ConfigManager;
import com.ericdebouwer.petdragon.listeners.DragonListener;
import com.ericdebouwer.petdragon.listeners.EggListener;
import com.ericdebouwer.petdragon.listeners.EntitiesLoadListener;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;


@Getter
public class PetDragon extends JavaPlugin {

	private ConfigManager configManager;
	private DragonFactory factory;
	private CustomItems customItems;

	@Override
	public void onLoad() {
		this.factory = new DragonFactory(this);
	}

	@Override
	public void onEnable() {
		String logPrefix = "[" + this.getName() + "] ";

		if (!factory.isCorrectVersion()){
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" + ChatColor.RED + logPrefix + "Unsupported minecraft version! Check the download page for supported versions!");
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" + ChatColor.RED + logPrefix + "Plugin will disable to prevent crashing!");
			return;
		}

		this.configManager = new ConfigManager(this);
		if (!this.configManager.isValid()){
			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" + ChatColor.RED + logPrefix + "Invalid config.yml, plugin will disable to prevent crashing!");
 			getServer().getConsoleSender().sendMessage(ChatColor.BOLD + "" + ChatColor.RED + logPrefix + "See the header of the config.yml about fixing the problem.");
			return;
		}
		getLogger().info("Configuration has been successfully loaded!");

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () ->
				getLogger().info("If you really love this project, consider getting the improved Premium version at https://spigotmc.org/resources/99995/"),
				20);


		new BaseCommand(this);

		customItems = new CustomItems(this);

		getServer().getPluginManager().registerEvents(new EggListener(this), this);
		getServer().getPluginManager().registerEvents(new DragonListener(this), this);
		new EntitiesLoadListener(this);

		if (configManager.isCollectMetrics()) {
			new Metrics(this, 13486);
		}
		
		if (configManager.isCheckUpdates()) {
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
}
