package com.nisovin.magicspells.spelleffects;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;

class ItemSprayEffect extends SpellEffect {

	@Override
	public void playEffect(Location location, String param) {
		int type = 331;
		short dura = 0;
		int num = 15;
		int duration = 6;
		float force = 1.0F;
		if (param != null) {
			String[] data = param.split(" ");
			if (data.length >= 1) {
				if (data[0].contains(":")) {
					try {
						String[] typeData = data[0].split(":");
						type = Integer.parseInt(typeData[0]);
						dura = Short.parseShort(typeData[1]);
					} catch (NumberFormatException e) {						
					}
				} else {
					try {
						type = Integer.parseInt(data[0]);
					} catch (NumberFormatException e) {
					}
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
			items[i] = loc.getWorld().dropItem(loc, new ItemStack(type, 0, dura));
			items[i].setVelocity(new Vector((rand.nextDouble()-.5) * force, (rand.nextDouble()-.5) * force, (rand.nextDouble()-.5) * force));
			items[i].setPickupDelay(duration * 2);
		}
		
		// schedule item deletion
		MagicSpells.scheduleDelayedTask(new Runnable() {
			public void run() {
				for (int i = 0; i < items.length; i++) {
					items[i].remove();
				}
			}
		}, duration);
	}
	
}
