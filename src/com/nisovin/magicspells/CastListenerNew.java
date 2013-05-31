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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.CastItem;

public class CastListenerNew implements Listener {

	MagicSpells plugin;
	
	private HashSet<Player> noCast = new HashSet<Player>();
	private HashMap<Player,Long> lastCast = new HashMap<Player, Long>();
	private HashSet<Player> cycling = new HashSet<Player>();

	public CastListenerNew(MagicSpells plugin) {
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
					m == Material.ENDER_CHEST ||
					m == Material.FURNACE || 
					m == Material.LEVER ||
					m == Material.STONE_BUTTON ||
					m == Material.WOOD_BUTTON ||
					m == Material.ENCHANTMENT_TABLE ||
					m == Material.ANVIL ||
					m == Material.BEACON) {
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
				castSpell(event.getPlayer(), Action.LEFT_CLICK_AIR);
			}
		} else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			// right click - cast
			boolean casted = castSpell(event.getPlayer(), Action.RIGHT_CLICK_AIR);
			if (casted) {
				event.setCancelled(true);
				return;
			}
			
			// check for mana pots
			ItemStack inHand = player.getItemInHand();
			if (MagicSpells.enableManaBars && MagicSpells.manaPotions != null && inHand != null && inHand.getTypeId() != 0) {
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
	
	@EventHandler
	public void onItemHeldChange(final PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		if (player.isSneaking()) {
			// get cast item
			int slot = event.getPreviousSlot();
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			ItemStack item = player.getInventory().getItem(slot);
			CastItem castItem = spellbook.getCastItemForCycling(item);
			if (castItem == null) return;
			// get spell
			Spell spell = null;
			if (event.getNewSlot() == event.getPreviousSlot() + 1 || (event.getNewSlot() == 0 && event.getPreviousSlot() == 8)) {
				// cycling next
				spell = spellbook.nextSpell(castItem);
			} else if (event.getNewSlot() == event.getPreviousSlot() - 1 || (event.getNewSlot() == 8 && event.getPreviousSlot() == 0)) {
				// cycling previous
				spell = spellbook.prevSpell(castItem);
			} else if (cycling.contains(player)) {
				// jumped to other slot
				spell = spellbook.getActiveSpell(castItem);
			}
			// player is cycling
			cycling.add(player);
			if (spell != null) {
				// show active spell
				ItemStack fakeItem = spell.getSpellIcon().clone();
				ItemMeta meta = fakeItem.getItemMeta();
				meta.setDisplayName(MagicSpells.textColor + "Spell: " + spell.getName());
				fakeItem.setItemMeta(meta);
				MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, fakeItem);
				player.getInventory().setHeldItemSlot(slot);
			} else {
				// cycled to no spell
				endCycling(player, slot);
				player.getInventory().setHeldItemSlot(slot);
			}
		} else if (cycling.contains(player)) {
			// no longer sneaking
			endCycling(player, event.getPreviousSlot());
		}
		
		if (MagicSpells.spellIconSlot >= 0 && MagicSpells.spellIconSlot <= 8) {
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
	
	@EventHandler
	public void onToggleSneak(PlayerToggleSneakEvent event) {
		if (!event.isSneaking() && cycling.contains(event.getPlayer())) {
			endCycling(event.getPlayer(), event.getPlayer().getInventory().getHeldItemSlot());
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
			castSpell(p, Action.LEFT_CLICK_AIR);
		}
	}
	
	private boolean castSpell(final Player player, Action action) {
		ItemStack inHand = player.getItemInHand();
		if (!MagicSpells.allowCastWithFist && (inHand == null || inHand.getType() == Material.AIR)) return false;
		
		final Spell spell = MagicSpells.getSpellbook(player).getActiveSpell(inHand);
		if (spell != null && spell.canCastWithItem() && (
				(action == Action.LEFT_CLICK_AIR && spell.canCastWithLeftClick()) ||
				(action == Action.RIGHT_CLICK_AIR && spell.canCastWithRightClick())
				)) {
			// first check global cooldown
			if (MagicSpells.globalCooldown > 0 && !spell.ignoreGlobalCooldown) {
				Long lastCastTime = lastCast.get(player);
				if (lastCastTime != null && lastCastTime + MagicSpells.globalCooldown > System.currentTimeMillis()) {
					return false;
				} else {
					lastCast.put(player, System.currentTimeMillis());
				}
			}
			// cast spell -- delay by 1 tick to escape the event
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					spell.cast(player);
				}
			}, 1);
			return true;
		}
		return false;
	}
	
	private void showIcon(Player player, int slot, ItemStack icon) {
		if (icon == null) icon = player.getInventory().getItem(MagicSpells.spellIconSlot);
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, icon);
	}
	
	private void endCycling(Player player, int slot) {
		cycling.remove(player);
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, player.getInventory().getItem(slot));
	}

}
