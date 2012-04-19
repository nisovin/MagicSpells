package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

class MagicPlayerListener implements Listener {
	
	private MagicSpells plugin;
	
	private HashSet<Player> noCast = new HashSet<Player>();
	private HashMap<Player,Long> lastCast = new HashMap<Player, Long>();
	
	public MagicPlayerListener(MagicSpells plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {		
		// set up spell book
		Spellbook spellbook = new Spellbook(event.getPlayer(), plugin);
		MagicSpells.spellbooks.put(event.getPlayer().getName(), spellbook);
		
		// set up mana bar
		if (MagicSpells.mana != null) {
			MagicSpells.mana.createManaBar(event.getPlayer());
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {		
		MagicSpells.spellbooks.remove(event.getPlayer().getName());
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {		
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
					m == Material.STONE_BUTTON) {
				noInteract = true;
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
			Player player = event.getPlayer();
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
					MagicSpells.sendMessage(player, MagicSpells.strSpellChange, "%s", spell.getName());
				}
				
				// check for mana pots
				if (MagicSpells.enableManaBars && MagicSpells.manaPotions != null) {
					ItemStack item = new ItemStack(inHand.getType(), 1, inHand.getDurability());
					if (MagicSpells.manaPotions.containsKey(item)) {
						// check cooldown
						if (MagicSpells.manaPotionCooldown > 0) {
							Long c = MagicSpells.manaPotionCooldowns.get(player);
							if (c != null && c > System.currentTimeMillis()) {
								MagicSpells.sendMessage(player, MagicSpells.strManaPotionOnCooldown.replace("%c", ""+(int)((c-System.currentTimeMillis())/1000)));
								return;
							}
						}
						// add mana
						int amt = MagicSpells.manaPotions.get(item);
						boolean added = MagicSpells.mana.addMana(player, amt);
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

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		if (MagicSpells.separatePlayerSpellsPerWorld) {
			MagicSpells.debug(2, "Player '" + event.getPlayer().getName() + "' changed from world '" + event.getFrom().getName() + "' to '" + event.getPlayer().getWorld().getName() + "', reloading spells");
			MagicSpells.getSpellbook(event.getPlayer()).reload();
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerAnimation(PlayerAnimationEvent event) {		
		if (event.isCancelled() || !MagicSpells.castOnAnimate) return;
		
		Player p = event.getPlayer();
		if (noCast.contains(p)) {
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
	
}
