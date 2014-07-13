package com.nisovin.magicspells.variables;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

public class PlayerVariable extends Variable {

	Map<String, Double> map = new HashMap<String, Double>();
	
	@Override
	public boolean modify(String player, double amount) {
		double value = getValue(player);
		double newvalue = value + amount;
		if (newvalue > maxValue) {
			newvalue = maxValue;
		} else if (newvalue < minValue) {
			newvalue = minValue;
		}
		if (value != newvalue) {
			map.put(player, newvalue);
			if (objective != null) {
				objective.getScore(Bukkit.getOfflinePlayer(player)).setScore((int)newvalue);
			}
			return true;
		} else {
			return false;
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
