package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OffhandCondition extends Condition {

	int[] ids;
	short[] datas;
	boolean[] checkData;
	String[] names;
	boolean[] checkName;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] vardata = var.split(",");
			ids = new int[vardata.length];
			datas = new short[vardata.length];
			checkData = new boolean[vardata.length];
			names = new String[vardata.length];
			checkName = new boolean[vardata.length];
			for (int i = 0; i < vardata.length; i++) {
				if (vardata[i].contains("|")) {
					String[] subvardata = vardata[i].split("\\|");
					vardata[i] = subvardata[0];
					names[i] = ChatColor.translateAlternateColorCodes('&', subvardata[1]).replace("__", " ");
					if (names[i].isEmpty()) names[i] = null;
					checkName[i] = true;
				} else {
					names[i] = null;
					checkName[i] = false;
				}
				if (vardata[i].contains(":")) {
					String[] subvardata = vardata[i].split(":");
					ids[i] = Integer.parseInt(subvardata[0]);
					if (subvardata[1].equals("*")) {
						datas[i] = 0;
						checkData[i] = false;
					} else {
						datas[i] = Short.parseShort(subvardata[1]);
						checkData[i] = true;
					}
				} else {
					ids[i] = Integer.parseInt(vardata[i]);
					datas[i] = 0;
					checkData[i] = false;
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		ItemStack item = MagicSpells.getVolatileCodeHandler().getOffhand(player);
		return check(item);
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
	
	private boolean check(ItemStack item) {
		if (item == null) return false;
		int thisid = item == null ? 0 : item.getTypeId();
		short thisdata = item == null ? 0 : item.getDurability();
		String thisname = null;
		try {
			if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
				thisname = item.getItemMeta().getDisplayName();
			}
		} catch (Exception e) {}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == thisid && (!checkData[i] || datas[i] == thisdata) && (!checkName[i] || strEquals(names[i], thisname))) {
				return true;
			}
		}
		return false;
	}
	
	private boolean strEquals(String s1, String s2) {
		if (s1 == s2) return true;
		if (s1 == null || s2 == null) return false;
		return s1.equals(s2);
	}

}
