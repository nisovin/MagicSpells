package com.nisovin.magicspells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class NoMagicZone {

	public static enum Type {
		CUBOID, WORLDGUARD, RESIDENCE
	}
	
	private String worldName;
	private String regionName = null;
	private ProtectedRegion region = null;
	private Vector point1 = null;
	private Vector point2 = null;
	private String message;
	private List<String> allowedSpells;
	private Type regionType = Type.CUBOID;
	
	public NoMagicZone(String worldName, String regionName, String message, List<String> allowedSpells, Type regionType) {
		this.worldName = worldName;
		this.regionName = regionName;
		this.message = message;
		this.allowedSpells = allowedSpells;
		this.regionType = regionType;
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
		} else if (regionName != null) {
			if (regionType == Type.WORLDGUARD) {
				// get region, if necessary
				if (region == null) {
					WorldGuardPlugin worldGuard = null;
					if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
						worldGuard = (WorldGuardPlugin)Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
					}
					if (worldGuard != null) {
						World w = Bukkit.getServer().getWorld(worldName);
						if (w != null) {
							RegionManager rm = worldGuard.getRegionManager(w);
							if (rm != null) {
								region = rm.getRegion(regionName);
							}
						}
					}
				}
				// check if contains
				if (region != null) {
					com.sk89q.worldedit.Vector v = new com.sk89q.worldedit.Vector(location.getX(), location.getY(), location.getZ());
					return region.contains(v);
				} else {
					MagicSpells.error("Failed to access WorldGuard region '" + regionName + "'");
					return false;
				}
			} else if (regionType == Type.RESIDENCE) {
				if (Bukkit.getServer().getPluginManager().isPluginEnabled("Residence")) {
					ClaimedResidence res = Residence.getResidenceManager().getByLoc(location);
					if (res != null) {
						if (res.getName().equalsIgnoreCase(regionName)) {
							return true;
						} else {
							return false;
						}
					} else {
						return false;
					}
				} else {
					MagicSpells.error("Failed to access Residence region '" + regionName + "'");
					return false;
				}
			} else {
				MagicSpells.error("Invalid region '" + regionName + "', it is not a recognized type");
				return false;
			}
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
