package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.materials.MagicMaterial;

public class WearingInSlotCondition extends Condition {

	int slot = -1;
	MagicMaterial mat = null;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] data = var.split("=");
			String s = data[0].toLowerCase();
			if (s.startsWith("helm") || s.startsWith("hat") || s.startsWith("head")) {
				slot = 0;
			} else if (s.startsWith("chest") || s.startsWith("tunic")) {
				slot = 1;
			} else if (s.startsWith("leg") || s.startsWith("pant")) {
				slot = 2;
			} else if (s.startsWith("boot") || s.startsWith("shoe") || s.startsWith("feet")) {
				slot = 3;
			}
			if (slot == -1) return false;
			if (data[1].equals("0") || data[1].equals("air") || data[1].equals("empty")) {
				mat = null;
			} else {
				mat = MagicSpells.getItemNameResolver().resolveItem(data[1]);
				if (mat == null) return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(Player player) {
		ItemStack item = player.getInventory().getArmorContents()[slot];
		if (mat == null && (item == null || item.getType() == Material.AIR)) {
			return true;
		} else if (mat != null && item != null && mat.getMaterial() == item.getType()) {
			return true;
		}
		return false;
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
