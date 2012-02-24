package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class StonevisionSpell extends BuffSpell {
	
	private int range;
	private int transparentType;
	private boolean unobfuscate;
	
	private HashMap<String,TransparentBlockSet> seers;

	public StonevisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);		
		
		range = config.getInt("spells." + spellName + ".range", 4);
		transparentType = config.getInt("spells." + spellName + ".transparent-type", Material.STONE.getId());
		unobfuscate = getConfigBoolean("unobfuscate", false);
		
		seers = new HashMap<String, TransparentBlockSet>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (seers.containsKey(player.getName())) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			seers.put(player.getName(), new TransparentBlockSet(player, range, transparentType));
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		if (seers.containsKey(p.getName())) {
			if (isExpired(p)) {
				turnOff(p);
			} else {
				boolean moved = seers.get(p.getName()).moveTransparency();
				if (moved) {
					addUse(p);
					chargeUseCost(p);
				}
			}
		}
	}
	
	@Override
	public void turnOff(Player player) {
		super.turnOff(player);
		if (seers.containsKey(player.getName())) {
			seers.get(player.getName()).removeTransparency();
			seers.remove(player.getName());
			sendMessage(player, strFade);
		}
	}

	@Override
	protected void turnOff() {
		for (TransparentBlockSet tbs : seers.values()) {
			tbs.removeTransparency();
		}
		seers.clear();
	}
	
	private class TransparentBlockSet {
		Player player;
		Block center;
		int range;
		int type;
		List<Block> blocks;
		Set<Chunk> chunks;
		
		public TransparentBlockSet(Player player, int range, int type) {
			this.player = player;
			this.center = player.getLocation().getBlock();
			this.range = range;
			this.type = type;
			
			blocks = new ArrayList<Block>();
			if (unobfuscate) {
				chunks = new HashSet<Chunk>();
			}
			
			setTransparency();
		}
		
		public void setTransparency() {
			List<Block> newBlocks = new ArrayList<Block>();
			
			// get blocks to set to transparent
			int px = center.getX();
			int py = center.getY();
			int pz = center.getZ();
			Block block;
			if (!unobfuscate) {
				// handle normally
				for (int x = px - range; x <= px + range; x++) {
					for (int y = py - range; y <= py + range; y++) {
						for (int z = pz - range; z <= pz + range; z++) {
							block = center.getWorld().getBlockAt(x,y,z);
							if (block.getType() == Material.getMaterial(type)) {
								player.sendBlockChange(block.getLocation(), Material.GLASS, (byte)0);
								newBlocks.add(block);
							}
						}
					}
				}
			} else {
				// unobfuscate everything
				int dx, dy, dz;
				for (int x = px - range - 1; x <= px + range + 1; x++) {
					for (int y = py - range - 1; y <= py + range + 1; y++) {
						for (int z = pz - range - 1; z <= pz + range + 1; z++) {
							dx = Math.abs(x - px);
							dy = Math.abs(y - py);
							dz = Math.abs(z - pz);
							block = center.getWorld().getBlockAt(x,y,z);
							if (block.getType() == Material.getMaterial(type) && dx <= range && dy <= range && dz <= range) {
								player.sendBlockChange(block.getLocation(), Material.GLASS, (byte)0);
								newBlocks.add(block);
							} else {
								player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
							}
							
							// save chunk for resending after spell ends
							Chunk c = block.getChunk();
							chunks.add(c);
						}
					}
				}
			}
			
			// remove old transparent blocks
			for (Block b : blocks) {
				if (!newBlocks.contains(b)) {
					player.sendBlockChange(b.getLocation(), b.getType(), b.getData());
				}
			}
			
			// update block set
			blocks = newBlocks;
		}
		
		public boolean moveTransparency() {
			if (player.isDead()) {
				player = Bukkit.getServer().getPlayer(player.getName());
			}
			Location loc = player.getLocation();
			if (!center.getWorld().equals(loc.getWorld()) || center.getX() != loc.getBlockX() || center.getY() != loc.getBlockY() || center.getZ() != loc.getBlockZ()) {
				// moved
				this.center = loc.getBlock();
				setTransparency();
				return true;
			}
			return false;
		}
		
		public void removeTransparency() {
			for (Block b : blocks) {
				player.sendBlockChange(b.getLocation(), b.getType(), b.getData());
			}
			blocks = null;
			
			// queue up chunks for resending
			if (unobfuscate) {
				MagicSpells.craftbukkit.queueChunksForUpdate(player, chunks);
			}
		}
	}

}
