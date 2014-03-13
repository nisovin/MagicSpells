package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class PitchAboveCondition extends Condition {

	float pitch;
	
	@Override
	public boolean setVar(String var) {
		try {
			pitch = Float.parseFloat(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return player.getLocation().getPitch() >= pitch;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target.getLocation().getPitch() >= pitch;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
