package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.castmodifiers.Condition;

public class HasItemCondition extends Condition {

	int id;
	short data;
	boolean checkData;
	String name;
	boolean checkName;
	
	@Override
	public boolean setVar(String var) {
		try {
			if (var.contains("|")) {
				String[] subvardata = var.split("\\|");
				var = subvardata[0];
				name = ChatColor.translateAlternateColorCodes('&', subvardata[1]).replace("__", " ");
				if (name.isEmpty()) name = null;
				checkName = true;
			} else {
				name = null;
				checkName = false;
			}
			if (var.contains(":")) {
				String[] vardata = var.split(":");
				id = Integer.parseInt(vardata[0]);
				if (vardata[1].equals("*")) {
					data = 0;
					checkData = false;
				} else {
					data = Short.parseShort(vardata[1]);
					checkData = true;
				}
			} else {
				id = Integer.parseInt(var);
				checkData = false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		if (checkData || checkName) {
			for (ItemStack item : player.getInventory().getContents()) {
				if (item != null) {
					String thisname = null;
					try {
						if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
							thisname = item.getItemMeta().getDisplayName();
						}
					} catch (Exception e) {}
					if (item.getTypeId() == id && (!checkData || item.getDurability() == data) && (!checkName || strEquals(thisname, name))) {
						return true;
					}
				}
			}
			return false;
		} else {
			return player.getInventory().contains(Material.getMaterial(id));
		}
	}
	
	private boolean strEquals(String s1, String s2) {
		if (s1 == s2) return true;
		if (s1 == null || s2 == null) return false;
		return s1.equals(s2);
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
