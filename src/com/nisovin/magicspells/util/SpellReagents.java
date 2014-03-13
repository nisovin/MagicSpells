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
	private int durability;
	private float money;
	
	public SpellReagents() {
		this.items = new HashSet<ItemStack>();
		this.mana = 0;
		this.health = 0;
		this.hunger = 0;
		this.experience = 0;
		this.levels = 0;
		this.money = 0;
	}
	
	public SpellReagents(SpellReagents other) {
		this.items = new HashSet<ItemStack>();
		for (ItemStack item : other.items) {
			this.items.add(item.clone());
		}
		this.mana = other.mana;
		this.health = other.health;
		this.hunger = other.hunger;
		this.experience = other.experience;
		this.levels = other.levels;
		this.money = other.money;
	}
	
	/*public SpellReagents(ItemStack[] items, int mana, int health, int hunger, int experience, int levels, int durability, float money) {
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
		this.durability = durability;
		this.money = money;
	}*/
	
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
	
	public void addItem(ItemStack item) {
		this.items.add(item);
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
	
	public int getDurability() {
		return durability;
	}
	
	public void setDurability(int durability) {
		this.durability = durability;
	}
	
	public float getMoney() {
		return money;
	}
	
	public void setMoney(float money) {
		this.money = money;
	}
	
	public SpellReagents clone() {
		SpellReagents other = new SpellReagents();
		for (ItemStack item : this.items) {
			other.items.add(item.clone());
		}
		other.mana = this.mana;
		other.health = this.health;
		other.hunger = this.hunger;
		other.experience = this.experience;
		other.levels = this.levels;
		other.durability = this.durability;
		other.money = this.money;
		return other;
	}
	
	public SpellReagents multiply(float x) {
		SpellReagents other = new SpellReagents();
		for (ItemStack item : this.items) {
			ItemStack i = item.clone();
			i.setAmount(Math.round(i.getAmount() * x));
			other.items.add(i);
		}
		other.mana = Math.round(this.mana * x);
		other.health = Math.round(this.health * x);
		other.hunger = Math.round(this.hunger * x);
		other.experience = Math.round(this.experience * x);
		other.levels = Math.round(this.levels * x);
		other.durability = Math.round(this.durability * x);
		other.money = this.money * x;
		return other;
	}
	
}
