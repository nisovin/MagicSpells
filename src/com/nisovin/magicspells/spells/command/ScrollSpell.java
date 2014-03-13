package com.nisovin.magicspells.spells.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.util.Util;

public class ScrollSpell extends CommandSpell {

	private boolean castForFree;
	private boolean ignoreCastPerm;
	private boolean bypassNormalChecks;
	private int defaultUses;
	private int maxUses;
	private MagicMaterial itemType;
	private boolean rightClickCast;
	private boolean leftClickCast;
	private boolean removeScrollWhenDepleted;
	private boolean chargeReagentsForSpellPerCharge;
	private boolean requireTeachPerm;
	private boolean requireScrollCastPermOnUse;
	private boolean textContainsUses;
	private String strScrollName;
	private String strScrollSubtext;
	private String strUsage;
	private String strNoSpell;
	private String strCantTeach;
	private String strOnUse;
	private String strUseFail;
	
	private List<String> predefinedScrolls;
	private Map<Integer, Spell> predefinedScrollSpells;
	private Map<Integer, Integer> predefinedScrollUses;
		
	public ScrollSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castForFree = getConfigBoolean("cast-for-free", true);
		ignoreCastPerm = getConfigBoolean("ignore-cast-perm", false);
		bypassNormalChecks = getConfigBoolean("bypass-normal-checks", false);
		defaultUses = getConfigInt("default-uses", 5);
		maxUses = getConfigInt("max-uses", 10);
		itemType = MagicSpells.getItemNameResolver().resolveItem(getConfigString("item-id", "paper"));
		rightClickCast = getConfigBoolean("right-click-cast", true);
		leftClickCast = getConfigBoolean("left-click-cast", false);
		removeScrollWhenDepleted = getConfigBoolean("remove-scroll-when-depleted", true);
		chargeReagentsForSpellPerCharge = getConfigBoolean("charge-reagents-for-spell-per-charge", false);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		requireScrollCastPermOnUse = getConfigBoolean("require-scroll-cast-perm-on-use", true);
		strScrollName = getConfigString("str-scroll-name", "Magic Scroll: %s");
		strScrollSubtext = getConfigString("str-scroll-subtext", "Uses remaining: %u");
		strUsage = getConfigString("str-usage", "You must hold a single blank paper \nand type /cast scroll <spell> <uses>.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a scroll with that spell.");
		strOnUse = getConfigString("str-on-use", "Spell Scroll: %s used. %u uses remaining.");
		strUseFail = getConfigString("str-use-fail", "Unable to use this scroll right now.");
		
		predefinedScrolls = getConfigStringList("predefined-scrolls", null);
		
		textContainsUses = strScrollName.contains("%u") || strScrollSubtext.contains("%u");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (predefinedScrolls != null && predefinedScrolls.size() > 0) {
			predefinedScrollSpells = new HashMap<Integer, Spell>();
			predefinedScrollUses = new HashMap<Integer, Integer>();
			for (String s : predefinedScrolls) {
				String[] data = s.split(" ");
				try {
					int id = Integer.parseInt(data[0]);
					Spell spell = MagicSpells.getSpellByInternalName(data[1]);
					int uses = defaultUses;
					if (data.length > 2) uses = Integer.parseInt(data[2]);
					if (id > 0 && spell != null) {
						predefinedScrollSpells.put(id, spell);
						predefinedScrollUses.put(id, uses);
					} else {
						MagicSpells.error("Scroll spell has invalid predefined scroll: " + s);
					}
				} catch (Exception e) {
					MagicSpells.error("Scroll spell has invalid predefined scroll: " + s);
				}
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length == 0) {
				// fail -- no args
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get item in hand
			ItemStack inHand = player.getItemInHand();
			if (inHand.getAmount() != 1 || !itemType.equals(inHand)) {
				// fail -- incorrect item in hand
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
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
			if (uses > maxUses || (maxUses > 0 && uses <= 0)) {
				uses = maxUses;
			}
			
			// get additional reagent cost
			if (chargeReagentsForSpellPerCharge && uses > 0) {
				SpellReagents reagents = spell.getReagents().multiply(uses);
				if (!hasReagents(player, reagents)) {
					// missing reagents
					sendMessage(player, strMissingReagents);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					// has reagents, so just remove them
					removeReagents(player, reagents);
				}
			}
			
			// create scroll
			inHand = createScroll(spell, uses, inHand);
			player.setItemInHand(inHand);
			
			// done
			sendMessage(player, formatMessage(strCastSelf, "%s", spell.getName()));
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public ItemStack createScroll(Spell spell, int uses, ItemStack item) {
		if (item == null) {
			item = itemType.toItemStack(1);
		}
		item.setDurability((short)0);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', strScrollName.replace("%s", spell.getName()).replace("%u", (uses>=0?uses+"":"many"))));
		if (strScrollSubtext != null && !strScrollSubtext.isEmpty()) {
			List<String> lore = new ArrayList<String>();
			lore.add(ChatColor.translateAlternateColorCodes('&', strScrollSubtext.replace("%s", spell.getName()).replace("%u", (uses>=0?uses+"":"many"))));
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		Util.setLoreData(item, internalName + ":" + spell.getInternalName() + (uses > 0 ? "," + uses : ""));
		item = MagicSpells.getVolatileCodeHandler().addFakeEnchantment(item);
		return item;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) {
			return tabCompleteSpellName(sender, args[0]);
		} else {
			return null;
		}
	}
	
	private String getSpellDataFromScroll(ItemStack item) {
		String loreData = Util.getLoreData(item);
		if (loreData != null && loreData.startsWith(internalName + ":")) {
			return loreData.replace(internalName + ":", "");
		}
		return null;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if ((rightClickCast && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) ||
			(leftClickCast && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK))) {
			Player player = event.getPlayer();
			ItemStack inHand = player.getItemInHand();
			if (itemType.getMaterial() != inHand.getType() || inHand.getAmount() > 1) return;
			
			// check for predefined scroll
			if (inHand.getDurability() > 0 && predefinedScrollSpells != null) {
				Spell spell = predefinedScrollSpells.get(Integer.valueOf(inHand.getDurability()));
				if (spell != null) {
					int uses = predefinedScrollUses.get(Integer.valueOf(inHand.getDurability()));
					inHand = createScroll(spell, uses, inHand);
					player.setItemInHand(inHand);
				}
			}
			
			// get scroll data (spell and uses)
			String scrollDataString = getSpellDataFromScroll(inHand);
			if (scrollDataString == null || scrollDataString.isEmpty()) return;
			String[] scrollData = scrollDataString.split(",");
			Spell spell = MagicSpells.getSpellByInternalName(scrollData[0]);
			if (spell == null) return;
			int uses = 0;
			if (scrollData.length > 1 && scrollData[1].matches("^[0-9]+$")) {
				uses = Integer.parseInt(scrollData[1]);
			}			
	
			// check for permission
			if (requireScrollCastPermOnUse && !MagicSpells.getSpellbook(player).canCast(this)) {
				sendMessage(player, strUseFail);
				return;
			}
					
			
			// cast spell
			if (ignoreCastPerm && !player.hasPermission("magicspells.cast." + spell.getInternalName())) {
				player.addAttachment(MagicSpells.plugin, "magicspells.cast." + spell.getInternalName(), true, 1);
			}
			if (castForFree && !player.hasPermission("magicspells.noreagents")) {
				player.addAttachment(MagicSpells.plugin, "magicspells.noreagents", true, 1);
			}
			SpellCastState state;
			PostCastAction action;
			if (bypassNormalChecks) {
				state = SpellCastState.NORMAL;
				action = spell.castSpell(player, SpellCastState.NORMAL, 1.0F, null);
			} else {
				SpellCastResult result = spell.cast(player);
				state = result.state;
				action = result.action;
			}

			if (state == SpellCastState.NORMAL && action != PostCastAction.ALREADY_HANDLED) {
				// remove use
				if (uses > 0) {
					uses -= 1;
					if (uses > 0) {
						inHand = createScroll(spell, uses, inHand);
						if (textContainsUses) {
							player.setItemInHand(inHand);
						}
					} else {
						if (removeScrollWhenDepleted) {
							player.setItemInHand(null);
						} else {
							player.setItemInHand(itemType.toItemStack(1));
						}
					}
				}
				
				// send msg
				sendMessage(player, formatMessage(strOnUse, "%s", spell.getName(), "%u", (uses>=0?uses+"":"many")));
			}
		}
	}
	
	@EventHandler
	public void onItemSwitch(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack inHand = player.getInventory().getItem(event.getNewSlot());
		
		if (inHand == null || inHand.getType() != itemType.getMaterial()) return;
		
		// check for predefined scroll
		if (inHand.getDurability() > 0 && predefinedScrollSpells != null) {
			Spell spell = predefinedScrollSpells.get(Integer.valueOf(inHand.getDurability()));
			if (spell != null) {
				int uses = predefinedScrollUses.get(Integer.valueOf(inHand.getDurability()));
				inHand = createScroll(spell, uses, inHand);
				player.getInventory().setItem(event.getNewSlot(), inHand);
			}
		}
	}

}
