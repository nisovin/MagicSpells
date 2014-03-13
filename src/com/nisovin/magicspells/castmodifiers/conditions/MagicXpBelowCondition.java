package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.MagicXpHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class MagicXpBelowCondition extends Condition {

	static MagicXpHandler handler;
	
	String[] school;
	int[] amount;
	
	@Override
	public boolean setVar(String var) {
		try {
			handler = MagicSpells.getMagicXpHandler();
			if (handler == null) return false;
			
			String[] vars = var.split(",");
			school = new String[vars.length];
			amount = new int[vars.length];
			for (int i = 0; i < vars.length; i++) {
				String[] split = vars[i].split(":");
				school[i] = split[0];
				amount[i] = Integer.parseInt(split[1]);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		for (int i = 0; i < school.length; i++) {
			if (handler.getXp(player, school[i]) >= amount[i]) {
				return false;
			}
		}
		return true;
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
