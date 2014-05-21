package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class TransmuteSpell extends TargetedSpell implements TargetedLocationSpell {

	List<MagicMaterial> blockTypes;
	MagicMaterial transmuteType;
	BlockFace[] checkDirs = new BlockFace[] { BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	boolean checkAll = false;
	
	public TransmuteSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> list = getConfigStringList("transmutable-types", null);
		blockTypes = new ArrayList<MagicMaterial>();
		if (list != null && list.size() > 0) {
			for (String s : list) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
				if (m != null) {
					blockTypes.add(m);
				}
			}
		} else {
			blockTypes.add(MagicSpells.getItemNameResolver().resolveBlock("iron_block"));
		}
		
		transmuteType = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("transmute-type", "gold_block"));
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block block = getTargetedBlock(player, power);
			if (block == null) {
				return noTarget(player);
			} else {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, block.getLocation(), power);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return noTarget(player);
				} else {
					block = event.getTargetLocation().getBlock();
				}
			}
			
			if (!canTransmute(block)) {
				return noTarget(player);
			}
			
			transmuteType.setBlock(block);
			playSpellEffects(player, block.getLocation().add(0.5, 0.5, 0.5));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block block = target.getBlock();
		if (canTransmute(block)) {
			transmuteType.setBlock(block);
			playSpellEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
			return true;
		} else {
			Vector v = target.getDirection();
			block = target.clone().add(v).getBlock();
			if (canTransmute(block)) {
				transmuteType.setBlock(block);
				playSpellEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		Block block = target.getBlock();
		if (canTransmute(block)) {
			transmuteType.setBlock(block);
			playSpellEffects(EffectPosition.TARGET, block.getLocation().add(0.5, 0.5, 0.5));
			return true;
		}
		return false;
	}
	
	private boolean canTransmute(Block block) {
		for (MagicMaterial m : blockTypes) {
			if (m.equals(block)) return true;
		}
		return false;
	}

}
