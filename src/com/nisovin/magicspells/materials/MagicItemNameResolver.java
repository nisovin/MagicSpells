package com.nisovin.magicspells.materials;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.Dye;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;

import com.nisovin.magicspells.MagicSpells;

public class MagicItemNameResolver implements ItemNameResolver {

	Map<String, Material> materialMap = new HashMap<String, Material>();
	Map<String, MaterialData> materialDataMap = new HashMap<String, MaterialData>();
	Random rand = new Random();
	
	public MagicItemNameResolver() {
		for (Material mat : Material.values()) {
			materialMap.put(mat.name().toLowerCase(), mat);
		}
		
		File file = new File(MagicSpells.getInstance().getDataFolder(), "itemnames.yml");
		if (!file.exists()) {
			MagicSpells.getInstance().saveResource("itemnames.yml", false);
		}
		YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(file);
			for (String s : config.getKeys(false)) {
				Material m = materialMap.get(config.getString(s).toLowerCase());
				if (m != null) {
					materialMap.put(s.toLowerCase(), m);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map<String, Material> toAdd = new HashMap<String, Material>();
		for (String s : materialMap.keySet()) {
			if (s.contains("_")) {
				toAdd.put(s.replace("_", ""), materialMap.get(s));
			}
		}
		materialMap.putAll(toAdd);
	}
	
	@Override
	public ItemTypeAndData resolve(String string) {
		if (string == null || string.isEmpty()) return null;
		ItemTypeAndData item = new ItemTypeAndData();
		if (string.contains(":")) {
			String[] split = string.split(":");
			if (split[0].matches("[0-9]+")) {
				item.id = Integer.parseInt(split[0]);
			} else {
				Material mat = Material.getMaterial(split[0].toUpperCase());
				if (mat == null) return null;
				item.id = mat.getId();
			}
			if (split[1].matches("[0-9]+")) {
				item.data = Short.parseShort(split[1]);
			} else {
				return null;
			}
		} else {
			if (string.matches("[0-9]+")) {
				item.id = Integer.parseInt(string);
			} else {
				Material mat = Material.getMaterial(string.toUpperCase());
				if (mat == null) return null;
				item.id = mat.getId();
			}
		}
		return item;
	}
	
	@Override
	public MagicMaterial resolveItem(String string) {
		if (string == null || string.isEmpty()) return null;
		
		// first check for predefined material datas
		MaterialData matData = materialDataMap.get(string.toLowerCase());
		if (matData != null) {
			if (matData.getItemType().isBlock()) {
				return new MagicBlockMaterial(matData);
			} else {
				return new MagicItemMaterial(matData);
			}
		}
		
		// split type and data
		String stype;
		String sdata;
		if (string.contains(":")) {
			String[] split = string.split(":", 2);
			stype = split[0].toLowerCase();
			sdata = split[1].toLowerCase();
		} else if (string.contains(" ")) {
			String[] split = string.split(" ", 2);
			sdata = split[0].toLowerCase();
			stype = split[1].toLowerCase();
		} else {
			stype = string.toLowerCase();
			sdata = "";
		}
		
		Material type = materialMap.get(stype);
		if (type == null) {
			return resolveUnknown(stype, sdata);
		}
		
		if (type.isBlock()) {
			return new MagicBlockMaterial(resolveBlockData(type, sdata));
		} else {
			if (sdata.equals("*")) return new MagicItemAnyDataMaterial(type);
			MaterialData itemData = resolveItemData(type, sdata);
			if (itemData != null) {
				return new MagicItemMaterial(itemData);
			}
			short durability = 0;
			try {
				durability = Short.parseShort(sdata);
			} catch (NumberFormatException e) {}
			return new MagicItemMaterial(type, durability);
		}
	}
	
	@Override
	public MagicMaterial resolveBlock(String string) {
		if (string == null || string.isEmpty()) return null;
		
		if (string.contains("|")) {
			return resolveRandomBlock(string);
		}
		
		String stype;
		String sdata;
		if (string.contains(":")) {
			String[] split = string.split(":", 2);
			stype = split[0].toLowerCase();
			sdata = split[1];
		} else {
			stype = string.toLowerCase();
			sdata = "";
		}
		
		Material type = materialMap.get(stype);
		if (type == null) {
			return resolveUnknown(stype, sdata);
		}
		
		if (type.isBlock()) {
			if (sdata.equals("*")) {
				return new MagicBlockAnyDataMaterial(new MaterialData(type));
			} else {
				return new MagicBlockMaterial(resolveBlockData(type, sdata));
			}
		} else {
			return null;
		}
	}
	
	private MagicMaterial resolveRandomBlock(String string) {
		List<MagicMaterial> materials = new ArrayList<MagicMaterial>();
		String[] strings = string.split("\\|");
		for (String s : strings) {
			MagicMaterial mat = resolveBlock(s.trim());
			if (mat != null) {
				materials.add(mat);
			}
		}
		return new MagicBlockRandomMaterial(materials.toArray(new MagicMaterial[materials.size()]));
	}
	
	private MaterialData resolveBlockData(Material type, String sdata) {
		if (type == Material.LOG || type == Material.SAPLING || type == Material.WOOD) {
			return getTree(sdata);
		} else if (type == Material.LEAVES) {
			return getLeaves(sdata);
		} else if (type == Material.WOOL) {
			return getWool(sdata);
		} else if (sdata.matches("^[0-9]+$")) {
			return new MaterialData(type, Byte.parseByte(sdata));
		} else {
			return new MaterialData(type);
		}
	}
	
	private MaterialData resolveItemData(Material type, String sdata) {
		if (type == Material.INK_SACK) {
			return getDye(sdata);
		} else {
			return null;
		}
	}
	
	private MagicMaterial resolveUnknown(String stype, String sdata) {
		try {
			int type = Integer.parseInt(stype);
			if (sdata.equals("*")) {
				return new MagicUnknownAnyDataMaterial(type);
			} else {
				short data = ((sdata == null || sdata.isEmpty()) ? 0 : Short.parseShort(sdata));
				return new MagicUnknownMaterial(type, data);
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private Dye getDye(String data) {
		Dye dye = new Dye();
		dye.setColor(getDyeColor(data));
		return dye;
	}
	
	private Wool getWool(String data) {
		return new Wool(getDyeColor(data));
	}
	
	private DyeColor getDyeColor(String data) {
		if (data != null && data.equalsIgnoreCase("random")) {
			return DyeColor.values()[rand.nextInt(DyeColor.values().length)];
		} else {
			DyeColor color = DyeColor.WHITE;
			if (data != null && data.length() > 0) {
				data = data.replace("_", "").replace(" ", "").toLowerCase();
				for (DyeColor c : DyeColor.values()) {
					if (data.equals(c.name().replace("_", "").toLowerCase())) {
						color = c;
						break;
					}
				}
			}
			return color;
		}
	}
	
	private Tree getTree(String data) {
		TreeSpecies species = TreeSpecies.GENERIC;
		BlockFace dir = BlockFace.UP;
		if (data != null && data.length() > 0) {
			String[] split = data.split("[: ]");
			if (split.length >= 1) {
				species = getTreeSpecies(split[0]);
			}
			if (split.length >= 2) {
				if (split[1].equalsIgnoreCase("east")) {
					dir = BlockFace.EAST;
				} else if (split[1].equalsIgnoreCase("west")) {
					dir = BlockFace.WEST;
				} else if (split[1].equalsIgnoreCase("north")) {
					dir = BlockFace.NORTH;
				} else if (split[1].equalsIgnoreCase("south")) {
					dir = BlockFace.SOUTH;
				} else if (split[1].equalsIgnoreCase("random")) {
					int r = rand.nextInt(3);
					if (r == 0) {
						dir = BlockFace.EAST;
					} else if (r == 1) {
						dir = BlockFace.NORTH;
					}
				}
			}
		}
		return new Tree(species, dir);
	}
	
	private Leaves getLeaves(String data) {
		return new Leaves(getTreeSpecies(data));
	}
	
	private TreeSpecies getTreeSpecies(String data) {
		if (data.equalsIgnoreCase("birch")) {
			return TreeSpecies.BIRCH;
		} else if (data.equalsIgnoreCase("jungle")) {
			return TreeSpecies.JUNGLE;
		} else if (data.equalsIgnoreCase("redwood")) {
			return TreeSpecies.REDWOOD;
		} else if (data.equalsIgnoreCase("random")) {
			return TreeSpecies.values()[rand.nextInt(TreeSpecies.values().length)];
		} else {
			return TreeSpecies.GENERIC;
		}
	}

}
