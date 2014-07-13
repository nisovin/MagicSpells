package com.nisovin.magicspells.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
	private HashMap<String, Double> variables;
	
	public SpellReagents() {
		this.items = null;
		this.mana = 0;
		this.health = 0;
		this.hunger = 0;
		this.experience = 0;
		this.levels = 0;
		this.money = 0;
		this.variables = null;
	}
	
	public SpellReagents(SpellReagents other) {
		if (other.items != null) {
			this.items = new HashSet<ItemStack>();
			for (ItemStack item : other.items) {
				this.items.add(item.clone());
			}
		}
		this.mana = other.mana;
		this.health = other.health;
		this.hunger = other.hunger;
		this.experience = other.experience;
		this.levels = other.levels;
		this.money = other.money;
		if (other.variables != null) {
			this.variables = new HashMap<String, Double>();
			for (String var : other.variables.keySet()) {
				this.variables.put(var, other.variables.get(var));
			}
		}
	}
	
	public HashSet<ItemStack> getItems() {
		return items;
	}
	
	public ItemStack[] getItemsAsArray() {
		if (items == null || items.size() == 0) return null;
		ItemStack[] arr = new ItemStack[items.size()];
		arr = items.toArray(arr);
		return arr;
	}
	
	public void setItems(Collection<ItemStack> items) {
		if (items == null || items.size() == 0) {
			this.items = null;
		} else {
			this.items = new HashSet<ItemStack>();
			this.items.addAll(items);
		}
	}
	
	public void setItems(ItemStack[] items) {
		if (items == null || items.length == 0) {
			this.items = null;
		} else {
			this.items = new HashSet<ItemStack>();
			for (ItemStack i : items) {
				this.items.add(i);
			}
		}
	}
	
	public void addItem(ItemStack item) {
		if (this.items == null) this.items = new HashSet<ItemStack>();
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
	
	public HashMap<String, Double> getVariables() {
		return variables;
	}
	
	public void addVariable(String var, double val) {
		if (variables == null) variables = new HashMap<String, Double>();
		variables.put(var, val);
	}
	
	public void setVariables(Map<String, Double> variables) {
		if (variables == null || variables.size() == 0) {
			this.variables = null;
		} else {
			this.variables = new HashMap<String, Double>();
			this.variables.putAll(variables);
		}
	}
	
	public SpellReagents clone() {
		SpellReagents other = new SpellReagents();
		if (this.items != null) {
			other.items = new HashSet<ItemStack>();
			for (ItemStack item : this.items) {
				other.items.add(item.clone());
			}
		}
		other.mana = this.mana;
		other.health = this.health;
		other.hunger = this.hunger;
		other.experience = this.experience;
		other.levels = this.levels;
		other.durability = this.durability;
		other.money = this.money;
		if (this.variables != null) {
			other.variables = new HashMap<String, Double>();
			for (String var : this.variables.keySet()) {
				other.variables.put(var, this.variables.get(var));
			}
		}
		return other;
	}
	
	public SpellReagents multiply(float x) {
		SpellReagents other = new SpellReagents();
		if (this.items != null) {
			other.items = new HashSet<ItemStack>();
			for (ItemStack item : this.items) {
				ItemStack i = item.clone();
				i.setAmount(Math.round(i.getAmount() * x));
				other.items.add(i);
			}
		}
		other.mana = Math.round(this.mana * x);
		other.health = Math.round(this.health * x);
		other.hunger = Math.round(this.hunger * x);
		other.experience = Math.round(this.experience * x);
		other.levels = Math.round(this.levels * x);
		other.durability = Math.round(this.durability * x);
		other.money = this.money * x;
		if (this.variables != null) {
			other.variables = new HashMap<String, Double>();
			for (String var : this.variables.keySet()) {
				other.variables.put(var, this.variables.get(var) * x);
			}
		}
		return other;
	}
	
}
