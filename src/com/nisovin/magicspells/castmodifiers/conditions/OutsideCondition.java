package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OutsideCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(Player player) {
		return player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getEyeLocation().getY();
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target.getWorld().getHighestBlockYAt(target.getLocation()) <= target.getEyeLocation().getY();
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return location.getWorld().getHighestBlockYAt(location) <= location.getY();
	}

}
