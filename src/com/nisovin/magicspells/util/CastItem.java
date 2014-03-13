package com.nisovin.magicspells.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;

public class CastItem {
	private int type = 0;
	private short data = 0;
	private String name = "";
	private int[][] enchants = null;
	
	public CastItem() {
	}
	
	public CastItem(int type) {
		this.type = type;
	}
	
	public CastItem(int type, short data) {
		this.type = type;
		if (MagicSpells.ignoreCastItemDurability(type)) {
			this.data = 0;
		} else {
			this.data = data;
		}
	}
	
	public CastItem(ItemStack item) {
		if (item == null) {
			this.type = 0;
			this.data = 0;
		} else {
			this.type = item.getTypeId();
			if (this.type == 0 || MagicSpells.ignoreCastItemDurability(type)) {
				this.data = 0;
			} else {
				this.data = item.getDurability();
			}
			if (this.type > 0 && !MagicSpells.ignoreCastItemNames() && item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta.hasDisplayName()) {
					if (MagicSpells.ignoreCastItemNameColors()) {
						this.name = ChatColor.stripColor(meta.getDisplayName());
					} else {
						this.name = meta.getDisplayName();
					}
				}
			}
			if (this.type > 0 && !MagicSpells.ignoreCastItemEnchants()) {
				enchants = getEnchants(item);
			}
		}
	}
	
	public CastItem(String string) {
		String s = string;
		if (s.contains("|")) {
			String[] temp = s.split("\\|");
			s = temp[0];
			if (!MagicSpells.ignoreCastItemNames() && temp.length > 1) {
				if (MagicSpells.ignoreCastItemNameColors()) {
					name = ChatColor.stripColor(temp[1]);
				} else {
					name = temp[1];
				}
			}
		}
		if (s.contains(";")) {
			String[] temp = s.split(";");
			s = temp[0];
			if (!MagicSpells.ignoreCastItemEnchants()) {
				String[] split = temp[1].split("\\+");
				enchants = new int[split.length][];
				for (int i = 0; i < enchants.length; i++) {
					String[] enchantData = split[i].split("-");
					enchants[i] = new int[] { Integer.parseInt(enchantData[0]), Integer.parseInt(enchantData[1]) };
				}
				sortEnchants(enchants);
			}
		}
		if (s.contains(":")) {
			String[] split = s.split(":");
			this.type = Integer.parseInt(split[0]);
			if (MagicSpells.ignoreCastItemDurability(type)) {
				this.data = 0;
			} else {
				this.data = Short.parseShort(split[1]);
			}
		} else {
			this.type = Integer.parseInt(s);
			this.data = 0;
		}
	}
	
	public int getItemTypeId() {
		return this.type;
	}
	
	public boolean equals(CastItem i) {
		return i.type == this.type && i.data == this.data && (MagicSpells.ignoreCastItemNames() || i.name.equals(this.name)) && (MagicSpells.ignoreCastItemEnchants() || compareEnchants(this.enchants, i.enchants));
	}
	
	public boolean equals(ItemStack i) {
		return i.getTypeId() == type && i.getDurability() == data && (MagicSpells.ignoreCastItemNames() || namesEqual(i)) && (MagicSpells.ignoreCastItemEnchants() || compareEnchants(this.enchants, getEnchants(i)));
	}
	
	private boolean namesEqual(ItemStack i) {
		String n = null;
		if (i.hasItemMeta()) {
			ItemMeta meta = i.getItemMeta();
			if (meta.hasDisplayName()) {
				if (MagicSpells.ignoreCastItemNameColors()) {
					n = ChatColor.stripColor(meta.getDisplayName());
				} else {
					n = meta.getDisplayName();
				}
			}
		}
		if (n == null && (name == null || name.isEmpty())) return true;
		if (n == null || name == null) return false;
		return n.equals(name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof CastItem) {
			return equals((CastItem)o);
		} else if (o instanceof ItemStack) {
			return equals((ItemStack)o);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		String s;
		if (data == 0) {
			s = type+"";
		} else {
			s = type + ":" + data;
		}
		if (enchants != null) {
			s += ";";
			for (int i = 0; i < enchants.length; i++) {
				s += enchants[i][0] + "-" + enchants[i][1];
				if (i < enchants.length-1) {
					s += "+";
				}
			}
		}
		if (name != null && !name.isEmpty()) {
			s += "|" + name;
		}
		return s;
	}
	
	private int[][] getEnchants(ItemStack item) {
		if (item != null) {
			Map<Enchantment, Integer> enchantments = item.getEnchantments();
			if (enchantments != null && enchantments.size() > 0) {
				int[][] enchants = new int[enchantments.size()][];
				int i = 0;
				for (Enchantment e : enchantments.keySet()) {
					enchants[i] = new int[] { e.getId(), enchantments.get(e) };
					i++;
				}
				sortEnchants(enchants);
				return enchants;
			}
		}
		return null;
	}
	
	private static void sortEnchants(int[][] enchants) {
		Arrays.sort(enchants, new Comparator<int[]>() {
			public int compare(int[] o1, int[] o2) {
				if (o1[0] > o2[0]) return 1;
				if (o1[0] < o2[0]) return -1;
				return 0;
			}
		});
	}
	
	private boolean compareEnchants(int[][] o1, int[][] o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		if (o1.length != o2.length) return false;
		for (int i = 0; i < o1.length; i++) {
			if (o1[i][0] != o2[i][0] || o1[i][1] != o2[i][1]) 
				return false;
		}
		return true;
	}
	
}
