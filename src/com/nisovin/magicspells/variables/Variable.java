package com.nisovin.magicspells.variables;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

public abstract class Variable {

	protected double defaultValue = 0;
	protected double maxValue = Double.MAX_VALUE;
	protected double minValue = 0;
	protected boolean permanent;
	protected Objective objective;
	protected String bossBar;
	protected boolean expBar;
	
	public Variable() {
	}
	
	public final void init(double defaultValue, double minValue, double maxValue, boolean permanent, Objective objective, String bossBar, boolean expBar) {
		this.defaultValue = defaultValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.permanent = permanent;
		this.objective = objective;
		this.bossBar = bossBar;
		this.expBar = expBar;
		init();
	}
	
	protected void init() {}
	
	public final boolean modify(Player player, double amount) {
		return modify(player.getName(), amount);
	}
	
	public abstract boolean modify(String player, double amount);
	
	public final void set(Player player, double amount) {
		set(player.getName(), amount);
	}
	
	public abstract void set(String player, double amount);
	
	public double getValue(Player player) {
		if (player == null) return getValue("");
		return getValue(player.getName());
	}
	
	public abstract double getValue(String player);
	
	public final void reset(Player player) {
		reset(player.getName());
	}
	
	public abstract void reset(String player);
	
}
