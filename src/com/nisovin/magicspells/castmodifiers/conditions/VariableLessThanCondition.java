package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class VariableLessThanCondition extends Condition {

	String variable;
	double value = 0;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] s = var.split(":");
			variable = s[0];
			value = Double.parseDouble(s[1]);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return MagicSpells.getVariableManager().getValue(variable, player) < value;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		} else {
			return check(player);
		}
	}

	@Override
	public boolean check(Player player, Location location) {
		return check(player);
	}

}
