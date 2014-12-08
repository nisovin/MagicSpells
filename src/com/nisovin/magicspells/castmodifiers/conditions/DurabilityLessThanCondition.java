package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.castmodifiers.Condition;

public class DurabilityLessThanCondition extends Condition {

	private int durability;
	int slot;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] data = var.split(":");
			durability = Integer.parseInt(data[1]);
			if (data[0].equalsIgnoreCase("helm")) {
				slot = 0;
			} else if (data[0].equalsIgnoreCase("chestplate")) {
				slot = 1;
			} else if (data[0].equalsIgnoreCase("leggings")) {
				slot = 2;
			} else if (data[0].equalsIgnoreCase("boots")) {
				slot = 3;
			} else {
				slot = -1;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		ItemStack item = null;
		if (slot == -1) {
			item = player.getItemInHand();
		} else if (slot == 0) {
			item = player.getInventory().getHelmet();
		} else if (slot == 1) {
			item = player.getInventory().getChestplate();
		} else if (slot == 2) {
			item = player.getInventory().getLeggings();
		} else if (slot == 3) {
			item = player.getInventory().getBoots();
		}
		if (item != null) {
			int max = item.getType().getMaxDurability();
			if (max > 0) {
				return max - item.getDurability() < durability;
			}
		} else {
			return true;
		}
		return false;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		}
		return false;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}
	
}
