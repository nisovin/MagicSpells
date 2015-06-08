package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class MaterializeSpell extends TargetedSpell implements TargetedLocationSpell {

	private MagicMaterial material;
	private int resetDelay;
	private boolean falling;
	private boolean applyPhysics;
	private boolean checkPlugins;
	private boolean playBreakEffect;
	private String strFailed;
	
	public MaterializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String s = getConfigString("block-type", "1");
		material = MagicSpells.getItemNameResolver().resolveBlock(s);
		if (material == null) MagicSpells.error("Invalid block-type on materialize spell '" + internalName + "'");
		resetDelay = getConfigInt("reset-delay", 0);
		falling = getConfigBoolean("falling", false);
		applyPhysics = getConfigBoolean("apply-physics", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("play-break-effect", true);
		strFailed = getConfigString("str-failed", "");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> lastTwo = null;
			try {
				lastTwo = getLastTwoTargetedBlocks(player, power);
			} catch (IllegalStateException e) {
				lastTwo = null;
			}
			if (lastTwo != null && lastTwo.size() == 2 && lastTwo.get(1).getType() != Material.AIR && lastTwo.get(0).getType() == Material.AIR) {
				Block block = lastTwo.get(0);
				Block against = lastTwo.get(1);
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, block.getLocation(), power);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return noTarget(player, strFailed);
				} else {
					block = event.getTargetLocation().getBlock();
				}
				
				boolean done = materialize(player, block, against);
				if (!done) {
					return noTarget(player, strFailed);
				}
			} else {
				// fail no target
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean materialize(Player player, final Block block, Block against) {
		BlockState blockState = block.getState();
		
		if (checkPlugins && player != null) {
			material.setBlock(block, false);
			BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, against, player.getItemInHand(), player, true);
			Bukkit.getPluginManager().callEvent(event);
			blockState.update(true);
			if (event.isCancelled()) {
				return false;
			}
		}
		if (!falling) {
			material.setBlock(block, applyPhysics);
		} else {
			material.spawnFallingBlock(block.getLocation().add(0.5, 0.5, 0.5));
		}
		
		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
			playSpellEffects(EffectPosition.TARGET, block.getLocation());
			playSpellEffectsTrail(player.getLocation(), block.getLocation());
		}
		if (playBreakEffect) {
			block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
		}
		
		if (resetDelay > 0 && !falling) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					if (material.getMaterial().equals(block.getType())) {
						block.setType(Material.AIR);
						playSpellEffects(EffectPosition.DELAYED, block.getLocation());
						if (playBreakEffect) {
							block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
						}
					}
				}
			}, resetDelay);
		}
		
		return true;
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block block = target.getBlock();
		Block against = target.clone().add(target.getDirection()).getBlock();
		if (block.equals(against)) {
			against = block.getRelative(BlockFace.DOWN);
		}
		if (block.getType() == Material.AIR) {
			return materialize(caster, block, against);
		} else {
			Block block2 = block.getRelative(BlockFace.UP);
			if (block2.getType() == Material.AIR) {
				return materialize(null, block2, block);
			}
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Block block = target.getBlock();
		if (block.getType() == Material.AIR) {
			return materialize(null, block, block);
		} else {
			Block block2 = block.getRelative(BlockFace.UP);
			if (block2.getType() == Material.AIR) {
				return materialize(null, block2, block);
			}
		}
		return false;
	}

}
