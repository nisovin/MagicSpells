package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.util.BlockUtils;

public class LookingAtBlockCondition extends Condition {

	MagicMaterial blockType;
	int dist = 4;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] varsplit = var.split(",");
			blockType = MagicSpells.getItemNameResolver().resolveBlock(varsplit[0]);
			if (blockType == null) return false;
			if (varsplit.length > 1) {
				dist = Integer.parseInt(varsplit[1]);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return check(player, player);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		Block block = BlockUtils.getTargetBlock(null, target, dist);
		return blockType.equals(block);
	}

	@Override
	public boolean check(Player player, Location location) {
		return check(player);
	}
	
}
