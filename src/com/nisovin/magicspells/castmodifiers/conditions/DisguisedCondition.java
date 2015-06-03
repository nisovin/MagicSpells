package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell.Disguise;

public class DisguisedCondition extends Condition {

	String disguiseName;
	
	@Override
	public boolean setVar(String var) {
		disguiseName = var;
		return true;
	}

	@Override
	public boolean check(Player player) {
		Disguise disguise = DisguiseSpell.getDisguiseManager().getDisguise(player);
		if (disguise != null) {
			if (disguiseName == null || disguiseName.isEmpty()) {
				return true;
			} else if (disguise.getSpell().getInternalName().equals(disguiseName)) {
				return true;
			}
		}
		return false;
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
