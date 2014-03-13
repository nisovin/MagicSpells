package com.nisovin.magicspells.variables;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class PlayerVariable extends Variable {

	Map<String, Double> map = new HashMap<String, Double>();

	public PlayerVariable(double defaultValue, double minValue, double maxValue, boolean permanent) {
		super(defaultValue, minValue, maxValue, permanent);
	}
	
	@Override
	public void modify(String player, double amount) {
		double value = getValue(player);
		value += amount;
		if (value > maxValue) {
			value = maxValue;
		} else if (value < minValue) {
			value = minValue;
		}
		map.put(player, value);
	}

	@Override
	public void set(String player, double amount) {
		map.put(player, amount);
	}

	@Override
	public double getValue(String player) {
		if (map.containsKey(player)) {
			return map.get(player).doubleValue();
		} else {
			return defaultValue;
		}
	}

	@Override
	public void reset(Player player) {
		map.remove(player.getName());
	}

}
