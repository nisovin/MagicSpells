package com.nisovin.magicspells.util;

import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.nisovin.magicspells.MagicSpells;

public class TemporaryBlockSet implements Runnable {
	
	private static HashSet<TemporaryBlockSet> blockSets = new HashSet<TemporaryBlockSet>();
	
	private Material original;
	private Material replaceWith;
	
	private ArrayList<Block> blocks;
	
	private BlockSetRemovalCallback callback;
	
	public TemporaryBlockSet(Material original, Material replaceWith) {
		this.original = original;
		this.replaceWith = replaceWith;
		
		this.blocks = new ArrayList<Block>();
		
		blockSets.add(this);
	}
	
	public void add(Block block) {
		if (block.getType() == original) {
			block.setType(replaceWith);
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
			MagicSpells.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, this, seconds*20);
		}
	}
	
	public void run() {
		if (callback != null) {
			callback.run(this);
		}
		for (Block block : blocks) {
			if (block.getType() == replaceWith) {
				block.setType(original);
			}
		}
		
		blockSets.remove(this);
	}
	
	public static boolean isTemporary(Block block) {
		if (blockSets == null || blockSets.size() == 0) {
			return false;
		} else {
			for (TemporaryBlockSet set : blockSets) {
				for (Block b : set.blocks) {	
					if (b.equals(block)) {
						return true;
					}
				}
			}
			return false;
		}
	}
	
	public interface BlockSetRemovalCallback {
		public void run(TemporaryBlockSet set);
	}

}