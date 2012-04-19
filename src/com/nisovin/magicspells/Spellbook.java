package com.nisovin.magicspells;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.World;
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
	private HashMap<Spell,Set<CastItem>> customBindings = new HashMap<Spell,Set<CastItem>>();
	
	public Spellbook(Player player, MagicSpells plugin) {
		this.plugin = plugin;
		this.player = player;
		this.playerName = player.getName();

		MagicSpells.debug(1, "Loading player spell list: " + playerName);
		load();
	}
	
	public void load() {
		load(player.getWorld());
	}
	
	public void load(World playerWorld) {
		// load spells from file
		loadFromFile(playerWorld);
		
		// give all spells to ops
		if (player.isOp() && MagicSpells.opsHaveAllSpells) {
			MagicSpells.debug(2, "  Op, granting all spells...");
			for (Spell spell : MagicSpells.spellsOrdered) {
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
	
	private void loadFromFile(World playerWorld) {
		try {
			MagicSpells.debug(2, "  Loading spells from player file...");
			File file;
			if (MagicSpells.separatePlayerSpellsPerWorld) {
				File folder = new File(plugin.getDataFolder(), "spellbooks" + File.separator + player.getWorld().getName());
				if (!folder.exists()) {
					folder.mkdir();
				}
				file = new File(plugin.getDataFolder(), "spellbooks" + File.separator + playerWorld.getName() + File.separator + playerName.toLowerCase() + ".txt");
			} else {
				file = new File(plugin.getDataFolder(), "spellbooks" + File.separator + playerName.toLowerCase() + ".txt");
			}
			Scanner scanner = new Scanner(file);
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
						if (spell != null) {
							CastItem[] items = null;
							if (data[1].matches("^(-?[0-9]+(:-?[0-9]+)?)(,(-?[0-9]+(:-?[0-9]+)?))*$")) { // ^(-?[0-9:]+)(,(-?[0-9:]+))*$
								String[] s = data[1].split(",");
								items = new CastItem[s.length];
								for (int i = 0; i < s.length; i++) {
									items[i] = new CastItem(s[i]);
								}
							}
							addSpell(spell, items);
						}
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
		}
	}
	
	public void addGrantedSpells() {
		MagicSpells.debug(2, "  Adding granted spells...");
		boolean added = false;
		for (Spell spell : MagicSpells.spellsOrdered) {
			MagicSpells.debug(3, "    Checking spell " + spell.getInternalName() + "...");
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
		if (spell.prerequisites != null) {
			for (String spellName : spell.prerequisites) {
				Spell sp = MagicSpells.getSpellByInternalName(spellName);
				if (sp == null || !hasSpell(sp)) {
					return false;
				}
			}
		}
		return player.hasPermission("magicspells.learn." + spell.getInternalName());
	}
	
	public boolean canCast(Spell spell) {
		return player.hasPermission("magicspells.cast." + spell.getInternalName());
	}
	
	public boolean canTeach(Spell spell) {
		return player.hasPermission("magicspells.teach." + spell.getInternalName());
	}
	
	public boolean hasAdvancedPerm(String spell) {
		return player.hasPermission("magicspells.advanced." + spell);
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
		CastItem castItem;
		if (item != null) {
			castItem = new CastItem(item);
		} else {
			castItem = new CastItem(0);
		}
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
		CastItem castItem;
		if (item != null) {
			castItem = new CastItem(item);
		} else {
			castItem = new CastItem(0);
		}
		Integer i = activeSpells.get(castItem); // get the index of the active spell for the cast item
		if (i != null) {
			ArrayList<Spell> spells = itemSpells.get(castItem); // get all the spells for the cast item
			if (spells.size() > 1 || i.equals(-1) || MagicSpells.allowCycleToNoSpell) {
				int count = 0;
				while (count++ < spells.size()) {
					i--;
					if (i < 0) {
						if (MagicSpells.allowCycleToNoSpell && i == -1) {
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
			MagicSpells.debug(2, "Adding granted spell for " + player.getName() + ": " + spell.getName());
			addSpell(spell);
			save();
			return true;
		} else {
			return false;
		}
	}
	
	public void addSpell(Spell spell) {
		addSpell(spell, (CastItem[])null);
	}
	
	public void addSpell(Spell spell, CastItem castItem) {
		addSpell(spell, new CastItem[] {castItem});
	}
	
	public void addSpell(Spell spell, CastItem[] castItems) {
		if (spell == null) return;
		MagicSpells.debug(3, "    Added spell: " + spell.getInternalName());
		allSpells.add(spell);
		if (spell.canCastWithItem()) {
			CastItem[] items = spell.getCastItems();
			if (castItems != null && castItems.length > 0) {
				items = castItems;
				customBindings.put(spell, new HashSet<CastItem>(Arrays.asList(items)));
			} else if (MagicSpells.ignoreDefaultBindings) {
				return; // no cast item provided and ignoring default, so just stop here
			}
			for (CastItem i : items) {
				MagicSpells.debug(3, "        Cast item: " + i + (castItems!=null?" (custom)":" (default)"));
			}
			for (CastItem i : items) {
				ArrayList<Spell> temp = itemSpells.get(i);
				if (temp != null) {
					temp.add(spell);
				} else {
					temp = new ArrayList<Spell>();
					temp.add(spell);
					itemSpells.put(i, temp);
					activeSpells.put(i, MagicSpells.allowCycleToNoSpell ? -1 : 0);
				}
			}
		}
		// remove any spells that this spell replaces
		if (spell.replaces != null) {
			for (String spellName : spell.replaces) {
				Spell sp = MagicSpells.getSpellByInternalName(spellName);
				if (sp != null) {
					MagicSpells.debug(3, "        Removing replaced spell: " + sp.getInternalName());
					removeSpell(sp);
				}
			}
		}
	}
	
	public void removeSpell(Spell spell) {
		if (spell instanceof BuffSpell) {
			((BuffSpell)spell).turnOff(player);
		}
		CastItem[] items = spell.getCastItems();
		if (customBindings.containsKey(spell)) {
			items = customBindings.remove(spell).toArray(new CastItem[]{});
		}
		for (CastItem item : items) {
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
		}
		allSpells.remove(spell);
	}
	
	public void addCastItem(Spell spell, CastItem castItem) {
		// add to custom bindings
		Set<CastItem> bindings = customBindings.get(spell);
		if (bindings == null) {
			bindings = new HashSet<CastItem>();
			customBindings.put(spell, bindings);
		}
		if (!bindings.contains(castItem)) {
			bindings.add(castItem);
		}
		
		// add to item bindings
		ArrayList<Spell> bindList = itemSpells.get(castItem);
		if (bindList == null) {
			bindList = new ArrayList<Spell>();
			itemSpells.put(castItem, bindList);
			activeSpells.put(castItem, MagicSpells.allowCycleToNoSpell ? -1 : 0);
		}
		bindList.add(spell);
	}
	
	public boolean removeCastItem(Spell spell, CastItem castItem) {
		boolean removed = false;
		
		// remove from custom bindings
		Set<CastItem> bindings = customBindings.get(spell);
		if (bindings != null) {
			removed = bindings.remove(castItem);
			if (bindings.size() == 0) {
				bindings.add(new CastItem(-1));
			}
		}
		
		// remove from active bindings
		ArrayList<Spell> bindList = itemSpells.get(castItem);
		if (bindList != null) {
			removed = bindList.remove(spell) || removed;
			if (bindList.size() == 0) {
				itemSpells.remove(castItem);
				activeSpells.remove(castItem);
			} else {
				activeSpells.put(castItem, -1);
			}
		}
		
		return removed;
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
	
	public void reload() {
		MagicSpells.debug(1, "Reloading player spell list: " + playerName);
		removeAllSpells();
		load();
	}
	
	public void save() {
		try {
			File file;
			if (MagicSpells.separatePlayerSpellsPerWorld) {
				File folder = new File(plugin.getDataFolder(), "spellbooks" + File.separator + player.getWorld().getName());
				if (!folder.exists()) {
					folder.mkdir();
				}
				file = new File(plugin.getDataFolder(), "spellbooks" + File.separator + player.getWorld().getName() + File.separator + playerName.toLowerCase() + ".txt");
			} else {
				file = new File(plugin.getDataFolder(), "spellbooks" + File.separator + playerName.toLowerCase() + ".txt");
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
			for (Spell spell : allSpells) {
				writer.append(spell.getInternalName());
				if (customBindings.containsKey(spell)) {
					Set<CastItem> items = customBindings.get(spell);
					String s = "";
					for (CastItem i : items) {
						s += (s.isEmpty()?"":",") + i;
					}
					writer.append(":" + s);
				}
				writer.newLine();
			}
			writer.close();
			MagicSpells.debug(2, "Saved spellbook file: " + playerName.toLowerCase());
		} catch (Exception e) {
			plugin.getServer().getLogger().severe("Error saving player spellbook: " + playerName);
		}		
	}
	
}
