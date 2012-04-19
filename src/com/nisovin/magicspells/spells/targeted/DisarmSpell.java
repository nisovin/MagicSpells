package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class DisarmSpell extends TargetedEntitySpell {

	private List<Integer> disarmable;
	private int disarmDuration;
	private boolean dontDrop;
	private boolean preventTheft;
	private boolean obeyLos;	
	private String strInvalidItem;
	
	private HashMap<Item, Player> disarmedItems;
	
	public DisarmSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		disarmable = getConfigIntList("disarmable-items", null);
		
		disarmDuration = getConfigInt("disarm-duration", 100);
		dontDrop = getConfigBoolean("dont-drop", false);
		preventTheft = getConfigBoolean("prevent-theft", true);
		obeyLos = getConfigBoolean("obey-los", true);
		strInvalidItem = getConfigString("str-invalid-item", "Your target could not be disarmed.");
		
		if (dontDrop) preventTheft = false;
		if (preventTheft) {
			disarmedItems = new HashMap<Item, Player>();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target
			Player target = getTargetedPlayer(player, range, obeyLos);
			if (target == null) {
				// fail
				sendMessage(player, strNoTarget);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
			
			boolean disarmed = disarm(target);
			if (disarmed) {
				playGraphicalEffects(player, target);
				// send messages
				sendMessages(player, target);
				return PostCastAction.NO_MESSAGES;
			} else {
				// fail
				sendMessage(player, strInvalidItem);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean disarm(final Player target) {
		final ItemStack inHand = target.getItemInHand();
		if (disarmable == null || disarmable.contains(inHand.getTypeId())) {
			if (dontDrop) {
				// hide item
				target.setItemInHand(null);
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						// give the item back
						if (target.getItemInHand() == null || target.getItemInHand().getType() == Material.AIR) {
							// put back in hand
							target.setItemInHand(inHand);
						} else {
							// hand is full
							int slot = target.getInventory().firstEmpty();
							if (slot >= 0) {
								// put in first available slot
								target.getInventory().setItem(slot, inHand);
							} else {
								// no slots available, drop at feet
								Item item = target.getWorld().dropItem(target.getLocation(), inHand);
								item.setPickupDelay(0);
							}
						}
					}
				}, disarmDuration);
			} else {
				// drop item
				target.setItemInHand(null);
				Item item = target.getWorld().dropItemNaturally(target.getLocation(), inHand.clone());
				item.setPickupDelay(disarmDuration);
				if (preventTheft) {
					disarmedItems.put(item, target);
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			boolean disarmed =  disarm((Player)target);
			if (disarmed) {
				playGraphicalEffects(caster, target);
			}
			return disarmed;
		} else {
			return false;
		}
	}
	
	@EventHandler
	public void onItemPickup(PlayerPickupItemEvent event) {
		if (!preventTheft || event.isCancelled()) return;
		
		Item item = event.getItem();
		if (disarmedItems.containsKey(item)) {
			if (disarmedItems.get(item).equals(event.getPlayer())) {
				disarmedItems.remove(item);
			} else {
				event.setCancelled(true);
			}
		}
	}

}
