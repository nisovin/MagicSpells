package com.nisovin.magicspells.materials;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicUnknownMaterial extends MagicMaterial {
	int type;
	short data;
	
	public MagicUnknownMaterial(int type, short data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public Material getMaterial() {
		return Material.getMaterial(type);
	}
	
	@Override
	public MaterialData getMaterialData() {
		if (data == (byte)data) {
			return new MaterialData(type, (byte)data);
		} else {
			return new MaterialData(type);
		}
	}
	
	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		if (data < 16) {
			block.setTypeIdAndData(type, (byte)data, applyPhysics);
		}
	}
	
	@Override
	public FallingBlock spawnFallingBlock(Location location) {
		return location.getWorld().spawnFallingBlock(location, getMaterial(), getMaterialData().getData());
	}
	
	@Override
	public ItemStack toItemStack(int quantity) {
		return new ItemStack(type, quantity, data);
	}
	
	@Override
	public boolean equals(MaterialData matData) {
		return matData.getItemTypeId() == type && matData.getData() == data;
	}
	
	@Override
	public boolean equals(ItemStack itemStack) {
		return itemStack.getTypeId() == type && itemStack.getDurability() == data;
	}
	
	@Override
	public int hashCode() {
		return (type + ":" + data).hashCode();
	}
}