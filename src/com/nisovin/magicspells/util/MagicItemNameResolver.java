package com.nisovin.magicspells.util;

import org.bukkit.Material;

public class MagicItemNameResolver implements ItemNameResolver {

	@Override
	public ItemTypeAndData resolve(String string) {
		if (string == null || string.isEmpty()) return null;
		ItemTypeAndData item = new ItemTypeAndData();
		if (string.contains(":")) {
			String[] split = string.split(":");
			if (split[0].matches("[0-9]+")) {
				item.id = Integer.parseInt(split[0]);
			} else {
				Material mat = Material.getMaterial(split[0].toUpperCase());
				if (mat == null) return null;
				item.id = mat.getId();
			}
			if (split[1].matches("[0-9]+")) {
				item.data = Short.parseShort(split[1]);
			} else {
				return null;
			}
		} else {
			if (string.matches("[0-9]+")) {
				item.id = Integer.parseInt(string);
			} else {
				Material mat = Material.getMaterial(string.toUpperCase());
				if (mat == null) return null;
				item.id = mat.getId();
			}
		}
		return item;
	}

}
