package com.nisovin.magicspells.materials;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

public class MagicBlockRandomMaterial extends MagicBlockMaterial {
	MagicMaterial[] materials;
	
	public MagicBlockRandomMaterial(MagicMaterial[] materials) {
		super(null);
		this.materials = materials;
	}
	
	@Override
	public Material getMaterial() {
		return materials[ItemNameResolver.rand.nextInt(materials.length)].getMaterial();
	}
	
	@Override
	public MaterialData getMaterialData() {
		return materials[ItemNameResolver.rand.nextInt(materials.length)].getMaterialData();
	}
	
	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		MagicMaterial material = materials[ItemNameResolver.rand.nextInt(materials.length)];
		BlockState state = block.getState();
		MaterialData data = material.getMaterialData();
		state.setType(data.getItemType());
		state.setData(data);
		state.update(true, applyPhysics);
	}
	
	@Override
	public boolean equals(MaterialData data) {
		for (MagicMaterial m : materials) {
			if (m.equals(data)) return true;
		}
		return false;
	}
}