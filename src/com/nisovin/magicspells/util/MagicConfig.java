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
			// load main config
			mainConfig = new YamlConfiguration();
			mainConfig.load(file);
			
			// load alt config
			String s = this.getString("general.alt-config", null);
			if (s != null && !s.trim().equals("")) {
				s = s.trim();
				File f = new File(MagicSpells.plugin.getDataFolder(), s);
				if (f.exists()) {
					altConfig = new YamlConfiguration();
					altConfig.load(f);
				}
			}
			
			// load mini configs
			File spellConfigsFolder = new File(MagicSpells.plugin.getDataFolder(), "spellconfigs");
			if (spellConfigsFolder.exists()) {
				loadSpellConfigs(spellConfigsFolder);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void loadSpellConfigs(File folder) {
		YamlConfiguration conf;
		String name;
		File[] files = folder.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				loadSpellConfigs(file);
			} else if (file.getName().endsWith(".yml")) {
				name = file.getName().replace(".yml", "");
				conf = new YamlConfiguration();
				try {
					conf.load(file);
					for(String key : conf.getKeys(false)) {
						mainConfig.set("spells." + name + "." + key, conf.get(key));
					}
				} catch (Exception e) {
					MagicSpells.error("Error reading spell config file: " + file.getName());
					e.printStackTrace();
				}
			}
		}
	}
	
	public boolean isLoaded() {
		return (mainConfig.contains("general") && mainConfig.contains("spells"));
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
	
	public List<Byte> getByteList(String path, List<Byte> def) {
		if (altConfig != null && altConfig.contains(path)) {
			return altConfig.getByteList(path);
		} else if (mainConfig.contains(path)) {
			List<Byte> l = mainConfig.getByteList(path);
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
	
	public Set<String> getSpellKeys() {
		if (mainConfig != null && mainConfig.contains("spells") && mainConfig.isConfigurationSection("spells")) {
			Set<String> keys = mainConfig.getConfigurationSection("spells").getKeys(false);
			if (altConfig != null && altConfig.contains("spells") && altConfig.isConfigurationSection("spells")) {
				Set<String> altkeys = altConfig.getConfigurationSection("spells").getKeys(false);
				keys.addAll(altkeys);
			}
			return keys;
		} else {
			return null;
		}
	}
	
	public static void explode() {
		try {
			File spellConfFolder = new File(MagicSpells.plugin.getDataFolder(), "spellconfigs");
			if (!spellConfFolder.exists()) {
				spellConfFolder.mkdir();
			}
			
			YamlConfiguration config = new YamlConfiguration();
			config.load(new File(MagicSpells.plugin.getDataFolder(), "config.yml"));
			for (String spellName : config.getConfigurationSection("spells").getKeys(false)) {
				File spellFile = new File(spellConfFolder, spellName + ".yml");
				if (!spellFile.exists()) {
					YamlConfiguration spellConf = new YamlConfiguration();
					ConfigurationSection spellSec = config.getConfigurationSection("spells." + spellName);
					for (String key : spellSec.getKeys(false)) {
						spellConf.set(key, spellSec.get(key));
					}
					spellConf.save(spellFile);
				}
			}
		} catch (Exception e) {
			MagicSpells.error("Failed to explode config");
			e.printStackTrace();
		}
	}
	
}
