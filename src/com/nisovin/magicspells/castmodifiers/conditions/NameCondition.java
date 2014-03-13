package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class NameCondition extends Condition {

	String name;
	
	@Override
	public boolean setVar(String var) {
		if (var == null || var.isEmpty()) return false;
		name = var;
		return true;
	}

	@Override
	public boolean check(Player player) {
		if (player.getName().equalsIgnoreCase(name) || ChatColor.stripColor(player.getDisplayName()).equalsIgnoreCase(name)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		} else {
			String n = target.getCustomName();
			if (n != null && !n.isEmpty()) {
				return name.equalsIgnoreCase(ChatColor.stripColor(n));
			}
			return false;
		}
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
