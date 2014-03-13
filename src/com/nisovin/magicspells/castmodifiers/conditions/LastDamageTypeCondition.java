package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.castmodifiers.Condition;

public class LastDamageTypeCondition extends Condition {

	DamageCause cause;
	
	@Override
	public boolean setVar(String var) {
		for (DamageCause dc : DamageCause.values()) {
			if (dc.name().equalsIgnoreCase(var)) {
				cause = dc;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target.getLastDamageCause().getCause() == cause;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
