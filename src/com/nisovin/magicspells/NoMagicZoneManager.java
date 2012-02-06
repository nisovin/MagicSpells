package com.nisovin.magicspells;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.util.MagicConfig;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class NoMagicZoneManager {
	
	private HashSet<NoMagicZone> zones;

	public NoMagicZoneManager(MagicConfig config) {
		zones = new HashSet<NoMagicZone>();
		
		WorldGuardPlugin worldGuard = null;
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
			worldGuard = (WorldGuardPlugin)Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
		}
		
		Set<String> zoneNodes = config.getKeys("no-magic-zones");
		if (zoneNodes != null) {
			for (String node : zoneNodes) {
				ConfigurationSection zoneConfig = config.getSection("no-magic-zones." + node);
				String worldName = zoneConfig.getString("world", Bukkit.getServer().getWorlds().get(0).getName());
				String type = zoneConfig.getString("type", "cuboid");
				String message = zoneConfig.getString("message", "You are in a no-magic zone.");
				@SuppressWarnings("unchecked")
				List<String> allowedSpells = zoneConfig.getList("allowed-spells", null);
				if (type.equalsIgnoreCase("worldguard") && worldGuard != null) {
					World w = Bukkit.getServer().getWorld(worldName);
					if (w != null) {
						RegionManager rm = worldGuard.getRegionManager(w);
						if (rm != null) {
							ProtectedRegion region = rm.getRegion(zoneConfig.getString("region"));
							if (region != null) {
								zones.add(new NoMagicZone(worldName, region, message, allowedSpells));
							} else {
								Bukkit.getServer().getLogger().severe("MagicSpells: Invalid no-magic zone WorldGuard region: " + node);
							}
						} else {
							Bukkit.getServer().getLogger().severe("MagicSpells: Failed to get WorldGuard region manager for world: " + worldName);
						}
					} else {
						Bukkit.getServer().getLogger().severe("MagicSpells: Invalid no-magic zone world: " + worldName);						
					}
				} else if (type.equalsIgnoreCase("cuboid")) {
					try {
						String[] p1 = zoneConfig.getString("point1").split(",");
						String[] p2 = zoneConfig.getString("point2").split(",");
						Vector point1 = new Vector(Integer.parseInt(p1[0]), Integer.parseInt(p1[1]), Integer.parseInt(p1[2]));
						Vector point2 = new Vector(Integer.parseInt(p2[0]), Integer.parseInt(p2[1]), Integer.parseInt(p2[2]));
						zones.add(new NoMagicZone(worldName, point1, point2, message, allowedSpells));
					} catch (NumberFormatException e) {
						Bukkit.getServer().getLogger().severe("MagicSpells: Invalid no-magic zone defined cuboid: " + node);							
					} catch (ArrayIndexOutOfBoundsException e) {
						Bukkit.getServer().getLogger().severe("MagicSpells: Invalid no-magic zone defined cuboid: " + node);							
					}
				}
			}
		}
	}
	
	public boolean willFizzle(Player player, Spell spell) {
		return willFizzle(player.getLocation(), spell);
	}
	
	public boolean willFizzle(Location location, Spell spell) {
		for (NoMagicZone zone : zones) {
			if (zone.willFizzle(location, spell)) {
				return true;
			}
		}
		return false;
	}
	
	public void sendNoMagicMessage(Player player, Spell spell) {
		for (NoMagicZone zone : zones) {
			if (zone.willFizzle(player, spell)) {
				MagicSpells.sendMessage(player, zone.getMessage());
				return;
			}
		}
	}
	
	public int zoneCount() {
		return zones.size();
	}
	
}
