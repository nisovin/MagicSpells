package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class CarpetSpell extends TargetedSpell implements TargetedLocationSpell {

	int radius;
	MagicMaterial block;
	int duration;
	boolean circle;
	
	int touchCheckInterval;
	boolean removeOnTouch;
	Subspell spellOnTouch;
	
	Map<Block, Player> blockMap;
	TouchChecker checker;
	
	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("radius", 1);
		block = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("block", "carpet:0"));
		duration = getConfigInt("duration", 0);
		circle = getConfigBoolean("circle", false);
		if (configKeyExists("spell-on-touch")) {
			touchCheckInterval = getConfigInt("touch-check-interval", 3);
			removeOnTouch = getConfigBoolean("remove-on-touch", true);
			spellOnTouch = new Subspell(getConfigString("spell-on-touch", ""));
			blockMap = new HashMap<Block, Player>();
			checker = new TouchChecker();
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (spellOnTouch != null && !spellOnTouch.process()) {
			MagicSpells.error("Invalid spell-on-touch for " + internalName);
		}
	}
	
	@Override
	public void turnOff() {
		if (blockMap != null) {
			blockMap.clear();
		}
		if (checker != null) {
			checker.stop();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (targetSelf) {
				loc = player.getLocation();
			} else {
				Block b = getTargetedBlock(player, power);
				if (b != null && b.getType() != Material.AIR) {
					loc = b.getLocation();
				}
			}
			if (loc == null) {
				return noTarget(player);
			}
			layCarpet(player, loc, power);
		}
		return PostCastAction.ALREADY_HANDLED;
	}
	
	void layCarpet(Player player, Location loc, float power) {
		if (!loc.getBlock().getType().isOccluding()) {
			int c = 0;
			while (!loc.getBlock().getRelative(0, -1, 0).getType().isOccluding() && c <= 2) {
				loc.subtract(0, 1, 0);
				c++;
			}
		} else {
			int c = 0;
			while (loc.getBlock().getType().isOccluding() && c <= 2) {
				loc.add(0, 1, 0);
				c++;
			}
		}
		int rad = Math.round(radius * power);
		Block b;
		int y = loc.getBlockY();
		final List<Block> blocks = new ArrayList<Block>();
		for (int x = loc.getBlockX() - rad; x <= loc.getBlockX() + rad; x++) {
			for (int z = loc.getBlockZ() - rad; z <= loc.getBlockZ() + rad; z++) {
				b = loc.getWorld().getBlockAt(x, y, z);
				if (circle) {
					if (loc.getBlock().getLocation().distanceSquared(b.getLocation()) > radius * radius) {
						continue;
					}
				}
				if (b.getType().isOccluding()) {
					b = b.getRelative(0, 1, 0);
				} else if (!b.getRelative(0, -1, 0).getType().isOccluding()) {
					b = b.getRelative(0, -1, 0);
				}
				if (b.getType() == Material.AIR && b.getRelative(0, -1, 0).getType().isSolid()) {
					block.setBlock(b, false);
					blocks.add(b);
					if (blockMap != null) {
						blockMap.put(b, player);
					}
					playSpellEffects(EffectPosition.TARGET, b.getLocation().add(0.5, 0, 0.5));
				}
			}
		}
		if (duration > 0 && blocks.size() > 0) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					for (Block b : blocks) {
						if (block.equals(b)) {
							b.setType(Material.AIR);
							if (blockMap != null) {
								blockMap.remove(b);
							}
						}
					}
				}
			}, duration);
		}
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
		}
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		if (targetSelf) {
			layCarpet(caster, caster.getLocation(), power);
		} else {
			layCarpet(caster, target, power);
		}
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		layCarpet(null, target, power);
		return true;
	}
	
	class TouchChecker implements Runnable {
		
		int taskId;
		
		public TouchChecker() {
			taskId = MagicSpells.scheduleRepeatingTask(this, touchCheckInterval, touchCheckInterval);
		}
		
		public void run() {
			if (blockMap.size() > 0) {
				for (Player player : Bukkit.getOnlinePlayers()) {
					Block b = player.getLocation().getBlock();
					Player caster = blockMap.get(b);
					if (caster != null && block.equals(b) && player != caster) {
						SpellTargetEvent event = new SpellTargetEvent(spellOnTouch.getSpell(), caster, player, 1f);
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							boolean casted = spellOnTouch.castAtEntity(caster, player, event.getPower());
							if (casted && removeOnTouch) {
								b.setType(Material.AIR);
								blockMap.remove(b);
							}
						}
					}
				}
			}
		}
		
		public void stop() {
			MagicSpells.cancelTask(taskId);
		}
		
	}

}
