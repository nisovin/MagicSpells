package com.nisovin.magicspells.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class BoundingBox {

	World world;
	double lowX, lowY, lowZ, highX, highY, highZ;
	
	public BoundingBox(Block center, double radius) {
		this(center.getLocation().add(0.5, 0, 0.5), radius, radius);
	}
	
	public BoundingBox(Location center, double radius) {
		this(center, radius, radius);
	}
	
	public BoundingBox(Block center, double horizRadius, double vertRadius) {
		this(center.getLocation().add(0.5, 0, 0.5), horizRadius, vertRadius);
	}
	
	public BoundingBox(Location center, double horizRadius, double vertRadius) {
		world = center.getWorld();
		lowX = center.getX() - horizRadius;
		lowY = center.getY() - vertRadius;
		lowZ = center.getZ() - horizRadius;
		highX = center.getX() + horizRadius;
		highY = center.getY() + vertRadius;
		highZ = center.getZ() + horizRadius;
	}
	
	public boolean contains(Location location) {
		if (!location.getWorld().equals(world)) {
			return false;
		}
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		return lowX <= x && x <= highX && lowY <= y && y <= highY && lowZ <= z && z <= highZ;
	}
	
	public boolean contains(Entity entity) {
		return contains(entity.getLocation());
	}
	
	public boolean contains(Block block) {
		return contains(block.getLocation().add(0.5, 0, 0.5));
	}
	
}
