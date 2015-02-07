package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.util.MagicLocation;

public class TestForBlockCondition extends Condition {

	MagicLocation location;
	MagicMaterial blockType;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] varsplit = var.split("=");
			String[] locsplit = varsplit[0].split(",");
			location = new MagicLocation(locsplit[0], Integer.parseInt(locsplit[1]), Integer.parseInt(locsplit[2]), Integer.parseInt(locsplit[3]));
			blockType = MagicSpells.getItemNameResolver().resolveBlock(varsplit[1]);
			if (blockType == null) return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		Location loc = location.getLocation();
		if (loc == null) return false;
		if (blockType.equals(loc.getBlock())) return true;
		return false;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(null);
	}

	@Override
	public boolean check(Player player, Location location) {
		return check(null);
	}

}
