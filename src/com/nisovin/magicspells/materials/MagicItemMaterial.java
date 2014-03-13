package com.nisovin.magicspells.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicItemMaterial extends MagicMaterial {
	Material type;
	MaterialData matData;
	short duraData;

	public MagicItemMaterial(Material type, short data) {
		this.type = type;
		this.duraData = data;
	}
	
	public MagicItemMaterial(MaterialData data) {
		type = data.getItemType();
		matData = data;
	}
	
	public short getDurability() {
		return duraData;
	}
	
	@Override
	public Material getMaterial() {
		return type;
	}
	
	@Override
	public MaterialData getMaterialData() {
		if (matData != null) {
			return matData;
		} else {
			return new MaterialData(type);
		}
	}

	@Override
	public ItemStack toItemStack(int quantity) {
		MaterialData matData = getMaterialData();
		if (matData != null) {
			return matData.toItemStack(quantity);
		}
		return new ItemStack(getMaterial(), quantity, getDurability());
	}
	
	@Override
	public boolean equals(ItemStack item) {
		if (matData != null) {
			ItemStack i = matData.toItemStack();
			return i.getType() == item.getType() && i.getDurability() == item.getDurability();
		} else {
			return type == item.getType() && duraData == item.getDurability();
		}
	}
}