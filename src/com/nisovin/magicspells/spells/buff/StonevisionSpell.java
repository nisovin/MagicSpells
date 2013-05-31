package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.Arrays;
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
	private int[] transparentTypes;
	private boolean unobfuscate;
	
	private HashMap<String,TransparentBlockSet> seers;

	public StonevisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);		
		
		range = getConfigInt("range", 4);
		unobfuscate = getConfigBoolean("unobfuscate", false);
		
		transparentType = getConfigInt("transparent-type", 0);
		List<Integer> types = getConfigIntList("transparent-types", null);
		if (types != null) {
			transparentTypes = new int[types.size()];
			for (int i = 0; i < types.size(); i++) {
				transparentTypes[i] = types.get(i);
			}
			Arrays.sort(transparentTypes);
		}
		if (transparentType == 0 && transparentTypes == null) {
			MagicSpells.error("Spell '" + internalName + "' does not define any transparent types");
		}
		
		seers = new HashMap<String, TransparentBlockSet>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (seers.containsKey(player.getName())) {
			turnOff(player);
			if (toggle) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		if (state == SpellCastState.NORMAL) {
			seers.put(player.getName(), new TransparentBlockSet(player, range, transparentType, transparentTypes));
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
		if (seers.containsKey(player.getName())) {
			super.turnOff(player);
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
		int[] types;
		List<Block> blocks;
		Set<Chunk> chunks;
		
		public TransparentBlockSet(Player player, int range, int type, int[] types) {
			this.player = player;
			this.center = player.getLocation().getBlock();
			this.range = range;
			this.type = type;
			this.types = types;
			
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
			int id;
			if (!unobfuscate) {
				// handle normally
				for (int x = px - range; x <= px + range; x++) {
					for (int y = py - range; y <= py + range; y++) {
						for (int z = pz - range; z <= pz + range; z++) {
							block = center.getWorld().getBlockAt(x,y,z);
							id = block.getTypeId();
							if (
									(type != 0 && id == type) ||
									(types != null && Arrays.binarySearch(types, id) >= 0)
								) {
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
							id = block.getTypeId();
							if ((
									(type != 0 && id == type) ||
									(types != null && Arrays.binarySearch(types, id) >= 0)
								) && 
								dx <= range && dy <= range && dz <= range) {
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
		}
	}

	@Override
	public boolean isActive(Player player) {
		return seers.containsKey(player.getName());
	}

}
