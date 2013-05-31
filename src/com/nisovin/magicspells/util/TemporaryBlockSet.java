package com.nisovin.magicspells.util;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.nisovin.magicspells.MagicSpells;

public class TemporaryBlockSet implements Runnable {
	
	private Material original;
	private int replaceWith;
	private byte replaceWithData;
	
	private ArrayList<Block> blocks;
	
	private BlockSetRemovalCallback callback;
	
	public TemporaryBlockSet(Material original, int replaceWith, byte replaceWithData) {
		this.original = original;
		this.replaceWith = replaceWith;
		this.replaceWithData = replaceWithData;
		
		this.blocks = new ArrayList<Block>();
	}
	
	public void add(Block block) {
		if (block.getType() == original) {
			block.setTypeIdAndData(replaceWith, replaceWithData, false);
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
			if (block.getTypeId() == replaceWith) {
				block.setType(original);
			}
		}
	}
	
	public interface BlockSetRemovalCallback {
		public void run(TemporaryBlockSet set);
	}

}