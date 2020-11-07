package com.ericdebouwer.enderdragonNMS;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;

public interface PetEnderDragon {
	
	public static final String DRAGON_ID = "CustomPetDragon";
	
	public static final String OWNER_ID = "OwnerOfTheDragon";
	
	public final float MAX_HEALTH = 60.0F;
	
	default void setupDefault(){
		EnderDragon dragon = this.getEntity();
		dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(MAX_HEALTH);
		dragon.setHealth(MAX_HEALTH);
		dragon.getScoreboardTags().add(PetEnderDragon.DRAGON_ID);
	}
	
	void copyFrom(EnderDragon dragon);
	
	public void spawn();
	
	public void setHealth(float health);

	public EnderDragon getEntity();
	
}
