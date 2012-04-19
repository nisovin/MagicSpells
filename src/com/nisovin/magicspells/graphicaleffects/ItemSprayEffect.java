package com.nisovin.magicspells.graphicaleffects;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;

public class ItemSprayEffect extends GraphicalEffect {

	@Override
	public void playEffect(Location location, String param) {
		int type = 331;
		int num = 15;
		int duration = 6;
		float force = 1.0F;
		if (param != null) {
			String[] data = param.split(" ");
			if (data.length >= 1) {
				try {
					type = Integer.parseInt(data[0]);
				} catch (NumberFormatException e) {
				}
			}
			if (data.length >= 2) {
				try {
					num = Integer.parseInt(data[1]);
				} catch (NumberFormatException e) {
				}
			}
			if (data.length >= 3) {
				try {
					duration = Integer.parseInt(data[2]);
				} catch (NumberFormatException e) {
				}
			}
			if (data.length >= 4) {
				try {
					force = Float.parseFloat(data[3]);
				} catch (NumberFormatException e) {
				}
			}
		}
		
		// spawn items
		Random rand = new Random();
		Location loc = location.clone().add(0, 1, 0);
		final Item[] items = new Item[num];
		for (int i = 0; i < num; i++) {
			items[i] = loc.getWorld().dropItem(loc, new ItemStack(type, 0));
			items[i].setVelocity(new Vector((rand.nextDouble()-.5) * force, (rand.nextDouble()-.5) * force, (rand.nextDouble()-.5) * force));
			items[i].setPickupDelay(duration + 10);
		}
		
		// schedule item deletion
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				for (int i = 0; i < items.length; i++) {
					items[i].remove();
				}
			}
		}, duration);
	}
	
}
