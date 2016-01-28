package com.nisovin.magicspells.util;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;

public class TemporaryBlockSet implements Runnable {
	
	private Material original;
	private MagicMaterial replaceWith;
	private boolean callPlaceEvent;
	private Player player;
	
	private ArrayList<Block> blocks;
	
	private BlockSetRemovalCallback callback;
	
	public TemporaryBlockSet(Material original, MagicMaterial replaceWith, boolean callPlaceEvent, Player player) {
		this.original = original;
		this.replaceWith = replaceWith;
		this.callPlaceEvent = callPlaceEvent;
		this.player = player;
		
		this.blocks = new ArrayList<Block>();
	}
	
	public void add(Block block) {
		if (block.getType() == original) {
			if (callPlaceEvent) {
				BlockState state = block.getState();
				replaceWith.setBlock(block, false);
				BlockPlaceEvent event = new BlockPlaceEvent(block, state, block, player.getItemInHand(), player, true);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					BlockUtils.setTypeAndData(block, original, (byte)0, false);
				} else {
					blocks.add(block);
				}
			} else {
				replaceWith.setBlock(block);
				blocks.add(block);
			}
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
		player = null;
	}
	
	public interface BlockSetRemovalCallback {
		public void run(TemporaryBlockSet set);
	}

}