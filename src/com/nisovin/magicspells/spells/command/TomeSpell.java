package com.nisovin.magicspells.spells.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class TomeSpell extends CommandSpell {

	private boolean cancelReadOnLearn;
	private boolean consumeBook;
	private boolean allowOverwrite;
	private int defaultUses;
	private int maxUses;
	private boolean requireTeachPerm;
	private String strUsage;
	private String strNoSpell;
	private String strCantTeach;
	private String strNoBook;
	private String strAlreadyHasSpell;
	private String strAlreadyKnown;
	private String strCantLearn;
	private String strLearned;
	
	public TomeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		cancelReadOnLearn = getConfigBoolean("cancel-read-on-learn", true);
		consumeBook = getConfigBoolean("consume-book", false);
		allowOverwrite = getConfigBoolean("allow-overwrite", false);
		defaultUses = getConfigInt("default-uses", -1);
		maxUses = getConfigInt("max-uses", 5);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		strUsage = getConfigString("str-usage", "Usage: While holding a book, /cast " + name + " <spell> [uses]");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell with that name.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a tome with that spell.");
		strNoBook = getConfigString("str-no-book", "You must be holding a book.");
		strAlreadyHasSpell = getConfigString("str-already-has-spell", "That book already contains a spell.");
		strAlreadyKnown = getConfigString("str-already-known", "You already know the %s spell.");
		strCantLearn = getConfigString("str-cant-learn", "You cannot learn the spell in this tome.");
		strLearned = getConfigString("str-learned", "You have learned the %s spell.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Spell spell;
			if (args == null || args.length == 0) {
				// fail -- no args
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				spell = MagicSpells.getSpellByInGameName(args[0]);
				if (spell == null || spellbook == null || !spellbook.hasSpell(spell)) {
					// fail -- no spell
					sendMessage(player, strNoSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else if (requireTeachPerm && !MagicSpells.getSpellbook(player).canTeach(spell)) {
					sendMessage(player, strCantTeach);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			
			ItemStack item = player.getItemInHand();
			if (item.getType() != Material.WRITTEN_BOOK) {
				// fail -- no book
				sendMessage(player, strNoBook);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			if (!allowOverwrite && getSpellDataFromTome(item) != null) {
				// fail -- already has a spell
				sendMessage(player, strAlreadyHasSpell);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				int uses = defaultUses;
				if (args.length > 1 && args[1].matches("^[0-9]+$")) {
					uses = Integer.parseInt(args[1]);
				}
				item = createTome(spell, uses, item);
				player.setItemInHand(item);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public ItemStack createTome(Spell spell, int uses, ItemStack item) {
		if (maxUses > 0 && uses > maxUses) {
			uses = maxUses;
		} else if (uses < 0) {
			uses = defaultUses;
		}
		if (item == null) {
			item = new ItemStack(Material.WRITTEN_BOOK, 1);
			BookMeta bookMeta = (BookMeta)item.getItemMeta();
			bookMeta.setTitle(getName() + ": " + spell.getName());
			item.setItemMeta(bookMeta);
		}
		Util.setLoreData(item, internalName + ":" + spell.getInternalName() + (uses>0?","+uses:""));
		return item;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	private String getSpellDataFromTome(ItemStack item) {
		String loreData = Util.getLoreData(item);
		if (loreData != null && loreData.startsWith(internalName + ":")) {
			return loreData.replace(internalName + ":", "");
		}
		return null;
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;
		ItemStack item = event.getItem();
		if (item.getType() != Material.WRITTEN_BOOK) return;
		
		String spellData = getSpellDataFromTome(item);
		if (spellData == null || spellData.isEmpty()) return;
		
		String[] data = spellData.split(",");
		Spell spell = MagicSpells.getSpellByInternalName(data[0]);
		int uses = -1;
		if (data.length > 1) {
			uses = Integer.parseInt(data[1]);
		}
		Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
		if (spell != null && spellbook != null) {
			if (spellbook.hasSpell(spell)) {
				// fail -- already known
				sendMessage(event.getPlayer(), formatMessage(strAlreadyKnown, "%s", spell.getName()));
			} else if (!spellbook.canLearn(spell)) {
				// fail -- can't learn
				sendMessage(event.getPlayer(), formatMessage(strCantLearn, "%s", spell.getName()));
			} else {
				// call event
				SpellLearnEvent learnEvent = new SpellLearnEvent(spell, event.getPlayer(), LearnSource.TOME, event.getPlayer().getItemInHand());
				Bukkit.getPluginManager().callEvent(learnEvent);
				if (learnEvent.isCancelled()) {
					// fail -- plugin cancelled
					sendMessage(event.getPlayer(), formatMessage(strCantLearn, "%s", spell.getName()));
				} else {
					// give spell
					spellbook.addSpell(spell);
					spellbook.save();
					sendMessage(event.getPlayer(), formatMessage(strLearned, "%s", spell.getName()));
					if (cancelReadOnLearn) {
						event.setCancelled(true);
					}
					// remove use
					if (uses > 0) {
						uses--;
						if (uses > 0) {
							Util.setLoreData(item, internalName + ":" + data[0] + "," + uses);
						} else {
							Util.removeLoreData(item);
						}
					}
					// consume
					if (uses <= 0 && consumeBook) {
						event.getPlayer().setItemInHand(null);
					}
					playSpellEffects(EffectPosition.DELAYED, event.getPlayer());
				}
			}
		}
	}

}
