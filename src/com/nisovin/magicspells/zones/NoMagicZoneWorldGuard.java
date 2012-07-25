package com.nisovin.magicspells.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class NoMagicZoneWorldGuard extends NoMagicZone {

	private String worldName;
	private String regionName;
	private ProtectedRegion region;
	
	@Override
	public void initialize(ConfigurationSection config) {
		this.worldName = config.getString("world", "");
		this.regionName = config.getString("region", "");
	}

	@Override
	public boolean inZone(Location location) {
		// check world
		if (!worldName.equals(location.getWorld().getName())) {
			return false;
		}
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
	}

}
