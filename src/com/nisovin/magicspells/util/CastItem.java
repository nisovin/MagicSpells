package com.nisovin.magicspells.util;

import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;

public class CastItem {
	private int type;
	private short data;
	
	public CastItem(int type) {
		this.type = type;
		this.data = 0;
	}
	
	public CastItem(int type, short data) {
		this.type = type;
		if (MagicSpells.ignoreCastItemDurability != null && MagicSpells.ignoreCastItemDurability.contains(type)) {
			this.data = 0;
		} else {
			this.data = data;
		}
	}
	
	public CastItem(ItemStack i) {
		if (i == null) {
			this.type = 0;
			this.data = 0;
		} else {
			this.type = i.getTypeId();
			if (MagicSpells.ignoreCastItemDurability != null && MagicSpells.ignoreCastItemDurability.contains(type)) {
				this.data = 0;
			} else {
				this.data = i.getDurability();
			}
		}
	}
	
	public CastItem(String s) {
		if (s.contains(":")) {
			String[] split = s.split(":");
			this.type = Integer.parseInt(split[0]);
			if (MagicSpells.ignoreCastItemDurability != null && MagicSpells.ignoreCastItemDurability.contains(type)) {
				this.data = 0;
			} else {
				this.data = Short.parseShort(split[1]);
			}
		} else {
			this.type = Integer.parseInt(s);
			this.data = 0;
		}
		
	}
	
	public boolean equals(CastItem i) {
		return (i.type == this.type && i.data == this.data);
	}
	
	public boolean equals(ItemStack i) {
		return (i.getTypeId() == type && i.getDurability() == data);
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
		return type * Short.MAX_VALUE + data;
	}
	
	@Override
	public String toString() {
		if (data == 0) {
			return type+"";
		} else {
			return type + ":" + data;
		}
	}
}
