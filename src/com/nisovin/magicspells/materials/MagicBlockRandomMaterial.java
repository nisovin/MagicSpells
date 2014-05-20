package com.nisovin.magicspells.materials;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;

public class MagicBlockRandomMaterial extends MagicBlockMaterial {
	MaterialData[] datas;
	
	public MagicBlockRandomMaterial(MaterialData[] datas) {
		super(null);
		this.datas = datas;
	}
	
	@Override
	public Material getMaterial() {
		return datas[ItemNameResolver.rand.nextInt(datas.length)].getItemType();
	}
	
	@Override
	public MaterialData getMaterialData() {
		return datas[ItemNameResolver.rand.nextInt(datas.length)];
	}
	
	@Override
	public void setBlock(Block block, boolean applyPhysics) {
		MaterialData data = datas[ItemNameResolver.rand.nextInt(datas.length)];
		BlockState state = block.getState();
		state.setType(data.getItemType());
		state.setData(data);
		state.update(true, applyPhysics);
	}
	
	@Override
	public boolean equals(MaterialData data) {
		for (MaterialData d : datas) {
			if (d.equals(data)) return true;
		}
		return false;
	}
}