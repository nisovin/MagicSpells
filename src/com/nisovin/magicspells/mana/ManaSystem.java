package com.nisovin.magicspells.mana;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.MagicConfig;

public class ManaSystem extends ManaHandler {

	private String manaBarPrefix;
	private int manaBarSize;
	private ChatColor manaBarColorFull;
	private ChatColor manaBarColorEmpty;
	private int manaBarToolSlot;
	
	private int regenInterval;
	private int defaultStartingMana;
	private int defaultMaxMana;
	private int defaultRegenAmount;
	
	private boolean showManaOnUse;
	private boolean showManaOnRegen;
	private boolean showManaOnWoodTool;
	private boolean showManaOnHungerBar;
	private boolean showManaOnExperienceBar;
	
	private List<String> modifierList;
	private ModifierSet modifiers;
	
	private ManaRank defaultRank;
	private ArrayList<ManaRank> ranks;
	
	private HashMap<String, ManaBar> manaBars;
	
	private int taskId = -1;

	public ManaSystem(MagicConfig config) {
		manaBarPrefix = config.getString("mana.mana-bar-prefix", "Mana:");
		manaBarSize = config.getInt("mana.mana-bar-size", 35);
		manaBarColorFull = ChatColor.getByChar(config.getString("mana.color-full", ChatColor.GREEN.getChar() + ""));
		manaBarColorEmpty = ChatColor.getByChar(config.getString("mana.color-empty", ChatColor.BLACK.getChar() + ""));
		manaBarToolSlot = config.getInt("mana.tool-slot", 8);
		
		regenInterval = config.getInt("mana.regen-interval", 20);
		defaultMaxMana = config.getInt("mana.default-max-mana", 100);
		defaultStartingMana = config.getInt("mana.default-starting-mana", defaultMaxMana);
		defaultRegenAmount = config.getInt("mana.default-regen-amount", 5);
		
		showManaOnUse = config.getBoolean("mana.show-mana-on-use", false);
		showManaOnRegen = config.getBoolean("mana.show-mana-on-regen", false);
		showManaOnWoodTool = config.getBoolean("mana.show-mana-on-wood-tool", true);
		showManaOnHungerBar = config.getBoolean("mana.show-mana-on-hunger-bar", false);
		showManaOnExperienceBar = config.getBoolean("mana.show-mana-on-experience-bar", false);

		modifierList = config.getStringList("mana.modifiers", null);
		
		defaultRank = new ManaRank();
		defaultRank.name = "default";
		defaultRank.startingMana = defaultStartingMana;
		defaultRank.maxMana = defaultMaxMana;
		defaultRank.regenAmount = defaultRegenAmount;
		defaultRank.prefix = manaBarPrefix;
		defaultRank.colorFull = manaBarColorFull;
		defaultRank.colorEmpty = manaBarColorEmpty;
		
		ranks = new ArrayList<ManaRank>();
		Set<String> rankKeys = config.getKeys("mana.ranks");
		if (rankKeys != null) {
			for (String key : rankKeys) {
				ManaRank r = new ManaRank();
				r.name = key;
				r.maxMana = config.getInt("mana.ranks." + key + ".max-mana", defaultMaxMana);
				r.startingMana = config.getInt("mana.ranks." + key + ".starting-mana", defaultStartingMana);
				r.regenAmount = config.getInt("mana.ranks." + key + ".regen-amount", defaultRegenAmount);
				r.prefix = config.getString("mana.ranks." + key + ".prefix", manaBarPrefix);
				r.colorFull = ChatColor.getByChar(config.getString("mana.ranks." + key + ".color-full", manaBarColorFull.getChar() + ""));
				r.colorEmpty = ChatColor.getByChar(config.getString("mana.ranks." + key + ".color-empty", manaBarColorEmpty.getChar() + ""));
				ranks.add(r);
			}
		}
		
		manaBars = new HashMap<String, ManaBar>();
		taskId = MagicSpells.scheduleRepeatingTask(new Regenerator(), regenInterval, regenInterval);
	}
	
	@Override
	public void initialize() {
		if (modifierList != null && modifierList.size() > 0) {
			MagicSpells.debug(2, "Adding mana modifiers");
			modifiers = new ModifierSet(modifierList);
			modifierList = null;
		}
	}
	
	private ManaBar getManaBar(Player player) {
		ManaBar bar = manaBars.get(player.getName().toLowerCase());
		if (bar == null) {
			// create the mana bar
			ManaRank rank = getRank(player);
			bar = new ManaBar(player, rank);
			MagicSpells.debug(1, "Creating mana bar for player " + player.getName() + " with rank " + rank.name);
			manaBars.put(player.getName().toLowerCase(), bar);
		}
		return bar;
	}
	
	@Override
	public void createManaBar(final Player player) {
		boolean update = manaBars.containsKey(player.getName().toLowerCase());
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			if (update) {
				ManaRank rank = getRank(player);
				if (rank != bar.getManaRank()) {
					MagicSpells.debug(1, "Updating mana bar for player " + player.getName() + " with rank " + rank.name);
					bar.setRank(rank);
				}
			}
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					showMana(player);
				}
			}, 11);
		}
	}
	
	@Override
	public boolean updateManaRankIfNecessary(Player player) {
		if (manaBars.containsKey(player.getName().toLowerCase())) {
			ManaBar bar = getManaBar(player);
			ManaRank rank = getRank(player);
			if (bar.getManaRank() != rank) {
				bar.setRank(rank);
				return true;
			}
		} else {
			getManaBar(player);
		}
		return false;
	}
	
	private ManaRank getRank(Player player) {
		MagicSpells.debug(3, "Fetching mana rank for player " + player.getName() + "...");
		for (ManaRank rank : ranks) {
			MagicSpells.debug(3, "    checking rank " + rank.name);
			if (player.hasPermission("magicspells.rank." + rank.name)) {
				MagicSpells.debug(3, "    rank found");
				return rank;
			}
		}
		MagicSpells.debug(3, "    no rank found");
		return defaultRank;
	}

	@Override
	public int getMaxMana(Player player) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			return bar.getMaxMana();
		} else {
			return 0;
		}
	}
	
	@Override
	public void setMaxMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			bar.setMaxMana(amount);
		}
	}
	
	@Override
	public int getRegenAmount(Player player) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			return bar.getRegenAmount();
		} else {
			return 0;
		}
	}

	@Override
	public void setRegenAmount(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			bar.setRegenAmount(amount);
		}
	}

	@Override
	public int getMana(Player player) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			return bar.getMana();
		}
		return 0;
	}
	
	@Override
	public boolean hasMana(Player player, int amount) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			return bar.has(amount);
		} else {
			return false;
		}
	}

	@Override
	public boolean addMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			boolean r = bar.changeMana(amount, reason);
			if (r) showMana(player, showManaOnUse);
			return r;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeMana(Player player, int amount, ManaChangeReason reason) {
		return addMana(player, -amount, reason);
	}
	
	@Override
	public boolean setMana(Player player, int amount, ManaChangeReason reason) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			boolean r = bar.setMana(amount, reason);
			if (r) showMana(player, showManaOnUse);
			return r;
		} else {
			return false;
		}
	}

	@Override
	public void showMana(Player player, boolean showInChat) {
		ManaBar bar = getManaBar(player);
		if (bar != null) {
			if (showInChat) showManaInChat(player, bar);
			if (showManaOnWoodTool) showManaOnWoodTool(player, bar);
			if (showManaOnHungerBar) showManaOnHungerBar(player, bar);
			if (showManaOnExperienceBar) showManaOnExperienceBar(player, bar);
		}
	}
	
	@Override
	public ModifierSet getModifiers() {
		return modifiers;
	}
	
	private void showManaInChat(Player player, ManaBar bar) {
		int segments = (int)(((double)bar.getMana()/(double)bar.getMaxMana()) * manaBarSize);
		String text = MagicSpells.getTextColor() + bar.getPrefix() + " {" + bar.getColorFull();
		int i = 0;
		for (; i < segments; i++) {
			text += "=";
		}
		text += bar.getColorEmpty();
		for (; i < manaBarSize; i++) {
			text += "=";
		}
		text += MagicSpells.getTextColor() + "} [" + bar.getMana() + "/" + bar.getMaxMana() + "]";
		player.sendMessage(text);
	}
	
	private void showManaOnWoodTool(Player player, ManaBar bar) {
		ItemStack item = player.getInventory().getItem(manaBarToolSlot);
		if (item != null) {
			Material type = item.getType();
			if (type == Material.WOOD_AXE || type == Material.WOOD_HOE || type == Material.WOOD_PICKAXE || type == Material.WOOD_SPADE || type == Material.WOOD_SWORD) {
				int dur = 60 - (int)(((double)bar.getMana()/(double)bar.getMaxMana()) * 60);
				if (dur == 60) {
					dur = 59;
				} else if (dur == 0) {
					dur = 1;
				}
				item.setDurability((short)dur);
				player.getInventory().setItem(manaBarToolSlot, item);
			}
		}
	}
	
	private void showManaOnHungerBar(Player player, ManaBar bar) {
		player.setFoodLevel(Math.round(((float)bar.getMana()/(float)bar.getMaxMana()) * 20));
		player.setSaturation(20);
	}
	
	private void showManaOnExperienceBar(Player player, ManaBar bar) {
		MagicSpells.getExpBarManager().update(player, bar.getMana(), (float)bar.getMana()/(float)bar.getMaxMana());
	}
	
	public boolean usingExpBar() {
		return showManaOnExperienceBar;
	}

	@Override
	public void turnOff() {
		ranks.clear();
		manaBars.clear();
		if (taskId > 0) {
			Bukkit.getScheduler().cancelTask(taskId);
		}
	}
	
	public class Regenerator implements Runnable {
		@Override
		public void run() {
			for (ManaBar bar : manaBars.values()) {
				boolean r = bar.regenerate();
				if (r) {
					Player p = bar.getPlayer();
					if (p != null) {
						showMana(p, showManaOnRegen);
					}
				}
			}
		}		
	}

}
