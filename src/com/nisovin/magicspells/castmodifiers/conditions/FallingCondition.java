package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class FallingCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(Player player) {
		return player.getFallDistance() > 0;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target.getFallDistance() > 0;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
