package com.nisovin.magicspells.zones;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class NoMagicZoneCuboid extends NoMagicZone {
	
	private String worldName;
	private int minx, miny, minz, maxx, maxy, maxz;
	
	@Override
	public void initialize(ConfigurationSection config) {
		this.worldName = config.getString("world", "");
		
		String[] p1 = config.getString("point1", "0,0,0").replace(" ", "").split(",");
		String[] p2 = config.getString("point2", "0,0,0").replace(" ", "").split(",");
		int x1 = Integer.parseInt(p1[0]);
		int y1 = Integer.parseInt(p1[1]);
		int z1 = Integer.parseInt(p1[2]);
		int x2 = Integer.parseInt(p2[0]);
		int y2 = Integer.parseInt(p2[1]);
		int z2 = Integer.parseInt(p2[2]);
		
		if (x1 < x2) {
			minx = x1;
			maxx = x2;
		} else {
			minx = x2;
			maxx = x1;
		}
		if (y1 < y2) {
			miny = y1;
			maxy = y2;
		} else {
			miny = y2;
			maxy = y1;
		}
		if (z1 < z2) {
			minz = z1;
			maxz = z2;
		} else {
			minz = z2;
			maxz = z1;
		}
	}

	@Override
	public boolean inZone(Location location) {
		if (!worldName.equalsIgnoreCase(location.getWorld().getName())) {
			return false;
		} else {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			if (minx <= x && x <= maxx &&
					miny <= y && y <= maxy &&
					minz <= z && z <= maxz) {
				return true;
			} else {
				return false;
			}
		}
	}
	
}
