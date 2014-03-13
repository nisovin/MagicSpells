package com.nisovin.magicspells.util;

import org.bukkit.Location;
import org.bukkit.World;

import com.nisovin.magicspells.MagicSpells;

public class MagicLocation {

	private String world;
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;
	
	public MagicLocation(String world, int x, int y, int z) {
		this(world, x, y, z, 0, 0);
	}
	
	public MagicLocation(Location l) {
		this(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
	}
	
	public MagicLocation(String world, double x, double y, double z, float yaw, float pitch) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public Location getLocation() {
		World realWorld = MagicSpells.plugin.getServer().getWorld(world);
		if (realWorld == null) {
			return null;
		} else {
			return new Location(realWorld, x, y, z, yaw, pitch);
		}
	}
	
	public String getWorld() {
		return this.world;
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public double getZ() {
		return this.z;
	}
	
	public float getYaw() {
		return this.yaw;
	}
	
	public float getPitch() {
		return this.pitch;
	}
	
	@Override
	public int hashCode() {
		int hash = 3;
	    hash = 19 * hash + this.world.hashCode();
	    hash = 19 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
	    hash = 19 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
	    hash = 19 * hash + (int) (Double.doubleToLongBits(this.z) ^ (Double.doubleToLongBits(this.z) >>> 32));
	    hash = 19 * hash + Float.floatToIntBits(this.pitch);
	    hash = 19 * hash + Float.floatToIntBits(this.yaw);
	    return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MagicLocation) {
			MagicLocation loc = (MagicLocation)o;
			if (loc.world.equals(this.world) && loc.x == this.x && loc.y == this.y && loc.z == this.z && loc.yaw == this.yaw && loc.pitch == this.pitch) {
				return true;
			} else {
				return false;
			}
		} else if (o instanceof Location) {
			Location loc = (Location)o;
			if (loc.getWorld().getName().equals(this.world) && loc.getX() == this.x && loc.getY() == this.y && loc.getZ() == this.z && loc.getYaw() == this.yaw && loc.getPitch() == this.pitch){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
}
