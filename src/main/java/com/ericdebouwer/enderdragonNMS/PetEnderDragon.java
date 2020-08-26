package com.ericdebouwer.enderdragonNMS;

import org.bukkit.entity.EnderDragon;

public interface PetEnderDragon {
	
	public static final String DRAGON_ID = "CustomPetDragon";
	
	public static final String OWNER_ID = "OwnerOfTheDragon";
	
	public final float MAX_HEALTH = 60.0F;
	
	default void copyFrom(EnderDragon dragon){
		this.setHealth((float) dragon.getHealth());
	}
	
	public void spawn();
	
	public void setHealth(float health);

	public EnderDragon getEntity();
	
}
