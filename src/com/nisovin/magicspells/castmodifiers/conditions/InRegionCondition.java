package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class InRegionCondition extends Condition {

	WorldGuardPlugin worldGuard;
	String worldName;
	String regionName;
	ProtectedRegion region;
	
	@Override
	public boolean setVar(String var) {
		if (var == null) return false;
		
		worldGuard = (WorldGuardPlugin)Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (worldGuard == null || !worldGuard.isEnabled()) return false;
		
		String[] split = var.split(":");
		if (split.length == 2) {
			worldName = split[0];
			regionName = split[1];
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		return check(player, player.getLocation());
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return check(player, target.getLocation());
	}

	@Override
	public boolean check(Player player, Location location) {
		if (region == null) {
			World world = Bukkit.getWorld(worldName);
			if (world == null) return false;
			if (!world.equals(location.getWorld())) return false;
			RegionManager regionManager = worldGuard.getRegionManager(world);
			if (regionManager == null) return false;
			region = regionManager.getRegion(regionName);
		}
		if (region == null) return false;
		return region.contains(new Vector(location.getX(), location.getY(), location.getZ()));
	}

}
