package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class GillsSpell extends BuffSpell {

	private boolean glassHeadEffect;
	
	private HashSet<String> fishes;
	private HashMap<Player,ItemStack> helmets;
	
	public GillsSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		glassHeadEffect = getConfigBoolean("glass-head-effect", true);
		
		fishes = new HashSet<String>();
		if (glassHeadEffect) {
			helmets = new HashMap<Player,ItemStack>();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (fishes.contains(player.getName())) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			fishes.add(player.getName());
			if (glassHeadEffect) {
				ItemStack helmet = player.getInventory().getHelmet();
				if (helmet != null && helmet.getType() != Material.AIR) {
					helmets.put(player, helmet);
				}
				player.getInventory().setHelmet(new ItemStack(Material.GLASS, 1));
			}
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		if (!event.isCancelled() && event.getEntity() instanceof Player && event.getCause() == DamageCause.DROWNING) {
			Player player = (Player)event.getEntity();
			if (fishes.contains(player.getName())) {
				if (isExpired(player)) {
					turnOff(player);
				} else {
					addUse(player);
					boolean ok = chargeUseCost(player);
					if (ok) {
						event.setCancelled(true);
						player.setRemainingAir(player.getMaximumAir());
					}
				}
			}
		}
	}

	@Override
	public void turnOff(Player player) {
		if (fishes.contains(player.getName())) {
			super.turnOff(player);
			fishes.remove(player.getName());
			if (glassHeadEffect) {
				if (helmets.containsKey(player)) {
					player.getInventory().setHelmet(helmets.get(player));
					helmets.remove(player);
				} else if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() == Material.GLASS) {
					player.getInventory().setHelmet(null);				
				}
			}
			sendMessage(player, strFade);
		}
	}
	
	@Override
	protected void turnOff() {
		for (String name : fishes) {
			if (glassHeadEffect) {
				Player player = Bukkit.getPlayerExact(name);
				if (player != null && player.isOnline()) {
					if (helmets.containsKey(player)) {
						player.getInventory().setHelmet(helmets.get(player));
					} else if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() == Material.GLASS) {
						player.getInventory().setHelmet(null);
					}
				}
			}
		}
		if (helmets != null) {
			helmets.clear();
		}
		fishes.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return fishes.contains(player.getName());
	}

}
