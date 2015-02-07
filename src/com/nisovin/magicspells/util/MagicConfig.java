package com.nisovin.magicspells.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.MagicSpells;

public class MagicConfig {

	private YamlConfiguration mainConfig;
	
	public MagicConfig(File file) {
		this(MagicSpells.plugin);
	}
	
	public MagicConfig(MagicSpells plugin) {
		try {
			File folder = plugin.getDataFolder();
			File file = new File(folder, "config.yml");
			
			// load main config
			mainConfig = new YamlConfiguration();
			if (file.exists()) {
				mainConfig.load(file);
			}
			if (!mainConfig.contains("general")) mainConfig.createSection("general");
			if (!mainConfig.contains("mana")) mainConfig.createSection("mana");
			if (!mainConfig.contains("spells")) mainConfig.createSection("spells");
			
			// load general
			File generalConfigFile = new File(folder, "general.yml");
			if (generalConfigFile.exists()) {
				YamlConfiguration generalConfig = new YamlConfiguration();
				try {
					generalConfig.load(generalConfigFile);
					Set<String> keys = generalConfig.getKeys(true);
					for (String key : keys) {
						mainConfig.set("general." + key, generalConfig.get(key));
					}
				} catch (Exception e) {
					MagicSpells.error("Error loading config file general.yml");
					MagicSpells.handleException(e);
				}
			}
			
			// load mana
			File manaConfigFile = new File(folder, "mana.yml");
			if (manaConfigFile.exists()) {
				YamlConfiguration manaConfig = new YamlConfiguration();
				try {
					manaConfig.load(manaConfigFile);
					Set<String> keys = manaConfig.getKeys(true);
					for (String key : keys) {
						mainConfig.set("mana." + key, manaConfig.get(key));
					}
				} catch (Exception e) {
					MagicSpells.error("Error loading config file mana.yml");
					MagicSpells.handleException(e);
				}
			}
			
			// load no magic zones
			File zonesConfigFile = new File(folder, "zones.yml");
			if (zonesConfigFile.exists()) {
				YamlConfiguration zonesConfig = new YamlConfiguration();
				try {
					zonesConfig.load(zonesConfigFile);
					Set<String> keys = zonesConfig.getKeys(true);
					for (String key : keys) {
						mainConfig.set("no-magic-zones." + key, zonesConfig.get(key));
					}
				} catch (Exception e) {
					MagicSpells.error("Error loading config file zones.yml");
					MagicSpells.handleException(e);
				}
			}
			
			// load spell configs
			for (File spellConfigFile : folder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith("spell") && name.endsWith(".yml");
				}
			})) {
				YamlConfiguration spellConfig = new YamlConfiguration();
				try {
					spellConfig.load(spellConfigFile);
					Set<String> keys = spellConfig.getKeys(false);
					for (String key : keys) {
						if (key.equals("predefined-items")) {
							ConfigurationSection sec = mainConfig.getConfigurationSection("general.predefined-items");
							if (sec == null) {
								sec = mainConfig.createSection("general.predefined-items");
							}
							for (String itemKey : spellConfig.getConfigurationSection("predefined-items").getKeys(false)) {
								sec.set(itemKey, spellConfig.get("predefined-items." + itemKey));
							}
						} else if (key.equals("variables")) {
							ConfigurationSection sec = mainConfig.getConfigurationSection("general.variables");
							if (sec == null) {
								sec = mainConfig.createSection("general.variables");
							}
							for (String itemKey : spellConfig.getConfigurationSection("variables").getKeys(false)) {
								sec.set(itemKey, spellConfig.get("variables." + itemKey));
							}
						} else {
							mainConfig.set("spells." + key, spellConfig.get(key));
						}
					}
				} catch (Exception e) {
					MagicSpells.error("Error loading config file " + spellConfigFile.getName());
					MagicSpells.handleException(e);
				}
			}
			
			// load mini configs
			File spellConfigsFolder = new File(folder, "spellconfigs");
			if (spellConfigsFolder.exists()) {
				loadSpellConfigs(spellConfigsFolder);
			}
		} catch (Exception ex) {
			MagicSpells.handleException(ex);
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
		if (mainConfig.contains(path)) {
			return true;
		} else {
			return false;
		}
	}
	
	public int getInt(String path, int def) {
		return mainConfig.getInt(path, def);
	}
	
	public long getLong(String path, long def) {
		return mainConfig.getLong(path, def);
	}
	
	public double getDouble(String path, double def) {
		if (mainConfig.contains(path) && mainConfig.isInt(path)) {
			return mainConfig.getInt(path);
		} else {
			return mainConfig.getDouble(path, def);
		}
	}
	
	public boolean getBoolean(String path, boolean def) {		
		return mainConfig.getBoolean(path, def);
	}
	
	public boolean isString(String path) {
		if (mainConfig.contains(path)) {
			return mainConfig.isString(path);
		} else {
			return false;
		}
	}
	
	public String getString(String path, String def) {
		if (mainConfig.contains(path)) {
			return mainConfig.get(path).toString();
		} else {
			return def;
		}
	}
	
	public boolean isList(String path) {
		if (mainConfig.contains(path)) {
			return mainConfig.isList(path);
		} else {
			return false;
		}
	}
	
	public List<Integer> getIntList(String path, List<Integer> def) {
		if (mainConfig.contains(path)) {
			List<Integer> l = mainConfig.getIntegerList(path);
			if (l != null) {
				return l;
			}
		}
		return def;
	}
	
	public List<Byte> getByteList(String path, List<Byte> def) {
		if (mainConfig.contains(path)) {
			List<Byte> l = mainConfig.getByteList(path);
			if (l != null) {
				return l;
			}
		}
		return def;
	}
	
	public List<String> getStringList(String path, List<String> def) {
		if (mainConfig.contains(path)) {
			List<String> l = mainConfig.getStringList(path);
			if (l != null) {
				return l;
			}
		}
		return def;
	}
	
	public List<?> getList(String path, List<?> def) {
		if (mainConfig.contains(path)) {
			List<?> l = mainConfig.getList(path);
			if (l != null) {
				return l;
			}
		}
		return def;
	}
	
	public Set<String> getKeys(String path) {
		if (mainConfig.contains(path) && mainConfig.isConfigurationSection(path)) {
			return mainConfig.getConfigurationSection(path).getKeys(false);
		} else {
			return null;
		}
	}
	
	public boolean isSection(String path) {
		if (mainConfig.contains(path)) {
			return mainConfig.isConfigurationSection(path);
		} else {
			return false;
		}
	}
	
	public ConfigurationSection getSection(String path) {
		if (mainConfig.contains(path)) {
			return mainConfig.getConfigurationSection(path);
		} else {
			return null;
		}
	}
	
	public Set<String> getSpellKeys() {
		if (mainConfig != null && mainConfig.contains("spells") && mainConfig.isConfigurationSection("spells")) {
			return mainConfig.getConfigurationSection("spells").getKeys(false);
		} else {
			return null;
		}
	}
	
}
