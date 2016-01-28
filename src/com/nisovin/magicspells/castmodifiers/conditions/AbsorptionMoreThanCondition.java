package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class AbsorptionMoreThanCondition extends Condition {

	float health = 0;

	@Override
	public boolean setVar(String var) {
		try {
			health = Float.parseFloat(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	@Override
	public boolean check(Player player) {
		return check(player, player);
	}
	
	@Override
	public boolean check(Player player, LivingEntity target) {
		return MagicSpells.getVolatileCodeHandler().getAbsorptionHearts(target) > health;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}
}
