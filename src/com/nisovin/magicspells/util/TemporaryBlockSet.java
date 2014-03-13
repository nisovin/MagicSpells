package com.nisovin.magicspells.util;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;

public class TemporaryBlockSet implements Runnable {
	
	private Material original;
	private MagicMaterial replaceWith;
	
	private ArrayList<Block> blocks;
	
	private BlockSetRemovalCallback callback;
	
	public TemporaryBlockSet(Material original, MagicMaterial replaceWith) {
		this.original = original;
		this.replaceWith = replaceWith;
		
		this.blocks = new ArrayList<Block>();
	}
	
	public void add(Block block) {
		if (block.getType() == original) {
			replaceWith.setBlock(block);
			blocks.add(block);
		}
	}
	
	public boolean contains(Block block) {
		return blocks.contains(block);
	}
	
	public void removeAfter(int seconds) {
		removeAfter(seconds, null);
	}
	
	public void removeAfter(int seconds, BlockSetRemovalCallback callback) {
		if (blocks.size() > 0) {
			this.callback = callback;
			MagicSpells.scheduleDelayedTask(this, seconds*20);
		}
	}
	
	public void run() {
		if (callback != null) {
			callback.run(this);
		}
		remove();
	}
	
	public void remove() {
		for (Block block : blocks) {
			if (replaceWith.equals(block)) {
				block.setType(original);
			}
		}
	}
	
	public interface BlockSetRemovalCallback {
		public void run(TemporaryBlockSet set);
	}

}