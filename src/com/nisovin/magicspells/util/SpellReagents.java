package com.nisovin.magicspells.util;

import java.util.Collection;
import java.util.HashSet;

import org.bukkit.inventory.ItemStack;

public class SpellReagents {
	
	private HashSet<ItemStack> items;
	private int mana;
	private int health;
	private int hunger;
	private int experience;
	private int levels;
	
	public SpellReagents(ItemStack[] items, int mana, int health, int hunger, int experience, int levels) {
		this.items = new HashSet<ItemStack>();
		if (items != null) {
			for (ItemStack i : items) {
				if (i != null) {
					this.items.add(i.clone());
				}
			}
		}
		this.mana = mana;
		this.health = health;
		this.hunger = hunger;
		this.experience = experience;
		this.levels = levels;
	}
	
	public HashSet<ItemStack> getItems() {
		return items;
	}
	
	public ItemStack[] getItemsAsArray() {
		ItemStack[] arr = new ItemStack[items.size()];
		arr = items.toArray(arr);
		return arr;
	}
	
	public void setItems(Collection<ItemStack> items) {
		this.items.clear();
		this.items.addAll(items);
	}
	
	public void setItems(ItemStack[] items) {
		this.items.clear();
		for (ItemStack i : items) {
			this.items.add(i);
		}
	}
	
	public int getMana() {
		return mana;
	}
	
	public void setMana(int mana) {
		this.mana = mana;
	}
	
	public int getHealth() {
		return health;
	}
	
	public void setHealth(int health) {
		this.health = health;
	}
	
	public int getHunger() {
		return hunger;
	}
	
	public void setHunger(int hunger) {
		this.hunger = hunger;
	}
	
	public int getExperience() {
		return experience;
	}
	
	public void setExperience(int experience) {
		this.experience = experience;
	}
	
	public int getLevels() {
		return levels;
	}
	
	public void setLevels(int levels) {
		this.levels = levels;
	}
	
}
