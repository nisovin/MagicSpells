package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnBlockCondition extends Condition {

	int[] ids;

	@Override
	public boolean setVar(String var) {
		try {
			String[] vardata = var.split(",");
			ids = new int[vardata.length];
			for (int i = 0; i < vardata.length; i++) {
				ids[i] = Integer.parseInt(vardata[i]);
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
		int blockId = target.getLocation().subtract(0, 1, 0).getBlock().getTypeId();
		for (int id : ids) {
			if (blockId == id) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
