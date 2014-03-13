package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class ChanceCondition extends Condition {

	int chance;
	Random random;
	
	@Override
	public boolean setVar(String var) {
		random = new Random();
		try {
			chance = Integer.parseInt(var);
			if (chance < 1 || chance > 100) {
				return false;
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		if (random.nextInt(100) < chance) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player);
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return check(player);
	}

}
