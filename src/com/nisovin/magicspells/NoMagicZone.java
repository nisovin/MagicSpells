package com.nisovin.magicspells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class NoMagicZone {

	private String worldName;
	private ProtectedRegion region = null;
	private Vector point1 = null;
	private Vector point2 = null;
	private String message;
	private List<String> allowedSpells;
	
	public NoMagicZone(String worldName, ProtectedRegion region, String message, List<String> allowedSpells) {
		this.worldName = worldName;
		this.region = region;
		this.message = message;
		this.allowedSpells = allowedSpells;
	}
	
	public NoMagicZone(String worldName, Vector v1, Vector v2, String message, List<String> allowedSpells) {
		this.worldName = worldName;
		int minx, miny, minz, maxx, maxy, maxz;
		if (v1.getX() < v2.getX()) {
			minx = v1.getBlockX();
			maxx = v2.getBlockX();
		} else {
			minx = v2.getBlockX();
			maxx = v1.getBlockX();
		}
		if (v1.getY() < v2.getY()) {
			miny = v1.getBlockY();
			maxy = v2.getBlockY();
		} else {
			miny = v2.getBlockY();
			maxy = v1.getBlockY();
		}
		if (v1.getZ() < v2.getZ()) {
			minz = v1.getBlockZ();
			maxz = v2.getBlockZ();
		} else {
			minz = v2.getBlockZ();
			maxz = v1.getBlockZ();
		}
		point1 = new Vector(minx, miny, minz);
		point2 = new Vector(maxx, maxy, maxz);
		this.message = message;
		this.allowedSpells = allowedSpells;
	}
	
	public boolean willFizzle(Player player, Spell spell) {
		return willFizzle(player.getLocation(), spell);
	}
	
	public boolean willFizzle(Location location, Spell spell) {
		if (allowedSpells != null && allowedSpells.contains(spell.getInternalName())) {
			return false;
		} else if (!worldName.equalsIgnoreCase(location.getWorld().getName())) {
			return false;
		} else if (region != null) {
			com.sk89q.worldedit.Vector v = new com.sk89q.worldedit.Vector(location.getX(), location.getY(), location.getZ());
			return region.contains(v);
		} else if (point1 != null && point2 != null) {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			if (point1.getBlockX() <= x && x <= point2.getBlockX() &&
					point1.getBlockY() <= y && y <= point2.getBlockY() &&
					point1.getBlockZ() <= z && z <= point2.getBlockZ()) {
				return true;
			} else {
				return false;
			}
		} else {
			Bukkit.getServer().getLogger().severe("MagicSpells: Invalid no-magic zone!");
			return false;
		}
	}
	
	public String getMessage() {
		return message;
	}
	
}
