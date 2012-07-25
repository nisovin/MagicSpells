package com.nisovin.magicspells.util;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import com.nisovin.magicspells.MagicSpells;

public class ExperienceBarManager {

	private HashMap<Player, Object> locks = new HashMap<Player, Object>();
	
	public void update(Player player, int level, float percent) {
		update(player, level, percent, null);
	}
	
	public void update(Player player, int level, float percent, Object object) {
		Object lock = locks.get(player);
		if (lock == null || (object != null && object.equals(lock))) {
			if (player.getOpenInventory().getType() != InventoryType.ENCHANTING) {
				MagicSpells.getVolatileCodeHandler().setExperienceBar(player, level, percent);
			}
		}
	}
	
	public void lock(Player player, Object object) {
		Object lock = locks.get(player);
		if (lock == null || lock.equals(object)) {
			locks.put(player, object);
		}
	}
	
	public void unlock(Player player, Object object) {
		Object lock = locks.get(player);
		if (lock != null && lock.equals(object)) {
			locks.remove(player);
		}
	}
	
}
