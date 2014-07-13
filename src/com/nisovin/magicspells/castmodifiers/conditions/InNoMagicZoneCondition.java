package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

public class InNoMagicZoneCondition extends Condition {

	String zone;
	
	@Override
	public boolean setVar(String var) {
		if (var == null) return false;
		zone = var;
		return true;
	}

	@Override
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}

	@Override
	public boolean check(Player player, Location location) {
		NoMagicZoneManager man = MagicSpells.getNoMagicZoneManager();
		if (man == null) return false;
		return man.inZone(location, zone);
	}

}
