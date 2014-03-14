package com.nisovin.magicspells.variables;

public class GlobalVariable extends Variable {

	double value = 0;
	
	@Override
	protected void init() {
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
	public void reset(String player) {
		value = defaultValue;
	}

}
