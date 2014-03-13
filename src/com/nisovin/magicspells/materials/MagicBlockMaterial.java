package com.nisovin.magicspells.materials;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class MagicBlockMaterial extends MagicMaterial {
	MaterialData data;
	
	public MagicBlockMaterial(MaterialData data) {
		this.data = data;
	}
	
	@Override
	public Material getMaterial() {
		return data.getItemType();
	}
	
	@Override
	public MaterialData getMaterialData() {
		return data;
	}
	
	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		BlockState state = block.getState();
		state.setType(getMaterial());
		state.setData(getMaterialData());
		state.update(true, applyPhysics);
	}
	
	@Override
	public FallingBlock spawnFallingBlock(Location location) {
		return location.getWorld().spawnFallingBlock(location, getMaterial(), getMaterialData().getData());
	}

	@Override
	public ItemStack toItemStack(int quantity) {
		return getMaterialData().toItemStack(quantity);
	}
}