package com.nisovin.magicspells;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NoMagicZoneHandler {

	public boolean willFizzle(Player player, Spell spell);
	
	public boolean willFizzle(Location location, Spell spell);
	
	public void sendNoMagicMessage(Player player, Spell spell);
	
	public int zoneCount();
	
}
