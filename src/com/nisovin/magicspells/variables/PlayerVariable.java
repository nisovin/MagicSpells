package com.nisovin.magicspells.variables;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

public class PlayerVariable extends Variable {

	Map<String, Double> map = new HashMap<String, Double>();
	
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
		if (objective != null) {
			objective.getScore(Bukkit.getOfflinePlayer(player)).setScore((int)value);
		}
	}

	@Override
	public void set(String player, double amount) {
		map.put(player, amount);
		if (objective != null) {
			objective.getScore(Bukkit.getOfflinePlayer(player)).setScore((int)amount);
		}
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
	public void reset(String player) {
		map.remove(player);
		if (objective != null) {
			objective.getScore(Bukkit.getOfflinePlayer(player)).setScore((int)defaultValue);
		}
	}

}
