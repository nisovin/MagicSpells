package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.Util;

public class HasItemLessThanCondition extends Condition {

	ItemStack item;
	int count;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] s = var.split(":");
			item = Util.predefinedItems.get(s[0]);
			if (item == null) return false;
			count = Integer.parseInt(s[1]);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		int c = 0;
		for (ItemStack i : player.getInventory().getContents()) {
			if (i != null && i.isSimilar(item)) {
				c += i.getAmount();
			}
		}
		return c < count;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		} else {
			return false;
		}
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
