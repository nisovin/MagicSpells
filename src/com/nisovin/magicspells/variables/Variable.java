package com.nisovin.magicspells.variables;

import org.bukkit.entity.Player;

public abstract class Variable {

	protected double defaultValue = 0;
	protected double maxValue = Double.MAX_VALUE;
	protected double minValue = 0;
	protected boolean permanent;
	
	public Variable(double defaultValue, double minValue, double maxValue, boolean permanent) {
		this.defaultValue = defaultValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public void modify(Player player, double amount) {
		modify(player.getName(), amount);
	}
	
	public abstract void modify(String player, double amount);
	
	public void set(Player player, double amount) {
		set(player.getName(), amount);
	}
	
	public abstract void set(String player, double amount);
	
	public double getValue(Player player) {
		return getValue(player.getName());
	}
	
	public abstract double getValue(String player);
	
	public abstract void reset(Player player);
	
}
