package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TargetedLocationSpell {
	
	public boolean castAtLocation(Player caster, Location target, float power);

	public boolean castAtLocation(Location target, float power);
	
}
