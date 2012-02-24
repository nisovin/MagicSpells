package com.nisovin.magicspells.spells.command;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ScrollSpell extends CommandSpell {

	private boolean castForFree;
	private int defaultUses;
	private int maxUses;
	private int itemId;
	private boolean rightClickCast;
	private boolean leftClickCast;
	private boolean ignoreCastPerm;
	private boolean setUnstackable;
	private boolean chargeReagentsForSpellPerCharge;
	private boolean requireTeachPerm;
	private boolean requireScrollCastPermOnUse;
	private String stackByDataVar;
	private int maxScrolls;
	private String strScrollOver;
	private String strUsage;
	private String strFail;
	private String strNoSpell;
	private String strCantTeach;
	private String strOnUse;
	private String strUseFail;
	
	private HashMap<Short,Spell> scrollSpells;
	private HashMap<Short,Integer> scrollUses;
	private boolean dirtyData;
	
	public ScrollSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castForFree = getConfigBoolean("cast-for-free", true);
		defaultUses = getConfigInt("default-uses", 5);
		maxUses = getConfigInt("max-uses", 10);
		itemId = getConfigInt("item-id", Material.PAPER.getId());
		rightClickCast = getConfigBoolean("right-click-cast", true);
		leftClickCast = getConfigBoolean("left-click-cast", false);
		ignoreCastPerm = getConfigBoolean("ignore-cast-perm", false);
		setUnstackable = getConfigBoolean("set-unstackable", true);
		chargeReagentsForSpellPerCharge = getConfigBoolean("charge-reagents-for-spell-per-charge", false);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		requireScrollCastPermOnUse = getConfigBoolean("require-scroll-cast-perm-on-use", true);
		stackByDataVar = getConfigString("stack-by-data-var", "bQ");
		maxScrolls = getConfigInt("max-scrolls", 500);
		strScrollOver = getConfigString("str-scroll-over", "Spell Scroll: %s (%u uses remaining)");
		strUsage = getConfigString("str-usage", "You must hold a single blank paper \nand type /cast scroll <spell> <uses>.");
		strFail = getConfigString("str-fail", "You cannot create a spell scroll at this time.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a scroll with that spell.");
		strOnUse = getConfigString("str-on-use", "Spell Scroll: %s used. %u uses remaining.");
		strUseFail = getConfigString("str-use-fail", "Unable to use this scroll right now.");
		
		scrollSpells = new HashMap<Short,Spell>();
		scrollUses = new HashMap<Short,Integer>();
		
		// prevent paper stacking
		if (setUnstackable) {
			MagicSpells.craftbukkit.stackByData(itemId, stackByDataVar);
		}
	}
	
	@Override
	protected void initialize() {
		load();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length == 0) {
				// fail -- no args
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			} 
			
			// check for base scroll
			if (MagicSpells.getSpellbook(player).hasAdvancedPerm() && args[0].equalsIgnoreCase("-base")) {
				createBaseScroll(args, player);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get item in hand
			ItemStack inHand = player.getItemInHand();
			short id = inHand.getDurability();
			if (inHand.getTypeId() != itemId || inHand.getAmount() != 1 || scrollSpells.containsKey(id)) {
				// fail -- incorrect item in hand
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get scroll id
			if (id == 0) {
				id = getNextId();
				if (id == 0) {
					// fail -- no more scroll space
					sendMessage(player, strFail);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			
			// get spell
			Spell spell = MagicSpells.getSpellByInGameName(args[0]);
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			if (spell == null || spellbook == null || !spellbook.hasSpell(spell)) {
				// fail -- no such spell
				sendMessage(player, strNoSpell);
				return PostCastAction.ALREADY_HANDLED;			
			} else if (requireTeachPerm && !spellbook.canTeach(spell)) {
				sendMessage(player, strCantTeach);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get uses
			int uses = defaultUses;
			if (args.length > 1 && args[1].matches("^-?[0-9]+$")) {
				uses = Integer.parseInt(args[1]);
			}
			if (uses > maxUses || (maxUses > 0 && uses < 0)) {
				uses = maxUses;
			}
			
			// get additional reagent cost
			if (chargeReagentsForSpellPerCharge && uses > 0) {
				ItemStack[] spellReagents = spell.getReagentCost();
				ItemStack[] reagents = new ItemStack[spellReagents.length];
				for (int i = 0; i < reagents.length; i++) {
					ItemStack item = spellReagents[i];
					if (item != null) {
						item = item.clone();
						item.setAmount(item.getAmount() * uses);
					}
					reagents[i] = item;
				}
				int manaCost = spell.getManaCost() * uses;
				int healthCost = spell.getHealthCost() * uses;
				int hungerCost = spell.getHungerCost() * uses;
				int experienceCost = spell.getExperienceCost() * uses;
				int levelsCost = spell.getLevelsCost() * uses;
				if (!hasReagents(player, reagents, healthCost, manaCost, hungerCost, experienceCost, levelsCost)) {
					// missing reagents
					sendMessage(player, MagicSpells.strMissingReagents);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					// has reagents, so just remove them
					removeReagents(player, reagents, healthCost, manaCost, hungerCost, experienceCost, levelsCost);
				}
			}
			
			// create scroll
			inHand.setDurability(id);
			player.setItemInHand(inHand);
			scrollSpells.put(id, spell);
			scrollUses.put(id, uses);
			dirtyData = true;
			
			// done
			sendMessage(player, formatMessage(strCastSelf, "%s", spell.getName()));
			save();
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public short createScroll(Spell spell, int uses) {
		short id = getNextId();
		if (id == 0) {
			// no more scroll space
			return 0;
		}		

		scrollSpells.put(id, spell);
		scrollUses.put(id, uses);
		dirtyData = true;
		save();
		
		return id;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length >= 2 && (args[0].equalsIgnoreCase("base") || args[0].equalsIgnoreCase("-base"))) {
			createBaseScroll(args, sender);
		} else if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
			save();
			sender.sendMessage("Scrolls saved");
		} else if (args.length == 1 && args[0].equalsIgnoreCase("load")) {
			load();
			sender.sendMessage("Scrolls loaded");
		}
		return true;
	}
	
	private void createBaseScroll(String[] args, CommandSender sender) {
		// get spell
		Spell spell = MagicSpells.getSpellByInGameName(args[1]);
		if (spell == null) {
			sender.sendMessage("No such spell");
		}
		
		// get uses
		int uses = defaultUses;
		if (args.length > 2 && args[2].matches("^-?[0-9]+$")) {
			uses = Integer.parseInt(args[2]);
		}
		
		// get id
		short id = getNextNegativeId();
		if (id == 0) {
			sender.sendMessage("Out of scroll space");
		}
		
		// create scroll
		scrollSpells.put(id, spell);
		scrollUses.put(id, uses);
		dirtyData = true;
		save();
		
		// set paper
		if (sender instanceof Player) {
			Player player = (Player)sender;
			ItemStack inHand = player.getItemInHand();
			if (inHand.getTypeId() == itemId && inHand.getAmount() == 1 && inHand.getDurability() == 0) {
				inHand.setDurability(id);
				player.setItemInHand(inHand);
			}
		}
		
		sender.sendMessage("Base scroll created for spell " + spell.getName() + ": id = " + id);
	}
	
	private short getNextId() {
		for (short i = 1; i < maxScrolls; i++) {
			if (!scrollSpells.containsKey(i)) {
				return i;
			}
		}
		return 0;
	}
	
	private short getNextNegativeId() {
		for (short i = -1; i > -maxScrolls; i--) {
			if (!scrollSpells.containsKey(i)) {
				return i;
			}
		}
		return 0;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if ((rightClickCast && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) ||
			(leftClickCast && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK))) {
			Player player = event.getPlayer();
			ItemStack inHand = player.getItemInHand();
			if (inHand.getTypeId() == itemId && inHand.getDurability() != 0) {		
				short id = inHand.getDurability();
				Spell spell = scrollSpells.get(id);
			
				if (spell != null) {
					// check for permission
					if (requireScrollCastPermOnUse && !MagicSpells.getSpellbook(player).canCast(this)) {
						sendMessage(player, strUseFail);
						return;
					}
					
					// check for base scroll
					if (id < 0) {
						// make a copy of the base scroll
						short newId = getNextId();
						if (newId == 0) {
							// fail -- no more scroll space
							sendMessage(player, strFail);
							return;
						}
						inHand.setDurability(newId);
						player.setItemInHand(inHand);
						scrollSpells.put(newId, spell);
						scrollUses.put(newId, scrollUses.get(id));
						id = newId;
						dirtyData = true;
					}

					// cast spell
					if (ignoreCastPerm && !player.hasPermission("magicspells.cast." + spell.getInternalName())) {
						player.addAttachment(MagicSpells.plugin, "magicspells.cast." + spell.getInternalName(), true, 1);
					}
					if (castForFree && !player.hasPermission("magicspells.noreagents")) {
						player.addAttachment(MagicSpells.plugin, "magicspells.noreagents", true, 1);
					}
					SpellCastResult result = spell.cast(player);

					if (result.state == SpellCastState.NORMAL && result.action != PostCastAction.ALREADY_HANDLED) {
						// remove use
						int uses = scrollUses.get(id);
						if (uses > 0) {
							uses -= 1;
							if (uses > 0) {
								scrollUses.put(id, uses);
							} else {
								scrollSpells.remove(id);
								scrollUses.remove(id);
								player.setItemInHand(null);
							}
							dirtyData = true;
						}
						
						// send msg
						sendMessage(player, formatMessage(strOnUse, "%s", spell.getName(), "%u", (uses>=0?uses+"":"many")));
					}
				}
			}		
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		ItemStack inHand = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (inHand != null && inHand.getTypeId() == itemId && inHand.getDurability() != 0) {
			Spell spell = scrollSpells.get(inHand.getDurability());
			if (spell != null) {
				sendMessage(event.getPlayer(), formatMessage(strScrollOver, "%s", spell.getName(), "%u", scrollUses.get(inHand.getDurability())+""));
			}
		}
	}
	
	public Spell getSpellScrollById(short id) {
		if (scrollSpells.containsKey(id)) {
			return scrollSpells.get(id);
		} else {
			return null;
		}
	}
	
	@Override
	protected void turnOff() {
		save();
	}
	
	private void save() {
		if (dirtyData) {
			MagicSpells.debug("Saving scrolls...");
			File file = new File(MagicSpells.plugin.getDataFolder(), "scrolls.txt");
			if (file.exists()) {
				file.delete();
			}
			YamlConfiguration c = new YamlConfiguration();
			String data;
			for (short i : scrollSpells.keySet()) {
				data = scrollSpells.get(i).getInternalName() + "|" + scrollUses.get(i);
				MagicSpells.debug("    " + i + " : " + data);
				c.set(i+"", data);
			}
			try {
				c.save(file);
			} catch (IOException e) {
				System.out.println("MagicSpells: Error: Failed to save scrolls");
			}
		}
	}
	
	private void load() {
		File file = new File(MagicSpells.plugin.getDataFolder(), "scrolls.txt");
		if (file.exists()) {
			MagicSpells.debug("Loading scrolls...");
			YamlConfiguration c = new YamlConfiguration();
			try {
				c.load(file);
			} catch (Exception e) {
				System.out.println("MagicSpells: Error: Failed to load scrolls");
				return;
			}
			Set<String> keys = c.getKeys(false);
			for (String s : keys) {
				short id = Short.parseShort(s);
				String[] data = c.getString(s).split("\\|");
				MagicSpells.debug("    Raw data: " + c.getString(s));
				Spell spell = MagicSpells.getSpellByInternalName(data[0]);
				int uses = Integer.parseInt(data[1]);
				if (spell != null) {
					scrollSpells.put(id, spell);
					scrollUses.put(id, uses);
					MagicSpells.debug("        Loaded scroll: " + id + " - " + spell.getInternalName() + " - " + uses);
				}
			}
			dirtyData = false;
		}
	}

}
