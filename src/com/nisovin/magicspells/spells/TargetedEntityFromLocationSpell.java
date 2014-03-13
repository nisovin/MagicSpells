package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface TargetedEntityFromLocationSpell {
	
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power);
	
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power);

}
