package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.EnumSet;
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
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicBlockMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class StonevisionSpell extends BuffSpell {
	
	private int range;
	private Set<Material> transparentTypes;
	private boolean unobfuscate;
	
	private MagicMaterial glass = new MagicBlockMaterial(new MaterialData(Material.GLASS));
	
	private HashMap<String, TransparentBlockSet> seers;

	public StonevisionSpell(MagicConfig config, String spellName) {
		super(config, spellName);		
		
		range = getConfigInt("range", 4);
		unobfuscate = getConfigBoolean("unobfuscate", false);
		
		transparentTypes = EnumSet.noneOf(Material.class);
		MagicMaterial type = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("transparent-type", "stone"));
		if (type != null) {
			transparentTypes.add(type.getMaterial());
		}
		List<String> types = getConfigStringList("transparent-types", null);
		if (types != null) {
			for (int i = 0; i < types.size(); i++) {
				type = MagicSpells.getItemNameResolver().resolveBlock(types.get(i));
				if (type != null) {
					transparentTypes.add(type.getMaterial());
				}
			}
		}
		if (transparentTypes.size() == 0) {
			MagicSpells.error("Spell '" + internalName + "' does not define any transparent types");
		}
		
		String s = getConfigString("glass", "");
		if (!s.isEmpty()) {
			glass = MagicSpells.getItemNameResolver().resolveBlock(s);
		}
		if (glass == null) {
			glass = new MagicBlockMaterial(new MaterialData(Material.GLASS));
		}
		
		seers = new HashMap<String, TransparentBlockSet>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		seers.put(player.getName(), new TransparentBlockSet(player, range, transparentTypes));
		return true;
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
	public void turnOffBuff(Player player) {
		TransparentBlockSet t = seers.remove(player.getName());
		if (t != null) {
			t.removeTransparency();
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
		Set<Material> types;
		List<Block> blocks;
		Set<Chunk> chunks;
		
		public TransparentBlockSet(Player player, int range, Set<Material> types) {
			this.player = player;
			this.center = player.getLocation().getBlock();
			this.range = range;
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
			if (!unobfuscate) {
				// handle normally
				for (int x = px - range; x <= px + range; x++) {
					for (int y = py - range; y <= py + range; y++) {
						for (int z = pz - range; z <= pz + range; z++) {
							block = center.getWorld().getBlockAt(x,y,z);
							if (types.contains(block.getType())) {
								Util.sendFakeBlockChange(player, block, glass);
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
							if (types.contains(block.getType()) && dx <= range && dy <= range && dz <= range) {
								Util.sendFakeBlockChange(player, block, glass);
								newBlocks.add(block);
							} else if (block.getType() != Material.AIR) {
								Util.restoreFakeBlockChange(player, block);
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
					Util.restoreFakeBlockChange(player, b);
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
				Util.restoreFakeBlockChange(player, b);
			}
			blocks = null;
		}
	}

	@Override
	public boolean isActive(Player player) {
		return seers.containsKey(player.getName());
	}

}
