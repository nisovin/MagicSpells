package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class LastLifeShorterThan extends Condition {
	
	int time;
	
	@Override
	public boolean setVar(String var) {
		try {
			time = Integer.parseInt(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return MagicSpells.getLifeLengthTracker().getLastLifeLength(player) < time;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		}
		return false;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

	
	
}
