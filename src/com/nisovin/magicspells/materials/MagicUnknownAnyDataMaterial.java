package com.nisovin.magicspells.materials;

import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicUnknownAnyDataMaterial extends MagicUnknownMaterial {

	public MagicUnknownAnyDataMaterial(int type) {
		super(type, (short)0);
	}
	
	@Override
	public boolean equals(MaterialData matData) {
		return matData.getItemTypeId() == type;
	}
	
	@Override
	public boolean equals(ItemStack itemStack) {
		return itemStack.getTypeId() == type;
	}
	
	@Override
	public int hashCode() {
		return (type + ":*").hashCode();
	}

}
