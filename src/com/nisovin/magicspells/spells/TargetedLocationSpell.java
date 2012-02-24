package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.MagicConfig;

public abstract class TargetedLocationSpell extends TargetedSpell {

	public TargetedLocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}
	
	public abstract boolean castAtLocation(Player caster, Location target, float power);

}
