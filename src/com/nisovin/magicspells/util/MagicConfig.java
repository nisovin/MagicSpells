package com.nisovin.magicspells.util;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.MagicSpells;

public class MagicConfig {

	private YamlConfiguration mainConfig;
	private YamlConfiguration altConfig;
	
	public MagicConfig(File file) {
		try {
			mainConfig = new YamlConfiguration();
			mainConfig.load(file);
			String s = this.getString("general.alt-config", null);
			if (s != null && !s.trim().equals("")) {
				s = s.trim();
				File f = new File(MagicSpells.plugin.getDataFolder(), s);
				if (f.exists()) {
					altConfig = new YamlConfiguration();
					altConfig.load(f);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public boolean contains(String path) {
		if (altConfig != null && altConfig.contains(path)) {
			return true;
		} else if (mainConfig.contains(path)) {
			return true;
		} else {
			return false;
		}
	}
	
	public int getInt(String path, int def) {
		if (altConfig != null && altConfig.contains(path) && altConfig.isInt(path)) {
			return altConfig.getInt(path);
		} else {
			return mainConfig.getInt(path, def);
		}
	}
	
	public double getDouble(String path, double def) {
		if (altConfig != null && altConfig.contains(path) && (altConfig.isDouble(path) || altConfig.isInt(path))) {
			if (altConfig.isDouble(path)) {
				return altConfig.getDouble(path);
			} else {
				return altConfig.getInt(path);
			}
		} else if (mainConfig.contains(path) && mainConfig.isInt(path)) {
			return mainConfig.getInt(path);
		} else {
			return mainConfig.getDouble(path, def);
		}
	}
	
	public boolean getBoolean(String path, boolean def) {
		if (altConfig != null && altConfig.contains(path) && altConfig.isBoolean(path)) {
			return altConfig.getBoolean(path);
		} else {
			return mainConfig.getBoolean(path, def);
		}
	}
	
	public String getString(String path, String def) {
		if (altConfig != null && altConfig.contains(path)) {
			return altConfig.get(path).toString();
		} else if (mainConfig.contains(path)) {
			return mainConfig.get(path).toString();
		} else {
			return def;
		}
	}
	
	public List<Integer> getIntList(String path, List<Integer> def) {
		if (altConfig != null && altConfig.contains(path)) {
			return altConfig.getIntegerList(path);
		} else if (mainConfig.contains(path)) {
			List<Integer> l = mainConfig.getIntegerList(path);
			if (l != null) {
				return l;
			}
		}
		return def;
	}
	
	public List<String> getStringList(String path, List<String> def) {
		if (altConfig != null && altConfig.contains(path)) {
			return altConfig.getStringList(path);
		} else if (mainConfig.contains(path)) {
			List<String> l = mainConfig.getStringList(path);
			if (l != null) {
				return l;
			}
		}
		return def;
	}
	
	public Set<String> getKeys(String path) {
		if (altConfig != null && altConfig.contains(path) && altConfig.isConfigurationSection(path)) {
			return altConfig.getConfigurationSection(path).getKeys(false);
		} else if (mainConfig.contains(path) && mainConfig.isConfigurationSection(path)) {
			return mainConfig.getConfigurationSection(path).getKeys(false);
		} else {
			return null;
		}
	}
	
	public ConfigurationSection getSection(String path) {
		if (altConfig != null && altConfig.contains(path)) {
			return altConfig.getConfigurationSection(path);
		} else if (mainConfig.contains(path)) {
			return mainConfig.getConfigurationSection(path);
		} else {
			return null;
		}
	}
	
	public ConfigurationSection getSpellSection() {
		return mainConfig.getConfigurationSection("spells");
	}
	
	public Set<String> getSpellKeys() {
		Set<String> keys = mainConfig.getConfigurationSection("spells").getKeys(false);
		if (altConfig != null && altConfig.contains("spells") && altConfig.isConfigurationSection("spells")) {
			Set<String> altkeys = altConfig.getConfigurationSection("spells").getKeys(false);
			keys.addAll(altkeys);
		}
		return keys;
	}
	
}
