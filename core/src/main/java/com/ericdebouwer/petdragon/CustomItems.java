package com.ericdebouwer.petdragon;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CustomItems {

	@Getter
	private final ItemStack egg;

	public CustomItems(PetDragon plugin){
		egg = new ItemStack(Material.DRAGON_EGG);
		ItemMeta meta = egg.getItemMeta();
		meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "egg"), PersistentDataType.SHORT, (short) 1);
		meta.setDisplayName(plugin.getConfigManager().getDragonEggName());
		egg.setItemMeta(meta);

	}
}
