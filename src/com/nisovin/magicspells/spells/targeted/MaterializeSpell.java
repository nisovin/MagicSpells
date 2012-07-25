package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.ItemNameResolver.ItemTypeAndData;
import com.nisovin.magicspells.util.MagicConfig;

public class MaterializeSpell extends TargetedLocationSpell {

	private int type;
	private byte data;
	private int resetDelay;
	private boolean applyPhysics;
	private boolean checkPlugins;
	private String strFailed;
	
	public MaterializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String s = getConfigString("block-type", "1");
		ItemTypeAndData typeAndData = MagicSpells.getItemNameResolver().resolve(s);
		if (typeAndData != null) {
			type = typeAndData.id;
			data = (byte)typeAndData.data;
		}
		resetDelay = getConfigInt("reset-delay", 0);
		applyPhysics = getConfigBoolean("apply-physics", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		strFailed = getConfigString("str-failed", "");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> lastTwo = null;
			try {
				lastTwo = player.getLastTwoTargetBlocks(null, range);
			} catch (IllegalStateException e) {
				lastTwo = null;
			}
			if (lastTwo != null && lastTwo.size() == 2 && lastTwo.get(1).getType() != Material.AIR && lastTwo.get(0).getType() == Material.AIR) {
				boolean done = materialize(player, lastTwo.get(0), lastTwo.get(1));
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
		
		if (checkPlugins) {
			block.setTypeIdAndData(type, data, false);
			BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, against, player.getItemInHand(), player, true);
			Bukkit.getPluginManager().callEvent(event);
			blockState.update();
			if (event.isCancelled()) {
				return false;
			} else {
				block.setTypeIdAndData(type, data, applyPhysics);
			}
		} else {
			block.setTypeIdAndData(type, data, applyPhysics);
		}
		
		playSpellEffects(EffectPosition.CASTER, player);
		playSpellEffects(EffectPosition.TARGET, block.getLocation(), type + "");
		playSpellEffectsTrail(player.getLocation(), block.getLocation(), null);
		
		if (resetDelay > 0) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					if (block.getTypeId() == type && block.getData() == data) {
						block.setType(Material.AIR);
						playSpellEffects(EffectPosition.DELAYED, block.getLocation(), type + "");
					}
				}
			}, resetDelay);
		}
		
		return true;
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block b = target.getBlock();
		return materialize(caster, b, b.getRelative(BlockFace.DOWN));
	}

}
