package com.nisovin.magicspells.caster;

import org.bukkit.Location;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellReagents;

public abstract class Caster {

	public abstract String getName();
	
	public abstract String getDisplayName();
	
	public abstract Location getLocation();
	
	public abstract boolean canCast(Spell spell);
	
	public abstract void sendMessage(String message);
	
	public abstract boolean hasPermission(String perm);
	
	public abstract boolean hasReagents(SpellReagents reagents);
	
	public abstract void removeReagents(SpellReagents reagents);
	
	public abstract void giveExp(int exp);
	
	public abstract boolean isValid();
	
}
