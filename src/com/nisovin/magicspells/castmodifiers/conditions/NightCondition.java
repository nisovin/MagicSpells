package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class NightCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(Player player) {
		long time = player.getWorld().getTime();
		return (time > 13000 && time < 23000);
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player);
	}
	
	@Override
	public boolean check(Player player, Location location) {
		long time = location.getWorld().getTime();
		return (time > 13000 && time < 23000);
	}
	
}
