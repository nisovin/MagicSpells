package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.mana.ManaChangeReason;

public class CastListener implements Listener {

	MagicSpells plugin;
	
	private HashSet<Player> noCast = new HashSet<Player>();
	private HashMap<Player,Long> lastCast = new HashMap<Player, Long>();

	public CastListener(MagicSpells plugin) {
		this.plugin = plugin;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		
		// first check if player is interacting with a special block
		boolean noInteract = false;
		if (event.hasBlock()) {
			Material m = event.getClickedBlock().getType();
			if (m == Material.WOODEN_DOOR || 
					m == Material.BED || 
					m == Material.WORKBENCH ||
					m == Material.CHEST || 
					m == Material.FURNACE || 
					m == Material.LEVER ||
					m == Material.STONE_BUTTON ||
					m == Material.ENCHANTMENT_TABLE) {
				noInteract = true;
			}
			if (m == Material.ENCHANTMENT_TABLE) {
				// force exp bar back to show exp when trying to enchant
				MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());
			}
		}
		if (noInteract) {
			// special block -- don't do normal interactions
			noCast.add(event.getPlayer());
		} else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// left click - cast
			if (!MagicSpells.castOnAnimate) {
				castSpell(event.getPlayer());
			}
		} else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			// right click -- cycle spell and/or process mana pots
			ItemStack inHand = player.getItemInHand();
			
			if ((inHand != null && inHand.getType() != Material.AIR) || MagicSpells.allowCastWithFist) {
			
				// cycle spell
				Spell spell = null;
				if (!player.isSneaking()) {
					spell = MagicSpells.getSpellbook(player).nextSpell(inHand);
				} else {
					spell = MagicSpells.getSpellbook(player).prevSpell(inHand);
				}
				if (spell != null) {
					// send message
					MagicSpells.sendMessage(player, MagicSpells.strSpellChange, "%s", spell.getName());
					// show spell icon
					if (MagicSpells.spellIconSlot >= 0) {
						showIcon(player, MagicSpells.spellIconSlot, spell.getSpellIcon());
					}
					// use cool new text thingy
					boolean yay = false;
					if (yay) {
						final ItemStack fake = inHand.clone();
						ItemMeta meta = fake.getItemMeta();
						meta.setDisplayName("Spell: " + spell.getName());
						fake.setItemMeta(meta);
						MagicSpells.scheduleDelayedTask(new Runnable() {
							public void run() {
								MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, player.getInventory().getHeldItemSlot(), fake);
							}
						}, 0);
					}
				}
				
				// check for mana pots
				if (MagicSpells.enableManaBars && MagicSpells.manaPotions != null) {
					// find mana potion TODO: fix this, it's not good
					int restoreAmt = 0;
					for (Map.Entry<ItemStack, Integer> entry : MagicSpells.manaPotions.entrySet()) {
						if (inHand.isSimilar(entry.getKey())) {
							restoreAmt = entry.getValue();
							break;
						}
					}
					if (restoreAmt > 0) {
						// check cooldown
						if (MagicSpells.manaPotionCooldown > 0) {
							Long c = MagicSpells.manaPotionCooldowns.get(player);
							if (c != null && c > System.currentTimeMillis()) {
								MagicSpells.sendMessage(player, MagicSpells.strManaPotionOnCooldown.replace("%c", ""+(int)((c-System.currentTimeMillis())/1000)));
								return;
							}
						}
						// add mana
						boolean added = MagicSpells.mana.addMana(player, restoreAmt, ManaChangeReason.POTION);
						if (added) {
							// set cooldown
							if (MagicSpells.manaPotionCooldown > 0) {
								MagicSpells.manaPotionCooldowns.put(player, System.currentTimeMillis() + MagicSpells.manaPotionCooldown*1000);
							}
							// remove item
							if (inHand.getAmount() == 1) {
								inHand = null;
							} else {
								inHand.setAmount(inHand.getAmount()-1);
							}
							player.setItemInHand(inHand);
							player.updateInventory();
						}
					}
				}
				
			}
		}
	}
	
	@EventHandler
	public void onItemHeldChange(final PlayerItemHeldEvent event) {
		if (MagicSpells.spellIconSlot >= 0 && MagicSpells.spellIconSlot <= 8) {
			Player player = event.getPlayer();
			if (event.getNewSlot() == MagicSpells.spellIconSlot) {
				showIcon(player, MagicSpells.spellIconSlot, null);
			} else {
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				Spell spell = spellbook.getActiveSpell(player.getInventory().getItem(event.getNewSlot()));
				if (spell != null) {
					showIcon(player, MagicSpells.spellIconSlot, spell.getSpellIcon());
				} else {
					showIcon(player, MagicSpells.spellIconSlot, null);
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerAnimation(PlayerAnimationEvent event) {		
		if (!MagicSpells.castOnAnimate) return;
		
		Player p = event.getPlayer();
		if (noCast.contains(p)) {
			// clicking on special block -- don't cast
			noCast.remove(p);
			lastCast.put(p, System.currentTimeMillis());
		} else {
			// left click -- cast spell
			castSpell(p);
		}
	}
	
	private void castSpell(Player player) {
		ItemStack inHand = player.getItemInHand();
		if (!MagicSpells.allowCastWithFist && (inHand == null || inHand.getType() == Material.AIR)) return;
		
		Spell spell = MagicSpells.getSpellbook(player).getActiveSpell(inHand);
		if (spell != null && spell.canCastWithItem()) {
			// first check global cooldown
			if (MagicSpells.globalCooldown > 0 && !spell.ignoreGlobalCooldown) {
				Long lastCastTime = lastCast.get(player);
				if (lastCastTime != null && lastCastTime + MagicSpells.globalCooldown > System.currentTimeMillis()) {
					return;
				} else {
					lastCast.put(player, System.currentTimeMillis());
				}
			}
			// cast spell
			spell.cast(player);
		}		
	}
	
	private void showIcon(Player player, int slot, ItemStack icon) {
		if (icon == null) icon = player.getInventory().getItem(MagicSpells.spellIconSlot);
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, icon);
	}

}
