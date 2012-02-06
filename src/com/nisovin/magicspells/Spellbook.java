package com.nisovin.magicspells;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.CastItem;

public class Spellbook {

	private MagicSpells plugin;
	
	private Player player;
	private String playerName;
	
	private TreeSet<Spell> allSpells = new TreeSet<Spell>();
	private HashMap<CastItem,ArrayList<Spell>> itemSpells = new HashMap<CastItem,ArrayList<Spell>>();
	private HashMap<CastItem,Integer> activeSpells = new HashMap<CastItem,Integer>();
	private HashMap<Spell,CastItem> customBindings = new HashMap<Spell,CastItem>();
	
	public Spellbook(Player player, MagicSpells plugin) {
		MagicSpells.debug("Loading player spell list: " + player.getName());
		this.plugin = plugin;
		this.player = player;
		this.playerName = player.getName();
		
		// load spells from file
		loadFromFile();
		
		// give all spells to ops
		if (player.isOp() && MagicSpells.opsHaveAllSpells) {
			MagicSpells.debug("  Op, granting all spells...");
			for (Spell spell : MagicSpells.spells.values()) {
				if (!allSpells.contains(spell)) {
					addSpell(spell);
				}
			}
		}
		
		// add spells granted by permissions
		addGrantedSpells();
		
		// sort spells or pre-select if just one
		for (CastItem i : itemSpells.keySet()) {
			ArrayList<Spell> spells = itemSpells.get(i);
			if (spells.size() == 1 && !MagicSpells.allowCycleToNoSpell) {
				activeSpells.put(i, 0);
			} else {
				Collections.sort(spells);
			}
		}
	}
	
	public void addGrantedSpells() {
		MagicSpells.debug("  Adding granted spells...");
		boolean added = false;
		for (Spell spell : MagicSpells.spells.values()) {
			MagicSpells.debug("    Checking spell " + spell.getInternalName() + "...");
			if (!hasSpell(spell, false)) {
				if (player.hasPermission("magicspells.grant." + spell.getInternalName())) {
					addSpell(spell);
					added = true;
				}
			}
		}
		if (added) {
			save();
		}
	}
	
	public boolean canLearn(Spell spell) {
		return player.hasPermission("magicspells.learn." + spell.getInternalName());
	}
	
	public boolean canCast(Spell spell) {
		return player.hasPermission("magicspells.cast." + spell.getInternalName());
	}
	
	public boolean canTeach(Spell spell) {
		return player.hasPermission("magicspells.teach." + spell.getInternalName());
	}
	
	public boolean hasAdvancedPerm() {
		return player.hasPermission("magicspells.advanced");
	}
	
	private void loadFromFile() {
		try {
			MagicSpells.debug("  Loading spells from player file...");
			Scanner scanner = new Scanner(new File(plugin.getDataFolder(), "spellbooks/" + playerName.toLowerCase() + ".txt"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (!line.equals("")) {
					if (!line.contains(":")) {
						Spell spell = MagicSpells.spells.get(line);
						if (spell != null) {
							addSpell(spell);
						}
					} else {
						String[] data = line.split(":",2);
						Spell spell = MagicSpells.spells.get(data[0]);
						if (spell != null && data[1].matches("^-?[0-9:]+$")) {
							addSpell(spell, new CastItem(data[1]));
						}
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
		}
	}
	
	public Spell getSpellByName(String spellName) {
		for (Spell spell : allSpells) {
			if (spell.getName().equalsIgnoreCase(spellName)) {
				return spell;
			}
		}
		return null;
	}
	
	public Set<Spell> getSpells() {
		return this.allSpells;
	}
	
	protected Spell nextSpell(ItemStack item) {
		CastItem castItem = new CastItem(item);
		Integer i = activeSpells.get(castItem); // get the index of the active spell for the cast item
		if (i != null) {
			ArrayList<Spell> spells = itemSpells.get(castItem); // get all the spells for the cast item
			if (spells.size() > 1 || i.equals(-1) || MagicSpells.allowCycleToNoSpell) {
				int count = 0;
				while (count++ < spells.size()) {
					i++;
					if (i >= spells.size()) {
						if (MagicSpells.allowCycleToNoSpell) {
							activeSpells.put(castItem, -1);
							MagicSpells.sendMessage(player, MagicSpells.strSpellChangeEmpty);
							return null;
						} else {
							i = 0;
						}
					}
					if (!MagicSpells.onlyCycleToCastableSpells || canCast(spells.get(i))) {
						activeSpells.put(castItem, i);
						return spells.get(i);
					}
				}
				return null;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	protected Spell prevSpell(ItemStack item) {
		CastItem castItem = new CastItem(item);
		Integer i = activeSpells.get(castItem); // get the index of the active spell for the cast item
		if (i != null) {
			ArrayList<Spell> spells = itemSpells.get(castItem); // get all the spells for the cast item
			if (spells.size() > 1 || i.equals(-1) || MagicSpells.allowCycleToNoSpell) {
				int count = 0;
				while (count++ < spells.size()) {
					i--;
					if (i < 0) {
						if (MagicSpells.allowCycleToNoSpell) {
							activeSpells.put(castItem, -1);
							MagicSpells.sendMessage(player, MagicSpells.strSpellChangeEmpty);
							return null;
						} else {
							i = spells.size() - 1;
						}
					}
					if (!MagicSpells.onlyCycleToCastableSpells || canCast(spells.get(i))) {
						activeSpells.put(castItem, i);
						return spells.get(i);
					}
				}
				return null;
			} else {
				return null;
			}
		} else {
			return null;
		}		
	}
	
	public Spell getActiveSpell(ItemStack item) {
		CastItem castItem = new CastItem(item);
		Integer i = activeSpells.get(castItem);
		if (i != null && i != -1) {
			return itemSpells.get(castItem).get(i);
		} else {
			return null;
		}		
	}
	
	public boolean hasSpell(Spell spell) {
		return hasSpell(spell, true);
	}
	
	public boolean hasSpell(Spell spell, boolean checkGranted) {
		boolean has = allSpells.contains(spell);
		if (has) {
			return true;
		} else if (checkGranted && player.hasPermission("magicspells.grant." + spell.getInternalName())) {
			MagicSpells.debug("Adding granted spell for " + player.getName() + ": " + spell.getName());
			addSpell(spell);
			save();
			return true;
		} else {
			return false;
		}
	}
	
	public void addSpell(Spell spell) {
		addSpell(spell, null);
	}
	
	public void addSpell(Spell spell, CastItem castItem) {
		MagicSpells.debug("    Added spell: " + spell.getInternalName());
		allSpells.add(spell);
		if (spell.canCastWithItem()) {
			CastItem item = spell.getCastItem();
			if (castItem != null) {
				item = castItem;
				customBindings.put(spell, castItem);
			} else if (MagicSpells.ignoreDefaultBindings) {
				return; // no cast item provided and ignoring default, so just stop here
			}
			MagicSpells.debug("        Cast item: " + item + (castItem!=null?" (custom)":" (default)"));
			ArrayList<Spell> temp = itemSpells.get(item);
			if (temp != null) {
				temp.add(spell);
			} else {
				temp = new ArrayList<Spell>();
				temp.add(spell);
				itemSpells.put(item, temp);
				activeSpells.put(item, -1);
			}
		}
	}
	
	public void removeSpell(Spell spell) {
		if (spell instanceof BuffSpell) {
			((BuffSpell)spell).turnOff(player);
		}
		CastItem item = spell.getCastItem();
		if (customBindings.containsKey(spell)) {
			item = customBindings.remove(spell);
		}
		ArrayList<Spell> temp = itemSpells.get(item);
		if (temp != null) {
			temp.remove(spell);
			if (temp.size() == 0) {
				itemSpells.remove(item);
				activeSpells.remove(item);
			} else {
				activeSpells.put(item, -1);
			}
		}
		allSpells.remove(spell);
	}
	
	public void removeAllSpells() {
		for (Spell spell : allSpells) {
			if (spell instanceof BuffSpell) {
				((BuffSpell)spell).turnOff(player);
			}
		}
		allSpells.clear();
		itemSpells.clear();
		activeSpells.clear();
		customBindings.clear();
	}
	
	public void save() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(plugin.getDataFolder(), "spellbooks/" + playerName.toLowerCase() + ".txt"), false));
			for (Spell spell : allSpells) {
				writer.append(spell.getInternalName());
				if (customBindings.containsKey(spell)) {
					writer.append(":" + customBindings.get(spell));
				}
				writer.newLine();
			}
			writer.close();
			MagicSpells.debug("Saved spellbook file: " + playerName.toLowerCase());
		} catch (Exception e) {
			plugin.getServer().getLogger().severe("Error saving player spellbook: " + playerName);
		}		
	}
	
}
