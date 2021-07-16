package com.ericdebouwer.petdragon.enderdragonNMS;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;

public interface PetEnderDragon {
	
	String DRAGON_ID = "CustomPetDragon";
	
	String OWNER_ID = "OwnerOfTheDragon";
	
	float MAX_HEALTH = 60.0F;
	
	default void setupDefault(){
		EnderDragon dragon = this.getEntity();
		dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(MAX_HEALTH);
		dragon.setHealth(MAX_HEALTH);
		dragon.getScoreboardTags().add(PetEnderDragon.DRAGON_ID);
	}
	
	void copyFrom(EnderDragon dragon);
	
	void spawn();

	EnderDragon getEntity();
	
}
