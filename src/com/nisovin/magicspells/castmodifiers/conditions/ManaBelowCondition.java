package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.mana.ManaHandler;

public class ManaBelowCondition extends Condition {

	ManaHandler mana;
	int num;
	boolean percent;
	
	@Override
	public boolean setVar(String var) {
		mana = MagicSpells.getManaHandler();
		if (mana == null) return false;
		
		if (var.endsWith("%")) {
			percent = true;
			var = var.replace("%", "");
		} else {
			percent = false;
		}
		
		try {
			num = Integer.parseInt(var);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		if (percent) {
			int max = mana.getMaxMana(player);
			int amt = (int)(max * (num / 100F));
			return !mana.hasMana(player, amt);
		} else {
			return !mana.hasMana(player, num);
		}
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		} else {
			return false;
		}
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}
}
