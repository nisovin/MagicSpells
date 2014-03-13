package com.nisovin.magicspells.variables;

import org.bukkit.entity.Player;

public class GlobalVariable extends Variable {

	double value = 0;
	
	public GlobalVariable(double defaultValue, double minValue, double maxValue, boolean permanent) {
		super(defaultValue, minValue, maxValue, permanent);
		value = defaultValue;
	}
	
	@Override
	public void modify(String player, double amount) {
		value += amount;
		if (value > maxValue) {
			value = maxValue;
		} else if (value < minValue) {
			value = minValue;
		}
	}

	@Override
	public void set(String player, double amount) {
		value = amount;
	}

	@Override
	public double getValue(String player) {
		return value;
	}

	@Override
	public void reset(Player player) {
		value = defaultValue;
	}

}
