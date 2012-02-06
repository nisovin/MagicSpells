package com.nisovin.magicspells;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.events.ManaChangeEvent;

public class ManaBar {
	private int mana;
	private int maxMana;
	
	public ManaBar(int maxMana) {
		this.maxMana = maxMana;
		this.mana = maxMana;
	}
	
	public boolean has(int amount) {
		return (mana >= amount);
	}
	
	public boolean remove(int amount) {
		return add(amount * -1);
	}
	
	public boolean add(int amount) {
		if (amount > 0) {
			if (mana == maxMana) {
				return false;
			}
			mana += amount;
			if (mana > maxMana) {
				mana = maxMana;
			}
			return true;
		} else {
			amount *= -1;
			if (amount > mana) {
				return false;
			} else {
				mana -= amount;
				return true;
			}
		}
	}
	
	public void showInChat(Player player) {
		int segments = (int)(((double)mana/(double)maxMana) * ManaBarManager.manaBarSize);
		String text = MagicSpells.textColor + ManaBarManager.manaBarPrefix + " {" + ManaBarManager.manaBarColorFull;
		int i = 0;
		for (; i < segments; i++) {
			text += "=";
		}
		text += ManaBarManager.manaBarColorEmpty;
		for (; i < ManaBarManager.manaBarSize; i++) {
			text += "=";
		}
		text += MagicSpells.textColor + "} [" + mana + "/" + maxMana + "]";
		player.sendMessage(text);
	}
	
	public void showOnTool(Player player) {
		ItemStack item = player.getInventory().getItem(ManaBarManager.manaBarToolSlot);
		Material type = item.getType();
		if (type == Material.WOOD_AXE || type == Material.WOOD_HOE || type == Material.WOOD_PICKAXE || type == Material.WOOD_SPADE || type == Material.WOOD_SWORD) {
			int dur = 60 - (int)(((double)mana/(double)maxMana) * 60);
			if (dur == 60) {
				dur = 59;
			} else if (dur == 0) {
				dur = 1;
			}
			item.setDurability((short)dur);
			player.getInventory().setItem(ManaBarManager.manaBarToolSlot, item);
		}
	}
	
	public void callManaChangeEvent(Player player) {
		ManaChangeEvent event = new ManaChangeEvent(player, mana, maxMana);
		Bukkit.getPluginManager().callEvent(event);
	}
}