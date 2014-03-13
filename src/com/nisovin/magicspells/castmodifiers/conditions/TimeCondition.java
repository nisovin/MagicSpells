package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class TimeCondition extends Condition {

	int start;
	int end;

	@Override
	public boolean setVar(String var) {
		try {
			String[] vardata = var.split("-");
			start = Integer.parseInt(vardata[0]);
			end = Integer.parseInt(vardata[1]);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	@Override
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}
	
	@Override
	public boolean check(Player player, Location location) {
		long time = location.getWorld().getTime();
		if (end >= start) {
			if (start <= time && time <= end) {
				return true;
			} else {
				return false;
			}
		} else {
			if (time >= start || time <= end) {
				return true;
			} else {
				return false;
			}
		}
	}

}
