package com.ericdebouwer.petdragon;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import com.ericdebouwer.enderdragonNMS.PetEnderDragon;

public class PetDragon extends JavaPlugin  {

	//TODO: 
	//player lower on dragon
	//remove 'free the end' message (achievement is already gone)
	
	//SUPPORTED:
	// 1.15, 1.15.1 (not tested), 1.15.2 (1.15-R1)
	// 1.16.1, 1.16.2
	//1.14.4, 1.14.x(not tested)
	
	public String logPrefix;
	private ConfigManager configManager;
	private Class<?> dragonClass;
	
	@Override
	public void onEnable(){
		this.logPrefix = "[" + this.getName() + "] ";
		
		if (!this.setUpDragonClass()){
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
		getServer().getConsoleSender().sendMessage(logPrefix +"Configuration has been successfully loaded!");
		
		new DragonCommand(this);
		DragonEvents dragonEvents = new DragonEvents(this);
		getServer().getPluginManager().registerEvents(dragonEvents, this);
	}
	
	public ConfigManager getConfigManager(){
		return this.configManager;
	}
	
	public boolean setUpDragonClass(){
		String packageName = this.getServer().getClass().getPackage().getName();
        	String version = packageName.substring(packageName.lastIndexOf('.') + 1);

        	try {
            		final Class<?> clazz = Class.forName("com.ericdebouwer.enderdragonNMS.PetEnderDragon_" + version);
            		if (PetEnderDragon.class.isAssignableFrom(clazz)) { 
            			this.dragonClass = clazz;
            			return true;
            		}
        	} catch (final Exception e) {
            		e.printStackTrace();
       		}
        	return false;
	}
	
	public PetEnderDragon createPetDragon(Location loc){
		try {
			return (PetEnderDragon) this.dragonClass.getConstructor(Location.class, PetDragon.class).newInstance(loc, this);
		} catch (NoSuchMethodException | ClassCastException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e){
			e.printStackTrace();
		}
		return null;
	}
	
	
	
}
